package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.entity.ContestRegistrationEntity;
import com.hcmute.codesphere_server.model.payload.request.ContestProblemRequest;
import com.hcmute.codesphere_server.model.payload.request.CreateContestRequest;
import com.hcmute.codesphere_server.model.payload.response.*;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminContestService;
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
@RequestMapping("${base.url}/admin/contests")
@RequiredArgsConstructor
public class AdminContestController {

    private final AdminContestService adminContestService;

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
                .map(auth -> auth.getAuthority())
                .orElse("UNKNOWN");
    }

    @GetMapping
    public ResponseEntity<DataResponse<Page<ContestResponse>>> getContests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ContestResponse> contests = adminContestService.getContests(pageable, search, type);
            return ResponseEntity.ok(DataResponse.success(contests));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<ContestDetailResponse>> getContestById(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            ContestDetailResponse contest = adminContestService.getContestById(id);
            return ResponseEntity.ok(DataResponse.success(contest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<DataResponse<ContestDetailResponse>> createContest(
            @Valid @RequestBody CreateContestRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long authorId = Long.parseLong(userPrinciple.getUserId());

                ContestDetailResponse contest = adminContestService.createContest(
                    request,
                    authorId,
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
                );
            return ResponseEntity.ok(DataResponse.success(contest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<ContestDetailResponse>> updateContest(
            @PathVariable Long id,
            @Valid @RequestBody CreateContestRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long authorId = Long.parseLong(userPrinciple.getUserId());

                ContestDetailResponse contest = adminContestService.updateContest(
                    id,
                    request,
                    authorId,
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
                );
            return ResponseEntity.ok(DataResponse.success(contest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deleteContest(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            adminContestService.deleteContest(
                    id,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success("Xóa contest thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/problems")
    public ResponseEntity<DataResponse<String>> addProblemToContest(
            @PathVariable Long id,
            @Valid @RequestBody ContestProblemRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            adminContestService.addProblemToContest(
                    id,
                    request,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success("Thêm problem vào contest thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/problems/{problemId}")
    public ResponseEntity<DataResponse<String>> removeProblemFromContest(
            @PathVariable Long id,
            @PathVariable Long problemId,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            adminContestService.removeProblemFromContest(
                    id,
                    problemId,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success("Xóa problem khỏi contest thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/registrations")
    public ResponseEntity<DataResponse<List<ContestRegistrationEntity>>> getContestRegistrations(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            List<ContestRegistrationEntity> registrations = adminContestService.getContestRegistrations(id);
            return ResponseEntity.ok(DataResponse.success(registrations));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-visibility")
    public ResponseEntity<DataResponse<String>> toggleContestVisibility(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            adminContestService.toggleContestVisibility(
                    id,
                    Long.parseLong(userPrinciple.getUserId()),
                    userPrinciple.getEmail(),
                    getActorRole(authentication),
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(DataResponse.success("Đã cập nhật trạng thái ẩn/hiện contest"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

