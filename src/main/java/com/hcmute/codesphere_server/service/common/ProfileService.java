package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.AccountEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.payload.request.UpdateProfileRequest;
import com.hcmute.codesphere_server.model.payload.response.PostResponse;
import com.hcmute.codesphere_server.model.payload.response.UserProfileResponse;
import com.hcmute.codesphere_server.model.payload.response.UserPublicProfileResponse;
import com.hcmute.codesphere_server.model.payload.response.UserSearchResponse;
import com.hcmute.codesphere_server.repository.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FollowService followService;
    private final PostService postService;
    private final UserProblemBestRepository userProblemBestRepository;
    private final ContestRegistrationRepository contestRegistrationRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Tìm account
        AccountEntity account = accountRepository.findAll().stream()
                .filter(acc -> acc.getUser() != null && acc.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy account"));

        // Tính thống kê: số bài tập đã hoàn thành (AC)
        Long completedProblems = userProblemBestRepository.countSolvedByUserId(userId);
        if (completedProblems == null) {
            completedProblems = 0L;
        }

        // Tính thống kê: số contest đã tham gia
        Long contestsParticipated = (long) contestRegistrationRepository.findByUserId(userId).size();

        return UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(account.getEmail())
                .avatar(user.getAvatar())
                .dob(user.getDob())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .status(user.getStatus())
                .lastOnline(user.getLastOnline())
                .role(account.getRole() != null ? account.getRole().getName() : null)
                .authenWith(account.getAuthenWith())
                .isBlocked(account.getIsBlocked())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .completedProblems(completedProblems)
                .contestsParticipated(contestsParticipated)
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            // Kiểm tra username đã tồn tại chưa (trừ chính mình)
            userRepository.findAll().stream()
                    .filter(u -> u.getUsername() != null && 
                               u.getUsername().equals(request.getUsername()) && 
                               !u.getId().equals(userId))
                    .findFirst()
                    .ifPresent(u -> {
                        throw new RuntimeException("Username đã tồn tại");
                    });
            user.setUsername(request.getUsername());
        }

        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }

        if (request.getPhoneNumber() != null) {
            // Kiểm tra phoneNumber đã tồn tại chưa (trừ chính mình)
            if (!request.getPhoneNumber().isEmpty()) {
                userRepository.findAll().stream()
                        .filter(u -> u.getPhoneNumber() != null && 
                                   u.getPhoneNumber().equals(request.getPhoneNumber()) && 
                                   !u.getId().equals(userId))
                        .findFirst()
                        .ifPresent(u -> {
                            throw new RuntimeException("Số điện thoại đã tồn tại");
                        });
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        return getProfile(userId);
    }

    @Transactional
    public UserProfileResponse uploadAvatar(Long userId, String avatarUrl) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        user.setAvatar(avatarUrl);
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        return getProfile(userId);
    }

    @Transactional(readOnly = true)
    public UserPublicProfileResponse getPublicProfile(Long userId, Long currentUserId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Long postCount = postRepository.countByAuthorId(userId);

        // Đếm số followers và following
        Long followerCount = followService.getFollowerCount(userId);
        Long followingCount = followService.getFollowingCount(userId);

        // Kiểm tra có đang follow không
        Boolean isFollowing = null;
        if (currentUserId != null && !currentUserId.equals(userId)) {
            isFollowing = followService.checkFollowing(currentUserId, userId);
        }

        return UserPublicProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .dob(user.getDob())
                .gender(user.getGender())
                .lastOnline(user.getLastOnline())
                .postCount(postCount)
                .isFollowing(isFollowing)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(Long userId, Long currentUserId, Pageable pageable) {
        return postService.getPosts(userId, null, null, null, null, currentUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UserSearchResponse> searchUsers(String query, Long currentUserId, Pageable pageable) {
        Page<UserEntity> users = userRepository.searchUsers(query, pageable);
        return users.map(user -> UserSearchResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .build());
    }
}

