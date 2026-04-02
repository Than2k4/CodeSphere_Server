package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.AuditLogEntity;
import com.hcmute.codesphere_server.model.payload.request.AuditLogRecordRequest;
import com.hcmute.codesphere_server.model.payload.response.AuditLogResponse;
import com.hcmute.codesphere_server.repository.common.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(AuditLogRecordRequest request) {
        if (request == null || request.getAction() == null || request.getObjectType() == null) {
            return;
        }

        AuditLogEntity entity = AuditLogEntity.builder()
                .actorId(request.getActorId())
                .actorUsername(safeTrim(request.getActorUsername(), 100))
                .actorRole(safeTrim(request.getActorRole(), 50))
                .action(safeTrim(request.getAction(), 80))
                .objectType(safeTrim(request.getObjectType(), 80))
                .objectId(request.getObjectId())
                .objectLabel(safeTrim(request.getObjectLabel(), 200))
                .beforeState(request.getBeforeState())
                .afterState(request.getAfterState())
                .changeSummary(request.getChangeSummary())
                .ipAddress(safeTrim(request.getIpAddress(), 64))
                .userAgent(safeTrim(request.getUserAgent(), 512))
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            Pageable pageable,
            String action,
            String objectType,
            Long actorId,
            Long objectId,
            Instant fromTime,
            Instant toTime,
            String q
    ) {
        Specification<AuditLogEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (action != null && !action.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("action")), action.trim().toUpperCase()));
            }

            if (objectType != null && !objectType.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("objectType")), objectType.trim().toUpperCase()));
            }

            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }

            if (objectId != null) {
                predicates.add(cb.equal(root.get("objectId"), objectId));
            }

            if (fromTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromTime));
            }

            if (toTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toTime));
            }

            if (q != null && !q.trim().isEmpty()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("actorUsername")), pattern),
                        cb.like(cb.lower(root.get("action")), pattern),
                        cb.like(cb.lower(root.get("objectType")), pattern),
                        cb.like(cb.lower(root.get("objectLabel")), pattern),
                        cb.like(cb.lower(root.get("changeSummary")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AuditLogEntity entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .actorId(entity.getActorId())
                .actorUsername(entity.getActorUsername())
                .actorRole(entity.getActorRole())
                .action(entity.getAction())
                .objectType(entity.getObjectType())
                .objectId(entity.getObjectId())
                .objectLabel(entity.getObjectLabel())
                .beforeState(entity.getBeforeState())
                .afterState(entity.getAfterState())
                .changeSummary(entity.getChangeSummary())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String safeTrim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
