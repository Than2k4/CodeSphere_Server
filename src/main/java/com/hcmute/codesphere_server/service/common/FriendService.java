package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.FriendRequestEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.enums.FriendRequestStatus;
import com.hcmute.codesphere_server.model.payload.request.SendFriendRequestRequest;
import com.hcmute.codesphere_server.model.payload.response.FriendRequestResponse;
import com.hcmute.codesphere_server.model.payload.response.FriendResponse;
import com.hcmute.codesphere_server.repository.common.FriendRequestRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public FriendRequestResponse sendFriendRequest(SendFriendRequestRequest request, Long senderId) {
        if (senderId.equals(request.getReceiverId())) {
            throw new RuntimeException("Không thể gửi lời mời kết bạn cho chính mình");
        }

        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user gửi"));

        UserEntity receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user nhận"));

        // Kiểm tra đã có request chưa
        var existingRequest = friendRequestRepository.findBySenderIdAndReceiverId(senderId, request.getReceiverId());
        if (existingRequest.isPresent()) {
            FriendRequestEntity requestEntity = existingRequest.get();
            if (requestEntity.getStatus() == FriendRequestStatus.PENDING) {
                throw new RuntimeException("Đã gửi lời mời kết bạn rồi");
            } else if (requestEntity.getStatus() == FriendRequestStatus.ACCEPTED) {
                throw new RuntimeException("Đã là bạn bè rồi");
            }
        }

        // Kiểm tra request ngược lại
        var reverseRequest = friendRequestRepository.findBySenderIdAndReceiverId(request.getReceiverId(), senderId);
        if (reverseRequest.isPresent()) {
            FriendRequestEntity reverseEntity = reverseRequest.get();
            if (reverseEntity.getStatus() == FriendRequestStatus.PENDING) {
                // Nếu có request ngược lại đang pending, tự động accept
                reverseEntity.setStatus(FriendRequestStatus.ACCEPTED);
                reverseEntity.setUpdatedAt(Instant.now());
                friendRequestRepository.save(reverseEntity);
                
                // Tạo request mới với status ACCEPTED
                FriendRequestEntity newRequest = FriendRequestEntity.builder()
                        .sender(sender)
                        .receiver(receiver)
                        .status(FriendRequestStatus.ACCEPTED)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                newRequest = friendRequestRepository.save(newRequest);
                return mapToFriendRequestResponse(newRequest);
            } else if (reverseEntity.getStatus() == FriendRequestStatus.ACCEPTED) {
                throw new RuntimeException("Đã là bạn bè rồi");
            }
        }

        // Tạo request mới
        FriendRequestEntity friendRequest = FriendRequestEntity.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        friendRequest = friendRequestRepository.save(friendRequest);

        // Gửi notification
        try {
            notificationService.notifyFriendRequest(
                    request.getReceiverId(),
                    senderId,
                    sender.getUsername()
            );
        } catch (Exception e) {
            // Log error nhưng không throw
        }

        return mapToFriendRequestResponse(friendRequest);
    }

    @Transactional
    public FriendRequestResponse acceptFriendRequest(Long requestId, Long userId) {
        FriendRequestEntity request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lời mời kết bạn"));

        // Kiểm tra quyền
        if (!request.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chấp nhận lời mời này");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Lời mời này không còn ở trạng thái PENDING");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setUpdatedAt(Instant.now());
        request = friendRequestRepository.save(request);

        // Tạo request ngược lại với status ACCEPTED để dễ query
        var reverseRequest = friendRequestRepository.findBySenderIdAndReceiverId(
                request.getReceiver().getId(), 
                request.getSender().getId());
        
        if (reverseRequest.isEmpty()) {
            FriendRequestEntity newReverseRequest = FriendRequestEntity.builder()
                    .sender(request.getReceiver())
                    .receiver(request.getSender())
                    .status(FriendRequestStatus.ACCEPTED)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            friendRequestRepository.save(newReverseRequest);
        }

        // Gửi notification cho sender
        try {
            notificationService.notifyFriendAccepted(
                    request.getSender().getId(),
                    userId,
                    request.getReceiver().getUsername()
            );
        } catch (Exception e) {
            // Log error nhưng không throw
        }

        return mapToFriendRequestResponse(request);
    }

    @Transactional
    public FriendRequestResponse rejectFriendRequest(Long requestId, Long userId) {
        FriendRequestEntity request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lời mời kết bạn"));

        // Kiểm tra quyền
        if (!request.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền từ chối lời mời này");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Lời mời này không còn ở trạng thái PENDING");
        }

        request.setStatus(FriendRequestStatus.REJECTED);
        request.setUpdatedAt(Instant.now());
        request = friendRequestRepository.save(request);

        return mapToFriendRequestResponse(request);
    }

    @Transactional
    public void cancelFriendRequest(Long requestId, Long userId) {
        FriendRequestEntity request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lời mời kết bạn"));

        // Kiểm tra quyền
        if (!request.getSender().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy lời mời này");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy lời mời đang ở trạng thái PENDING");
        }

        friendRequestRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getFriendRequests(Long userId, String type) {
        List<FriendRequestEntity> requests;
        
        if ("sent".equalsIgnoreCase(type)) {
            requests = friendRequestRepository.findPendingRequestsBySenderId(userId);
        } else if ("received".equalsIgnoreCase(type)) {
            requests = friendRequestRepository.findPendingRequestsByReceiverId(userId);
        } else {
            // Lấy cả sent và received
            List<FriendRequestEntity> sent = friendRequestRepository.findPendingRequestsBySenderId(userId);
            List<FriendRequestEntity> received = friendRequestRepository.findPendingRequestsByReceiverId(userId);
            requests = new java.util.ArrayList<>();
            requests.addAll(sent);
            requests.addAll(received);
        }

        return requests.stream()
                .map(this::mapToFriendRequestResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(Long userId) {
        List<FriendRequestEntity> acceptedRequests = friendRequestRepository.findAcceptedRequestsByUserId(userId);
        
        return acceptedRequests.stream()
                .map(request -> {
                    // Xác định friend là sender hay receiver
                    UserEntity friend = request.getSender().getId().equals(userId) 
                            ? request.getReceiver() 
                            : request.getSender();
                    
                    return FriendResponse.builder()
                            .userId(friend.getId())
                            .username(friend.getUsername())
                            .avatar(friend.getAvatar())
                            .friendSince(request.getUpdatedAt()) // Thời điểm accept
                            .build();
                })
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public void unfriend(Long friendId, Long userId) {
        // Xóa cả 2 chiều của friend request
        var request1 = friendRequestRepository.findBySenderIdAndReceiverId(userId, friendId);
        var request2 = friendRequestRepository.findBySenderIdAndReceiverId(friendId, userId);

        if (request1.isPresent()) {
            FriendRequestEntity req = request1.get();
            if (req.getStatus() == FriendRequestStatus.ACCEPTED) {
                friendRequestRepository.delete(req);
            }
        }

        if (request2.isPresent()) {
            FriendRequestEntity req = request2.get();
            if (req.getStatus() == FriendRequestStatus.ACCEPTED) {
                friendRequestRepository.delete(req);
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean checkFriendship(Long userId1, Long userId2) {
        var request1 = friendRequestRepository.findBySenderIdAndReceiverId(userId1, userId2);
        var request2 = friendRequestRepository.findBySenderIdAndReceiverId(userId2, userId1);
        
        return (request1.isPresent() && request1.get().getStatus() == FriendRequestStatus.ACCEPTED) ||
               (request2.isPresent() && request2.get().getStatus() == FriendRequestStatus.ACCEPTED);
    }

    private FriendRequestResponse mapToFriendRequestResponse(FriendRequestEntity entity) {
        return FriendRequestResponse.builder()
                .id(entity.getId())
                .senderId(entity.getSender() != null ? entity.getSender().getId() : null)
                .senderName(entity.getSender() != null ? entity.getSender().getUsername() : null)
                .senderAvatar(entity.getSender() != null ? entity.getSender().getAvatar() : null)
                .receiverId(entity.getReceiver() != null ? entity.getReceiver().getId() : null)
                .receiverName(entity.getReceiver() != null ? entity.getReceiver().getUsername() : null)
                .receiverAvatar(entity.getReceiver() != null ? entity.getReceiver().getAvatar() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

