package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    
    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findUnreadByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") Long userId);
    
    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findByUserIdAndType(@Param("userId") Long userId, 
                                                 @Param("type") NotificationEntity.NotificationType type, 
                                                 Pageable pageable);
    
    @Query("SELECT COUNT(n) > 0 FROM NotificationEntity n WHERE n.user.id = :userId AND n.type = :type AND n.relatedContestId = :contestId AND n.createdAt >= :since")
    boolean existsByUserIdAndTypeAndContestIdSince(@Param("userId") Long userId,
                                                    @Param("type") NotificationEntity.NotificationType type,
                                                    @Param("contestId") Long contestId,
                                                    @Param("since") java.time.Instant since);
}

