package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.FriendRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, Long> {
    
    @Query("SELECT fr FROM FriendRequestEntity fr " +
           "WHERE fr.sender.id = :senderId AND fr.receiver.id = :receiverId")
    Optional<FriendRequestEntity> findBySenderIdAndReceiverId(
            @Param("senderId") Long senderId, 
            @Param("receiverId") Long receiverId);
    
    @Query("SELECT fr FROM FriendRequestEntity fr " +
           "WHERE fr.receiver.id = :userId AND fr.status = 'PENDING'")
    List<FriendRequestEntity> findPendingRequestsByReceiverId(@Param("userId") Long userId);
    
    @Query("SELECT fr FROM FriendRequestEntity fr " +
           "WHERE fr.sender.id = :userId AND fr.status = 'PENDING'")
    List<FriendRequestEntity> findPendingRequestsBySenderId(@Param("userId") Long userId);
    
    @Query("SELECT fr FROM FriendRequestEntity fr " +
           "WHERE (fr.sender.id = :userId OR fr.receiver.id = :userId) AND fr.status = 'ACCEPTED'")
    List<FriendRequestEntity> findAcceptedRequestsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(fr) FROM FriendRequestEntity fr " +
           "WHERE fr.receiver.id = :userId AND fr.status = 'PENDING'")
    Long countPendingRequestsByReceiverId(@Param("userId") Long userId);
}

