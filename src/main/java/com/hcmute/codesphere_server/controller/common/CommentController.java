package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.CreateCommentRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateCommentRequest;
import com.hcmute.codesphere_server.model.payload.request.VoteRequest;
import com.hcmute.codesphere_server.model.payload.response.CommentResponse;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.VoteResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<DataResponse<CommentResponse>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            CommentResponse comment = commentService.createComment(postId, request, userId);
            return ResponseEntity.ok(DataResponse.success(comment));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<DataResponse<Page<CommentResponse>>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir,
            Authentication authentication) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Long userId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
                    userId = Long.parseLong(userPrinciple.getUserId());
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            Page<CommentResponse> comments = commentService.getCommentsByPost(postId, userId, pageable);
            return ResponseEntity.ok(DataResponse.success(comments));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/comments/{id}/reply")
    public ResponseEntity<DataResponse<CommentResponse>> replyToComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            CommentResponse reply = commentService.replyToComment(id, request, userId);
            return ResponseEntity.ok(DataResponse.success(reply));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/comments/{id}/vote")
    public ResponseEntity<DataResponse<VoteResponse>> voteComment(
            @PathVariable Long id,
            @Valid @RequestBody VoteRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            VoteResponse response = commentService.toggleCommentVote(id, request, userId);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<DataResponse<CommentResponse>> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            CommentResponse comment = commentService.updateComment(id, request, userId);
            return ResponseEntity.ok(DataResponse.success(comment));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<DataResponse<String>> deleteComment(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            commentService.deleteComment(id, userId);
            return ResponseEntity.ok(DataResponse.success("Đã xóa comment thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // Endpoints for Problem comments
    @PostMapping("/problems/{problemId}/comments")
    public ResponseEntity<DataResponse<CommentResponse>> createProblemComment(
            @PathVariable Long problemId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            CommentResponse comment = commentService.createCommentForProblem(problemId, request, userId);
            return ResponseEntity.ok(DataResponse.success(comment));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/problems/{problemId}/comments")
    public ResponseEntity<DataResponse<Page<CommentResponse>>> getProblemComments(
            @PathVariable Long problemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Long userId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
                    userId = Long.parseLong(userPrinciple.getUserId());
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            Page<CommentResponse> comments = commentService.getCommentsByProblem(problemId, userId, pageable);
            return ResponseEntity.ok(DataResponse.success(comments));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

