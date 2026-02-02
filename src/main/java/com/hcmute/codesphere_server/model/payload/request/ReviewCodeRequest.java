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
public class ReviewCodeRequest {
    @NotNull(message = "Problem ID không được để trống")
    private Long problemId;

    @NotBlank(message = "Code không được để trống")
    private String code;

    @NotBlank(message = "Language không được để trống")
    private String language;
}

