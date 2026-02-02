package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    
    @Query("SELECT DISTINCT c FROM ConversationEntity c " +
           "JOIN ConversationParticipantEntity cp ON cp.conversation.id = c.id " +
           "WHERE cp.user.id = :userId " +
           "ORDER BY c.updatedAt DESC")
    List<ConversationEntity> findByParticipantId(@Param("userId") Long userId);
    
    @Query("SELECT c FROM ConversationEntity c " +
           "JOIN ConversationParticipantEntity cp ON cp.conversation.id = c.id " +
           "WHERE c.id = :id AND cp.user.id = :userId")
    Optional<ConversationEntity> findByIdAndParticipantId(@Param("id") Long id, @Param("userId") Long userId);
    
    @Query("SELECT c FROM ConversationEntity c " +
           "WHERE c.type = 'DIRECT' " +
           "AND EXISTS (SELECT 1 FROM ConversationParticipantEntity cp1 WHERE cp1.conversation.id = c.id AND cp1.user.id = :userId1) " +
           "AND EXISTS (SELECT 1 FROM ConversationParticipantEntity cp2 WHERE cp2.conversation.id = c.id AND cp2.user.id = :userId2) " +
           "AND (SELECT COUNT(cp) FROM ConversationParticipantEntity cp WHERE cp.conversation.id = c.id) = 2")
    Optional<ConversationEntity> findDirectConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}

