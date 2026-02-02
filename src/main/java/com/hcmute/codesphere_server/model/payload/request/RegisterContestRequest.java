package com.hcmute.codesphere_server.model.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterContestRequest {
    private String accessCode; // Required for private contests
}

