package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.FollowEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.payload.response.FollowResponse;
import com.hcmute.codesphere_server.repository.common.FollowRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public void followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new RuntimeException("Không thể follow chính mình");
        }

        // Kiểm tra đã follow chưa
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new RuntimeException("Đã follow user này rồi");
        }

        UserEntity follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user follower"));
        
        UserEntity followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user followee"));

        // Tạo follow relationship - không dùng builder để tránh lỗi với @EmbeddedId
        FollowEntity follow = new FollowEntity();
        follow.setCreatedAt(Instant.now());
        follow.setFollower(follower);
        follow.setFollowee(followee);
        followRepository.save(follow);
    }

    // Tách notification ra khỏi transaction chính để tránh rollback
    public void sendFollowNotification(Long followeeId, Long followerId, String followerName) {
        // Gửi notification cho followee (người được follow)
        try {
            System.out.println("Sending follow notification: followerId=" + followerId + ", followeeId=" + followeeId + ", followerName=" + followerName);
            notificationService.notifyFollow(
                    followeeId,  // Người được follow sẽ nhận notification
                    followerId,  // Người follow (để hiển thị trong notification)
                    followerName
            );
            System.out.println("Follow notification sent successfully");
        } catch (Exception e) {
            // Log error nhưng không throw để không ảnh hưởng đến follow action
            System.err.println("Error sending follow notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void unfollowUser(Long followerId, Long followeeId) {
        FollowEntity follow = followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .orElseThrow(() -> new RuntimeException("Chưa follow user này"));

        followRepository.delete(follow);
    }

    @Transactional(readOnly = true)
    public boolean checkFollowing(Long followerId, Long followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Transactional(readOnly = true)
    public List<FollowResponse> getFollowers(Long userId) {
        List<FollowEntity> follows = followRepository.findByFolloweeId(userId);
        return follows.stream()
                .map(f -> FollowResponse.builder()
                        .userId(f.getFollower().getId())
                        .username(f.getFollower().getUsername())
                        .avatar(f.getFollower().getAvatar())
                        .followedAt(f.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FollowResponse> getFollowing(Long userId) {
        List<FollowEntity> follows = followRepository.findByFollowerId(userId);
        return follows.stream()
                .map(f -> FollowResponse.builder()
                        .userId(f.getFollowee().getId())
                        .username(f.getFollowee().getUsername())
                        .avatar(f.getFollowee().getAvatar())
                        .followedAt(f.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long getFollowerCount(Long userId) {
        return followRepository.countByFolloweeId(userId);
    }

    @Transactional(readOnly = true)
    public Long getFollowingCount(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    @Transactional(readOnly = true)
    public String getFollowerInfo(Long userId) {
        return userRepository.findById(userId)
                .map(UserEntity::getUsername)
                .orElse(null);
    }
}

