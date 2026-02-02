package com.hcmute.codesphere_server.model.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationRequest {
    
    private String name;
    
    private String avatar;
}

