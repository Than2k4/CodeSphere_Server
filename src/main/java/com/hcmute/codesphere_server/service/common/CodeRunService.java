package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.payload.request.RunCodeRequest;
import com.hcmute.codesphere_server.model.payload.response.RunCodeResponse;
import com.hcmute.codesphere_server.repository.common.*;
import com.hcmute.codesphere_server.service.common.DockerExecutionHelper.ExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service để chạy code với sample testcases (không tạo submission)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeRunService {

    private final ProblemRepository problemRepository;
    private final LanguageRepository languageRepository;
    private final TestCaseRepository testCaseRepository;
    private final DockerExecutionHelper dockerExecutionHelper;

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "java", "python", "cpp", "c", "javascript", "node", "php"
    );

    @Transactional(readOnly = true)
    public RunCodeResponse runCode(RunCodeRequest request) {
        // Kiểm tra problem tồn tại và active
        ProblemEntity problem = problemRepository.findByIdAndStatusTrue(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        // Kiểm tra language tồn tại
        LanguageEntity language = languageRepository.findById(request.getLanguageId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngôn ngữ"));

        // Kiểm tra language có được hỗ trợ bởi problem không
        boolean isLanguageSupported = problem.getLanguages().stream()
                .anyMatch(lang -> lang.getId().equals(request.getLanguageId()));
        
        if (!isLanguageSupported) {
            throw new RuntimeException("Ngôn ngữ này không được hỗ trợ cho bài tập này");
        }

        String languageCode = language.getCode().toLowerCase();
        
        if (!SUPPORTED_LANGUAGES.contains(languageCode)) {
            throw new RuntimeException("Ngôn ngữ không được hỗ trợ: " + languageCode);
        }

        List<RunCodeResponse.TestResult> testResults = new ArrayList<>();
        int totalPassed = 0;
        int totalTests = 0;

        // Lấy custom testcases từ request
        List<RunCodeRequest.CustomTestCase> customTestCases = request.getCustomTestCases();
        boolean hasCustomTestCases = customTestCases != null && !customTestCases.isEmpty();
        
        // Lấy sample testcases (isSample = true) - để user test code, không dùng để chấm điểm
        List<TestCaseEntity> sampleTestCases = testCaseRepository.findAllTestCasesByProblemId(request.getProblemId())
                .stream()
                .filter(tc -> tc.getIsSample() != null && tc.getIsSample())
                .toList();

        // Nếu không có sample testcases và không có custom testcases, throw error
        if (sampleTestCases.isEmpty() && !hasCustomTestCases) {
            throw new RuntimeException("Không tìm thấy sample test cases");
        }

        log.info("Found {} sample test cases", sampleTestCases.size());
        if (hasCustomTestCases) {
            log.info("Also running with {} custom test cases", customTestCases.size());
        }

        // Kiểm tra compile error trước (chỉ với compiled languages)
        if (languageCode.equals("cpp") || languageCode.equals("c") || languageCode.equals("java")) {
            ExecutionResult compileResult = dockerExecutionHelper.compileCode(
                    request.getCodeContent(),
                    languageCode,
                    problem.getTimeLimitMs(),
                    256 // Default memory limit: 256MB
            );
            
            if (!compileResult.isSuccess()) {
                String compileError = compileResult.getStderr() != null && !compileResult.getStderr().isEmpty()
                        ? compileResult.getStderr()
                        : (compileResult.getErrorMessage() != null ? compileResult.getErrorMessage() : "Compilation error");
                
                return RunCodeResponse.builder()
                        .success(false)
                        .message("Compilation Error")
                        .testResults(List.of(RunCodeResponse.TestResult.builder()
                                .testCaseId(null)
                                .input("")
                                .expectedOutput("")
                                .actualOutput("")
                                .isPassed(false)
                                .runtime("0 ms")
                                .memory("0 KB")
                                .errorMessage(compileError)
                                .build()))
                        .totalPassed(0)
                        .totalTests(0)
                        .build();
            }
        }

        // Chạy code với từng sample testcase
        for (TestCaseEntity testCase : sampleTestCases) {
            ExecutionResult result = dockerExecutionHelper.runCode(
                    request.getCodeContent(),
                    languageCode,
                    testCase.getInput(),
                    problem.getTimeLimitMs(),
                    256 // Default memory limit: 256MB
            );

            String actualOutput = result.getStdout() != null ? result.getStdout() : "";
            boolean passed = result.isSuccess() && 
                    normalizeOutput(actualOutput).equals(normalizeOutput(testCase.getExpectedOutput()));

            if (passed) totalPassed++;

            testResults.add(RunCodeResponse.TestResult.builder()
                    .testCaseId(testCase.getId())
                    .input(testCase.getInput())
                    .expectedOutput(testCase.getExpectedOutput())
                    .actualOutput(actualOutput)
                    .isPassed(passed)
                    .runtime(result.getRuntimeMs() + " ms")
                    .memory(result.getMemoryKb() + " KB")
                    .errorMessage(result.getStderr())
                    .build());
            
            totalTests++;
        }

        // Chạy code với từng custom testcase
        if (hasCustomTestCases) {
            for (RunCodeRequest.CustomTestCase customTestCase : customTestCases) {
                if (customTestCase.getInput() == null || customTestCase.getInput().trim().isEmpty()) {
                    continue; // Skip empty testcases
                }
                
                ExecutionResult result = dockerExecutionHelper.runCode(
                        request.getCodeContent(),
                        languageCode,
                        customTestCase.getInput(),
                        problem.getTimeLimitMs(),
                        256 // Default memory limit: 256MB
                );

                String actualOutput = result.getStdout() != null ? result.getStdout() : "";
                String expectedOutput = customTestCase.getExpectedOutput() != null 
                        ? customTestCase.getExpectedOutput() 
                        : "";
                
                boolean passed = result.isSuccess() && 
                        (expectedOutput.isEmpty() || normalizeOutput(actualOutput).equals(normalizeOutput(expectedOutput)));

                if (passed) totalPassed++;

                testResults.add(RunCodeResponse.TestResult.builder()
                        .testCaseId(null)
                        .input(customTestCase.getInput())
                        .expectedOutput(expectedOutput)
                        .actualOutput(actualOutput)
                        .isPassed(passed)
                        .runtime(result.getRuntimeMs() + " ms")
                        .memory(result.getMemoryKb() + " KB")
                        .errorMessage(result.getStderr())
                        .build());
                
                totalTests++;
            }
        }

        boolean allPassed = totalPassed == totalTests;
        
        return RunCodeResponse.builder()
                .success(allPassed)
                .message(allPassed 
                        ? String.format("Tất cả %d test case đã pass", totalPassed)
                        : String.format("%d/%d test case đã pass", totalPassed, totalTests))
                .testResults(testResults)
                .totalPassed(totalPassed)
                .totalTests(totalTests)
                .build();
    }

    private String normalizeOutput(String output) {
        if (output == null) return "";
        return output.trim().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
    }
}

