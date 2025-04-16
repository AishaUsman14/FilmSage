package com.filmsage.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service to enhance chatbot responses with context-aware formatting
 * Adds appropriate structure using headings, lists, line breaks, and emojis
 */
@Service
public class ResponseFormatter {
    private static final Logger logger = LoggerFactory.getLogger(ResponseFormatter.class);
    
    // Emojis (Add spacing where needed when used)
    private static final String MOVIE_EMOJI = "🎬";
    private static final String STAR_EMOJI = "⭐";
    private static final String POPCORN_EMOJI = "🍿";
    private static final String TV_EMOJI = "📺";
    private static final String TICKET_EMOJI = "🎟️";
    private static final String DIRECTOR_EMOJI = "🎮"; // Changed from Joystick to Clapper
    private static final String QUESTION_EMOJI = "❓";
    private static final String THINKING_EMOJI = "🤔";
    private static final String WAVING_EMOJI = "👋";
    private static final String SMILEY_EMOJI = "☺️";
    private static final String PENSIVE_EMOJI = "😔";
    private static final String STARSTRUCK_EMOJI = "🤩"; // For excellent/great
    
    // Map of genres to emojis - Consider adding more
    private static final Map<String, String> GENRE_EMOJIS = new HashMap<>();
    static {
        GENRE_EMOJIS.put("sci-fi", "🚀");
        GENRE_EMOJIS.put("science fiction", "🚀");
        GENRE_EMOJIS.put("action", "💥");
        GENRE_EMOJIS.put("comedy", "😂");
        GENRE_EMOJIS.put("drama", "🎭"); // Changed
        GENRE_EMOJIS.put("horror", "👻");
        GENRE_EMOJIS.put("romantic", "❤️");
        GENRE_EMOJIS.put("romance", "❤️");
        GENRE_EMOJIS.put("thriller", "🔪");
        GENRE_EMOJIS.put("documentary", "📰");
        GENRE_EMOJIS.put("animation", "🎨");
    }
    
    // Known directors
    private static final Map<String, String> KNOWN_DIRECTORS = new HashMap<>();
    static {
        KNOWN_DIRECTORS.put("christopher nolan", "Christopher Nolan");
        KNOWN_DIRECTORS.put("steven spielberg", "Steven Spielberg");
        KNOWN_DIRECTORS.put("quentin tarantino", "Quentin Tarantino");
        KNOWN_DIRECTORS.put("martin scorsese", "Martin Scorsese");
        KNOWN_DIRECTORS.put("ridley scott", "Ridley Scott");
        KNOWN_DIRECTORS.put("tim burton", "Tim Burton");
        // Add more as needed
    }
    
