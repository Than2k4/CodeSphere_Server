package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.ProblemDetailResponse;
import com.hcmute.codesphere_server.model.payload.response.ProblemResponse;
import com.hcmute.codesphere_server.model.payload.response.TestCaseResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.ProblemService;
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
@RequestMapping("${base.url}/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    public ResponseEntity<DataResponse<Page<ProblemResponse>>> getProblems(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String bookmarkStatus, // "bookmarked", "not_bookmarked", "all"
            @RequestParam(required = false) String status, // "NOT_ATTEMPTED", "ATTEMPTED_NOT_COMPLETED", "COMPLETED", "all"
            @RequestParam(required = false) String search,
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
            
            // Nếu có filter theo bookmarkStatus hoặc status thì yêu cầu authentication
            if ((bookmarkStatus != null && !bookmarkStatus.isEmpty() && !"all".equalsIgnoreCase(bookmarkStatus)) ||
                (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status))) {
                if (authentication == null || !authentication.isAuthenticated()) {
                    return ResponseEntity.status(401)
                            .body(DataResponse.error("Unauthorized - Cần đăng nhập để sử dụng filter theo bookmark hoặc status"));
                }
            }
            
            // Lấy userId từ authentication nếu có
            Long userId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
                    userId = Long.parseLong(userPrinciple.getUserId());
                } catch (Exception e) {
                    // Nếu không parse được userId, để null
                }
            }
            
            Page<ProblemResponse> problems = problemService.getProblems(
                    level, category, language, 
                    bookmarkStatus, status, search, userId, 
                    pageable);
            
            return ResponseEntity.ok(DataResponse.success(problems));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<ProblemDetailResponse>> getProblemById(
            @PathVariable Long id,
            @RequestParam(required = false) Long contestId,
            Authentication authentication) {
        
        try {
            // Nếu có contestId, validate contest access
            if (contestId != null) {
                Long userId = null;
                if (authentication != null && authentication.isAuthenticated()) {
                    try {
                        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
                        userId = Long.parseLong(userPrinciple.getUserId());
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                problemService.validateContestAccess(contestId, userId);
            }
            
            ProblemDetailResponse problem = problemService.getProblemById(id);
            return ResponseEntity.ok(DataResponse.success(problem));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/sample-testcases")
    public ResponseEntity<DataResponse<List<TestCaseResponse>>> getSampleTestCases(
            @PathVariable Long id) {
        
        try {
            // Lấy sample testcases (isSample = true, isHidden = false) - không cần authentication
            List<TestCaseResponse> testCases = problemService.getSampleTestCases(id);
            return ResponseEntity.ok(DataResponse.success(testCases));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/testcases")
    public ResponseEntity<DataResponse<List<TestCaseResponse>>> getTestCases(
            @PathVariable Long id,
            Authentication authentication) {
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        // Kiểm tra quyền admin - chỉ admin mới được xem test cases
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        boolean isAdmin = userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền xem test cases"));
        }
        
        try {
            // Admin: lấy tất cả test cases (bao gồm hidden)
            List<TestCaseResponse> testCases = problemService.getTestCases(id);
            return ResponseEntity.ok(DataResponse.success(testCases));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

