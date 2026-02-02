package com.hcmute.codesphere_server.model.payload.response;

import com.hcmute.codesphere_server.model.enums.ContestType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContestResponse {
    private Long id;
    private String title;
    private String description;
    private ContestType contestType;
    private Integer durationMinutes; // Chỉ có cho PRACTICE contest
    private Instant startTime;
    private Instant endTime;
    private Instant registrationStartTime; // Nullable, không cần nữa
    private Instant registrationEndTime; // Nullable, không cần nữa
    private Boolean isPublic;
    private Boolean hasAccessCode;
    private Boolean isHidden; // true = ẩn khỏi danh sách public (chỉ admin thấy)
    private String status; // UPCOMING, REGISTRATION, ONGOING, ENDED
    private Long authorId;
    private String authorName;
    private Boolean isRegistered; // Whether current user is registered
    private Integer totalProblems;
    private Integer totalRegistrations;
}

