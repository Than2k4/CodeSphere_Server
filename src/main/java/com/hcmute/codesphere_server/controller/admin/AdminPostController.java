package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.entity.PostEntity;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminPostService;

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        return userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public ResponseEntity<DataResponse<Page<PostEntity>>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String isBlocked,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<PostEntity> posts = adminPostService.getPosts(pageable, search, isBlocked);
            return ResponseEntity.ok(DataResponse.success(posts));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deletePost(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            adminPostService.deletePost(id);
            return ResponseEntity.ok(DataResponse.success("Xóa post thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-block")
    public ResponseEntity<DataResponse<String>> toggleBlock(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            adminPostService.toggleBlock(id);
            return ResponseEntity.ok(DataResponse.success("Thay đổi trạng thái block thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

