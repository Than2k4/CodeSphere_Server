package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.CreateSubmissionRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.ProblemSolutionResponse;
import com.hcmute.codesphere_server.model.payload.response.SubmissionDetailResponse;
import com.hcmute.codesphere_server.model.payload.response.SubmissionResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${base.url}/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<DataResponse<SubmissionDetailResponse>> createSubmission(
            @Valid @RequestBody CreateSubmissionRequest request,
            @RequestParam(required = false) Long contestId,
            Authentication authentication) {
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            SubmissionDetailResponse submission = submissionService.createSubmission(request, userId, contestId);
            return ResponseEntity.ok(DataResponse.success(submission));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<DataResponse<Page<SubmissionResponse>>> getSubmissions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<SubmissionResponse> submissions = submissionService.getSubmissions(
                    userId, problemId, search, status, pageable);
            
            return ResponseEntity.ok(DataResponse.success(submissions));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<DataResponse<SubmissionDetailResponse>> getSubmissionById(
            @PathVariable Long id) {
        
        try {
            SubmissionDetailResponse submission = submissionService.getSubmissionById(id);
            return ResponseEntity.ok(DataResponse.success(submission));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<DataResponse<Page<SubmissionResponse>>> getMySubmissions(
            Authentication authentication,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Sử dụng getSubmissions với userId từ token để đảm bảo user chỉ xem được submissions của mình
            Page<SubmissionResponse> submissions = submissionService.getSubmissions(
                    userId, problemId, search, status, pageable);
            return ResponseEntity.ok(DataResponse.success(submissions));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/solutions")
    public ResponseEntity<DataResponse<List<ProblemSolutionResponse>>> getProblemSolutions(
            Authentication authentication,
            @RequestParam Long problemId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String languageCode,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer limit) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());

            List<ProblemSolutionResponse> solutions = submissionService.getProblemSolutions(
                    problemId,
                    userId,
                    search,
                    languageCode,
                    offset == null ? 0 : offset,
                    limit == null ? 10 : limit
            );

            return ResponseEntity.ok(DataResponse.success(solutions));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

