package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotEmpty;
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
public class CreateConversationRequest {
    
    @NotNull(message = "Type không được để trống")
    private String type; // DIRECT hoặc GROUP
    
    private String name; // Cho GROUP
    
    private String avatar; // Cho GROUP
    
    @NotEmpty(message = "Danh sách participants không được để trống")
    private List<Long> participantIds;
}

