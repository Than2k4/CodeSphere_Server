package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.ChatRequest;
import com.hcmute.codesphere_server.model.payload.request.RefactorCodeRequest;
import com.hcmute.codesphere_server.model.payload.request.ReviewCodeRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.RefactorCodeResponse;
import com.hcmute.codesphere_server.model.payload.response.ReviewCodeResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("${base.url}/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/refactor")
    public ResponseEntity<DataResponse<RefactorCodeResponse>> refactorCode(
            @Valid @RequestBody RefactorCodeRequest request,
            Authentication authentication
    ) {
        try {
            // Verify user is authenticated
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrinciple)) {
                return ResponseEntity.status(401)
                        .body(DataResponse.<RefactorCodeResponse>builder()
                                .status("error")
                                .message("Unauthorized")
                                .data(null)
                                .build());
            }

            RefactorCodeResponse response = aiService.refactorCode(request);
            return ResponseEntity.ok(DataResponse.<RefactorCodeResponse>builder()
                    .status("success")
                    .message("Refactor thành công")
                    .data(response)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.<RefactorCodeResponse>builder()
                            .status("error")
                            .message("Lỗi khi refactor code: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<DataResponse<String>> chat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        try {
            // Verify user is authenticated
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrinciple)) {
                return ResponseEntity.status(401)
                        .body(DataResponse.<String>builder()
                                .status("error")
                                .message("Unauthorized")
                                .data(null)
                                .build());
            }

            String response = aiService.chatWithContext(request);
            return ResponseEntity.ok(DataResponse.<String>builder()
                    .status("success")
                    .message("Chat thành công")
                    .data(response)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.<String>builder()
                            .status("error")
                            .message("Lỗi khi chat: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        log.info("Received streaming chat request. Auth: {}", authentication != null ? "present" : "null");
        
        SseEmitter emitter = new SseEmitter(60000L); // 60 seconds timeout

        // Verify user is authenticated
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrinciple)) {
            log.warn("Unauthorized streaming chat request. Authentication: {}", authentication);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"Unauthorized - Please login first\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Error sending unauthorized error: {}", e.getMessage());
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        log.info("User authenticated: {}", ((UserPrinciple) authentication.getPrincipal()).getEmail());

        // Run async để không block request
        new Thread(() -> {
            try {
                log.info("Starting AI chat stream for request: {}", request);
                String response = aiService.chatWithContext(request);
                
                log.info("AI response received, length: {}", response != null ? response.length() : 0);
                
                if (response == null || response.isEmpty()) {
                    log.warn("Empty response from AI");
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"error\":\"Empty response from AI\"}"));
                    emitter.complete();
                    return;
                }
                
                // Split response thành chunks để simulate streaming
                // Split by words but keep punctuation attached
                String[] chunks = response.split("(?<=\\s)");
                log.info("Splitting response into {} chunks", chunks.length);
                
                for (int i = 0; i < chunks.length; i++) {
                    String chunk = chunks[i];
                    // Escape JSON special characters
                    String escapedChunk = chunk
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t");
                    
                    String jsonData = "{\"content\":\"" + escapedChunk + "\"}";
                    log.debug("Sending chunk {}/{}: {}", i + 1, chunks.length, chunk.substring(0, Math.min(50, chunk.length())));
                    
                    // Send SSE event with proper format
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(jsonData)
                            .id(String.valueOf(i)));
                    Thread.sleep(30); // Delay để simulate streaming
                }

                log.info("All chunks sent, sending complete event");
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("{\"done\":true}"));
                emitter.complete();

            } catch (Exception e) {
                log.error("Error in streaming chat: {}", e.getMessage(), e);
                try {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    String escapedError = errorMsg
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"");
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"error\":\"" + escapedError + "\"}"));
                } catch (Exception ex) {
                    log.error("Error sending error event: {}", ex.getMessage());
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    @PostMapping("/review")
    public ResponseEntity<DataResponse<ReviewCodeResponse>> reviewCode(
            @Valid @RequestBody ReviewCodeRequest request,
            Authentication authentication
    ) {
        try {
            // Verify user is authenticated
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrinciple)) {
                return ResponseEntity.status(401)
                        .body(DataResponse.<ReviewCodeResponse>builder()
                                .status("error")
                                .message("Unauthorized")
                                .data(null)
                                .build());
            }

            ReviewCodeResponse response = aiService.reviewCode(request);
            return ResponseEntity.ok(DataResponse.<ReviewCodeResponse>builder()
                    .status("success")
                    .message("Code review completed successfully")
                    .data(response)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.<ReviewCodeResponse>builder()
                            .status("error")
                            .message("Error reviewing code: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }
}

