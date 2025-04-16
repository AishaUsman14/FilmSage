package com.filmsage.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_conversations")
public class ChatConversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();
    
    // Constructor
    public ChatConversation() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Constructor with title
    public ChatConversation(String title, User user) {
        this.title = title;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Get a preview of the conversation (first 50 characters of first message)
    public String getPreview() {
        if (messages == null || messages.isEmpty()) {
            return "No messages";
        }
        
        String firstMessageContent = messages.get(0).getContent();
        if (firstMessageContent.length() > 50) {
            return firstMessageContent.substring(0, 50) + "...";
        }
        return firstMessageContent;
    }
    
    // Calculate a title based on the conversation content
    public void generateTitle() {
        if (messages == null || messages.isEmpty()) {
            this.title = "New Conversation";
            return;
        }
        
        // Find first user message
        for (ChatMessage message : messages) {
            if (message.getType() == ChatMessage.MessageType.USER) {
                String content = message.getContent();
                // Use the first 30 characters of the user's first message as title
                this.title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
                return;
            }
        }
        
        this.title = "Conversation " + createdAt.toString();
    }
    
    // Add a message to the conversation
    public void addMessage(ChatMessage message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
} 