package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.entity.PostEntity;
import com.hcmute.codesphere_server.model.payload.request.ModeratePostRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.ModerationReasonTemplateResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminPostService;
import jakarta.servlet.http.HttpServletRequest;
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

    private String getActorRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "UNKNOWN";
        }
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        return userPrinciple.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("UNKNOWN");
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

    @GetMapping("/report-queue")
    public ResponseEntity<DataResponse<Page<PostEntity>>> getReportQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(Sort.Order.desc("reportCount"), Sort.Order.desc("lastReportedAt"))
            );
            Page<PostEntity> queue = adminPostService.getReportQueue(pageable);
            return ResponseEntity.ok(DataResponse.success(queue));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/moderation-reasons")
    public ResponseEntity<DataResponse<List<ModerationReasonTemplateResponse>>> getModerationReasons(
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        return ResponseEntity.ok(DataResponse.success(adminPostService.getReasonTemplates()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deletePost(
            @PathVariable Long id,
            @Valid @RequestBody ModeratePostRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long moderatorId = Long.parseLong(userPrinciple.getUserId());
                adminPostService.deletePost(
                    id,
                    moderatorId,
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    request,
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
                );
            return ResponseEntity.ok(DataResponse.success("Xóa post thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-block")
    public ResponseEntity<DataResponse<String>> toggleBlock(
            @PathVariable Long id,
            @Valid @RequestBody ModeratePostRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long moderatorId = Long.parseLong(userPrinciple.getUserId());
                adminPostService.toggleBlock(
                    id,
                    moderatorId,
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    request,
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
                );
            return ResponseEntity.ok(DataResponse.success("Thay đổi trạng thái block thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

