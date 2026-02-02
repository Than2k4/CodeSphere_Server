package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.entity.embedded.SubmissionTestcaseKey;
import com.hcmute.codesphere_server.repository.common.*;
import com.hcmute.codesphere_server.service.common.DockerExecutionHelper.ExecutionResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Judge Service sử dụng Docker để chạy code
 * Thay thế Judge0 API bằng Docker containers để:
 * - Không bị giới hạn rate limit
 * - Nhanh hơn (local execution)
 * - Kiểm soát được resource (memory, CPU)
 * - Ổn định hơn (không phụ thuộc service ngoài)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionTestcaseRepository submissionTestcaseRepository;
    private final TestCaseRepository testCaseRepository;
    private final DockerExecutionHelper dockerExecutionHelper;
    private final UserProblemBestService userProblemBestService;
    private final EntityManager entityManager;
    private final ContestSubmissionRepository contestSubmissionRepository;
    private final ContestProblemRepository contestProblemRepository;

    // Mapping language code - giữ nguyên để tương thích
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "java", "python", "cpp", "c", "javascript", "node", "php"
    );

    /**
     * Chạy code và judge submission (async để không block request)
     */
    @Async
    @Transactional
    public CompletableFuture<Void> judgeSubmission(Long submissionId) {
        log.info("🚀 Starting judge for submission {}", submissionId);
        try {
            SubmissionEntity submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found"));

            // Lấy test cases để chấm điểm: chỉ lấy testcases có isSample = false AND isHidden = false
            List<TestCaseEntity> testCases = testCaseRepository.findAllTestCasesByProblemId(
                    submission.getProblem().getId())
                    .stream()
                    .filter(tc -> (tc.getIsSample() == null || !tc.getIsSample()) && 
                                  (tc.getIsHidden() == null || !tc.getIsHidden()))
                    .collect(Collectors.toList());

            log.info("📋 Found {} test cases for submission {} (excluding sample and hidden testcases)", testCases.size(), submissionId);

            if (testCases.isEmpty()) {
                log.warn("⚠️ No test cases found for submission {}", submissionId);
                updateSubmissionStatus(submission, false, 0, testCases.size(),
                        "No test cases found", "ERROR");
                return CompletableFuture.completedFuture(null);
            }

            // Lấy language code
            String languageCode = submission.getLanguage().getCode().toLowerCase();

            log.info("🌐 Language: {}", languageCode);

            if (!SUPPORTED_LANGUAGES.contains(languageCode)) {
                log.error("❌ Language not supported: {} for submission {}", languageCode, submissionId);
                updateSubmissionStatus(submission, false, 0, testCases.size(),
                        "Language not supported: " + languageCode, "ERROR");
                return CompletableFuture.completedFuture(null);
            }

            // Kiểm tra compile error trước khi chạy test cases (chỉ với compiled languages)
            String compileError = null;
            String fullCompileError = null;
            if (languageCode.equals("cpp") || languageCode.equals("c") || languageCode.equals("java")) {
                log.info("🔨 Checking compilation for submission {}", submissionId);
                ExecutionResult compileResult = dockerExecutionHelper.compileCode(
                        submission.getCodeContent(),
                        languageCode,
                        submission.getProblem().getTimeLimitMs(),
                        256 // Default memory limit: 256MB
                );
                
                if (!compileResult.isSuccess()) {
                    // Có lỗi compile
                    fullCompileError = compileResult.getStderr() != null && !compileResult.getStderr().isEmpty()
                            ? compileResult.getStderr()
                            : (compileResult.getErrorMessage() != null ? compileResult.getErrorMessage() : "Compilation error");
                    
                    // Làm sạch error message để hiển thị (compileError - rút gọn)
                    compileError = cleanErrorMessage(fullCompileError);
                    
                    // Giới hạn độ dài compileError để tránh lỗi database (5000 ký tự)
                    // fullCompileError sẽ lưu đầy đủ
                    if (compileError != null && compileError.length() > 5000) {
                        compileError = compileError.substring(0, 5000) + "... (truncated)";
                    }
                    
                    // Giới hạn fullCompileError (65535 ký tự cho TEXT, hoặc có thể lớn hơn với LONGTEXT)
                    if (fullCompileError != null && fullCompileError.length() > 65535) {
                        fullCompileError = fullCompileError.substring(0, 65535) + "... (truncated)";
                    }
                    
                    log.error("❌ Compilation error for submission {}: {}", submissionId, compileError);
                    
                    // Lưu compile error vào submission
                    submission.setCompileError(compileError);
                    submission.setFullCompileError(fullCompileError);
                    updateSubmissionStatus(submission, false, 0, testCases.size(),
                            "Compilation Error", "COMPILE_ERROR");
                    
                    return CompletableFuture.completedFuture(null);
                }
                log.info("✅ Code compiles successfully for submission {}", submissionId);
            }

            // Chạy code với từng test case
            int totalCorrect = 0;
            int totalTestcases = testCases.size();
            List<SubmissionTestcaseEntity> submissionTestcases = new ArrayList<>();

            for (TestCaseEntity testCase : testCases) {
                log.info("▶️ Running test case {}: input='{}', expected='{}'", 
                    testCase.getId(), testCase.getInput(), testCase.getExpectedOutput());
                
                ExecutionResult result = dockerExecutionHelper.runCode(
                        submission.getCodeContent(),
                        languageCode,
                        testCase.getInput(),
                        submission.getProblem().getTimeLimitMs(),
                        256 // Default memory limit: 256MB
                );

                log.info("📊 Test case {} result: success={}, stdout='{}', stderr='{}', runtime={}ms", 
                    testCase.getId(), 
                    result.isSuccess(),
                    result.getStdout(),
                    result.getStderr(),
                    result.getRuntimeMs());

                // Kiểm tra kết quả: success và output khớp
                boolean passed = result.isSuccess() && 
                        result.getStdout() != null &&
                        normalizeOutput(result.getStdout()).equals(normalizeOutput(testCase.getExpectedOutput()));

                if (passed) {
                    totalCorrect++;
                    log.info("✅ Test case {} PASSED", testCase.getId());
                } else {
                    log.warn("❌ Test case {} FAILED - Expected: '{}', Got: '{}'", 
                        testCase.getId(), 
                        testCase.getExpectedOutput(), 
                        result.getStdout());
                }

                // Tạo SubmissionTestcaseEntity
                SubmissionTestcaseKey key = new SubmissionTestcaseKey(
                        submission.getId(),
                        testCase.getId()
                );

                SubmissionTestcaseEntity submissionTestcase = SubmissionTestcaseEntity.builder()
                        .id(key)
                        .submission(submission)
                        .testCase(testCase)
                        .status(passed ? "PASSED" : "FAILED")
                        .runtimeMs((int) result.getRuntimeMs())
                        .memoryKb(result.getMemoryKb() > 0 ? (int) result.getMemoryKb() : null)
                        .stdout(truncateIfNeeded(result.getStdout(), 2000)) // Giới hạn 2000 ký tự để tránh lỗi database
                        .stderr(truncateIfNeeded(
                            result.getStderr() != null ? result.getStderr() : 
                            (result.getErrorMessage() != null ? result.getErrorMessage() : null),
                            2000 // Giới hạn 2000 ký tự để tránh lỗi database
                        ))
                        .isDeleted(false)
                        .build();

                submissionTestcases.add(submissionTestcase);
            }

            // Lưu tất cả submission testcases
            submissionTestcaseRepository.saveAll(submissionTestcases);

            // Tính score (phần trăm)
            int score = totalTestcases > 0 ? (totalCorrect * 100 / totalTestcases) : 0;
            boolean isAccepted = totalCorrect == totalTestcases;

            // Cập nhật submission
            updateSubmissionStatus(submission, isAccepted, score, totalTestcases,
                    isAccepted ? "Accepted" : String.format("Wrong Answer (%d/%d)", totalCorrect, totalTestcases),
                    isAccepted ? "ACCEPTED" : "WRONG_ANSWER");

            // Cập nhật ContestSubmissionEntity.score nếu submission này thuộc contest
            updateContestSubmissionScore(submissionId, totalCorrect, totalTestcases);

            log.info("✅ Judged submission {}: {}/{} test cases passed, score={}%, isAccepted={}", 
                submissionId, totalCorrect, totalTestcases, score, isAccepted);

        } catch (Exception e) {
            log.error("Error judging submission {}", submissionId, e);
            try {
                SubmissionEntity submission = submissionRepository.findById(submissionId).orElse(null);
                if (submission != null) {
                    updateSubmissionStatus(submission, false, 0, 0,
                            "Judge error: " + e.getMessage(), "ERROR");
                }
            } catch (Exception ex) {
                log.error("Error updating submission status", ex);
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Validate code syntax (chỉ kiểm tra compile, không chạy)
     */
    /**
     * Validate code syntax/compilation - Real-time checking trong editor
     * - Compiled languages (C++, C, Java): Chỉ compile, không chạy
     * - Interpreted languages (Python, JavaScript): Chạy với empty input để check syntax
     */
    public ValidationResult validateCode(String sourceCode, String languageCode) {
        try {
            String lang = languageCode.toLowerCase();
            if (!SUPPORTED_LANGUAGES.contains(lang)) {
                return ValidationResult.error("Language not supported: " + languageCode);
            }

            // Với compiled languages, chỉ compile (không chạy) để check compile error nhanh hơn
            // Với interpreted languages, chạy với empty input để check syntax
            ExecutionResult result;
            
            if (lang.equals("cpp") || lang.equals("c") || lang.equals("java")) {
                // Chỉ compile, không chạy
                result = dockerExecutionHelper.compileCode(
                        sourceCode,
                        lang,
                        5000, // 5 seconds timeout for compilation
                        256 // 256MB memory limit
                );
            } else {
                // Interpreted languages: chạy với empty input để check syntax
                result = dockerExecutionHelper.runCode(
                        sourceCode,
                        lang,
                        "", // Empty input
                        5000, // 5 seconds timeout for validation
                        256 // 256MB memory limit
                );
            }

            if (result.isSuccess()) {
                String msg = (lang.equals("cpp") || lang.equals("c") || lang.equals("java")) 
                        ? "Code compiles successfully" 
                        : "Code syntax is valid";
                return ValidationResult.success(msg);
            } else {
                // Lấy error message từ stderr hoặc errorMessage
                String errorMsg = result.getStderr() != null && !result.getStderr().isEmpty() 
                        ? result.getStderr() 
                        : (result.getErrorMessage() != null ? result.getErrorMessage() : "Compilation/Syntax error");
                
                // Làm sạch error message (loại bỏ path không cần thiết)
                errorMsg = cleanErrorMessage(errorMsg);
                
                return ValidationResult.error(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error validating code", e);
            return ValidationResult.error("Validation error: " + e.getMessage());
        }
    }

    /**
     * Làm sạch error message - loại bỏ path không cần thiết
     */
    private String cleanErrorMessage(String errorMsg) {
        if (errorMsg == null) return "";
        // Loại bỏ path như /src/main.cpp, /src/Main.java, etc.
        return errorMsg.replaceAll("/src/[^\\s:]+", "file")
                      .replaceAll("\\s+", " ")
                      .trim();
    }

    /**
     * Truncate string nếu quá dài để tránh lỗi database
     * Giới hạn 2000 ký tự (an toàn cho hầu hết các database column, kể cả VARCHAR)
     */
    private String truncateIfNeeded(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        // Truncate và thêm "..." để biết đã bị cắt
        // Log warning nếu bị truncate để debug
        log.warn("Truncating text from {} to {} characters (stderr/stdout too long)", 
                text.length(), maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Cập nhật trạng thái submission
     * Sử dụng REQUIRES_NEW để tạo transaction mới và commit ngay, đảm bảo frontend có thể poll được ngay lập tức
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSubmissionStatus(SubmissionEntity submission, boolean isAccepted,
                                       int score, int totalTestcases, String statusMsg, String state) {
        // Reload submission để đảm bảo có entity mới nhất
        SubmissionEntity freshSubmission = submissionRepository.findById(submission.getId())
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submission.getId()));
        
        freshSubmission.setIsAccepted(isAccepted);
        freshSubmission.setScore(score);
        freshSubmission.setTotalCorrect(isAccepted ? totalTestcases : score * totalTestcases / 100);
        freshSubmission.setTotalTestcases(totalTestcases);
        freshSubmission.setStatusMsg(statusMsg);
        freshSubmission.setState(state);
        // Set statusCode: 0 = PENDING, 1 = ACCEPTED, 2 = WRONG_ANSWER, 3 = ERROR/COMPILE_ERROR
        if ("ACCEPTED".equals(state)) {
            freshSubmission.setStatusCode(1);
        } else if ("WRONG_ANSWER".equals(state)) {
            freshSubmission.setStatusCode(2);
        } else if ("ERROR".equals(state) || "COMPILE_ERROR".equals(state)) {
            freshSubmission.setStatusCode(3);
        } else {
            freshSubmission.setStatusCode(0); // PENDING
        }
        freshSubmission.setUpdatedAt(Instant.now());

        // Tính runtime và memory trung bình từ submission testcases
        List<SubmissionTestcaseEntity> testcases = submissionTestcaseRepository.findBySubmissionId(freshSubmission.getId());
        if (testcases != null && !testcases.isEmpty()) {
            // Tính trung bình runtime (ms)
            double avgRuntime = testcases.stream()
                    .filter(tc -> tc.getRuntimeMs() != null && tc.getRuntimeMs() > 0)
                    .mapToInt(SubmissionTestcaseEntity::getRuntimeMs)
                    .average()
                    .orElse(0.0);
            
            // Tính trung bình memory (KB)
            double avgMemoryKb = testcases.stream()
                    .filter(tc -> tc.getMemoryKb() != null && tc.getMemoryKb() > 0)
                    .mapToInt(SubmissionTestcaseEntity::getMemoryKb)
                    .average()
                    .orElse(0.0);
            
            // Format runtime: "X ms" hoặc "0 ms" nếu không có data
            if (avgRuntime > 0) {
                freshSubmission.setStatusRuntime(String.format("%.0f ms", avgRuntime));
            } else {
                freshSubmission.setStatusRuntime("0 ms");
            }
            
            // Format memory: "X KB" hoặc "X MB" nếu >= 1024 KB, hoặc "0 KB" nếu không có data
            if (avgMemoryKb > 0) {
                if (avgMemoryKb >= 1024) {
                    double memoryMb = avgMemoryKb / 1024.0;
                    freshSubmission.setStatusMemory(String.format("%.2f MB", memoryMb));
                } else {
                    freshSubmission.setStatusMemory(String.format("%.0f KB", avgMemoryKb));
                }
            } else {
                freshSubmission.setStatusMemory("0 KB");
            }
        } else {
            // Nếu không có testcases, giữ giá trị mặc định
            freshSubmission.setStatusRuntime("0 ms");
            freshSubmission.setStatusMemory("0 KB");
        }

        submissionRepository.save(freshSubmission);
        // Flush và commit ngay để frontend có thể poll được
        entityManager.flush();
        
        log.info("💾 Saved submission {} status: state={}, isAccepted={}, score={}%", 
                freshSubmission.getId(), state, isAccepted, score);
        
        // Cập nhật best submission của user cho problem này (trong transaction riêng để không block)
        try {
            userProblemBestService.updateBestSubmission(freshSubmission);
        } catch (Exception e) {
            log.error("❌ Error updating best submission for submission {}: {}", 
                    freshSubmission.getId(), e.getMessage(), e);
            // Không throw exception để không ảnh hưởng đến quá trình judge
        }
    }

    /**
     * Cập nhật score cho ContestSubmissionEntity sau khi judge xong
     * Tính lại score dựa trên problem points (không phải phần trăm)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateContestSubmissionScore(Long submissionId, int totalCorrect, int totalTestcases) {
        try {
            // Tìm tất cả ContestSubmissionEntity có submissionId này
            List<ContestSubmissionEntity> contestSubmissions = contestSubmissionRepository.findBySubmissionId(submissionId);
            
            if (contestSubmissions.isEmpty()) {
                return; // Không thuộc contest nào
            }

            for (ContestSubmissionEntity contestSubmission : contestSubmissions) {
                // Lấy problem points từ contest problem
                Long contestId = contestSubmission.getContest().getId();
                Long problemId = contestSubmission.getSubmission().getProblem().getId();
                
                Optional<ContestProblemEntity> contestProblemOpt = contestProblemRepository
                        .findByContestIdAndProblemId(contestId, problemId);
                
                if (contestProblemOpt.isPresent()) {
                    ContestProblemEntity contestProblem = contestProblemOpt.get();
                    Integer problemPoints = contestProblem.getPoints() != null ? contestProblem.getPoints() : 100;
                    
                    // Tính lại score: (số testcase đúng / tổng số testcase) * điểm của bài
                    Integer newScore = 0;
                    if (totalTestcases > 0 && totalCorrect >= 0) {
                        double scoreDouble = ((double) totalCorrect / (double) totalTestcases) * problemPoints;
                        newScore = (int) Math.round(scoreDouble);
                    }
                    
                    // Cập nhật score
                    contestSubmission.setScore(newScore);
                    contestSubmissionRepository.save(contestSubmission);
                    // Flush để đảm bảo score được lưu ngay lập tức
                    entityManager.flush();
                    // Refresh entity để đảm bảo có giá trị mới nhất
                    entityManager.refresh(contestSubmission);
                } else {
                    log.warn("ContestProblemEntity not found: contestId={}, problemId={}", contestId, problemId);
                }
            }
        } catch (Exception e) {
            log.error("Error updating contest submission score for submission {}: {}", 
                    submissionId, e.getMessage(), e);
            // Không throw exception để không ảnh hưởng đến quá trình judge
        }
    }


    /**
     * Chuẩn hóa output để so sánh (loại bỏ whitespace, newline)
     */
    private String normalizeOutput(String output) {
        if (output == null) return "";
        return output.trim().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
    }

    // ============= DTO Classes =============

    @Data
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private List<String> errors;

        public static ValidationResult success(String message) {
            ValidationResult result = new ValidationResult();
            result.valid = true;
            result.message = message;
            return result;
        }

        public static ValidationResult error(String error) {
            ValidationResult result = new ValidationResult();
            result.valid = false;
            result.message = error;
            result.errors = List.of(error);
            return result;
        }
    }
}

