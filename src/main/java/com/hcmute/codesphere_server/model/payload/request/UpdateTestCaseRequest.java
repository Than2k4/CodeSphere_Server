package com.hcmute.codesphere_server.model.payload.request;

import lombok.Data;

@Data
public class UpdateTestCaseRequest {
    private String input;
    private String expectedOutput;
    private Boolean isSample;
    private Boolean isHidden;
    private Integer weight;
}

