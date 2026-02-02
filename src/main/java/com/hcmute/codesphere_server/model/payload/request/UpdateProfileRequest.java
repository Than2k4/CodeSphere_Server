package com.hcmute.codesphere_server.model.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    
    private String username;
    
    private Date dob;
    
    private String phoneNumber;
    
    private String gender;
}

