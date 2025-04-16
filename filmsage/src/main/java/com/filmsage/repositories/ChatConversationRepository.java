package com.filmsage.repositories;

import com.filmsage.models.ChatConversation;
import com.filmsage.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    // Find all conversations for a specific user
    List<ChatConversation> findByUserOrderByUpdatedAtDesc(User user);
    
    // Find conversations by user and with a title containing the search term
    List<ChatConversation> findByUserAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(User user, String searchTerm);
} 