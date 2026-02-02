package com.hcmute.codesphere_server.model.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    
    private String content; // Required cho TEXT, null cho IMAGE/FILE
    
    private String messageType; // TEXT, IMAGE, hoặc FILE
    
    private String imageUrl; // Required cho IMAGE
    
    private String fileUrl; // Required cho FILE
    
    private String fileName; // Tên file gốc
    
    private String fileType; // Loại file (pdf, doc, etc.)
}

