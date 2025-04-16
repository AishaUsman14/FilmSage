package com.filmsage.models;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private String sender;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING)
    private MessageType type;
    
    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private ChatConversation conversation;

    public enum MessageType {
        USER,
        BOT
    }

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String content, String sender, MessageType type) {
        this.content = content;
        this.sender = sender;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatMessage(String content, String sender, MessageType type, ChatConversation conversation) {
        this.content = content;
        this.sender = sender;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.conversation = conversation;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
    
    public ChatConversation getConversation() {
        return conversation;
    }
    
    public void setConversation(ChatConversation conversation) {
        this.conversation = conversation;
    }
} 