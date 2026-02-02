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
public class SendFriendRequestRequest {
    
    @NotNull(message = "Receiver ID không được để trống")
    private Long receiverId;
}

