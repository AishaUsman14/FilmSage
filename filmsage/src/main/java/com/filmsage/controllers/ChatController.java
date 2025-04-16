package com.filmsage.controllers;

import com.filmsage.models.Movie;
import com.filmsage.services.MovieService;
import com.filmsage.services.OllamaService;
import com.filmsage.services.ResponseFormatter;
import com.filmsage.models.ChatConversation;
import com.filmsage.models.ChatMessage;
import com.filmsage.models.User;
// import com.filmsage.models.ChatSession; // Commented out
import com.filmsage.services.ChatService;
import com.filmsage.services.UserService;
// import com.filmsage.services.ChatSessionService; // Commented out
import com.filmsage.models.dtos.ChatRequestPayload; // Import the new DTO
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Allow requests from any origin
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final OllamaService ollamaService;
    private final MovieService movieService;
    private final ResponseFormatter responseFormatter;
    private final ChatService chatService;
    private final UserService userService;
    // private final ChatSessionService chatSessionService; // Commented out
    
    @Autowired
    public ChatController(OllamaService ollamaService, MovieService movieService, ResponseFormatter responseFormatter, 
                          ChatService chatService, UserService userService /*, ChatSessionService chatSessionService*/) { // Commented out
        this.ollamaService = ollamaService;
        this.movieService = movieService;
        this.responseFormatter = responseFormatter;
        this.chatService = chatService;
        this.userService = userService;
        // this.chatSessionService = chatSessionService; // Commented out
        logger.info("ChatController initialized with services: ollamaService={}, movieService={}, responseFormatter={}, chatService={}, userService={}" /*, chatSessionService={}*/, // Commented out
                    ollamaService != null ? "available" : "null", 
                    movieService != null ? "available" : "null",
                    responseFormatter != null ? "available" : "null",
                    chatService != null ? "available" : "null",
                    userService != null ? "available" : "null");
    }
    
    @PostMapping("/chat")
    // @PreAuthorize("hasRole('USER')") // Temporarily comment out auth for testing
    public ResponseEntity<?> processMessage(@RequestBody ChatRequestPayload payload, Principal principal) { // Use DTO
        String message = payload.getMessage(); // Get message from DTO
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message cannot be empty");
        }

        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userId = user.getId().toString();
        
        // Check if message is a direct movie search query
        if (isDirectMovieSearchQuery(message)) {
            Map<String, String> response = handleDirectMovieSearch(message, userId);
            return ResponseEntity.ok(response);
        }

        // Get or create a chat session
        // ChatSession chatSession = chatSessionService.getOrCreateSession(userId); // Commented out
        
        // Add user message to chat session
        // chatSessionService.addMessage(chatSession.getId(), "user", message); // Commented out
        
        // Check if message contains keyword for trailer
        // if (containsTrailerKeyword(message)) { // Temporarily comment out trailer handling dependent on session
        //     return handleTrailerRequest(message, userId, chatSession); // Commented out
        // }

        try {
            // Enhance prompt without session context for now
            // String enhancedPrompt = chatService.enhancePromptWithMovieContext(message, chatSession); // Commented out
            // String enhancedPrompt = message; // Use original message for now
            // logger.info("Enhanced prompt for LLM (session context skipped): {}", enhancedPrompt);
            
            // --- Correctly build message list --- 
            List<Map<String, String>> messagesToSend = new ArrayList<>();
            
            // 1. Add the system prompt
            messagesToSend.add(ollamaService.createSystemPrompt(getSystemPrompt()));
            
            // 2. Add history from the payload (if any)
            if (payload.getHistory() != null) {
                messagesToSend.addAll(payload.getHistory());
            }
            
            // 3. Add the current user message (already handled by getChatResponse)
            // We pass the user message separately to getChatResponse
            // --- End build message list ---
            
            // Generate response without session context for now
            // Map<String, Object> response = ollamaService.generateResponse(enhancedPrompt, chatSession); // Commented out
            // Placeholder: Simulating a response structure for now
            Map<String, Object> responseMap = new HashMap<>();
            // Pass the constructed history (including system prompt) and the user message
            String botResponseContent = ollamaService.getChatResponse(message, messagesToSend);
            responseMap.put("response", botResponseContent);

            
            // Handle different response formats - simplified as getChatResponse now returns String
            // The handling below might be redundant now but kept for structure
            if (responseMap.containsKey("response")) {
                Object responseContent = responseMap.get("response");
                
                // If response content is already a String, use it directly
                if (responseContent instanceof String) {
                    String responseText = (String) responseContent;
                    // chatSessionService.addMessage(chatSession.getId(), "assistant", responseText); // Commented out
                    logger.info("Ollama response: {}", responseText); // Log the actual response
                } 
                // This else-if might not be reachable now if getChatResponse always returns String
                else if (responseContent instanceof Map) { 
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedMap = (Map<String, Object>) responseContent;
                    if (nestedMap.containsKey("response")) {
                        String responseText = nestedMap.get("response").toString();
                        // chatSessionService.addMessage(chatSession.getId(), "assistant", responseText); // Commented out
                        logger.info("Ollama nested response: {}", responseText);
                    }
                }
            }
            
            // Process the response for movie mentions and enhance with movie data
            // Map<String, Object> processedResponse = movieService.processResponseForMovieMentions(responseMap); // Commented out
            // Return the raw response for now as processing method is missing
            return ResponseEntity.ok(responseMap); 
        } catch (Exception e) {
            logger.error("Error generating response: ", e);
            // Ensure a consistent error response structure
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate response: " + e.getMessage());
            errorResponse.put("response", "Sorry, an internal error occurred. Please try again."); // Add user-friendly message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Check if message contains trailer-related keywords
     */
    private boolean containsTrailerKeyword(String message) {
        String lowerMessage = message.toLowerCase().trim();
        return lowerMessage.contains("trailer") || 
               lowerMessage.contains("watch") || 
               lowerMessage.contains("view") || 
               lowerMessage.contains("show me");
    }
    
    /**
     * Handle trailer request
     */
    // private ResponseEntity<?> handleTrailerRequest(String message, String userId, ChatSession chatSession) { // Commented out
    private ResponseEntity<?> handleTrailerRequest(String message, String userId /*, ChatSession chatSession*/) { // Commented out signature
        // Logic to extract movie title and find trailer
        // This is a placeholder implementation
        Map<String, Object> response = new HashMap<>();
        response.put("response", "Here's the trailer you requested.");
        response.put("type", "trailer");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if this is a direct movie search query that can be processed without LLM
     */
    private boolean isDirectMovieSearchQuery(String message) {
        String lowerMessage = message.toLowerCase().trim();
        return (lowerMessage.startsWith("search for ") || 
                lowerMessage.startsWith("find ") || 
                lowerMessage.startsWith("lookup ") ||
                lowerMessage.startsWith("show me ")) && 
               (lowerMessage.contains(" movie") || 
                lowerMessage.contains(" film") || 
                lowerMessage.contains(" movies") || 
                lowerMessage.contains(" films"));
    }
    
    /**
     * Handle direct movie search without using LLM
     */
    private Map<String, String> handleDirectMovieSearch(String query, String userId) {
        // Extract search terms
        String searchTerm = query.toLowerCase()
            .replace("search for", "")
            .replace("find", "")
            .replace("lookup", "")
            .replace("show me", "")
            .replace("movies", "")
            .replace("movie", "")
            .replace("films", "")
            .replace("film", "")
            .trim();
        
        logger.info("Direct search for term: '{}'", searchTerm);
        
        // Get search results from MovieService
        List<Movie> movies = movieService.searchMovies(searchTerm);
        
        if (movies.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("response", "I couldn't find any movies matching '" + searchTerm + "'. Would you like me to suggest something similar?");
            return response;
        }
        
        // Build a formatted response with the top 5 results
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("Here are some movies matching '").append(searchTerm).append("':\n\n");
        
        int count = 0;
        for (Movie movie : movies) {
            if (count++ >= 5) break; // Limit to 5 results
            
            String year = movie.getReleaseDate() != null && movie.getReleaseDate().length() >= 4 
                ? movie.getReleaseDate().substring(0, 4) 
                : "N/A";
                
            responseBuilder.append("* **").append(movie.getTitle()).append("** (").append(year).append(")");
            
            if (movie.getRating() != null && movie.getRating() > 0) {
                responseBuilder.append(" - Rating: ").append(String.format("%.1f", movie.getRating())).append("/10");
            }
            
            responseBuilder.append("\n");
            
            if (movie.getOverview() != null && !movie.getOverview().isEmpty()) {
                responseBuilder.append("  ").append(movie.getOverview()).append("\n\n");
            } else {
                responseBuilder.append("\n");
            }
        }
        
        if (movies.size() > 5) {
            responseBuilder.append("*...and ").append(movies.size() - 5).append(" more results*\n\n");
        }
        
        responseBuilder.append("Would you like more details about any of these films?");
        
        Map<String, String> response = new HashMap<>();
        response.put("response", responseBuilder.toString());
        return response;
    }
    
    private String getSystemPrompt() {
        // Only fetch trending movies for the initial system prompt
        // But make their usage conditional with stronger language
        List<Movie> trendingMovies = movieService.getTrendingMovies();
        
        StringBuilder moviesContext = new StringBuilder();
        if (trendingMovies != null && !trendingMovies.isEmpty()) {
            moviesContext.append("For context only (DO NOT MENTION UNLESS SPECIFICALLY ASKED ABOUT TRENDING OR POPULAR MOVIES), ");
            moviesContext.append("current trending movies include: ");
            for (int i = 0; i < Math.min(5, trendingMovies.size()); i++) {
                Movie movie = trendingMovies.get(i);
                String year = movie.getReleaseDate() != null && movie.getReleaseDate().length() >= 4
                    ? movie.getReleaseDate().substring(0, 4) : "Unknown";
                moviesContext.append(movie.getTitle()).append(" (").append(year).append(")");
                
                if (i < Math.min(4, trendingMovies.size() - 1)) {
                    moviesContext.append(", ");
                }
            }
            moviesContext.append(".\n\n");
        }
        
        return "You are FilmSage, an intelligent movie recommendation assistant. "
            + "Your primary function is to provide accurate, helpful information about movies, directors, actors, and cinema. "
            + moviesContext.toString()
            + "Follow these guidelines for optimal responses:\n\n"
            + "1. SOURCE OF TRUTH: Always prioritize using TMDB data when answering about movies. "
            + "This ensures accuracy and relevance of your movie recommendations.\n\n"
            + "2. RESPONSE STYLE: Use a friendly, conversational tone. Format movie titles in bold (**Title**) "
            + "with release years in parentheses when first mentioned. Organize information clearly with headings and lists.\n\n"
            + "3. RECOMMENDATION APPROACH: When recommending movies, include brief reasons why they match "
            + "the user's preferences, focusing on plot elements, themes, or directorial style that align with their interests.\n\n"
            + "4. COMMON QUERIES: For actor/director filmographies, list their notable works. For movie comparisons, "
            + "focus on thematic similarities rather than just genres.\n\n"
            + "5. BREVITY: Keep responses concise (2-3 sentences per point) unless detailed information is explicitly requested. "
            + "Avoid lengthy plot summaries unless specifically asked.\n\n"
            + "6. UNCERTAINTY: If unsure about specific movie details, acknowledge the uncertainty rather than inventing facts. "
            + "Suggest the user verify on TMDB or IMDb if appropriate.\n\n"
            + "7. MOVIE RECOMMENDATIONS: If the user asks for recommendations \"like\" a certain movie, first identify the key characteristics (genre, themes, director, actors) of the reference movie. Then, suggest 3-5 movies that share those characteristics, explaining *why* each is a good match. Include the release year for each recommendation.\n\n"
            + "8. MOVIE DETAILS & TRAILER MARKER (VERY IMPORTANT - READ CAREFULLY): This marker `[SHOW_TRAILER:movie_id]` enables a 'View Details' button.\n"
            + "   - **WHEN NOT TO USE:** **CRITICAL RULE:** After you have mentioned a movie and potentially used the `[SHOW_TRAILER:id]` marker ONCE, **DO NOT** use the marker or mention the trailer again in subsequent responses about the same movie (e.g., when answering about the director, actors, plot, or other details), UNLESS the user specifically asks about the trailer again. Stick to the user's follow-up topic.\n"
            + "   - **WHEN TO USE:** ONLY use this marker in these specific scenarios:\n"
            + "     1. **Direct User Request:** If the user EXPLICITLY says 'show me the trailer', 'show trailer', 'watch trailer', or similar for a specific movie, respond with **ONLY** the exact marker `[SHOW_TRAILER:id]` and NOTHING else (not even links or surrounding text).\n"
            + "     2. **Initial Introduction/Recommendation:** When you *first* introduce or recommend a specific movie in the conversation, you MAY add the marker `[SHOW_TRAILER:id]` **ONCE** at the very end of that *single* message.\n"
            + "   - **FORMAT:** The marker MUST be exactly `[SHOW_TRAILER:id]`. NEVER include YouTube links.\n\n"
            + "9. CONVERSATIONAL FLOW: Pay attention to the user's follow-up questions. If they ask about a director after you recommended a movie, focus your answer **only** on the director. **DO NOT repeat information about the movie itself, especially DO NOT mention the trailer again.** Adapt your response to the user's current focus."
            ;
    }
    
    private Map<String, Object> enhanceWithMovieContext(String message) {
        // Skip context enhancement for simple queries
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.equals("thanks") || 
            lowerMessage.equals("thank you") || 
            lowerMessage.equals("hello") ||
            lowerMessage.equals("hi") ||
            lowerMessage.equals("ok") ||
            lowerMessage.length() < 10) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", message);
            result.put("movieId", null);
            return result;
        }
        
        // Only add movie context for movie-related queries
        boolean isMovieQuery = lowerMessage.contains("movie") || 
                              lowerMessage.contains("watch") ||
                              lowerMessage.contains("film") ||
                              lowerMessage.contains("recommend") ||
                              lowerMessage.contains("trending") ||
                              lowerMessage.contains("popular") ||
                              lowerMessage.contains("actor") ||
                              lowerMessage.contains("director") ||
                              lowerMessage.contains("similar") ||
                              lowerMessage.contains("like") ||
                              isRecommendationRequest(message);
        
        if (!isMovieQuery) {
            // Return map with original message and null ID
            Map<String, Object> result = new HashMap<>();
            result.put("message", message);
            result.put("movieId", null);
            return result;
        }
        
        // Initialize result map before try-catch
        Map<String, Object> result = new HashMap<>();
        result.put("message", message);
        result.put("movieId", null);
        
        try {
            // For specific movie requests, try to preload data from TMDB
            Map<String, Object> enhancedResult = enhanceWithSpecificMovieData(message);
            // If enhancement found a movie ID, update the main result map
            if (enhancedResult.get("movieId") != null) {
                 result = enhancedResult; // Use the map returned by enhanceWithSpecificMovieData
            } // Otherwise, result retains the original message and null ID
            
            return result;
        } catch (Exception e) {
            logger.error("Error enhancing message with movie context: {}", e.getMessage(), e);
            // Return the initialized result map (original message, null ID) on error
            return result;
        }
    }
    
    /**
     * Tries to identify a specific movie title in the message and enhance the prompt.
     * Returns the enhanced message, or the original message if no movie is found.
     * Also returns the ID of the found movie, if any.
     */
    private Map<String, Object> enhanceWithSpecificMovieData(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", message); // Default to original message
        result.put("movieId", null);     // Default to no ID found
        
        String possibleTitle = extractPotentialMovieTitle(message);
        if (possibleTitle != null) {
            // Remove trailing punctuation or common words manually to avoid regex escape issues
            if (possibleTitle.endsWith("?") || possibleTitle.endsWith(".") || possibleTitle.endsWith("!")) {
                possibleTitle = possibleTitle.substring(0, possibleTitle.length() - 1).trim();
            }
            if (possibleTitle.toLowerCase().endsWith(" movie")) {
                possibleTitle = possibleTitle.substring(0, possibleTitle.length() - 6).trim();
            } else if (possibleTitle.toLowerCase().endsWith(" film")) {
                possibleTitle = possibleTitle.substring(0, possibleTitle.length() - 5).trim();
            } else if (possibleTitle.toLowerCase().endsWith(" about")) {
                possibleTitle = possibleTitle.substring(0, possibleTitle.length() - 6).trim();
            }
        }
        if (possibleTitle == null) {
            return result;
        }

        // Search for this movie
        List<Movie> matches = movieService.searchMovies(possibleTitle);
        if (matches.isEmpty()) {
            // No matches found, DO NOT enhance with unrelated context.
            // Return the original message for the LLM to handle.
            logger.warn("No TMDB match found for extracted title: '{}'. Skipping context enhancement.", possibleTitle);
            return result;
        }
        
        // Take the top match
        Movie topMatch = matches.get(0);
        Long movieId = topMatch.getId();
        result.put("movieId", movieId);

        // Get additional details for context
        Map<String, Object> details = movieService.getMovieDetails(movieId);
        if (details.isEmpty()) {
             logger.warn("Could not fetch details for matched movie ID: {}. Using basic info.", movieId);
             // Still return the basic info and ID
             String context = String.format("Provide information about %s (%s).", 
                                           topMatch.getTitle(), 
                                           topMatch.getReleaseDate() != null ? topMatch.getReleaseDate().substring(0,4) : "N/A");
            result.put("message", context + " Original query was: " + message);
            return result;
        }
        
        // Build context string (keep it concise)
        String title = (String) details.getOrDefault("title", topMatch.getTitle());
        String year = ((String) details.getOrDefault("release_date", "N/A")).substring(0, 4);
        String overview = (String) details.getOrDefault("overview", "");
        String shortOverview = overview.length() > 150 ? overview.substring(0, 150) + "..." : overview;
        
        // Include genres and key cast/crew if available
        List<Map<String, String>> genres = (List<Map<String, String>>) details.get("genres");
        String genreNames = genres != null ? genres.stream().map(g -> g.get("name")).collect(Collectors.joining(", ")) : "N/A";
        
        Map<String, Object> credits = (Map<String, Object>) details.get("credits");
        String director = "N/A";
        if (credits != null && credits.containsKey("crew")) {
            List<Map<String, String>> crew = (List<Map<String, String>>) credits.get("crew");
            director = crew.stream()
                .filter(c -> "Director".equals(c.get("job")))
                .map(c -> c.get("name"))
                .findFirst()
                .orElse("N/A");
        }
        
        String context = String.format("Context: %s (%s), directed by %s. Genres: %s. Overview: %s. User asked: %s",
                                       title, year, director, genreNames, shortOverview, message);
        
        logger.info("Enhanced prompt with context for movie ID {}: {}", movieId, title);
        result.put("message", context);
        return result;
    }

    private String extractPotentialMovieTitle(String message) {
        // Simple pattern: look for text in quotes or capitalized words before "movie"/"film"
        Matcher quoteMatcher = Pattern.compile("\"(.*?)\"").matcher(message);
        if (quoteMatcher.find()) {
            return quoteMatcher.group(1);
        }
        
        // Fixed: Replaced \s with space, added escapes for ( and )
        Matcher capitalizedMatcher = Pattern.compile("([A-Z][a-zA-Z0-9 ]+)(\\s+movie|\\s+film|\\s*\\(\\d{4}\\))").matcher(message);
        if (capitalizedMatcher.find()) {
            return capitalizedMatcher.group(1).trim();
        }
        
        // Very basic fallback: look for multi-word capitalized phrases
        Matcher basicMatcher = Pattern.compile("\b([A-Z][a-z]+(?:\s+[A-Z][a-z]+)+)\b").matcher(message);
        if (basicMatcher.find()) {
             // Be careful with this one, might grab names. Check keywords.
             String potential = basicMatcher.group(1);
             if (message.toLowerCase().contains(potential.toLowerCase()) && 
                 (message.toLowerCase().contains("movie") || message.toLowerCase().contains("film"))) {
                 return potential;
             }
        }
        
        return null;
    }
    
    private boolean isRecommendationRequest(String message) {
        String lower = message.toLowerCase();
        return lower.contains("recommend") || 
               lower.contains("suggest") || 
               lower.contains("similar to") || 
               lower.contains("like");
    }
    
    private Map<String, String> getQuickResponse(String message) {
        String lowerMessage = message.toLowerCase().trim();
        Map<String, String> response = new HashMap<>();
        
        // Simple conversational patterns
        if (lowerMessage.matches("(hi|hello|hey).*")) {
            response.put("response", "Hello! I'm FilmSage, your movie assistant. You can ask me about movies, directors, or get personalized recommendations. How can I help you today?");
            return response;
        } else if (lowerMessage.matches("(thanks|thank you|thx).*")) {
            response.put("response", "You're welcome! Let me know if you need anything else.");
            return response;
        } else if (lowerMessage.equals("bye") || lowerMessage.equals("goodbye")) {
            response.put("response", "Goodbye! Enjoy your movies!");
            return response;
        } else if (lowerMessage.matches("(how are you|how's it going).*")) {
            response.put("response", "I'm doing well, thanks for asking! Ready to talk about movies anytime.");
            return response;
        }
        
        // Non-movie queries that should get fast responses
        if (lowerMessage.contains("time") && (lowerMessage.contains("what") || lowerMessage.startsWith("time"))) {
            response.put("response", "I don't have access to your local time. As a movie assistant, I focus on helping you find great films to watch!");
            return response;
        } else if (lowerMessage.contains("weather") || lowerMessage.contains("forecast")) {
            response.put("response", "I don't have access to weather information. I'm specialized in movies and can help you find something great to watch instead!");
            return response;
        } else if (lowerMessage.contains("joke") || lowerMessage.contains("funny")) {
            String[] movieJokes = {
                "Why don't scientists trust atoms? Because they make up everything... just like Hollywood!",
                "What's a movie director's favorite food? Action rolls!",
                "Why did the actor fall through the floorboards? They were going for a dramatic breakthrough!",
                "What do you call a film about a procrastinating student? A cliff-hanger!",
                "Why was the movie star cool in the summer? Because they had all the fans!"
            };
            int jokeIndex = (int) (System.currentTimeMillis() % movieJokes.length);
            response.put("response", movieJokes[jokeIndex]);
            return response;
        } else if (lowerMessage.contains("what") && (
                   lowerMessage.contains("your name") || 
                   lowerMessage.contains("who are you") || 
                   lowerMessage.contains("about you"))) {
            response.put("response", "I'm FilmSage, your friendly movie recommendation assistant. I can help you discover new films, learn about actors, or find something great to watch tonight!");
            return response;
        } else if (lowerMessage.contains("date") && lowerMessage.contains("what")) {
            response.put("response", "I don't have access to the current date. However, I can definitely help you find a great movie to watch today!");
            return response;
        }
        
        // Check if it's a general question not related to movies
        boolean isMovieRelated = lowerMessage.contains("movie") || 
                                lowerMessage.contains("film") || 
                                lowerMessage.contains("watch") || 
                                lowerMessage.contains("actor") || 
                                lowerMessage.contains("director") ||
                                lowerMessage.contains("show") ||
                                lowerMessage.contains("series") ||
                                lowerMessage.contains("cinema") ||
                                lowerMessage.contains("seen") ||
                                isRecommendationRequest(message);
        
        if (!isMovieRelated && lowerMessage.length() > 5 && 
            (lowerMessage.contains("what") || 
             lowerMessage.contains("how") || 
             lowerMessage.contains("why") || 
             lowerMessage.contains("when") ||
             lowerMessage.contains("where") ||
             lowerMessage.contains("who") ||
             lowerMessage.contains("can you") ||
             lowerMessage.contains("could you"))) {
            // Updated response to state capabilities instead of deflecting
            response.put("response", "I'm FilmSage, your movie assistant! I can help you find movie recommendations, get details about films, actors, and directors, and answer your cinema-related questions.");
            return response;
        }
        
        // Not a simple query, return null to continue with normal processing
        return null;
    }
    
    private boolean isAmbiguousQuery(String message) {
        // Check for common ambiguous terms
        String lowerMessage = message.toLowerCase();
        
        // Better check for director names to avoid Christopher Nolan issue
        if (lowerMessage.contains("chris") && 
            !(lowerMessage.contains("christopher") || 
              lowerMessage.contains("nolan") || 
              lowerMessage.contains("evans") ||
              lowerMessage.contains("hemsworth") ||
              lowerMessage.contains("pratt") ||
              lowerMessage.contains("pine"))) {
            
            // Only consider it ambiguous if it seems like a movie request
            return lowerMessage.contains("movie") || 
                   lowerMessage.contains("film") || 
                   lowerMessage.contains("actor") ||
                   lowerMessage.contains("star") ||
                   lowerMessage.contains("play");
        }
        
        // Check for actor names without specific context
        if ((lowerMessage.contains("tom") && 
             !lowerMessage.contains("hanks") &&
             !lowerMessage.contains("cruise") &&
             !lowerMessage.contains("hardy") &&
             !lowerMessage.contains("holland")) ||
            (lowerMessage.contains("jennifer") &&
             !lowerMessage.contains("lawrence") &&
             !lowerMessage.contains("aniston") &&
             !lowerMessage.contains("lopez"))) {
            
            // Only consider it ambiguous if it seems like a movie request
            return lowerMessage.contains("movie") || 
                   lowerMessage.contains("film") || 
                   lowerMessage.contains("actor") ||
                   lowerMessage.contains("star") ||
                   lowerMessage.contains("play");
        }
        
        return false;
    }
    
    private String generateClarificationPrompt(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Special check for Christopher Nolan to fix issue from screenshots
        if (lowerMessage.contains("christopher nolan") || 
            (lowerMessage.contains("christopher") && lowerMessage.contains("nolan"))) {
            return null; // Return null to indicate no disambiguation needed
        }
        
        // Common ambiguous terms that need disambiguation
        if (lowerMessage.contains("tom")) {
            return "I notice you mentioned Tom. Could you clarify which Tom you're referring to? For example, Tom Hanks, Tom Cruise, Tom Hardy, or someone else?";
        } else if (lowerMessage.contains("chris") && !lowerMessage.contains("christopher")) {
            return "There are several actors named Chris. Are you referring to Chris Evans, Chris Hemsworth, Chris Pine, Chris Pratt, or someone else?";
        } else if (lowerMessage.contains("jennifer")) {
            return "When you mention Jennifer, are you talking about Jennifer Lawrence, Jennifer Aniston, Jennifer Lopez, or another Jennifer?";
        } else if (lowerMessage.contains("action")) {
            return "You mentioned action movies. Could you specify what type of action you prefer? For example, superhero, spy, martial arts, or something else?";
        } else if (lowerMessage.contains("comedy")) {
            return "When you say comedy, are you looking for romantic comedies, slapstick, dark comedy, or another type?";
        }
        
        // Generic disambiguation for other cases
        return "I'd like to give you a more specific response. Could you provide a bit more detail about what you're looking for?";
    }

    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            String username = authentication.getName();
            return userService.findByUsername(username).orElse(null);
        } catch (Exception e) {
            logger.error("Error getting current user", e);
            return null;
        }
    }

    // Add these conversation management endpoints
    
    /**
     * Get all conversations for the current user
     */
    @GetMapping("/conversations")
    // @PreAuthorize("hasRole('USER')") // Temporarily comment out for testing
    public ResponseEntity<?> getAllConversations(Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<ChatConversation> conversations = chatService.getConversationsForUser(user);
            
            // Convert to simplified format for frontend
            List<Map<String, Object>> result = conversations.stream()
                .map(conversation -> {
                    Map<String, Object> convMap = new HashMap<>();
                    convMap.put("id", conversation.getId());
                    convMap.put("title", conversation.getTitle());
                    convMap.put("createdAt", conversation.getCreatedAt());
                    convMap.put("updatedAt", conversation.getUpdatedAt());
                    convMap.put("preview", conversation.getPreview());
                    return convMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error retrieving conversations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve conversations: " + e.getMessage()));
        }
    }
    
    /**
     * Get a specific conversation by ID
     */
    @GetMapping("/conversations/{id}")
    // @PreAuthorize("hasRole('USER')") // Temporarily comment out for testing
    public ResponseEntity<?> getConversation(@PathVariable Long id, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Optional<ChatConversation> conversationOpt = chatService.getConversation(id);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ChatConversation conversation = conversationOpt.get();
            
            // Check if this conversation belongs to the current user
            if (!conversation.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have permission to access this conversation"));
            }
            
            // Get all messages for this conversation
            List<ChatMessage> messages = chatService.getAllMessagesForConversation(conversation);
            
            // Convert to format expected by frontend
            Map<String, Object> result = new HashMap<>();
            result.put("id", conversation.getId());
            result.put("title", conversation.getTitle());
            result.put("createdAt", conversation.getCreatedAt());
            result.put("updatedAt", conversation.getUpdatedAt());
            result.put("messages", chatService.convertMessagesToOllamaFormat(messages));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error retrieving conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve conversation: " + e.getMessage()));
        }
    }
    
    /**
     * Create a new conversation
     */
    @PostMapping("/conversations")
    // @PreAuthorize("hasRole('USER')") // Temporarily comment out for testing
    public ResponseEntity<?> createConversation(@RequestBody Map<String, Object> payload, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            String title = (String) payload.getOrDefault("title", "New Conversation");
            
            ChatConversation conversation = chatService.createConversation(title, user);
            
            // If messages were provided, add them to the conversation
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messages = (List<Map<String, String>>) payload.get("messages");
            
            if (messages != null && !messages.isEmpty()) {
                for (Map<String, String> message : messages) {
                    String role = message.get("role");
                    String content = message.get("content");
                    
                    if (content != null && !content.isEmpty()) {
                        ChatMessage.MessageType type = "user".equals(role) 
                            ? ChatMessage.MessageType.USER 
                            : ChatMessage.MessageType.BOT;
                        
                        chatService.addMessage(conversation, content, role, type);
                    }
                }
            }
            
            // Return the created conversation
            Map<String, Object> result = new HashMap<>();
            result.put("id", conversation.getId());
            result.put("title", conversation.getTitle());
            result.put("createdAt", conversation.getCreatedAt());
            result.put("updatedAt", conversation.getUpdatedAt());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            logger.error("Error creating conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create conversation: " + e.getMessage()));
        }
    }
    
    /**
     * Update an existing conversation
     */
    @PutMapping("/conversations/{id}")
    // @PreAuthorize("hasRole('USER')") // Temporarily comment out for testing
    public ResponseEntity<?> updateConversation(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> payload, 
            Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Optional<ChatConversation> conversationOpt = chatService.getConversation(id);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ChatConversation conversation = conversationOpt.get();
            
            // Check if this conversation belongs to the current user
            if (!conversation.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have permission to modify this conversation"));
            }
            
            // Update the title if provided
            if (payload.containsKey("title")) {
                String newTitle = (String) payload.get("title");
                conversation.setTitle(newTitle);
            } else if (conversation.getTitle() == null || conversation.getTitle().isEmpty()) {
                // Generate a title if none exists
                conversation.generateTitle();
            }
            
            // If messages were provided, replace the existing ones
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messages = (List<Map<String, String>>) payload.get("messages");
            
            if (messages != null && !messages.isEmpty()) {
                // Clear existing messages (note: in a production app, you might want to be more careful here)
                // conversation.getMessages().clear(); // This doesn't actually delete from DB
                // Instead, we'd need to delete via repository
                
                // For simplicity in this implementation, we'll just add new messages
                // assuming the frontend manages the full conversation state
                
                for (Map<String, String> message : messages) {
                    String role = message.get("role");
                    String content = message.get("content");
                    
                    if (content != null && !content.isEmpty()) {
                        ChatMessage.MessageType type = "user".equals(role) 
                            ? ChatMessage.MessageType.USER 
                            : ChatMessage.MessageType.BOT;
                        
                        chatService.addMessage(conversation, content, role, type);
                    }
                }
            }
            
            // Return the updated conversation
            Map<String, Object> result = new HashMap<>();
            result.put("id", conversation.getId());
            result.put("title", conversation.getTitle());
            result.put("createdAt", conversation.getCreatedAt());
            result.put("updatedAt", conversation.getUpdatedAt());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error updating conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update conversation: " + e.getMessage()));
        }
    }
    
    /**
     * Delete a conversation
     */
    @DeleteMapping("/conversations/{id}")
    // @PreAuthorize("hasRole('USER')") // Temporarily comment out for testing
    public ResponseEntity<?> deleteConversation(@PathVariable Long id, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Optional<ChatConversation> conversationOpt = chatService.getConversation(id);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ChatConversation conversation = conversationOpt.get();
            
            // Check if this conversation belongs to the current user
            if (!conversation.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have permission to delete this conversation"));
            }
            
            // Delete the conversation
            chatService.deleteConversation(id);
            
            return ResponseEntity.ok(Map.of("message", "Conversation deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete conversation: " + e.getMessage()));
        }
    }
} 