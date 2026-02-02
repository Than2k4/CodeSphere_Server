package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.ProblemEntity;
import com.hcmute.codesphere_server.model.payload.response.MyStatsResponse;
import com.hcmute.codesphere_server.model.payload.response.ProblemStatsResponse;
import com.hcmute.codesphere_server.repository.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SubmissionRepository submissionRepository;
    private final UserProblemBestRepository userProblemBestRepository;
    private final ProblemRepository problemRepository;

    /**
     * Lấy thống kê cá nhân của user
     * Bao gồm: số bài đúng, số bài theo độ khó, tiến độ
     */
    @Transactional(readOnly = true)
    public MyStatsResponse getMyStats(Long userId) {
        // Đếm số bài đã giải đúng theo độ khó (sử dụng query tối ưu)
        Long solvedEasy = userProblemBestRepository.countSolvedByUserIdAndLevel(userId, "EASY");
        Long solvedMedium = userProblemBestRepository.countSolvedByUserIdAndLevel(userId, "MEDIUM");
        Long solvedHard = userProblemBestRepository.countSolvedByUserIdAndLevel(userId, "HARD");
        
        // Tổng số bài đã giải đúng
        Long totalSolved = userProblemBestRepository.countSolvedByUserId(userId);

        // Đếm tổng số submissions và accepted submissions
        Long totalSubmissions = submissionRepository.countSubmissionsByUserId(userId);
        Long acceptedSubmissions = submissionRepository.countAcceptedSubmissionsByUserId(userId);

        // Tính acceptance rate
        double acceptanceRate = totalSubmissions > 0 
                ? (double) acceptedSubmissions / totalSubmissions * 100.0 
                : 0.0;

        // Đếm số bài đã thử (có ít nhất 1 submission)
        Long totalAttempted = submissionRepository.countDistinctProblemsAttemptedByUserId(userId);

        // Tính progress rate (số bài đã giải / số bài đã thử)
        double progressRate = totalAttempted > 0 
                ? (double) totalSolved / totalAttempted * 100.0 
                : 0.0;

        // Đếm số bài đã thử theo độ khó
        Long attemptedEasy = submissionRepository.countDistinctProblemsAttemptedByUserIdAndLevel(userId, "EASY");
        Long attemptedMedium = submissionRepository.countDistinctProblemsAttemptedByUserIdAndLevel(userId, "MEDIUM");
        Long attemptedHard = submissionRepository.countDistinctProblemsAttemptedByUserIdAndLevel(userId, "HARD");

        return MyStatsResponse.builder()
                .totalSolved(totalSolved.intValue())
                .solvedEasy(solvedEasy.intValue())
                .solvedMedium(solvedMedium.intValue())
                .solvedHard(solvedHard.intValue())
                .totalAttempted(totalAttempted.intValue())
                .totalSubmissions(totalSubmissions.intValue())
                .acceptedSubmissions(acceptedSubmissions.intValue())
                .acceptanceRate(Math.round(acceptanceRate * 100.0) / 100.0) // Làm tròn 2 chữ số
                .progressRate(Math.round(progressRate * 100.0) / 100.0) // Làm tròn 2 chữ số
                .attemptedEasy(attemptedEasy.intValue())
                .attemptedMedium(attemptedMedium.intValue())
                .attemptedHard(attemptedHard.intValue())
                .build();
    }

    /**
     * Lấy thống kê của một bài tập cụ thể
     * Bao gồm: tổng số submission, số submission đúng, tỷ lệ chấp nhận, số user đã thử/giải
     */
    @Transactional(readOnly = true)
    public ProblemStatsResponse getProblemStats(Long problemId) {
        if (problemId == null) {
            throw new RuntimeException("Problem ID không được để trống");
        }
        
        // Kiểm tra problem tồn tại
        ProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        // Đếm tổng số submissions và accepted submissions (sử dụng query tối ưu)
        // COUNT queries luôn trả về non-null Long (0 nếu không có kết quả)
        Long totalSubmissionsCount = submissionRepository.countSubmissionsByProblemId(problemId);
        Long acceptedSubmissionsCount = submissionRepository.countAcceptedSubmissionsByProblemId(problemId);
        Long totalSubmissions = totalSubmissionsCount != null ? totalSubmissionsCount : 0L;
        Long acceptedSubmissions = acceptedSubmissionsCount != null ? acceptedSubmissionsCount : 0L;

        // Tính acceptance rate
        double acceptanceRate = totalSubmissions > 0 
                ? (double) acceptedSubmissions / totalSubmissions * 100.0 
                : 0.0;

        // Đếm số user đã thử và đã giải đúng (sử dụng query tối ưu)
        Long totalUsersAttempted = submissionRepository.countDistinctUsersAttemptedByProblemId(problemId);
        Long totalUsersSolved = submissionRepository.countDistinctUsersSolvedByProblemId(problemId);

        // Tính solve rate (tỷ lệ user giải đúng)
        double solveRate = totalUsersAttempted > 0 
                ? (double) totalUsersSolved / totalUsersAttempted * 100.0 
                : 0.0;

        return ProblemStatsResponse.builder()
                .problemId(problem.getId())
                .problemTitle(problem.getTitle())
                .problemCode(problem.getCode())
                .level(problem.getLevel())
                .totalSubmissions(totalSubmissions)
                .acceptedSubmissions(acceptedSubmissions)
                .acceptanceRate(Math.round(acceptanceRate * 100.0) / 100.0) // Làm tròn 2 chữ số
                .totalUsersAttempted(totalUsersAttempted)
                .totalUsersSolved(totalUsersSolved)
                .solveRate(Math.round(solveRate * 100.0) / 100.0) // Làm tròn 2 chữ số
                .build();
    }
}

