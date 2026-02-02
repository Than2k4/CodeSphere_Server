package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestProblemRequest {
    @NotNull(message = "Problem ID không được để trống")
    private Long problemId;

    @NotBlank(message = "Order không được để trống")
    private String order; // A, B, C, D, etc.

    @NotNull(message = "Points không được để trống")
    private Integer points;
}

