package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.SubmissionEntity;
import com.hcmute.codesphere_server.model.entity.UserProblemBestEntity;
import com.hcmute.codesphere_server.model.payload.response.GlobalLeaderboardResponse;
import com.hcmute.codesphere_server.model.payload.response.LeaderboardResponse;
import com.hcmute.codesphere_server.repository.common.ProblemRepository;
import com.hcmute.codesphere_server.repository.common.UserProblemBestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserProblemBestRepository userProblemBestRepository;
    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getLeaderboard(Long problemId) {
        // Kiểm tra problem tồn tại
        problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        // Lấy tất cả best submissions từ bảng user_problem_best (đã được tính toán sẵn)
        // Đã được sắp xếp theo điểm DESC, thời gian ASC
        List<UserProblemBestEntity> bestSubmissions = userProblemBestRepository
                .findAllByProblemIdOrderByBestScoreDesc(problemId);

        if (bestSubmissions.isEmpty()) {
            return new ArrayList<>();
        }

        // Map sang LeaderboardResponse và gán rank
        List<LeaderboardResponse> leaderboard = new ArrayList<>();
        for (int i = 0; i < bestSubmissions.size(); i++) {
            UserProblemBestEntity best = bestSubmissions.get(i);
            SubmissionEntity bestSubmission = best.getBestSubmission();
            
            LeaderboardResponse response = LeaderboardResponse.builder()
                    .rank(i + 1)
                    .userId(best.getUser().getId())
                    .username(best.getUser().getUsername())
                    .bestScore(best.getBestScore())
                    .bestSubmissionId(bestSubmission.getId())
                    .totalSubmissions(best.getTotalSubmissions())
                    .bestSubmissionTime(bestSubmission.getCreatedAt())
                    .statusMsg(bestSubmission.getStatusMsg())
                    .statusRuntime(bestSubmission.getStatusRuntime())
                    .statusMemory(bestSubmission.getStatusMemory())
                    .isAccepted(bestSubmission.getIsAccepted())
                    .build();
            
            leaderboard.add(response);
        }

        return leaderboard;
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getMyRank(Long problemId, Long userId) {
        // Kiểm tra problem tồn tại
        problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        // Tìm best submission của user cho problem này
        return userProblemBestRepository.findByUserIdAndProblemId(userId, problemId)
                .map(best -> {
                    // Tìm rank của user trong leaderboard
                    List<LeaderboardResponse> leaderboard = getLeaderboard(problemId);
                    return leaderboard.stream()
                            .filter(entry -> entry.getUserId().equals(userId))
                            .findFirst()
                            .orElse(null);
                })
                .orElse(null); // null nếu user chưa nộp bài nào
    }

    /**
     * Lấy global leaderboard - xếp hạng tất cả users theo tổng số bài đã giải đúng
     */
    @Transactional(readOnly = true)
    public List<GlobalLeaderboardResponse> getGlobalLeaderboard() {
        List<Object[]> results = userProblemBestRepository.findAllUsersWithSolvedCount();
        
        List<GlobalLeaderboardResponse> leaderboard = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            // row[0] = userId (Long), row[1] = username (String), 
            // row[2] = totalSolved (BigInteger), row[3] = solvedEasy (BigInteger),
            // row[4] = solvedMedium (BigInteger), row[5] = solvedHard (BigInteger)
            
            Long userId = ((Number) row[0]).longValue();
            String username = (String) row[1];
            Integer totalSolved = ((Number) row[2]).intValue();
            Integer solvedEasy = ((Number) row[3]).intValue();
            Integer solvedMedium = ((Number) row[4]).intValue();
            Integer solvedHard = ((Number) row[5]).intValue();
            
            GlobalLeaderboardResponse response = GlobalLeaderboardResponse.builder()
                    .rank(i + 1)
                    .userId(userId)
                    .username(username)
                    .totalSolved(totalSolved)
                    .solvedEasy(solvedEasy)
                    .solvedMedium(solvedMedium)
                    .solvedHard(solvedHard)
                    .build();
            
            leaderboard.add(response);
        }
        
        return leaderboard;
    }
}

