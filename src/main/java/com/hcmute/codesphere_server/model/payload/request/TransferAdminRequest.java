package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferAdminRequest {
    
    @NotNull(message = "User ID của admin mới không được để trống")
    private Long newAdminId;
}

