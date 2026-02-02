package com.hcmute.codesphere_server.model.payload.response;

import com.hcmute.codesphere_server.model.enums.ContestType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContestDetailResponse {
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
    private String status; // UPCOMING, REGISTRATION, ONGOING, ENDED
    private Long authorId;
    private String authorName;
    private Boolean isRegistered;
    private Integer totalRegistrations;
    private Instant startedAt; // Thời gian user bắt đầu (chỉ cho PRACTICE contest, khi isRegistered = true)
    private Instant endedAt; // Thời gian user kết thúc (chỉ cho PRACTICE contest, khi isRegistered = true)
    private List<ContestProblemResponse> problems;
}

