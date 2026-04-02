package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.LanguageEntity;
import com.hcmute.codesphere_server.model.payload.request.AuditLogRecordRequest;
import com.hcmute.codesphere_server.model.payload.request.CreateLanguageRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateLanguageRequest;
import com.hcmute.codesphere_server.model.payload.response.LanguageResponse;
import com.hcmute.codesphere_server.repository.common.LanguageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLanguageService {

    private final LanguageRepository languageRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public LanguageResponse createLanguage(
            CreateLanguageRequest request,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
    ) {
        // Kiểm tra code đã tồn tại chưa
        if (languageRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Ngôn ngữ với code '" + request.getCode() + "' đã tồn tại");
        }

        // Tạo language mới
        LanguageEntity language = LanguageEntity.builder()
                .code(request.getCode().toLowerCase()) // Chuyển về lowercase
                .name(request.getName())
                .version(request.getVersion())
                .build();

        language = languageRepository.save(language);

        recordAudit(
            actorId,
            actorUsername,
            actorRole,
            "LANGUAGE_CREATE",
            language.getId(),
            language.getName(),
            null,
            toJson(snapshotLanguage(language)),
            "Create language",
            ipAddress,
            userAgent
        );

        // Map sang response
        return LanguageResponse.builder()
                .id(language.getId())
                .code(language.getCode())
                .name(language.getName())
                .version(language.getVersion())
                .build();
    }

    @Transactional
    public LanguageResponse updateLanguage(
            Long id,
            UpdateLanguageRequest request,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
    ) {
        LanguageEntity language = languageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngôn ngữ với ID: " + id));

        String beforeState = toJson(snapshotLanguage(language));

        // Nếu có thay đổi code, kiểm tra code mới có trùng không
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            String newCode = request.getCode().trim().toLowerCase();
            if (!language.getCode().equals(newCode)) {
                // Code đã thay đổi, kiểm tra trùng
                if (languageRepository.existsByCode(newCode)) {
                    throw new RuntimeException("Ngôn ngữ với code '" + newCode + "' đã tồn tại");
                }
                language.setCode(newCode);
            }
        }

        // Cập nhật các trường khác
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            language.setName(request.getName().trim());
        }

        if (request.getVersion() != null) {
            language.setVersion(request.getVersion().trim().isEmpty() ? null : request.getVersion().trim());
        }

        language = languageRepository.save(language);

        recordAudit(
            actorId,
            actorUsername,
            actorRole,
            "LANGUAGE_UPDATE",
            language.getId(),
            language.getName(),
            beforeState,
            toJson(snapshotLanguage(language)),
            "Update language",
            ipAddress,
            userAgent
        );

        // Map sang response
        return LanguageResponse.builder()
                .id(language.getId())
                .code(language.getCode())
                .name(language.getName())
                .version(language.getVersion())
                .build();
    }

    @Transactional
    public void deleteLanguage(
            Long id,
            Long actorId,
            String actorUsername,
            String actorRole,
            String ipAddress,
            String userAgent
    ) {
        LanguageEntity language = languageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngôn ngữ với ID: " + id));

        String beforeState = toJson(snapshotLanguage(language));

        // Kiểm tra xem có bài tập nào đang sử dụng ngôn ngữ này không
        // TODO: Có thể thêm validation ở đây nếu cần

        languageRepository.delete(language);

        recordAudit(
                actorId,
                actorUsername,
                actorRole,
                "LANGUAGE_DELETE",
                id,
                language.getName(),
                beforeState,
                null,
                "Delete language",
                ipAddress,
                userAgent
        );
    }

    private java.util.Map<String, Object> snapshotLanguage(LanguageEntity language) {
        java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("id", language.getId());
        snapshot.put("code", language.getCode());
        snapshot.put("name", language.getName());
        snapshot.put("version", language.getVersion());
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
                            .objectType("LANGUAGE")
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
            // Keep language action successful even if audit log fails
        }
    }
}

