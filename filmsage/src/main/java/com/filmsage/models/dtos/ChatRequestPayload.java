package com.filmsage.models.dtos;

import java.util.List;
import java.util.Map;

// DTO to represent the incoming chat request payload
public class ChatRequestPayload {
    private String message;
    private List<Map<String, String>> history; // Represents the chat history array
    private Long conversationId; // Assuming conversationId can be null or a number

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Map<String, String>> getHistory() {
        return history;
    }

    public void setHistory(List<Map<String, String>> history) {
        this.history = history;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
} 