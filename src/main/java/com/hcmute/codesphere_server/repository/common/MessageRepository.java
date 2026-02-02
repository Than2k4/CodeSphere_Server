package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    
    @Query("SELECT m FROM MessageEntity m " +
           "WHERE m.conversation.id = :conversationId AND m.isDeleted = false")
    Page<MessageEntity> findByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);
    
    @Query("SELECT m FROM MessageEntity m " +
           "WHERE m.id = :id AND m.isDeleted = false")
    Optional<MessageEntity> findByIdAndNotDeleted(@Param("id") Long id);
    
    @Query("SELECT COUNT(m) FROM MessageEntity m " +
           "WHERE m.conversation.id = :conversationId AND m.isDeleted = false")
    Long countByConversationId(@Param("conversationId") Long conversationId);
    
    @Query("SELECT m FROM MessageEntity m " +
           "WHERE m.conversation.id = :conversationId AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    List<MessageEntity> findAllByConversationId(@Param("conversationId") Long conversationId);
}

