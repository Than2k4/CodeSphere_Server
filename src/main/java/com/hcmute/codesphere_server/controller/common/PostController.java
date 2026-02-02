package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.CreatePostRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdatePostRequest;
import com.hcmute.codesphere_server.model.payload.request.VoteRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.PostDetailResponse;
import com.hcmute.codesphere_server.model.payload.response.PostResponse;
import com.hcmute.codesphere_server.model.payload.response.VoteResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.PostService;
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
@RequestMapping("${base.url}/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<DataResponse<PostDetailResponse>> createPost(
            @Valid @RequestBody CreatePostRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            PostDetailResponse post = postService.createPost(request, userId);
            return ResponseEntity.ok(DataResponse.success(post));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<DataResponse<Page<PostResponse>>> getPosts(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean isResolved,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean followedOnly,
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
            
            Page<PostResponse> posts = postService.getPosts(authorId, tag, isResolved, search, followedOnly, userId, pageable);
            return ResponseEntity.ok(DataResponse.success(posts));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<PostDetailResponse>> getPostById(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            Long userId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
                    userId = Long.parseLong(userPrinciple.getUserId());
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            PostDetailResponse post = postService.getPostById(id, userId);
            return ResponseEntity.ok(DataResponse.success(post));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<PostDetailResponse>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            PostDetailResponse post = postService.updatePost(id, request, userId);
            return ResponseEntity.ok(DataResponse.success(post));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deletePost(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            postService.deletePost(id, userId);
            return ResponseEntity.ok(DataResponse.success("Đã xóa bài thảo luận thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<DataResponse<VoteResponse>> votePost(
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
            
            VoteResponse response = postService.toggleVote(id, request, userId);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<DataResponse<PostDetailResponse>> markAsResolved(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            PostDetailResponse post = postService.markAsResolved(id, userId);
            return ResponseEntity.ok(DataResponse.success(post));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}

