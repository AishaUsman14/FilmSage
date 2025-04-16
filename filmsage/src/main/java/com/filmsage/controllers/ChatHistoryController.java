package com.filmsage.controllers;

import com.filmsage.models.ChatConversation;
import com.filmsage.models.ChatMessage;
import com.filmsage.models.User;
import com.filmsage.services.ChatService;
import com.filmsage.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat-history")
@CrossOrigin(origins = "*") // Allow requests from any origin
public class ChatHistoryController {
    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryController.class);
    
    private final ChatService chatService;
    private final UserService userService;
    
    @Autowired
    public ChatHistoryController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }
    
    /**
     * Get all conversations for the current user
     */
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            List<ChatConversation> conversations = chatService.getConversationsForUser(currentUser);
            List<Map<String, Object>> conversationData = conversations.stream()
                .map(conv -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", conv.getId());
                    data.put("title", conv.getTitle());
                    data.put("preview", conv.getPreview());
                    data.put("updatedAt", conv.getUpdatedAt().toString());
                    return data;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(conversationData);
        } catch (Exception e) {
            logger.error("Error retrieving conversations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving conversations: " + e.getMessage());
        }
    }
    
    /**
     * Create a new conversation
     */
    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(@RequestBody Map<String, String> data) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            String title = data.getOrDefault("title", "New Conversation");
            ChatConversation conversation = chatService.createConversation(title, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", conversation.getId());
            response.put("title", conversation.getTitle());
            response.put("createdAt", conversation.getCreatedAt().toString());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating conversation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating conversation: " + e.getMessage());
        }
    }
    
    /**
     * Delete a conversation
     */
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<?> deleteConversation(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            Optional<ChatConversation> conversationOpt = chatService.getConversation(id);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Conversation not found");
            }
            
            ChatConversation conversation = conversationOpt.get();
            if (!conversation.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to delete this conversation");
            }
            
            chatService.deleteConversation(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting conversation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting conversation: " + e.getMessage());
        }
    }
    
    /**
     * Get messages for a conversation
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            Optional<ChatConversation> conversationOpt = chatService.getConversation(id);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Conversation not found");
            }
            
            ChatConversation conversation = conversationOpt.get();
            if (!conversation.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to view this conversation");
            }
            
            Page<ChatMessage> messagePage = chatService.getMessagesForConversation(conversation, page, size);
            
            List<Map<String, Object>> messageData = messagePage.getContent().stream()
                .map(msg -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", msg.getId());
                    data.put("content", msg.getContent());
                    data.put("sender", msg.getSender());
                    data.put("type", msg.getType().toString());
                    data.put("timestamp", msg.getTimestamp().toString());
                    return data;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("messages", messageData);
            response.put("currentPage", messagePage.getNumber());
            response.put("totalPages", messagePage.getTotalPages());
            response.put("totalItems", messagePage.getTotalElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving messages: " + e.getMessage());
        }
    }
    
    /**
     * Search conversations
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchConversations(@RequestParam String term) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            List<ChatConversation> conversations = chatService.searchConversations(currentUser, term);
            List<Map<String, Object>> conversationData = conversations.stream()
                .map(conv -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", conv.getId());
                    data.put("title", conv.getTitle());
                    data.put("preview", conv.getPreview());
                    data.put("updatedAt", conv.getUpdatedAt().toString());
                    return data;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(conversationData);
        } catch (Exception e) {
            logger.error("Error searching conversations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error searching conversations: " + e.getMessage());
        }
    }
    
    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String username = authentication.getName();
        return userService.findByUsername(username).orElse(null);
    }
} 