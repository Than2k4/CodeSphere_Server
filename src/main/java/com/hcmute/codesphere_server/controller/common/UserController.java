package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.UpdateProfileRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.PostResponse;
import com.hcmute.codesphere_server.model.payload.response.UserProfileResponse;
import com.hcmute.codesphere_server.model.payload.response.UserPublicProfileResponse;
import com.hcmute.codesphere_server.model.payload.response.UserSearchResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.security.config.Cloudinary.CloudinaryService;
import com.hcmute.codesphere_server.service.common.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("${base.url}/users")
@RequiredArgsConstructor
public class UserController {

    private final ProfileService profileService;
    private final CloudinaryService cloudinaryService;

    @GetMapping("/me/profile")
    public ResponseEntity<DataResponse<UserProfileResponse>> getMyProfile(
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            UserProfileResponse profile = profileService.getProfile(userId);
            return ResponseEntity.ok(DataResponse.success(profile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<DataResponse<UserPublicProfileResponse>> getProfile(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            Long currentUserId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
                    currentUserId = Long.parseLong(userPrinciple.getUserId());
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            UserPublicProfileResponse profile = profileService.getPublicProfile(id, currentUserId);
            return ResponseEntity.ok(DataResponse.success(profile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/me/profile")
    public ResponseEntity<DataResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            UserProfileResponse profile = profileService.updateProfile(userId, request);
            return ResponseEntity.ok(DataResponse.success(profile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<DataResponse<UserProfileResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());
            
            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.upload(file);
            String avatarUrl = (String) uploadResult.get("url");
            
            UserProfileResponse profile = profileService.uploadAvatar(userId, avatarUrl);
            return ResponseEntity.ok(DataResponse.success(profile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<DataResponse<Page<PostResponse>>> getUserPosts(
            @PathVariable Long id,
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
            
            Long currentUserId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
                    currentUserId = Long.parseLong(userPrinciple.getUserId());
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            Page<PostResponse> posts = profileService.getUserPosts(id, currentUserId, pageable);
            return ResponseEntity.ok(DataResponse.success(posts));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<DataResponse<Page<UserSearchResponse>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            Long currentUserId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
                    currentUserId = Long.parseLong(userPrinciple.getUserId());
                } catch (Exception e) {
                    // Ignore
                }
            }

            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(DataResponse.error("Query không được để trống"));
            }

            Sort sort = Sort.by("username").ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<UserSearchResponse> users = profileService.searchUsers(query.trim(), currentUserId, pageable);
            return ResponseEntity.ok(DataResponse.success(users));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DataResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}

