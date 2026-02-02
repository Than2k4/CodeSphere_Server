package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestCaseResponse {
    private Long id;
    private String input;
    private String expectedOutput;
    private Boolean isSample;
    private Boolean isHidden; // Chỉ hiển thị cho admin
    private Integer weight;
}

