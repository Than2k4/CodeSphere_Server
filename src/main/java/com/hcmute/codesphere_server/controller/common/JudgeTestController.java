package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.service.common.JudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller để test Docker execution connection
 * Chỉ dùng cho testing, có thể xóa trong production
 */
@RestController
@RequestMapping("${base.url}/judge/test")
@RequiredArgsConstructor
public class JudgeTestController {

    private final JudgeService judgeService;

    /**
     * Test Docker execution với code đơn giản
     * GET /api/v1/judge/test?languageCode=python
     */
    @GetMapping
    public ResponseEntity<DataResponse<Map<String, Object>>> testDockerConnection(
            @RequestParam(defaultValue = "python") String languageCode) {
        
        try {
            // Test với code đơn giản
            String testCode = switch (languageCode.toLowerCase()) {
                case "python" -> "print('Hello from Docker')";
                case "java" -> "public class Main { public static void main(String[] args) { System.out.println(\"Hello from Docker\"); } }";
                case "javascript" -> "console.log('Hello from Docker')";
                case "cpp" -> "#include <iostream>\nint main() { std::cout << \"Hello from Docker\" << std::endl; return 0; }";
                default -> "print('Hello from Docker')";
            };

            var result = judgeService.validateCode(testCode, languageCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("languageCode", languageCode);
            response.put("testCode", testCode);
            response.put("valid", result.isValid());
            response.put("message", result.getMessage());
            response.put("errors", result.getErrors());
            response.put("dockerConnected", result.isValid() || 
                    (result.getMessage() != null && !result.getMessage().contains("Connection")));
            
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("dockerConnected", false);
            return ResponseEntity.badRequest()
                    .body(DataResponse.error("Test failed: " + e.getMessage()));
        }
    }
}

