package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.enums.ModerationReasonCode;
import com.hcmute.codesphere_server.model.enums.TagType;
import com.hcmute.codesphere_server.model.payload.request.CreatePostRequest;
import com.hcmute.codesphere_server.model.payload.request.ReportPostRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdatePostRequest;
import com.hcmute.codesphere_server.model.payload.request.VoteRequest;
import com.hcmute.codesphere_server.model.payload.response.*;
import com.hcmute.codesphere_server.repository.common.*;
import com.hcmute.codesphere_server.util.SlugUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final Set<String> ALLOWED_REACTION_TYPES = Set.of("LIKE", "LOVE", "HAHA", "WOW", "SAD", "ANGRY");

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostViewRepository postViewRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final PostReportRepository postReportRepository;
    private final PostShareRepository postShareRepository;
    private final NotificationService notificationService;
    private final FollowRepository followRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public PostDetailResponse createPost(CreatePostRequest request, Long userId) {
        UserEntity author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Convert images list to JSON string
        String imagesJson = null;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(request.getImages());
            } catch (Exception e) {
                throw new RuntimeException("Error serializing images", e);
            }
        }

        PostEntity post = PostEntity.builder()
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .images(imagesJson)
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                .isBlocked(false)
                .isResolved(false)
                .isDeleted(false)
                .reportCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .tags(new HashSet<>())
                .build();

        // Thêm tags nếu có - tự động tạo tag mới nếu chưa tồn tại
        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            Set<TagEntity> tags = new HashSet<>();
            for (String tagName : request.getTagNames()) {
                if (tagName == null || tagName.trim().isEmpty()) {
                    continue;
                }
                
                String trimmedName = tagName.trim();
                String slug = SlugUtils.generateSlug(trimmedName);
                
                if (slug.isEmpty()) {
                    continue; // Skip invalid tag names
                }
                
                // Tìm tag theo name và type = POST
                TagEntity tag = tagRepository.findByNameAndType(trimmedName, TagType.POST)
                        .orElse(null);
                
                // Nếu chưa tồn tại, tạo mới
                if (tag == null) {
                    // Kiểm tra xem slug đã tồn tại với type = POST chưa
                    if (tagRepository.existsBySlugAndType(slug, TagType.POST)) {
                        // Nếu slug đã tồn tại, thêm số vào cuối
                        int counter = 1;
                        String uniqueSlug = slug + "-" + counter;
                        while (tagRepository.existsBySlugAndType(uniqueSlug, TagType.POST)) {
                            counter++;
                            uniqueSlug = slug + "-" + counter;
                        }
                        slug = uniqueSlug;
                    }
                    
                    tag = TagEntity.builder()
                            .name(trimmedName)
                            .slug(slug)
                            .type(TagType.POST)
                            .build();
                    tag = tagRepository.save(tag);
                }
                
                tags.add(tag);
            }
            post.setTags(tags);
        }

        post = postRepository.save(post);
        return mapToPostDetailResponse(post, userId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(Long authorId, String tagSlug, 
                                       Boolean isResolved, String searchQuery, Boolean followedOnly, Long userId, Pageable pageable) {
        Specification<PostEntity> spec = buildSpecification(authorId, tagSlug, isResolved, searchQuery, followedOnly, userId);
        Page<PostEntity> posts = postRepository.findAll(spec, pageable);
        
        return posts.map(post -> mapToPostResponse(post, userId));
    }

    @Transactional
    public PostDetailResponse getPostById(Long postId, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));
        
        // Track view nếu user đã đăng nhập
        if (userId != null) {
            trackView(postId, userId);
        }
        
        return mapToPostDetailResponse(post, userId);
    }
    
    @Transactional
    public void trackView(Long postId, Long userId) {
        // Kiểm tra xem user đã xem chưa
        var existingView = postViewRepository.findByPostIdAndUserId(postId, userId);
        
        if (existingView.isEmpty()) {
            // Tạo view mới
            PostEntity post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            
            // Tạo view mới - không dùng builder để tránh lỗi với @EmbeddedId
            PostViewEntity newView = new PostViewEntity();
            newView.setViewedAt(Instant.now());
            // Set post và user - sẽ tự động set id thông qua setter
            newView.setPost(post);
            newView.setUser(user);
            postViewRepository.save(newView);
        }
    }

    @Transactional
    public PostDetailResponse updatePost(Long postId, UpdatePostRequest request, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        // Kiểm tra quyền sở hữu
        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bài thảo luận này");
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getImageUrl() != null) {
            post.setImageUrl(request.getImageUrl());
        }
        if (request.getImages() != null) {
            try {
                String imagesJson = objectMapper.writeValueAsString(request.getImages());
                post.setImages(imagesJson);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing images", e);
            }
        }
        if (request.getFileUrl() != null) {
            post.setFileUrl(request.getFileUrl());
        }
        if (request.getFileName() != null) {
            post.setFileName(request.getFileName());
        }
        if (request.getFileType() != null) {
            post.setFileType(request.getFileType());
        }

        // Cập nhật tags - tự động tạo tag mới nếu chưa tồn tại
        if (request.getTagNames() != null) {
            Set<TagEntity> tags = new HashSet<>();
            for (String tagName : request.getTagNames()) {
                if (tagName == null || tagName.trim().isEmpty()) {
                    continue;
                }
                
                String trimmedName = tagName.trim();
                String slug = SlugUtils.generateSlug(trimmedName);
                
                if (slug.isEmpty()) {
                    continue; // Skip invalid tag names
                }
                
                // Tìm tag theo name và type = POST
                TagEntity tag = tagRepository.findByNameAndType(trimmedName, TagType.POST)
                        .orElse(null);
                
                // Nếu chưa tồn tại, tạo mới
                if (tag == null) {
                    // Kiểm tra xem slug đã tồn tại với type = POST chưa
                    if (tagRepository.existsBySlugAndType(slug, TagType.POST)) {
                        // Nếu slug đã tồn tại, thêm số vào cuối
                        int counter = 1;
                        String uniqueSlug = slug + "-" + counter;
                        while (tagRepository.existsBySlugAndType(uniqueSlug, TagType.POST)) {
                            counter++;
                            uniqueSlug = slug + "-" + counter;
                        }
                        slug = uniqueSlug;
                    }
                    
                    tag = TagEntity.builder()
                            .name(trimmedName)
                            .slug(slug)
                            .type(TagType.POST)
                            .build();
                    tag = tagRepository.save(tag);
                }
                
                tags.add(tag);
            }
            post.setTags(tags);
        }

        post.setUpdatedAt(Instant.now());
        post = postRepository.save(post);
        return mapToPostDetailResponse(post, userId);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        // Kiểm tra quyền sở hữu
        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa bài thảo luận này");
        }

        post.setIsDeleted(true);
        post.setIsBlocked(true);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
    }

    @Transactional
    public VoteResponse toggleVote(Long postId, VoteRequest request, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        var existingVote = postLikeRepository.findByPostIdAndUserId(postId, userId);

        Integer voteValue = request.getVote();
        if (voteValue == null || (voteValue != 1 && voteValue != -1 && voteValue != 0)) {
            throw new RuntimeException("Vote phải là 1 (upvote), -1 (downvote), hoặc 0 (remove)");
        }

        String normalizedReactionType = normalizeReactionType(request.getReactionType());
        if (voteValue == 1 && normalizedReactionType == null) {
            normalizedReactionType = "LIKE";
        }

        if (existingVote.isPresent()) {
            PostLikeEntity vote = existingVote.get();
            if (voteValue == 0) {
                // Xóa vote
                postLikeRepository.delete(vote);
            } else if (vote.getVote().equals(voteValue)) {
                if (voteValue == 1 && normalizedReactionType != null) {
                    vote.setReactionType(normalizedReactionType);
                    postLikeRepository.save(vote);
                } else {
                    // Đã vote cùng loại -> xóa vote
                    postLikeRepository.delete(vote);
                    voteValue = 0;
                }
            } else {
                // Đổi vote
                vote.setVote(voteValue);
                vote.setReactionType(voteValue == 1 ? normalizedReactionType : null);
                postLikeRepository.save(vote);
            }
        } else {
            if (voteValue != 0) {
                // Tạo vote mới - không dùng builder để tránh lỗi với @EmbeddedId
                PostLikeEntity newVote = new PostLikeEntity();
                newVote.setVote(voteValue);
                newVote.setReactionType(voteValue == 1 ? normalizedReactionType : null);
                newVote.setCreatedAt(Instant.now());
                // Set post và user - sẽ tự động set id thông qua setter
                newVote.setPost(post);
                newVote.setUser(user);
                postLikeRepository.save(newVote);
            }
        }

        // Tính lại tổng votes
        Long totalVotes = postLikeRepository.getTotalVotesByPostId(postId);
        Long upvotes = postLikeRepository.countUpvotesByPostId(postId);
        Long downvotes = postLikeRepository.countDownvotesByPostId(postId);

        // Gửi notification nếu upvote (không gửi cho chính mình)
        if (voteValue == 1 && !post.getAuthor().getId().equals(userId)) {
            try {
                notificationService.notifyPostLike(
                        post.getAuthor().getId(),
                        userId,
                        user.getUsername(),
                        postId
                );
            } catch (Exception e) {
                // Log error nhưng không throw để không ảnh hưởng đến vote
            }
        }

        return VoteResponse.builder()
                .id(postId)
                .vote(voteValue)
                .totalVotes(totalVotes)
                .upvotes(upvotes)
                .downvotes(downvotes)
                .message(voteValue == 0 ? "Đã bỏ vote" : (voteValue == 1 ? "Đã upvote" : "Đã downvote"))
                .build();
    }

            @Transactional(readOnly = true)
            public List<PostReactionUserResponse> getPostReactionUsers(Long postId, int limit) {
            PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

            int safeLimit = Math.max(1, Math.min(limit, 100));
            List<PostLikeEntity> likes = postLikeRepository.findTopUpvoteUsersByPostId(
                post.getId(),
                PageRequest.of(0, safeLimit)
            );

            return likes.stream()
                .map(like -> PostReactionUserResponse.builder()
                    .userId(like.getUser().getId())
                    .username(like.getUser().getUsername())
                    .avatar(like.getUser().getAvatar())
                    .reactedAt(like.getCreatedAt())
                    .build())
                .collect(Collectors.toList());
            }

    @Transactional(readOnly = true)
    public PostReactionSummaryResponse getPostReactionSummary(Long postId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        List<Object[]> summaryRows = postLikeRepository.getReactionSummaryByPostId(post.getId());

        List<ReactionSummaryItemResponse> allReactions = summaryRows.stream()
                .map(row -> ReactionSummaryItemResponse.builder()
                        .reactionType(row[0] != null ? row[0].toString() : "LIKE")
                        .count(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .build())
                .collect(Collectors.toList());

        long totalReactions = allReactions.stream().mapToLong(ReactionSummaryItemResponse::getCount).sum();
        List<ReactionSummaryItemResponse> topReactions = allReactions.stream().limit(2).collect(Collectors.toList());

        return PostReactionSummaryResponse.builder()
                .postId(postId)
                .totalReactions(totalReactions)
                .topReactions(topReactions)
                .build();
    }

            @Transactional
            public void markPostShared(Long postId, Long userId) {
            PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

            UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

            boolean alreadyShared = postShareRepository.findByPostIdAndUserId(postId, userId).isPresent();
            if (alreadyShared) {
                return;
            }

            PostShareEntity share = PostShareEntity.builder()
                .post(post)
                .user(user)
                .createdAt(Instant.now())
                .build();

            postShareRepository.save(share);
            }

            @Transactional(readOnly = true)
            public List<PostShareUserResponse> getPostShareUsers(Long postId, int limit) {
            PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

            int safeLimit = Math.max(1, Math.min(limit, 100));
            List<PostShareEntity> shares = postShareRepository.findTopSharesByPostId(post.getId(), PageRequest.of(0, safeLimit));

            return shares.stream()
                .map(share -> PostShareUserResponse.builder()
                    .userId(share.getUser().getId())
                    .username(share.getUser().getUsername())
                    .avatar(share.getUser().getAvatar())
                    .sharedAt(share.getCreatedAt())
                    .build())
                .collect(Collectors.toList());
            }

    private String normalizeReactionType(String reactionType) {
        if (reactionType == null || reactionType.trim().isEmpty()) {
            return null;
        }

        String normalized = reactionType.trim().toUpperCase();
        if (!ALLOWED_REACTION_TYPES.contains(normalized)) {
            throw new RuntimeException("Reaction type không hợp lệ");
        }
        return normalized;
    }

    @Transactional
    public PostDetailResponse markAsResolved(Long postId, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        // Chỉ author mới có thể đánh dấu đã giải quyết
        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Chỉ tác giả mới có thể đánh dấu đã giải quyết");
        }

        post.setIsResolved(!post.getIsResolved());
        post.setUpdatedAt(Instant.now());
        post = postRepository.save(post);
        return mapToPostDetailResponse(post, userId);
    }

    @Transactional
    public void reportPost(Long postId, Long reporterId, ReportPostRequest request) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        if (post.getAuthor().getId().equals(reporterId)) {
            throw new RuntimeException("Bạn không thể tự báo cáo bài viết của mình");
        }

        if (postReportRepository.existsByPostIdAndReporterId(postId, reporterId)) {
            throw new RuntimeException("Bạn đã báo cáo bài viết này trước đó");
        }

        validateReasonCode(request.getReasonCode());

        UserEntity reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        PostReportEntity report = PostReportEntity.builder()
                .post(post)
                .reporter(reporter)
                .reasonCode(request.getReasonCode().trim().toUpperCase())
                .reasonDetail(request.getReasonDetail())
                .createdAt(Instant.now())
                .build();
        postReportRepository.save(report);

        int currentCount = post.getReportCount() != null ? post.getReportCount() : 0;
        post.setReportCount(currentCount + 1);
        post.setLastReportedAt(Instant.now());
        postRepository.save(post);
    }

    private void validateReasonCode(String reasonCode) {
        try {
            ModerationReasonCode.valueOf(reasonCode.trim().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Mã lý do không hợp lệ");
        }
    }

    private Specification<PostEntity> buildSpecification(Long authorId, String tagSlug, Boolean isResolved, String searchQuery, Boolean followedOnly, Long userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Chỉ lấy posts chưa bị block
            predicates.add(cb.equal(root.get("isBlocked"), false));
            predicates.add(cb.equal(root.get("isDeleted"), false));
            
            if (authorId != null) {
                predicates.add(cb.equal(root.get("author").get("id"), authorId));
            }
            
            // Filter theo following nếu followedOnly = true và userId không null
            if (followedOnly != null && followedOnly && userId != null) {
                // Lấy danh sách user IDs mà current user đang follow
                List<FollowEntity> follows = followRepository.findByFollowerId(userId);
                if (follows.isEmpty()) {
                    // Nếu không follow ai, trả về empty result
                    predicates.add(cb.disjunction()); // Always false
                } else {
                    List<Long> followedUserIds = follows.stream()
                            .map(f -> f.getFollowee().getId())
                            .collect(Collectors.toList());
                    predicates.add(root.get("author").get("id").in(followedUserIds));
                }
            }
            
            if (tagSlug != null && !tagSlug.isEmpty()) {
                Join<PostEntity, TagEntity> tagJoin = root.join("tags");
                predicates.add(cb.equal(tagJoin.get("slug"), tagSlug));
                // Chỉ lấy posts có tag với type = POST
                predicates.add(cb.equal(tagJoin.get("type"), TagType.POST));
            }
            
            if (isResolved != null) {
                predicates.add(cb.equal(root.get("isResolved"), isResolved));
            }
            
            // Search query - chỉ tìm trong title
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String searchPattern = "%" + searchQuery + "%";
                // MySQL thường case-insensitive với collation mặc định, nên không cần LOWER/UPPER
                predicates.add(cb.like(root.get("title"), searchPattern));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PostResponse mapToPostResponse(PostEntity entity, Long userId) {
        Long totalVotes = postLikeRepository.getTotalVotesByPostId(entity.getId());
        Long upvotes = postLikeRepository.countUpvotesByPostId(entity.getId());
        Long downvotes = postLikeRepository.countDownvotesByPostId(entity.getId());
        Long viewCount = postViewRepository.countViewsByPostId(entity.getId());
        Long commentCount = commentRepository.countByPostId(entity.getId());
        
        Integer userVote = null;
        if (userId != null) {
            var userVoteOpt = postLikeRepository.findByPostIdAndUserId(entity.getId(), userId);
            if (userVoteOpt.isPresent()) {
                userVote = userVoteOpt.get().getVote();
            }
        }

        // Parse images JSON to List<String>
        List<String> imagesList = null;
        if (entity.getImages() != null && !entity.getImages().trim().isEmpty()) {
            try {
                imagesList = objectMapper.readValue(entity.getImages(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                // If parsing fails, return empty list
                imagesList = new ArrayList<>();
            }
        }

        return PostResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .imageUrl(entity.getImageUrl())
                .images(imagesList)
                .fileUrl(entity.getFileUrl())
                .fileName(entity.getFileName())
                .fileType(entity.getFileType())
                .isAnonymous(entity.getIsAnonymous())
                .isResolved(entity.getIsResolved())
                .authorId(entity.getAuthor() != null ? entity.getAuthor().getId() : null)
                .authorName(entity.getIsAnonymous() ? null : (entity.getAuthor() != null ? entity.getAuthor().getUsername() : null))
                .authorAvatar(entity.getIsAnonymous() ? null : (entity.getAuthor() != null ? entity.getAuthor().getAvatar() : null))
                .totalVotes(totalVotes)
                .upvotes(upvotes)
                .downvotes(downvotes)
                .commentCount(commentCount)
                .viewCount(viewCount)
                .userVote(userVote)
                .tags(entity.getTags().stream()
                        .map(this::mapToTagResponse)
                        .collect(Collectors.toList()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private PostDetailResponse mapToPostDetailResponse(PostEntity entity, Long userId) {
        PostResponse baseResponse = mapToPostResponse(entity, userId);
        
        // Lấy comments
        List<CommentResponse> comments = commentRepository.findTopLevelCommentsByPostId(
                entity.getId(), 
                org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .map(comment -> mapToCommentResponse(comment, userId))
                .collect(Collectors.toList());

        return PostDetailResponse.builder()
                .id(baseResponse.getId())
                .title(baseResponse.getTitle())
                .content(baseResponse.getContent())
                .imageUrl(baseResponse.getImageUrl())
                .images(baseResponse.getImages())
                .fileUrl(baseResponse.getFileUrl())
                .fileName(baseResponse.getFileName())
                .fileType(baseResponse.getFileType())
                .isAnonymous(baseResponse.getIsAnonymous())
                .isResolved(baseResponse.getIsResolved())
                .authorId(baseResponse.getAuthorId())
                .authorName(baseResponse.getAuthorName())
                .authorAvatar(baseResponse.getAuthorAvatar())
                .totalVotes(baseResponse.getTotalVotes())
                .upvotes(baseResponse.getUpvotes())
                .downvotes(baseResponse.getDownvotes())
                .commentCount(baseResponse.getCommentCount())
                .viewCount(baseResponse.getViewCount())
                .userVote(baseResponse.getUserVote())
                .tags(baseResponse.getTags())
                .comments(comments)
                .createdAt(baseResponse.getCreatedAt())
                .updatedAt(baseResponse.getUpdatedAt())
                .build();
    }

    private CommentResponse mapToCommentResponse(CommentEntity entity, Long userId) {
        Long totalVotes = commentLikeRepository.getTotalVotesByCommentId(entity.getId());
        Long upvotes = commentLikeRepository.countUpvotesByCommentId(entity.getId());
        Long downvotes = commentLikeRepository.countDownvotesByCommentId(entity.getId());
        
        // Đếm replies
        Long replyCount = (long) commentRepository.findRepliesByParentId(entity.getId()).size();
        
        Integer userVote = null;
        if (userId != null) {
            var userVoteOpt = commentLikeRepository.findByCommentIdAndUserId(entity.getId(), userId);
            if (userVoteOpt.isPresent()) {
                userVote = userVoteOpt.get().getVote();
            }
        }

        // Lấy replies
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

    private TagResponse mapToTagResponse(TagEntity entity) {
        return TagResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .build();
    }

}

