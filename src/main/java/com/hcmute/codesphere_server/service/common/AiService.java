package com.hcmute.codesphere_server.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmute.codesphere_server.model.payload.request.ChatRequest;
import com.hcmute.codesphere_server.model.payload.request.RefactorCodeRequest;
import com.hcmute.codesphere_server.model.payload.request.ReviewCodeRequest;
import com.hcmute.codesphere_server.model.payload.response.RefactorCodeResponse;
import com.hcmute.codesphere_server.model.payload.response.ReviewCodeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ProblemService problemService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Value("${openai.api.model}")
    private String openaiApiModel;

    @Value("${openai.api.temperature:0.7}")
    private Double temperature;

    @Value("${openai.api.max-tokens:4096}")
    private Integer maxTokens;

    public RefactorCodeResponse refactorCode(RefactorCodeRequest request) {
        try {
            // Lấy thông tin problem để tạo context tốt hơn
            String problemContext = "";
            try {
                var problem = problemService.getProblemById(request.getProblemId());
                problemContext = String.format(
                    "Problem: %s\nDescription: %s\nConstraints: Time limit: %dms, Memory: %dMB",
                    problem.getTitle(),
                    problem.getContent() != null ? problem.getContent().substring(0, Math.min(500, problem.getContent().length())) : "",
                    problem.getTimeLimitMs(),
                    256 // Default memory limit: 256MB
                );
            } catch (Exception e) {
                log.warn("Could not fetch problem context: {}", e.getMessage());
            }

            // Tạo system prompt cho refactoring
            String systemPrompt;
            String userMessage;
            
            // If there are specific suggestions, optimize prompt to refactor only according to those suggestions
            if (request.getSuggestions() != null && !request.getSuggestions().isEmpty()) {
                String suggestionsText = String.join("\n\n", request.getSuggestions());
                systemPrompt = String.format(
                    "You are a professional code refactoring assistant. Your task is to refactor code ONLY according to the following specific suggestions:\n\n" +
                    "Refactoring suggestions:\n%s\n\n" +
                    "Requirements:\n" +
                    "1. Only refactor code sections related to the above suggestions\n" +
                    "2. Keep unrelated code sections unchanged\n" +
                    "3. Follow best practices for %s language\n" +
                    "4. Maintain logic and functionality\n" +
                    "5. Add explanatory comments if needed\n\n" +
                    "Problem context:\n%s\n\n" +
                    "Return only the refactored code, no additional explanations.",
                    suggestionsText,
                    request.getLanguage(),
                    problemContext
                );
                userMessage = String.format(
                    "Please refactor the following code (language: %s) according to the stated suggestions:\n\n```%s\n%s\n```",
                    request.getLanguage(),
                    request.getLanguage(),
                    request.getCode()
                );
            } else {
                // Refactor entire code as before
                systemPrompt = String.format(
                    "You are a professional code refactoring assistant. Your task is to refactor the given code to:\n" +
                    "1. Improve readability and maintainability\n" +
                    "2. Optimize performance if possible\n" +
                    "3. Follow best practices for %s language\n" +
                    "4. Maintain logic and functionality\n" +
                    "5. Add explanatory comments if needed\n\n" +
                    "Problem context:\n%s\n\n" +
                    "Return only the refactored code, no additional explanations.",
                    request.getLanguage(),
                    problemContext
                );
                userMessage = String.format(
                    "Please refactor the following code (language: %s):\n\n```%s\n%s\n```",
                    request.getLanguage(),
                    request.getLanguage(),
                    request.getCode()
                );
            }

            // Gọi OpenAI API
            String refactoredCode = callOpenAIAPI(systemPrompt, userMessage);

            return RefactorCodeResponse.builder()
                    .refactoredCode(refactoredCode)
                    .build();

        } catch (Exception e) {
            log.error("Error refactoring code: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to refactor code: " + e.getMessage(), e);
        }
    }

    public ReviewCodeResponse reviewCode(ReviewCodeRequest request) {
        try {
            // Lấy thông tin problem để tạo context tốt hơn
            String problemContext = "";
            try {
                var problem = problemService.getProblemById(request.getProblemId());
                problemContext = String.format(
                    "Problem: %s\nDescription: %s\nConstraints: Time limit: %dms, Memory: %dMB",
                    problem.getTitle(),
                    problem.getContent() != null ? problem.getContent().substring(0, Math.min(500, problem.getContent().length())) : "",
                    problem.getTimeLimitMs(),
                    256 // Default memory limit: 256MB
                );
            } catch (Exception e) {
                log.warn("Could not fetch problem context: {}", e.getMessage());
            }

            // Create system prompt for code review
            String systemPrompt = String.format(
                "You are a professional code review expert. Your task is to review the given code and provide detailed feedback.\n\n" +
                "IMPORTANT: You MUST return the result in the following format (each item must start with a number and **bold title**):\n\n" +
                "1. **Variable and Function Naming**\n" +
                "[Review content about variable and function naming]\n\n" +
                "2. **Code Structure**\n" +
                "[Review content about code structure]\n\n" +
                "3. **Best Practices**\n" +
                "[Review content about best practices for %s language]\n\n" +
                "4. **Performance**\n" +
                "[Review content about performance, if there are any issues]\n\n" +
                "5. **Strengths**\n" +
                "[Positive aspects of the code]\n\n" +
                "6. **Areas for Improvement**\n" +
                "[Areas that need improvement and specific suggestions]\n\n" +
                "Problem context:\n%s\n\n" +
                "Please provide a detailed, objective, and helpful review. Respond in English. " +
                "Ensure each item starts with a number and **bold title**.",
                request.getLanguage(),
                problemContext
            );

            // Create user message
            String userMessage = String.format(
                "Please review the following code (language: %s) according to the requested format:\n\n```%s\n%s\n```",
                request.getLanguage(),
                request.getLanguage(),
                request.getCode()
            );

            // Gọi OpenAI API
            String review = callOpenAIAPI(systemPrompt, userMessage);

            return ReviewCodeResponse.builder()
                    .review(review)
                    .build();

        } catch (Exception e) {
            log.error("Error reviewing code: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to review code: " + e.getMessage(), e);
        }
    }

    public String chatWithContext(ChatRequest request) {
        try {
            String systemPrompt;
            String userMessage = request.getMessage();

            if ("problem".equals(request.getContext()) && request.getProblemId() != null) {
                // Chat với context của problem
                try {
                    var problem = problemService.getProblemById(request.getProblemId());
                    systemPrompt = String.format(
                        "You are a coding assistant specialized in helping solve competitive programming problems.\n\n" +
                        "Current problem context:\n" +
                        "- Title: %s\n" +
                        "- Description: %s\n" +
                        "- Constraints: Time limit: %dms, Memory: %dMB\n" +
                        "- Language: %s\n" +
                        "- Current code: %s\n\n" +
                        "Please answer the user's questions based on this context. If the user asks about code, analyze the current code.",
                        problem.getTitle(),
                        problem.getContent() != null ? problem.getContent().substring(0, Math.min(1000, problem.getContent().length())) : "",
                        problem.getTimeLimitMs(),
                        256, // Default memory limit: 256MB
                        request.getLanguage() != null ? request.getLanguage() : "N/A",
                        request.getCode() != null ? request.getCode().substring(0, Math.min(500, request.getCode().length())) : "No code yet"
                    );
                } catch (Exception e) {
                    log.warn("Could not fetch problem context: {}", e.getMessage());
                    systemPrompt = "You are a coding assistant specialized in helping solve competitive programming problems.";
                }
            } else {
                // General chat
                systemPrompt = "You are a professional coding assistant. Help the user with questions about programming, algorithms, data structures, and best practices.";
            }

            return callOpenAIAPI(systemPrompt, userMessage);

        } catch (Exception e) {
            log.error("Error in chat: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to process chat: " + e.getMessage(), e);
        }
    }

    private String callOpenAIAPI(String systemPrompt, String userMessage) {
        try {
            log.info("Calling OpenAI API with model: {}", openaiApiModel);
            log.debug("System prompt length: {}, User message length: {}", 
                    systemPrompt != null ? systemPrompt.length() : 0,
                    userMessage != null ? userMessage.length() : 0);
            
            // Validate API key
            if (openaiApiKey == null || openaiApiKey.isEmpty()) {
                log.error("OpenAI API key is not configured!");
                throw new RuntimeException("OpenAI API key is not configured. Please set the OPENAI_API_KEY environment variable");
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openaiApiKey);

            // Build messages array for OpenAI API
            List<Map<String, Object>> messagesList = new ArrayList<>();
            
            // Add system message if provided
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, Object> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messagesList.add(systemMessage);
            }
            
            // Add user message
            Map<String, Object> userMessageMap = new HashMap<>();
            userMessageMap.put("role", "user");
            userMessageMap.put("content", userMessage);
            messagesList.add(userMessageMap);
            
            // Build request body for OpenAI API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openaiApiModel);
            requestBody.put("messages", messagesList);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            log.debug("OpenAI API request body: {}", requestBodyJson);

            HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);

            log.info("Sending request to OpenAI API: {}", openaiApiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    openaiApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("OpenAI API response status: {}", response.getStatusCode());
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("OpenAI API error response: {}", response.getBody());
                throw new RuntimeException("OpenAI API returned error: " + response.getStatusCode() + " - " + response.getBody());
            }
            
            log.debug("OpenAI API response body: {}", response.getBody());

            // Parse response từ OpenAI API
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            // Check for error in response
            if (jsonResponse.has("error")) {
                JsonNode error = jsonResponse.get("error");
                String errorMessage = error.has("message") ? error.get("message").asText() : error.toString();
                log.error("OpenAI API error: {}", errorMessage);
                throw new RuntimeException("OpenAI API error: " + errorMessage);
            }
            
            // Parse OpenAI response format: {choices: [{message: {content: "..."}}]}
            JsonNode choices = jsonResponse.path("choices");
            
            if (choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.path("message");
                if (message.has("content")) {
                    String responseText = message.get("content").asText();
                    log.info("OpenAI API response text length: {}", responseText.length());
                    return responseText;
                }
            }

            log.error("Invalid response from OpenAI API. Response: {}", response.getBody());
            throw new RuntimeException("Invalid response from OpenAI API: " + response.getBody());

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP error calling OpenAI API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            // Try to parse error message from OpenAI API
            String errorMessage = "Error calling OpenAI API";
            try {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && responseBody.contains("error")) {
                    JsonNode errorJson = objectMapper.readTree(responseBody);
                    if (errorJson.has("error")) {
                        JsonNode error = errorJson.get("error");
                        if (error.has("message")) {
                            String apiErrorMessage = error.get("message").asText();
                            
                            // Translate common errors to English
                            if (apiErrorMessage.contains("insufficient_quota") || apiErrorMessage.contains("You exceeded your current quota")) {
                                errorMessage = "OpenAI API account has insufficient credits. Please add credits at https://platform.openai.com/account/billing";
                            } else if (apiErrorMessage.contains("Invalid API key") || apiErrorMessage.contains("Incorrect API key")) {
                                errorMessage = "Invalid API key. Please check the OPENAI_API_KEY environment variable";
                            } else if (apiErrorMessage.contains("rate limit")) {
                                errorMessage = "Rate limit exceeded. Please try again later.";
                            } else {
                                errorMessage = "Error from OpenAI API: " + apiErrorMessage;
                            }
                        }
                    }
                }
            } catch (Exception parseEx) {
                log.warn("Could not parse error response: {}", parseEx.getMessage());
                errorMessage = "Error calling OpenAI API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
            }
            
            throw new RuntimeException(errorMessage);
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("Error calling OpenAI API: " + e.getMessage(), e);
        }
    }
}

