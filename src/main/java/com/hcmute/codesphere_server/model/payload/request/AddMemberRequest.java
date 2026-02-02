package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequest {
    
    @NotNull(message = "Danh sách user IDs không được để trống")
    private List<Long> userIds;
}