    // Patterns for Markdown conversion
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern BOLD_UNDERSCORE_PATTERN = Pattern.compile("__([^_]+)__");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*([^*]+)\\*");
    private static final Pattern ITALIC_UNDERSCORE_PATTERN = Pattern.compile("_([^_]+)_");
    private static final Pattern H1_PATTERN = Pattern.compile("(?m)^#\\s+(.*)$");
    private static final Pattern H2_PATTERN = Pattern.compile("(?m)^##\\s+(.*)$");
    private static final Pattern H3_PATTERN = Pattern.compile("(?m)^###\\s+(.*)$");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^\\d+\\.\\s+(.*)$");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^(?:\\*|-)\\s+(.*)");
    
    /**
     * Format the response based on content and context
     */
    public String formatResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return rawResponse;
        }
        
        try {
            // --- Step 0: Basic Cleanup ---
            String processed = rawResponse.trim();
            // Normalize line endings
            processed = processed.replace("\r\n", "\n").replace("\r", "\n");
            
            // --- NEW Step: Fix common text issues with contractions and emojis ---
            processed = fixCommonTextIssues(processed);
            
            // --- Step 1: Handle Special Cases (Quick Responses, Disambiguation) ---
            if (isKnownQuickResponse(processed)) {
                processed = formatSimpleResponse(processed); // Apply emoji + basic markdown
                return processed; // Return early
            }
            if (isDisambiguationRequest(processed)) {
                 return formatAsDisambiguation(processed); // Return early
            }
            
            // --- NEW Step: Check for and correct movie information errors ---
            processed = correctMovieInformation(processed);
            
            // --- Step 2: Structural Markdown to HTML (Lists, Headings) ---
            processed = convertMarkdownListsToHtml(processed);
            processed = convertMarkdownHeadingsToHtml(processed);
            
            // --- Step 3: Targeted Formatting (Recommendations, Directors) ---
            if (isRecommendationResponse(processed)) { // Check *after* potential list conversion
                processed = addRecommendationTitle(processed);
                // Temporarily disable movie-mention spans to fix highlighting
                // processed = addMovieMentionSpans(processed);
            }
            processed = highlightDirectorNames(processed);
            
            // --- Step 4: Inline Markdown to HTML (Bold, Italic) ---
            // Apply these last so they don't interfere with list/heading detection
            processed = convertInlineMarkdownToHtml(processed);
            
            // --- Step 5: Final Cleanup ---
            // Remove any remaining list markers if list conversion failed or wasn't applicable
            processed = processed.replaceAll("(?m)^(?:\\*|-)\\s+", "");
            processed = processed.replaceAll("(?m)^\\d+\\.\\s+", "");
             // Trim residual whitespace from lines
             processed = Arrays.stream(processed.split("\n"))
                             .map(String::trim)
                             .filter(line -> !line.isEmpty())
                             .collect(Collectors.joining("\n"));
             // Ensure reasonable spacing between paragraphs/blocks
             processed = processed.replaceAll("\n{3,}", "\n\n");
            
            return processed;
        } catch (Exception e) {
            logger.error("Error formatting response: {}", e.getMessage(), e);
            return rawResponse; // Return original on error
        }
    }
    
    /**
     * Check if the response matches known hardcoded quick response patterns.
     * This prevents them from being misinterpreted as recommendations or lists.
     */
    private boolean isKnownQuickResponse(String response) {
        String lowerResponse = response.toLowerCase();
        // Patterns from ChatController.getQuickResponse
        return lowerResponse.startsWith("hello! i'm filmsage") ||
               lowerResponse.startsWith("i'm filmsage, your movie assistant!") || // Updated pattern
               lowerResponse.startsWith("you're welcome!") ||
               lowerResponse.startsWith("goodbye! enjoy your movies!") ||
               lowerResponse.startsWith("i'm doing well") ||
               lowerResponse.startsWith("i don't have access to"); // Covers time, weather, date
    }
    
    /**
     * Determine if the response is a disambiguation request
     */
    private boolean isDisambiguationRequest(String response) {
        String lowerResponse = response.toLowerCase();
        return (lowerResponse.contains("could you clarify") ||
                lowerResponse.contains("are you referring to") ||
                lowerResponse.contains("did you mean") ||
                lowerResponse.contains("which") && (
                    lowerResponse.contains("are you talking about") ||
                    lowerResponse.contains("do you mean") ||
                    lowerResponse.contains("would you like")
                )) &&
                (lowerResponse.contains("?") || lowerResponse.contains("please specify"));
    }
    
    /**
     * Format a disambiguation response with special styling
     */
    private String formatAsDisambiguation(String response) {
        return "<div class='disambiguation-request'>\n" +
               THINKING_EMOJI + " " + response.trim() + // Add space after emoji
               "\n</div>";
    }
    
    /**
     * Format a simple response with basic markdown and relevant emoji
     */
    private String formatSimpleResponse(String response) {
        // Apply basic markdown and add relevant emoji with space
        String formatted = convertInlineMarkdownToHtml(response); // Apply bold/italic
        String lowerResponse = formatted.toLowerCase();
        
        if (lowerResponse.contains("hello") || lowerResponse.contains("hi")) return WAVING_EMOJI + " " + formatted;
        if (lowerResponse.contains("thank")) return SMILEY_EMOJI + " " + formatted;
        if (lowerResponse.contains("sorry") || lowerResponse.contains("apologize")) return PENSIVE_EMOJI + " " + formatted;
        if (lowerResponse.contains("clarify")) return QUESTION_EMOJI + " " + formatted;
        if (lowerResponse.contains("recommend") || lowerResponse.contains("suggest")) return POPCORN_EMOJI + " " + formatted;
        if (lowerResponse.contains("great") || lowerResponse.contains("excellent")) return STARSTRUCK_EMOJI + " " + formatted; // Changed emoji
        // Add more simple emoji rules if needed
        
        // Fallback: Add movie emoji if relevant
         if (lowerResponse.contains("watch") || lowerResponse.contains("movie") || lowerResponse.contains("film")) {
             return MOVIE_EMOJI + " " + formatted;
         }
        
        return formatted; // Return formatted text if no specific emoji rule matched
    }
    
    /**
     * Convert markdown headings to HTML
     */
    private String convertMarkdownHeadingsToHtml(String text) {
        text = H1_PATTERN.matcher(text).replaceAll("<h2>$1</h2>"); // H1 -> H2
        text = H2_PATTERN.matcher(text).replaceAll("<h3>$1</h3>"); // H2 -> H3
        text = H3_PATTERN.matcher(text).replaceAll("<h4>$1</h4>"); // H3 -> H4
        return text;
    }
    
    /**
     * Convert inline markdown to HTML
     */
    private String convertInlineMarkdownToHtml(String text) {
         // Apply replacements carefully. Italic first avoids issues with bold.
         text = ITALIC_PATTERN.matcher(text).replaceAll("<em>$1</em>");
         text = ITALIC_UNDERSCORE_PATTERN.matcher(text).replaceAll("<em>$1</em>");
         text = BOLD_PATTERN.matcher(text).replaceAll("<strong>$1</strong>");
         text = BOLD_UNDERSCORE_PATTERN.matcher(text).replaceAll("<strong>$1</strong>");
         return text;
    }
    
    /**
     * Convert markdown lists to HTML
     */
    private String convertMarkdownListsToHtml(String text) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n");
        boolean inList = false;
        boolean isOrdered = false;
        
        Pattern orderedPattern = ORDERED_LIST_PATTERN; // Use final static pattern
        Pattern unorderedPattern = UNORDERED_LIST_PATTERN; // Use final static pattern

        for (String line : lines) {
            Matcher orderedMatcher = orderedPattern.matcher(line);
            Matcher unorderedMatcher = unorderedPattern.matcher(line);
            
            if (orderedMatcher.matches()) {
                String itemContent = orderedMatcher.group(1).trim();
                if (!inList) { // Start new list
                    result.append("<ol>\n");
                    inList = true;
                    isOrdered = true;
                } else if (!isOrdered) { // Switch from ul to ol
                    result.append("</ul>\n<ol>\n");
                    isOrdered = true;
                }
                result.append("  <li>").append(itemContent).append("</li>\n");
            } else if (unorderedMatcher.matches()) {
                String itemContent = unorderedMatcher.group(1).trim();
                 if (!inList) { // Start new list
                    result.append("<ul>\n");
                    inList = true;
                    isOrdered = false;
                } else if (isOrdered) { // Switch from ol to ul
                    result.append("</ol>\n<ul>\n");
                    isOrdered = false;
                }
                 // Handle potential nested bold/italic markers *within* list items before adding
                 itemContent = convertInlineMarkdownToHtml(itemContent);
                result.append("  <li>").append(itemContent).append("</li>\n");
            } else { // Not a list item
                if (inList) { // Close the previous list
                    result.append(isOrdered ? "</ol>\n" : "</ul>\n");
                    inList = false;
                }
                 // Append non-list line, ensuring it's not empty
                 String trimmedLine = line.trim();
                 if (!trimmedLine.isEmpty()) {
                      // Apply heading conversion *before* wrapping in <p>
                      String headingConverted = convertMarkdownHeadingsToHtml(trimmedLine);
                      if (headingConverted.startsWith("<h")) {
                          result.append(headingConverted).append("\n");
                      } else {
                          // Wrap regular text in paragraph tags
                          result.append("<p>").append(headingConverted).append("</p>\n");
                      }
                 } else {
                      // Keep empty lines for potential spacing, or handle differently
                      result.append("\n");
                 }
            }
        }
        
        if (inList) { // Close any open list at the end
            result.append(isOrdered ? "</ol>\n" : "</ul>\n");
        }
        
        return result.toString();
    }
    
    /**
     * Add a recommendation title
     */
    private String addRecommendationTitle(String text) {
        // Check if a title like "<h3>...</h3>" already exists
        if (!text.trim().startsWith("<h")) {
             // Add a default recommendation title if none exists
             return "<h3>" + POPCORN_EMOJI + " Movie Recommendations</h3>\n" + text;
        }
        // Otherwise, maybe just prepend emoji to existing title?
        // Example: text = text.replaceFirst("<h3>", "<h3>" + POPCORN_EMOJI + " ");
        return text; // Return as is if title exists
    }
    
    /**
     * Highlight director names in the response
     */
    private String highlightDirectorNames(String text) {
        String modifiedText = text;
        for (Map.Entry<String, String> director : KNOWN_DIRECTORS.entrySet()) {
            String directorName = director.getValue();
            // Use regex to find the name ensuring it's not already inside a tag
            // Lookbehind assertion (?<!>) checks if the preceding character is not '>'
            // Lookahead assertion (?![^<]*>) checks if the match is not followed by '>' before '<'
             Pattern pattern = Pattern.compile("(?<![>])\b(" + Pattern.quote(directorName) + ")\b(?![^<]*>)", Pattern.CASE_INSENSITIVE);
             Matcher matcher = pattern.matcher(modifiedText);
             StringBuffer sb = new StringBuffer();
             while (matcher.find()) {
                 String replacement = DIRECTOR_EMOJI + " <span class='director-mention'>" + directorName + "</span>";
                 matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
             }
             matcher.appendTail(sb);
             modifiedText = sb.toString();
        }
        
        // Fix emoji spacing issues - ensure emojis are properly spaced
        // Look for cases where emoji is directly followed by text without space
        Pattern emojiPattern = Pattern.compile("([\uD83C-\uDBFF\uDC00-\uDFFF🎬🍿⭐📺🎟️🎮❓🤔👋☺️😔🤩]+)([a-zA-Z])");
        Matcher emojiMatcher = emojiPattern.matcher(modifiedText);
        StringBuffer emojiBuffer = new StringBuffer();
        while (emojiMatcher.find()) {
            emojiMatcher.appendReplacement(emojiBuffer, emojiMatcher.group(1) + " " + emojiMatcher.group(2));
        }
        emojiMatcher.appendTail(emojiBuffer);
        modifiedText = emojiBuffer.toString();
        
        // Fix additional special cases
        modifiedText = modifiedText.replaceAll("([a-zA-Z])/🎬", "$1/ 🎬");
        modifiedText = modifiedText.replaceAll("let🎬", "let 🎬");
        modifiedText = modifiedText.replaceAll("It🎬", "It 🎬");
        modifiedText = modifiedText.replaceAll("it🎬", "it 🎬");
        modifiedText = modifiedText.replaceAll("you🎬", "you 🎬");
        
        return modifiedText;
    }
    
    /**
     * Determine if the response is a recommendation
     */
    private boolean isRecommendationResponse(String response) {
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("recommend") || lowerResponse.contains("suggest") ||
               lowerResponse.contains("might enjoy") ||
               lowerResponse.contains("might like") ||
               lowerResponse.contains("check out") ||
               lowerResponse.contains("you could watch") ||
               lowerResponse.contains("similar to") ||
               lowerResponse.contains("fan of") && lowerResponse.contains("enjoy");
    }
    
    /**
     * Determine if the response is explaining a concept
     */
    private boolean isExplanatoryResponse(String response) {
        return response.length() > 150 && 
              (response.contains(". ") || response.contains(".\n")) &&
              !isListResponse(response);
    }
    
    /**
     * Determine if the response is short and simple
     */
    private boolean isSimpleResponse(String response) {
        return response.length() < 100 && !response.contains("\n");
    }
    
    /**
     * Determine if the response is a list of items
     */
    private boolean isListResponse(String response) {
        // More robust check based on common list patterns in markdown
        return response.matches("(?s).*^(\\*|-|\\d+\\.).*"); // Corrected escape
    }
    
    /**
     * Correct common movie information errors before displaying to users
     */
    private String correctMovieInformation(String text) {
        // Correct known movie-actor attribution errors
        text = text.replaceAll("The Siege \\(1998\\).*?Harrison Ford, Milla Jovovich", 
                              "The Siege (1998): (Denzel Washington, Bruce Willis)");
        
        text = text.replaceAll("Unbroken \\(2014\\).*?Brace Coburn, Jake Gyllenhaal", 
                              "Unbroken (2014): (Jack O'Connell, Domhnall Gleeson)");
        
        text = text.replaceAll("The Last Bullet \\(1992\\).*?Bruce Willis", 
                              "The Last Boy Scout (1991): (Bruce Willis)");
        
        text = text.replaceAll("Face/🎬\\s*Off \\(1997\\).*?John Travolta and Kiefer Sutherland", 
                              "Face/Off (1997): John Travolta and Nicolas Cage");
        
        text = text.replaceAll("Wayne🎬\\s*s World", 
                              "Wayne's World");
        
        return text;
    }
    
    private String fixCommonTextIssues(String text) {
        String modifiedText = text;
        
        // Fix common issues with spacing around movie emoji
        modifiedText = modifiedText.replaceAll("let🎬", "let 🎬");
        modifiedText = modifiedText.replaceAll("It🎬", "It 🎬");
        modifiedText = modifiedText.replaceAll("it🎬", "it 🎬");
        modifiedText = modifiedText.replaceAll("you🎬", "you 🎬");
        
        // Improve contraction handling to avoid interfering with movie title detection
        modifiedText = modifiedText.replaceAll("\\b(let|it|that|he|she|who|what|where|when|there|here|they|we|you)s\\b", "$1's");
        modifiedText = modifiedText.replaceAll("\\b(don|won|can|didn|couldn|wouldn|shouldn)t\\b", "$1't");
        modifiedText = modifiedText.replaceAll("\\b(they|we|you)re\\b", "$1're");
        
        return modifiedText;
    }
} 