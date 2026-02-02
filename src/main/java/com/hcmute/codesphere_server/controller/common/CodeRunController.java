package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.RunCodeRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.RunCodeResponse;
import com.hcmute.codesphere_server.service.common.CodeRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/code")
@RequiredArgsConstructor
public class CodeRunController {

    private final CodeRunService codeRunService;

    /**
     * Run code với sample testcases hoặc custom input
     * POST /api/v1/code/run
     */
    @PostMapping("/run")
    public ResponseEntity<DataResponse<RunCodeResponse>> runCode(
            @Valid @RequestBody RunCodeRequest request) {
        
        try {
            RunCodeResponse response = codeRunService.runCode(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

