package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCodeResponse {
    private String review;
    private Integer score; // Optional score from 0-100
    private List<String> suggestions; // Optional list of suggestions
}

