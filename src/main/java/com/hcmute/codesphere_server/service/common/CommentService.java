package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.CommentEntity;
import com.hcmute.codesphere_server.model.entity.CommentLikeEntity;
import com.hcmute.codesphere_server.model.entity.PostEntity;
import com.hcmute.codesphere_server.model.entity.ProblemEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.payload.request.CreateCommentRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateCommentRequest;
import com.hcmute.codesphere_server.model.payload.request.VoteRequest;
import com.hcmute.codesphere_server.model.payload.response.CommentResponse;
import com.hcmute.codesphere_server.model.payload.response.VoteResponse;
import com.hcmute.codesphere_server.repository.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest request, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        UserEntity author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        CommentEntity comment = CommentEntity.builder()
                .author(author)
                .post(post)
                .content(request.getContent())
                .isAccepted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Nếu là reply
        if (request.getParentCommentId() != null) {
            CommentEntity parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy comment cha"));
            comment.setParent(parent);
        }

        comment = commentRepository.save(comment);

        // Gửi notification
        try {
            if (comment.getParent() != null) {
                // Reply comment - notify comment author
                if (!comment.getParent().getAuthor().getId().equals(userId)) {
                    notificationService.notifyCommentReply(
                            comment.getParent().getAuthor().getId(),
                            userId,
                            author.getUsername(),
                            postId,
                            comment.getParent().getId()
                    );
                }
            } else {
                // Top-level comment - notify post author
                if (!post.getAuthor().getId().equals(userId)) {
                    notificationService.notifyPostComment(
                            post.getAuthor().getId(),
                            userId,
                            author.getUsername(),
                            postId
                    );
                }
            }
        } catch (Exception e) {
            // Log error nhưng không throw
        }

        return mapToCommentResponse(comment, userId);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByPost(Long postId, Long userId, Pageable pageable) {
        // Kiểm tra post tồn tại
        postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        Page<CommentEntity> comments = commentRepository.findTopLevelCommentsByPostId(postId, pageable);
        return comments.map(comment -> mapToCommentResponse(comment, userId));
    }

    // Methods for Problem comments
    @Transactional
    public CommentResponse createCommentForProblem(Long problemId, CreateCommentRequest request, Long userId) {
        ProblemEntity problem = problemRepository.findByIdAndStatusTrue(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        UserEntity author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        CommentEntity comment = CommentEntity.builder()
                .author(author)
                .problem(problem)
                .content(request.getContent())
                .isAccepted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Nếu là reply
        if (request.getParentCommentId() != null) {
            CommentEntity parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy comment cha"));
            comment.setParent(parent);
        }

        comment = commentRepository.save(comment);
        return mapToCommentResponse(comment, userId);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByProblem(Long problemId, Long userId, Pageable pageable) {
        // Kiểm tra problem tồn tại
        problemRepository.findByIdAndStatusTrue(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        Page<CommentEntity> comments = commentRepository.findTopLevelCommentsByProblemId(problemId, pageable);
        return comments.map(comment -> mapToCommentResponse(comment, userId));
    }

    @Transactional
    public CommentResponse replyToComment(Long commentId, CreateCommentRequest request, Long userId) {
        CommentEntity parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy comment"));

        UserEntity author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        CommentEntity reply = CommentEntity.builder()
                .author(author)
                .post(parent.getPost())
                .problem(parent.getProblem())
                .parent(parent)
                .content(request.getContent())
                .isAccepted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        reply = commentRepository.save(reply);

        // Gửi notification cho parent comment author
        try {
            if (!parent.getAuthor().getId().equals(userId)) {
                Long postId = parent.getPost() != null ? parent.getPost().getId() : null;
                Long problemId = parent.getProblem() != null ? parent.getProblem().getId() : null;
                // Note: Notification service có thể cần update để hỗ trợ problems
                if (postId != null) {
                    notificationService.notifyCommentReply(
                            parent.getAuthor().getId(),
                            userId,
                            author.getUsername(),
                            postId,
                            parent.getId()
                    );
                }
            }
        } catch (Exception e) {
            // Log error nhưng không throw
        }

        return mapToCommentResponse(reply, userId);
    }

    @Transactional
    public VoteResponse toggleCommentVote(Long commentId, VoteRequest request, Long userId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy comment"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        var existingVote = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

        Integer voteValue = request.getVote();
        if (voteValue == null || (voteValue != 1 && voteValue != -1 && voteValue != 0)) {
            throw new RuntimeException("Vote phải là 1 (upvote), -1 (downvote), hoặc 0 (remove)");
        }

        if (existingVote.isPresent()) {
            CommentLikeEntity vote = existingVote.get();
            if (voteValue == 0) {
                commentLikeRepository.delete(vote);
            } else if (vote.getVote().equals(voteValue)) {
                commentLikeRepository.delete(vote);
                voteValue = 0;
            } else {
                vote.setVote(voteValue);
                commentLikeRepository.save(vote);
            }
        } else {
            if (voteValue != 0) {
                CommentLikeEntity newVote = CommentLikeEntity.builder()
                        .comment(comment)
                        .user(user)
                        .vote(voteValue)
                        .createdAt(Instant.now())
                        .build();
                commentLikeRepository.save(newVote);
            }
        }

        Long totalVotes = commentLikeRepository.getTotalVotesByCommentId(commentId);
        Long upvotes = commentLikeRepository.countUpvotesByCommentId(commentId);
        Long downvotes = commentLikeRepository.countDownvotesByCommentId(commentId);

        return VoteResponse.builder()
                .id(commentId)
                .vote(voteValue)
                .totalVotes(totalVotes)
                .upvotes(upvotes)
                .downvotes(downvotes)
                .message(voteValue == 0 ? "Đã bỏ vote" : (voteValue == 1 ? "Đã upvote" : "Đã downvote"))
                .build();
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, Long userId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy comment"));

        // Kiểm tra quyền sở hữu
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa comment này");
        }

        comment.setContent(request.getContent());
        comment.setUpdatedAt(Instant.now());
        comment = commentRepository.save(comment);
        return mapToCommentResponse(comment, userId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy comment"));

        // Kiểm tra quyền sở hữu hoặc quyền admin
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa comment này");
        }

        // Xóa tất cả replies trước (cascade delete)
        List<CommentEntity> replies = commentRepository.findRepliesByParentId(commentId);
        for (CommentEntity reply : replies) {
            // Xóa đệ quy tất cả replies của reply này
            deleteCommentRecursive(reply.getId());
        }
        
        // Xóa comment chính
        commentRepository.delete(comment);
    }
    
    @Transactional
    private void deleteCommentRecursive(Long commentId) {
        // Xóa tất cả replies trước
        List<CommentEntity> replies = commentRepository.findRepliesByParentId(commentId);
        for (CommentEntity reply : replies) {
            deleteCommentRecursive(reply.getId());
        }
        // Xóa comment
        CommentEntity comment = commentRepository.findById(commentId)
                .orElse(null);
        if (comment != null) {
            commentRepository.delete(comment);
        }
    }

    private CommentResponse mapToCommentResponse(CommentEntity entity, Long userId) {
        Long totalVotes = commentLikeRepository.getTotalVotesByCommentId(entity.getId());
        Long upvotes = commentLikeRepository.countUpvotesByCommentId(entity.getId());
        Long downvotes = commentLikeRepository.countDownvotesByCommentId(entity.getId());
        
        Long replyCount = (long) commentRepository.findRepliesByParentId(entity.getId()).size();
        
        Integer userVote = null;
        if (userId != null) {
            var userVoteOpt = commentLikeRepository.findByCommentIdAndUserId(entity.getId(), userId);
            if (userVoteOpt.isPresent()) {
                userVote = userVoteOpt.get().getVote();
            }
        }

        List<CommentResponse> replies = commentRepository.findRepliesByParentId(entity.getId())
                .stream()
                .map(reply -> mapToCommentResponse(reply, userId))
                .collect(Collectors.toList());

        return CommentResponse.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .isAccepted(entity.getIsAccepted())
                .authorId(entity.getAuthor() != null ? entity.getAuthor().getId() : null)
                .authorName(entity.getAuthor() != null ? entity.getAuthor().getUsername() : null)
                .authorAvatar(entity.getAuthor() != null ? entity.getAuthor().getAvatar() : null)
                .parentCommentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .totalVotes(totalVotes)
                .upvotes(upvotes)
                .downvotes(downvotes)
                .userVote(userVote)
                .replyCount(replyCount)
                .replies(replies)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

