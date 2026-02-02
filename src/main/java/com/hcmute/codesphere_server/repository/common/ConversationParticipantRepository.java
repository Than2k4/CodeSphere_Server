package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.ConversationParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipantEntity, Long> {
    
    @Query("SELECT cp FROM ConversationParticipantEntity cp " +
           "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    Optional<ConversationParticipantEntity> findByConversationIdAndUserId(
            @Param("conversationId") Long conversationId, 
            @Param("userId") Long userId);
    
    @Query("SELECT cp FROM ConversationParticipantEntity cp " +
           "WHERE cp.conversation.id = :conversationId")
    List<ConversationParticipantEntity> findByConversationId(@Param("conversationId") Long conversationId);
    
    @Query("SELECT COUNT(cp) FROM ConversationParticipantEntity cp " +
           "WHERE cp.conversation.id = :conversationId")
    Long countByConversationId(@Param("conversationId") Long conversationId);
}

