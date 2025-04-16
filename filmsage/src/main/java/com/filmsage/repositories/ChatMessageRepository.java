package com.filmsage.repositories;

import com.filmsage.models.ChatMessage;
import com.filmsage.models.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // Find all messages for a specific conversation
    List<ChatMessage> findByConversationOrderByTimestampAsc(ChatConversation conversation);
    
    // Find messages with pagination for a specific conversation
    Page<ChatMessage> findByConversationOrderByTimestampAsc(ChatConversation conversation, Pageable pageable);
    
    // Find messages containing a specific term in a conversation
    List<ChatMessage> findByConversationAndContentContainingIgnoreCaseOrderByTimestampAsc(ChatConversation conversation, String searchTerm);
} 