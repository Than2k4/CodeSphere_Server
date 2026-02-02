package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.RegisterContestRequest;
import com.hcmute.codesphere_server.model.payload.request.VerifyAccessCodeRequest;
import com.hcmute.codesphere_server.model.payload.response.*;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.common.ContestService;
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
@RequestMapping("${base.url}/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    private Long getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        return Long.parseLong(userPrinciple.getUserId());
    }

    @GetMapping
    public ResponseEntity<DataResponse<Page<ContestResponse>>> getContests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String contestType,
            Authentication authentication) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
            Long userId = getUserId(authentication);
            Page<ContestResponse> contests = contestService.getContests(pageable, isPublic, status, contestType, userId);
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

        try {
            Long userId = getUserId(authentication);
            ContestDetailResponse contest = contestService.getContestById(id, userId);
            return ResponseEntity.ok(DataResponse.success(contest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<DataResponse<String>> registerContest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RegisterContestRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Cần đăng nhập để đăng ký contest"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());

            if (request == null) {
                request = new RegisterContestRequest();
            }

            contestService.registerContest(id, userId, request);
            return ResponseEntity.ok(DataResponse.success("Đăng ký contest thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<DataResponse<String>> startContest(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Cần đăng nhập để bắt đầu contest"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());

            contestService.startContest(id, userId);
            return ResponseEntity.ok(DataResponse.success("Bắt đầu contest thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<DataResponse<String>> finishContest(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Cần đăng nhập để hoàn thành contest"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());

            contestService.finishContest(id, userId);
            return ResponseEntity.ok(DataResponse.success("Hoàn thành contest thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/problems")
    public ResponseEntity<DataResponse<List<ContestProblemResponse>>> getContestProblems(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            Long userId = getUserId(authentication);
            List<ContestProblemResponse> problems = contestService.getContestProblems(id, userId);
            return ResponseEntity.ok(DataResponse.success(problems));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<DataResponse<List<ContestLeaderboardResponse>>> getContestLeaderboard(
            @PathVariable Long id) {

        try {
            List<ContestLeaderboardResponse> leaderboard = contestService.getContestLeaderboard(id);
            return ResponseEntity.ok(DataResponse.success(leaderboard));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<DataResponse<String>> submitToContest(
            @PathVariable Long id,
            @RequestParam Long submissionId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Cần đăng nhập để submit"));
        }

        try {
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            Long userId = Long.parseLong(userPrinciple.getUserId());

            contestService.submitToContest(id, submissionId, userId);
            return ResponseEntity.ok(DataResponse.success("Submit vào contest thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<DataResponse<List<ContestSubmissionResponse>>> getContestSubmissions(
            @PathVariable Long id,
            @RequestParam(required = false) Long problemId,
            Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            List<ContestSubmissionResponse> submissions = contestService.getContestSubmissions(id, problemId, userId);
            return ResponseEntity.ok(DataResponse.success(submissions));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/registrations")
    public ResponseEntity<DataResponse<List<ContestRegistrationResponse>>> getContestRegistrations(
            @PathVariable Long id) {
        try {
            List<ContestRegistrationResponse> registrations = contestService.getContestRegistrations(id);
            return ResponseEntity.ok(DataResponse.success(registrations));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-access-code")
    public ResponseEntity<DataResponse<ContestResponse>> verifyAccessCode(
            @Valid @RequestBody VerifyAccessCodeRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            ContestResponse contest = contestService.verifyAccessCodeAndGetContest(request.getAccessCode(), userId);
            return ResponseEntity.ok(DataResponse.success(contest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

