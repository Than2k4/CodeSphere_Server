package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModeratePostRequest {

    @NotBlank(message = "Mã lý do không được để trống")
    @Size(max = 50, message = "Mã lý do không được vượt quá 50 ký tự")
    private String reasonCode;

    @NotBlank(message = "Mô tả lý do không được để trống")
    @Size(max = 500, message = "Mô tả lý do không được vượt quá 500 ký tự")
    private String reasonDetail;
}
