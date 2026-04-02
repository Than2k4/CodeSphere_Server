package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.TagEntity;
import com.hcmute.codesphere_server.model.enums.TagType;
import com.hcmute.codesphere_server.model.payload.request.AuditLogRecordRequest;
import com.hcmute.codesphere_server.model.payload.request.CreateTagRequest;
import com.hcmute.codesphere_server.model.payload.response.TagResponse;
import com.hcmute.codesphere_server.repository.common.TagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminTagService {

    private final TagRepository tagRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public TagResponse createTag(
            CreateTagRequest request,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
    ) {
        // Admin tạo tag cho problems, nên type mặc định là PROBLEM
        TagType tagType = TagType.PROBLEM;
        
        // Kiểm tra slug đã tồn tại với type này chưa
        if (tagRepository.existsBySlugAndType(request.getSlug().toLowerCase(), tagType)) {
            throw new RuntimeException("Tag với slug '" + request.getSlug() + "' đã tồn tại cho loại " + tagType);
        }

        // Tạo tag mới
        TagEntity tag = TagEntity.builder()
                .name(request.getName())
                .slug(request.getSlug().toLowerCase()) // Chuyển về lowercase
                .type(tagType) // Tag cho problems
                .build();

        tag = tagRepository.save(tag);

        recordAudit(
            actorId,
            actorUsername,
            actorRole,
            "TAG_CREATE",
            tag.getId(),
            tag.getName(),
            null,
            toJson(snapshotTag(tag)),
            "Create tag",
            ipAddress,
            userAgent
        );

        // Map sang response
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .build();
    }

    @Transactional
    public void deleteTag(
            Long id,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
    ) {
        TagEntity tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag không tồn tại"));

        String beforeState = toJson(snapshotTag(tag));
        tagRepository.delete(tag);

        recordAudit(
                actorId,
                actorUsername,
                actorRole,
                "TAG_DELETE",
                id,
                tag.getName(),
                beforeState,
                null,
                "Delete tag",
                ipAddress,
                userAgent
        );
    }

    private java.util.Map<String, Object> snapshotTag(TagEntity tag) {
        java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("id", tag.getId());
        snapshot.put("name", tag.getName());
        snapshot.put("slug", tag.getSlug());
        snapshot.put("type", tag.getType() != null ? tag.getType().name() : null);
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
                            .objectType("TAG")
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
            // Keep tag action successful even if audit log fails
        }
    }
}

