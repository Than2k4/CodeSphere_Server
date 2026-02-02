package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.ValidateCodeRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.ValidationResponse;
import com.hcmute.codesphere_server.service.common.JudgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/code/validate")
@RequiredArgsConstructor
public class CodeValidationController {

    private final JudgeService judgeService;

    /**
     * Validate code syntax - Real-time checking trong editor
     * POST /api/v1/code/validate
     */
    @PostMapping
    public ResponseEntity<DataResponse<ValidationResponse>> validateCode(
            @Valid @RequestBody ValidateCodeRequest request) {
        
        try {
            JudgeService.ValidationResult result = judgeService.validateCode(
                    request.getCodeContent(), request.getLanguageCode());
            
            ValidationResponse response = ValidationResponse.builder()
                    .valid(result.isValid())
                    .message(result.getMessage())
                    .errors(result.getErrors())
                    .build();
            
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error("Validation error: " + e.getMessage()));
        }
    }
}

