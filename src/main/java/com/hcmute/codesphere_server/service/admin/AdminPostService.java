package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.NotificationEntity;
import com.hcmute.codesphere_server.model.entity.PostEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.enums.ModerationReasonCode;
import com.hcmute.codesphere_server.model.payload.request.AuditLogRecordRequest;
import com.hcmute.codesphere_server.model.payload.request.ModeratePostRequest;
import com.hcmute.codesphere_server.model.payload.response.ModerationReasonTemplateResponse;
import com.hcmute.codesphere_server.repository.common.PostRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import com.hcmute.codesphere_server.service.common.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<PostEntity> getPosts(Pageable pageable, String search, String isBlocked) {
        Specification<PostEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Eager fetch author to avoid lazy loading issues
            root.fetch("author", JoinType.LEFT);
            // Prevent duplicate results
            query.distinct(true);

            // Search by title or content
            if (search != null && !search.trim().isEmpty()) {
                String lowerCaseSearch = search.toLowerCase();
                String searchPattern = "%" + lowerCaseSearch + "%";
                // Search in title (case-insensitive)
                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), searchPattern);
                // Search in content - MySQL LOB columns can be searched directly with LIKE
                // We'll search the content as-is (MySQL default collation is case-insensitive)
                Predicate contentPredicate = cb.like(root.get("content"), searchPattern);
                predicates.add(cb.or(titlePredicate, contentPredicate));
            }

            // Filter by blocked status
            if (isBlocked != null && !isBlocked.trim().isEmpty()) {
                boolean blocked = Boolean.parseBoolean(isBlocked);
                predicates.add(cb.equal(root.get("isBlocked"), blocked));
            }

            // Soft-deleted posts are hidden from normal admin list
            predicates.add(cb.equal(root.get("isDeleted"), false));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return postRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PostEntity> getReportQueue(Pageable pageable) {
        Specification<PostEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            root.fetch("author", JoinType.LEFT);
            query.distinct(true);

            predicates.add(cb.equal(root.get("isDeleted"), false));
            predicates.add(cb.greaterThan(root.get("reportCount"), 0));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return postRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<ModerationReasonTemplateResponse> getReasonTemplates() {
        return List.of(
                ModerationReasonTemplateResponse.builder().code(ModerationReasonCode.SPAM.name()).label("Spam").description("Nội dung quảng cáo, lặp lại, gây nhiễu").build(),
                ModerationReasonTemplateResponse.builder().code(ModerationReasonCode.HARASSMENT.name()).label("Harassment").description("Quấy rối, xúc phạm cá nhân").build(),
                ModerationReasonTemplateResponse.builder().code(ModerationReasonCode.HATE_SPEECH.name()).label("Hate Speech").description("Nội dung thù ghét, phân biệt đối xử").build(),
                ModerationReasonTemplateResponse.builder().code(ModerationReasonCode.EXPLICIT_CONTENT.name()).label("Explicit Content").description("Nội dung nhạy cảm không phù hợp").build(),
                ModerationReasonTemplateResponse.builder().code(ModerationReasonCode.ILLEGAL_CONTENT.name()).label("Illegal Content").description("Nội dung vi phạm pháp luật").build(),
                ModerationReasonTemplateResponse.builder().code(ModerationReasonCode.MISINFORMATION.name()).label("Misinformation").description("Thông tin sai lệch gây hiểu nhầm").build(),
                ModerationReasonTemplateResponse.builder().code(ModerationReasonCode.COPYRIGHT_VIOLATION.name()).label("Copyright Violation").description("Nội dung vi phạm bản quyền").build(),
                ModerationReasonTemplateResponse.builder().code(ModerationReasonCode.OTHER.name()).label("Other").description("Lý do khác").build()
        );
    }

    @Transactional
        public void deletePost(
            Long id,
            Long moderatorId,
            String actorUsername,
            String actorRole,
            ModeratePostRequest request,
            String ipAddress,
            String userAgent
        ) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với ID: " + id));

        validateModerationReason(request);
        String beforeState = toJson(snapshotPost(post));

        post.setDeleted(true);
        post.setBlocked(true);
        post.setModerationReasonCode(request.getReasonCode().trim().toUpperCase());
        post.setModerationReasonDetail(request.getReasonDetail().trim());
        post.setModeratedAt(java.time.Instant.now());
        postRepository.save(post);
        String afterState = toJson(snapshotPost(post));

        recordAudit(
            moderatorId,
            actorUsername,
            actorRole,
            "POST_DELETE",
            post.getId(),
            post.getTitle(),
            beforeState,
            afterState,
            "Soft delete post with moderation reason",
            ipAddress,
            userAgent
        );

        notifyModeration(post, moderatorId, "xóa", request);
    }

    @Transactional
    public void toggleBlock(
            Long id,
            Long moderatorId,
            String actorUsername,
            String actorRole,
            ModeratePostRequest request,
            String ipAddress,
            String userAgent
    ) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với ID: " + id));

        validateModerationReason(request);
        String beforeState = toJson(snapshotPost(post));

        boolean willBeBlocked = !post.getBlocked();
        post.setBlocked(!post.getBlocked());
        post.setModerationReasonCode(request.getReasonCode().trim().toUpperCase());
        post.setModerationReasonDetail(request.getReasonDetail().trim());
        post.setModeratedAt(java.time.Instant.now());
        postRepository.save(post);
        String afterState = toJson(snapshotPost(post));

        recordAudit(
                moderatorId,
                actorUsername,
                actorRole,
                willBeBlocked ? "POST_BLOCK" : "POST_UNBLOCK",
                post.getId(),
                post.getTitle(),
                beforeState,
                afterState,
                willBeBlocked ? "Block post with moderation reason" : "Unblock post with moderation reason",
                ipAddress,
                userAgent
        );

        if (willBeBlocked) {
            notifyModeration(post, moderatorId, "chặn", request);
        }
    }

    private void validateModerationReason(ModeratePostRequest request) {
        if (request == null) {
            throw new RuntimeException("Thiếu thông tin lý do xử lý");
        }

        if (request.getReasonCode() == null || request.getReasonCode().trim().isEmpty()) {
            throw new RuntimeException("Mã lý do không được để trống");
        }

        if (request.getReasonDetail() == null || request.getReasonDetail().trim().isEmpty()) {
            throw new RuntimeException("Mô tả lý do không được để trống");
        }

        try {
            ModerationReasonCode.valueOf(request.getReasonCode().trim().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Mã lý do không hợp lệ");
        }
    }

    private void notifyModeration(PostEntity post, Long moderatorId, String action, ModeratePostRequest request) {
        UserEntity moderator = userRepository.findById(moderatorId).orElse(null);
        Long postAuthorId = post.getAuthor() != null ? post.getAuthor().getId() : null;
        if (postAuthorId == null || postAuthorId.equals(moderatorId)) {
            return;
        }

        try {
            notificationService.notifyPostModeration(
                    postAuthorId,
                    moderatorId,
                    post.getId(),
                    action,
                    request.getReasonCode().trim().toUpperCase(),
                    request.getReasonDetail().trim()
            );
        } catch (Exception ignored) {
            // Keep moderation action successful even if notification fails
        }
    }

    private java.util.Map<String, Object> snapshotPost(PostEntity post) {
        java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("id", post.getId());
        snapshot.put("title", post.getTitle());
        snapshot.put("isBlocked", post.getBlocked());
        snapshot.put("isDeleted", post.getDeleted());
        snapshot.put("isResolved", post.getResolved());
        snapshot.put("reportCount", post.getReportCount());
        snapshot.put("moderationReasonCode", post.getModerationReasonCode());
        snapshot.put("moderationReasonDetail", post.getModerationReasonDetail());
        snapshot.put("moderatedAt", post.getModeratedAt());
        return snapshot;
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void recordAudit(
            Long actorId,
            String actorUsername,
            String actorRole,
            String action,
            Long objectId,
            String objectLabel,
            String beforeState,
            String afterState,
            String changeSummary,
            String ipAddress,
            String userAgent
    ) {
        try {
            auditLogService.record(
                    AuditLogRecordRequest.builder()
                            .actorId(actorId)
                            .actorUsername(actorUsername)
                            .actorRole(actorRole)
                            .action(action)
                            .objectType("POST")
                            .objectId(objectId)
                            .objectLabel(objectLabel)
                            .beforeState(beforeState)
                            .afterState(afterState)
                            .changeSummary(changeSummary)
                            .ipAddress(ipAddress)
                            .userAgent(userAgent)
                            .build()
            );
        } catch (Exception ignored) {
            // Keep moderation action successful even if audit log fails
        }
    }
}

