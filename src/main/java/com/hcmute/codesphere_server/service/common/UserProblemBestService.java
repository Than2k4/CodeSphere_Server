package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.SubmissionEntity;
import com.hcmute.codesphere_server.model.entity.UserProblemBestEntity;
import com.hcmute.codesphere_server.repository.common.SubmissionRepository;
import com.hcmute.codesphere_server.repository.common.UserProblemBestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service để quản lý best submission của mỗi user cho mỗi problem
 * Tự động cập nhật khi có submission mới có điểm cao hơn
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProblemBestService {

    private final UserProblemBestRepository userProblemBestRepository;
    private final SubmissionRepository submissionRepository;

    /**
     * Cập nhật best submission khi có submission mới được judge xong
     * So sánh điểm và cập nhật nếu điểm mới cao hơn (hoặc bằng nhưng nộp sớm hơn)
     */
    @Transactional
    public void updateBestSubmission(SubmissionEntity submission) {
        Long userId = submission.getUser().getId();
        Long problemId = submission.getProblem().getId();
        Integer newScore = submission.getScore();

        // Tìm best submission hiện tại
        Optional<UserProblemBestEntity> existingBest = userProblemBestRepository
                .findByUserIdAndProblemId(userId, problemId);

        if (existingBest.isEmpty()) {
            // Chưa có best submission, tạo mới
            log.info("📊 Creating new best submission for user {} problem {} with score {}", 
                    userId, problemId, newScore);
            
            UserProblemBestEntity newBest = UserProblemBestEntity.builder()
                    .user(submission.getUser())
                    .problem(submission.getProblem())
                    .bestSubmission(submission)
                    .bestScore(newScore)
                    .totalSubmissions(1)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            
            userProblemBestRepository.save(newBest);
            return;
        }

        UserProblemBestEntity currentBest = existingBest.get();
        Integer currentBestScore = currentBest.getBestScore();

        // So sánh điểm
        boolean shouldUpdate = false;
        
        if (newScore > currentBestScore) {
            // Điểm mới cao hơn -> cập nhật
            shouldUpdate = true;
            log.info("📊 Updating best submission for user {} problem {}: {} -> {}", 
                    userId, problemId, currentBestScore, newScore);
        } else if (newScore.equals(currentBestScore)) {
            // Điểm bằng nhau, kiểm tra thời gian nộp (nộp sớm hơn thì tốt hơn)
            if (submission.getCreatedAt().isBefore(currentBest.getBestSubmission().getCreatedAt())) {
                shouldUpdate = true;
                log.info("📊 Updating best submission for user {} problem {}: same score but earlier submission", 
                        userId, problemId);
            }
        }

        if (shouldUpdate) {
            currentBest.setBestSubmission(submission);
            currentBest.setBestScore(newScore);
            currentBest.setUpdatedAt(Instant.now());
        }

        // Luôn cập nhật totalSubmissions
        Long totalSubmissions = submissionRepository.countSubmissionsByUserIdAndProblemId(userId, problemId);
        currentBest.setTotalSubmissions(totalSubmissions.intValue());
        
        userProblemBestRepository.save(currentBest);
    }
}

