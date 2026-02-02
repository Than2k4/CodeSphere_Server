package com.hcmute.codesphere_server.model.payload.request;

import com.hcmute.codesphere_server.model.enums.ContestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateContestRequest {
    @NotBlank(message = "Title không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Contest type không được để trống")
    private ContestType contestType; // PRACTICE | OFFICIAL

    private Integer durationMinutes; // Required cho PRACTICE, null cho OFFICIAL

    private Instant startTime; // Nullable cho PRACTICE, required cho OFFICIAL

    private Instant endTime; // Nullable cho PRACTICE, required cho OFFICIAL

    private Instant registrationStartTime; // Nullable, không cần nữa

    private Instant registrationEndTime; // Nullable, không cần nữa

    @NotNull(message = "isPublic không được để trống")
    private Boolean isPublic;

    private String accessCode; // Required if isPublic = false

    private List<ContestProblemRequest> problems; // Problems to add to contest
}

