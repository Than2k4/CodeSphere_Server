package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterVerifyRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "OTP không được để trống")
    @Size(min = 4, max = 4, message = "OTP phải có 4 ký tự")
    private String otp;
}

