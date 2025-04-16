package com.filmsage.services;

import com.filmsage.models.ChatConversation;
import com.filmsage.models.ChatMessage;
import com.filmsage.models.User;
import com.filmsage.repositories.ChatConversationRepository;
import com.filmsage.repositories.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    
    @Autowired
    public ChatService(ChatConversationRepository conversationRepository, ChatMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }
    
    /**
     * Create a new chat conversation
     */
    public ChatConversation createConversation(String title, User user) {
        ChatConversation conversation = new ChatConversation(title, user);
        return conversationRepository.save(conversation);
    }
    
    /**
     * Add a message to a conversation
     */
    public ChatMessage addMessage(ChatConversation conversation, String content, String sender, ChatMessage.MessageType type) {
        ChatMessage message = new ChatMessage(content, sender, type, conversation);
        conversation.addMessage(message);
        conversationRepository.save(conversation); // Update conversation timestamp
        return messageRepository.save(message);
    }
    
    /**
     * Get all conversations for a user
     */
    public List<ChatConversation> getConversationsForUser(User user) {
        return conversationRepository.findByUserOrderByUpdatedAtDesc(user);
    }
    
    /**
     * Get a specific conversation
     */
    public Optional<ChatConversation> getConversation(Long id) {
        return conversationRepository.findById(id);
    }
    
    /**
     * Delete a conversation
     */
    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }
    
    /**
     * Search conversations
     */
    public List<ChatConversation> searchConversations(User user, String searchTerm) {
        return conversationRepository.findByUserAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(user, searchTerm);
    }
    
    /**
     * Get messages for a conversation with pagination
     */
    public Page<ChatMessage> getMessagesForConversation(ChatConversation conversation, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByConversationOrderByTimestampAsc(conversation, pageable);
    }
    
    /**
     * Get all messages for a conversation
     */
    public List<ChatMessage> getAllMessagesForConversation(ChatConversation conversation) {
        return messageRepository.findByConversationOrderByTimestampAsc(conversation);
    }
    
    /**
     * Search messages in a conversation
     */
    public List<ChatMessage> searchMessages(ChatConversation conversation, String searchTerm) {
        return messageRepository.findByConversationAndContentContainingIgnoreCaseOrderByTimestampAsc(conversation, searchTerm);
    }
    
    /**
     * Convert a message list to the format expected by the Ollama API
     */
    public List<Map<String, String>> convertMessagesToOllamaFormat(List<ChatMessage> messages) {
        List<Map<String, String>> ollamaMessages = new java.util.ArrayList<>();
        
        for (ChatMessage message : messages) {
            Map<String, String> ollamaMessage = new HashMap<>();
            
            if (message.getType() == ChatMessage.MessageType.USER) {
                ollamaMessage.put("role", "user");
            } else {
                ollamaMessage.put("role", "assistant");
            }
            
            ollamaMessage.put("content", message.getContent());
            ollamaMessages.add(ollamaMessage);
        }
        
        return ollamaMessages;
    }
} 