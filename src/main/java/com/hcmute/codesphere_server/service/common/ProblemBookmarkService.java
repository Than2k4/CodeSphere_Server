package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.ProblemBookmarkEntity;
import com.hcmute.codesphere_server.model.entity.ProblemEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.repository.common.ProblemBookmarkRepository;
import com.hcmute.codesphere_server.repository.common.ProblemRepository;
import com.hcmute.codesphere_server.repository.common.SubmissionRepository;
import com.hcmute.codesphere_server.repository.common.UserProblemBestRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemBookmarkService {

    private final ProblemBookmarkRepository problemBookmarkRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final UserProblemBestRepository userProblemBestRepository;
    private final SubmissionRepository submissionRepository;

    /**
     * Toggle bookmark (thêm hoặc xóa bookmark)
     * @return true nếu đã bookmark, false nếu đã bỏ bookmark
     */
    @Transactional
    public boolean toggleBookmark(Long userId, Long problemId) {
        // Kiểm tra user và problem tồn tại
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        
        ProblemEntity problem = problemRepository.findByIdAndStatusTrue(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        // Tìm bookmark (kể cả đã bị xóa)
        var anyBookmark = problemBookmarkRepository.findByUserIdAndProblemIdIgnoreDeleted(userId, problemId);
        
        if (anyBookmark.isPresent()) {
            ProblemBookmarkEntity bookmark = anyBookmark.get();
            if (bookmark.getIsDeleted()) {
                // Đã bị xóa → khôi phục
                bookmark.setIsDeleted(false);
                bookmark.setCreatedAt(Instant.now());
            } else {
                // Đang active → xóa
                bookmark.setIsDeleted(true);
            }
            problemBookmarkRepository.save(bookmark);
            return !bookmark.getIsDeleted();
        } else {
            // Chưa có → tạo mới
            ProblemBookmarkEntity newBookmark = ProblemBookmarkEntity.builder()
                    .user(user)
                    .problem(problem)
                    .createdAt(Instant.now())
                    .isDeleted(false)
                    .build();
            problemBookmarkRepository.save(newBookmark);
            return true;
        }
    }

    /**
     * Kiểm tra user đã bookmark problem chưa
     */
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long problemId) {
        return problemBookmarkRepository.existsByUserIdAndProblemId(userId, problemId);
    }

    /**
     * Lấy danh sách problem IDs mà user đã bookmark
     */
    @Transactional(readOnly = true)
    public Set<Long> getBookmarkedProblemIds(Long userId) {
        List<Long> problemIds = problemBookmarkRepository.findBookmarkedProblemIdsByUserId(userId);
        return problemIds.stream().collect(Collectors.toSet());
    }

    /**
     * Lấy danh sách problem IDs mà user đã giải đúng
     */
    @Transactional(readOnly = true)
    public Set<Long> getSolvedProblemIds(Long userId) {
        return userProblemBestRepository.findAllByUserIdAndAccepted(userId)
                .stream()
                .map(upb -> upb.getProblem().getId())
                .collect(Collectors.toSet());
    }

    /**
     * Lấy danh sách problem IDs mà user đã thử (có ít nhất 1 submission)
     */
    @Transactional(readOnly = true)
    public Set<Long> getAttemptedProblemIds(Long userId) {
        return submissionRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .filter(s -> !s.getIsDeleted())
                .map(s -> s.getProblem().getId())
                .distinct()
                .collect(Collectors.toSet());
    }
}

