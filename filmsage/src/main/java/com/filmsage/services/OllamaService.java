package com.filmsage.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;

@Service
public class OllamaService {
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    
    private final RestTemplate restTemplate;
    private final String ollamaApiUrl;
    private final String modelName;
    private final int contextLength;
    private final double temperature;
    private final int maxTokens;
    private final String stopToken;
    
    private boolean ollamaAvailable = false;
    private boolean modelAvailable = false;
    private List<String> availableModels = new ArrayList<>();
    
    public OllamaService(
            RestTemplate restTemplate,
            @Value("${ollama.api.url}") String ollamaApiUrl,
            @Value("${ollama.model.name}") String modelName,
            @Value("${ollama.context.length:8192}") int contextLength,
            @Value("${ollama.temperature:0.9}") double temperature,
            @Value("${ollama.max.tokens:1024}") int maxTokens,
            @Value("${ollama.stop.token:}") String stopToken) {
        this.restTemplate = restTemplate;
        this.ollamaApiUrl = ollamaApiUrl;
        this.modelName = modelName;
        this.contextLength = contextLength;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stopToken = stopToken;
        
        logger.info("OllamaService initialized with URL: {}, model: {}, context: {}, temp: {}, maxTokens: {}, stop: '{}'", 
                    ollamaApiUrl, modelName, contextLength, temperature, maxTokens, stopToken);
    }
    
    @PostConstruct
    public void checkOllamaStatus() {
        try {
            String tagsUrl = ollamaApiUrl + "/api/tags";
            logger.info("Checking Ollama status and available models at: {}", tagsUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(tagsUrl, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ollamaAvailable = true;
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode modelsNode = root.get("models");
                availableModels.clear();
                if (modelsNode != null && modelsNode.isArray()) {
                    // Correct way to iterate over JsonNode array elements
                    StreamSupport.stream(modelsNode.spliterator(), false)
                                 .map(node -> node.get("name").asText())
                                 .forEach(availableModels::add);
                }
                logger.info("Ollama connection successful. Available models: {}", availableModels);
                
                // Check if the configured model is in the list
                String configuredModelBaseName = modelName.contains(":") ? modelName.substring(0, modelName.indexOf(':')) : modelName;
                modelAvailable = availableModels.stream()
                                                .anyMatch(available -> available.startsWith(configuredModelBaseName + ":")); // Match base name
                
                if (modelAvailable) {
                    logger.info("Configured model '{}' is available in Ollama.", modelName);
                } else {
                    logger.warn("Configured model '{}' NOT FOUND in Ollama's available models: {}. Please ensure the model is pulled and Ollama is serving it.", modelName, availableModels);
                }
            } else {
                logger.error("Failed to connect to Ollama at {}. Status code: {}", tagsUrl, response.getStatusCode());
                ollamaAvailable = false;
            }
        } catch (Exception e) {
            logger.error("Error checking Ollama status at {}: {}. Please ensure Ollama server is running.", ollamaApiUrl, e.getMessage());
            ollamaAvailable = false;
        }
    }
    
    /**
     * Send a message to Ollama chat API and get a response
     */
    public String getChatResponse(String userMessage, List<Map<String, String>> history) {
        try {
            // Create the request body for Ollama
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBody = mapper.createObjectNode();
            
            // Configure for non-streaming response (faster overall processing)
            requestBody.put("stream", false);
            requestBody.put("model", modelName);
            
            // Add model parameters using injected values
            ObjectNode options = mapper.createObjectNode();
            options.put("temperature", this.temperature);
            options.put("num_predict", this.maxTokens);
            if (this.stopToken != null && !this.stopToken.isEmpty()) {
                ArrayNode stopArray = mapper.createArrayNode();
                stopArray.add(this.stopToken);
                options.set("stop", stopArray);
            }
            requestBody.set("options", options);
            
            // Add messages
            ArrayNode messages = mapper.createArrayNode();
            for (Map<String, String> message : history) {
                ObjectNode msgNode = mapper.createObjectNode();
                msgNode.put("role", message.get("role"));
                msgNode.put("content", message.get("content"));
                messages.add(msgNode);
            }
            
            // Add the current user message
            ObjectNode userMessageNode = mapper.createObjectNode();
            userMessageNode.put("role", "user");
            userMessageNode.put("content", userMessage);
            messages.add(userMessageNode);
            
            requestBody.set("messages", messages);
            
            String fullUrl = ollamaApiUrl + "/api/chat";
            
            // Log the request being sent with more details
            logger.info("Sending request to Ollama at: {}", fullUrl);
            // Safely log the request body, truncating if necessary
            String requestBodyJson = mapper.writeValueAsString(requestBody);
            logger.info("Request body: {}", requestBodyJson.substring(0, Math.min(requestBodyJson.length(), 200)) + (requestBodyJson.length() > 200 ? "..." : ""));
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create the request entity
            HttpEntity<String> request = new HttpEntity<>(mapper.writeValueAsString(requestBody), headers);
            
            // Send the request to Ollama - correct path is /api/chat
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(
                        fullUrl, 
                        request, 
                        String.class);
                
                // Parse the response to extract the message
                JsonNode responseJson = mapper.readTree(response.getBody());
                return responseJson.get("message").get("content").asText();
            } catch (Exception e) {
                logger.error("Error during Ollama chat request: {}", e.getMessage());
                // Provide more specific error messages based on startup check
                if (!ollamaAvailable) {
                    return "I'm sorry, I cannot connect to the Ollama service right now. Please ensure it's running.";
                } else if (!modelAvailable) {
                    return String.format("I'm sorry, the configured model '%s' seems to be unavailable in Ollama. Please check the server.", modelName);
                } else {
                    // General error if Ollama was available but chat failed
                    return "I encountered an error while trying to generate a response. Please try again.";
                }
            }
        } catch (Exception e) {
            logger.error("Error preparing request for Ollama: {}", e.getMessage(), e);
            return "I'm sorry, I'm having trouble processing your request right now. Please try again later.";
        }
    }
    
    /**
     * Create a system prompt for the chat
     */
    public Map<String, String> createSystemPrompt(String systemPromptText) {
        Map<String, String> systemPrompt = new HashMap<>();
        systemPrompt.put("role", "system");
        systemPrompt.put("content", systemPromptText);
        return systemPrompt;
    }
} 