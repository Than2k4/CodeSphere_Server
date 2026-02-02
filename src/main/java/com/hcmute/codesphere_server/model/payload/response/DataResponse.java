package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DataResponse<T> {
    @Builder.Default
    private String status = "success";
    @Builder.Default
    private String message = "";
    private T data;

    public DataResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public DataResponse(T data) {
        this.data = data;
    }

    public static <T> DataResponse<T> of(String status, String message, T data) {
        return new DataResponse<>(status, message, data);
    }

    public static <T> DataResponse<T> success(T data) {
        DataResponse<T> response = new DataResponse<>();
        response.setStatus("success");
        response.setMessage("");
        response.setData(data);
        return response;
    }

    public static <T> DataResponse<T> error(String message) {
        DataResponse<T> response = new DataResponse<>();
        response.setStatus("error");
        response.setMessage(message);
        response.setData(null);
        return response;
    }

    @Data
    @Builder
    public static class LoginData {
        private UserEntity user;
        private String accessToken;
        private Long expiresIn;
        private String refreshToken;
    }
}