// Add chat history variable to store conversation state
let chatHistory = [];
let currentConversationId = null;
let lastMentionedMovieId = null; // Global variable for the last mentioned movie ID

// Add a global flag to track Barbie mentions
window.barbieMentioned = false;

// Define a local placeholder for images that might fail to load
const DEFAULT_POSTER_PLACEHOLDER = 'data:image/svg+xml;charset=UTF-8,%3Csvg%20width%3D%22500%22%20height%3D%22750%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%20500%20750%22%20preserveAspectRatio%3D%22none%22%3E%3Cdefs%3E%3Cstyle%20type%3D%22text%2Fcss%22%3E%23holder_18633031d6d%20text%20%7B%20fill%3A%23AAAAAA%3Bfont-weight%3Abold%3Bfont-family%3AArial%2C%20Helvetica%2C%20Open%20Sans%2C%20sans-serif%2C%20monospace%3Bfont-size%3A25pt%20%7D%20%3C%2Fstyle%3E%3C%2Fdefs%3E%3Cg%20id%3D%22holder_18633031d6d%22%3E%3Crect%20width%3D%22500%22%20height%3D%22750%22%20fill%3D%22%23424242%22%3E%3C%2Frect%3E%3Cg%3E%3Ctext%20x%3D%22160%22%20y%3D%22380%22%3ENo%20Poster%3C%2Ftext%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E';
const DEFAULT_PROFILE_PLACEHOLDER = 'data:image/svg+xml;charset=UTF-8,%3Csvg%20width%3D%22200%22%20height%3D%22200%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%20200%20200%22%20preserveAspectRatio%3D%22none%22%3E%3Cdefs%3E%3Cstyle%20type%3D%22text%2Fcss%22%3E%23holder_18633031d6d%20text%20%7B%20fill%3A%23AAAAAA%3Bfont-weight%3Abold%3Bfont-family%3AArial%2C%20Helvetica%2C%20Open%20Sans%2C%20sans-serif%2C%20monospace%3Bfont-size%3A13pt%20%7D%20%3C%2Fstyle%3E%3C%2Fdefs%3E%3Cg%20id%3D%22holder_18633031d6d%22%3E%3Crect%20width%3D%22200%22%20height%3D%22200%22%20fill%3D%22%23555555%22%3E%3C%2Frect%3E%3Cg%3E%3Ctext%20x%3D%2250%22%20y%3D%22100%22%3ENo%20Image%3C%2Ftext%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E';

// Add this function to check if the user is logged in
function isUserLoggedIn() {
    // Since we're using Spring Security's form-based (session) authentication,
    // we assume the user is logged in if they're on a protected page.
    // (The session cookie is HttpOnly, so we can't read it client-side.)
    return true;
}

// Update the sendMessage function to use the new Ollama API
function sendMessage() {
    const userInput = document.getElementById('userInput');
    const message = userInput.value.trim();
    
    if (message === '') {
        return;
    }
    
    // Check if the message is a "show details"/"show trailer" command
    if (extractMovieTitleFromShowDetailsCommand(message)) {
        if (!lastMentionedMovieId) {
            addMessage("No movie details available. Please ask about a movie first.", "chatbot-message error");
        } else {
            showMovieDetails(lastMentionedMovieId);
        }
        // Clear the input and exit early so we don't send this as a chat message
        userInput.value = '';
        return;
    }
    
    // Clear the input
    userInput.value = '';
    
    // Display user message in the chat
    if (document.getElementById('chatMessages')) {
        addMessage(message, 'user-message');
    } else {
        appendMessage('user', message);
    }
    
    // Add a typing indicator
    createTypingIndicator();
    
    // Generate a unique ID for this message to help with removal later
    const tempMessageId = Date.now();
    
    // For session-based auth, we do not need to include an Authorization header.
    fetch('/api/chat/chat', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            message: message,
            history: chatHistory, // Send current history
            conversationId: currentConversationId
        })
    })
    .then(response => {
        console.log('Response status:', response.status);
        if (!response.ok) {
            throw new Error(`Server responded with status: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        console.log('Response data:', data);
        
        // Remove typing indicator
        removeTypingIndicator();
        
        // Remove the temporary message - ADD CHECK
        const tempMessage = document.querySelector('.temp-message') || 
                           document.querySelector('.temp-message-' + tempMessageId);
        if (tempMessage && tempMessage.parentNode) { // Check if temp message exists
            tempMessage.remove();
        }
        
        if (data && data.response) {
            // Check if data.response is an object instead of a string
            const responseContent = typeof data.response === 'object' ? data.response.response : data.response;
            
            // Update chat history with responses
            chatHistory.push({ role: 'user', content: message });
            chatHistory.push({ role: 'assistant', content: responseContent });
            window.chatHistory = chatHistory;
            
            // Check if the response includes a movie ID
            if (data.lastMentionedMovieId) {
                console.log(`Setting lastMentionedMovieId from API response to: ${data.lastMentionedMovieId}`);
                lastMentionedMovieId = data.lastMentionedMovieId;
            }
            
            // Display the bot's response using the appropriate function
            if (document.getElementById('chatMessages')) {
                addMessage(responseContent, 'chatbot-message');
            } else {
                appendMessage('bot', responseContent);
            }

            // Extract movie ID from text if not provided directly
            if (!data.lastMentionedMovieId && typeof responseContent === 'string') {
                // Look for Barbie-specific mention
                if (responseContent.includes('Barbie (2023)') || 
                    responseContent.toLowerCase().includes('barbie movie')) {
                    console.log('Detected mention of Barbie movie, setting ID to 346698');
                    lastMentionedMovieId = '346698'; // This is Barbie's movie ID
                }
            }
            
            // Auto-save the conversation after each message exchange
            if (chatHistory.length >= 2) {
                saveCurrentConversation();
            }
        } else {
            console.error('Invalid response from server:', data);
            if (document.getElementById('chatMessages')) {
                addMessage("Sorry, I received an invalid response. Please try again.", 'chatbot-message error');
            } else {
                appendMessage('bot', 'Sorry, I received an invalid response. Please try again.');
            }
        }
        
        // Scroll to the bottom of the chat
        chatMessages.scrollTop = chatMessages.scrollHeight;
        
        // If a new conversation ID was assigned by the server in the chat response (less common now)
        // We now primarily rely on saveCurrentConversation to handle ID assignment and refresh
        if (data.conversationId && !currentConversationId) {
            console.warn(`Received conversationId ${data.conversationId} directly from /api/chat/chat response. Assigning.`);
            currentConversationId = data.conversationId;
            // Avoid immediate refresh here; let saveCurrentConversation handle it.
            // loadChatConversations(); 
        }
    })
    .catch(error => {
        console.error('Error sending message:', error);
        
        // Remove typing indicator
        removeTypingIndicator();
        
        // Remove the temporary message
        const tempMessage = document.querySelector('.temp-message') || 
                           document.querySelector('.temp-message-' + tempMessageId);
        if (tempMessage && tempMessage.parentNode) { // Check if temp message exists
            tempMessage.remove();
        }
        
        if (document.getElementById('chatMessages')) {
            addMessage(`Sorry, I'm having trouble connecting: ${error.message}`, 'chatbot-message error');
        } else {
            appendMessage('bot', `Sorry, I'm having trouble connecting: ${error.message}`);
        }
    });
}

// Function to add a message to the chat
function addMessage(content, className) {
    const chatMessages = document.getElementById('chatMessages');
    const messageDiv = document.createElement('div');
    
    messageDiv.className = className;
    
    // For user messages, escape HTML to prevent XSS
    if (className === 'user-message') {
        messageDiv.textContent = content;
    } else {
        // For bot messages, process the content which may contain HTML
        const processedHTML = processContent(content);
        messageDiv.innerHTML = processedHTML;
        
        // If the processed content includes our placeholder, trigger the async load
        const placeholder = messageDiv.querySelector('.movie-extras-container[data-movie-id]');
        if (placeholder) {
            displayMovieTrailerAndProviders(placeholder);
        }
    }
    
    chatMessages.appendChild(messageDiv);
    
    // Scroll to the bottom of the chat
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// Function to process both links and formatted content
function processContent(text) {
    // Add safety check for undefined or non-string text
    if (!text || typeof text !== 'string') {
        console.warn('processContent received invalid text:', text);
        return text || '';
    }

    // Known movie ID mappings - add more as needed
    const knownMovieIds = {
        'barbie': '346698',
        'oppenheimer': '872585',
        'dune': '438631',
        'dune part two': '693134',
        'the batman': '414906',
        'inception': '27205',
        'interstellar': '157336',
        'the dark knight': '155'
    };

    // Check for special trailer/provider command - modified to handle 't' prefix
    const trailerMatch = text.match(/\[SHOW_TRAILER:t?(\d+)\]/);
    if (trailerMatch) {
        const possibleId = trailerMatch[1];
        console.log(`Detected SHOW_TRAILER marker for possible movie ID: ${possibleId} in text.`);
        
        // Check for movie names in text and override with correct IDs if found
        let idToUse = possibleId;
        let movieMatch = null;
        
        // Check known movie titles in the text
        for (const [movieTitle, correctId] of Object.entries(knownMovieIds)) {
            if (text.toLowerCase().includes(movieTitle)) {
                console.log(`Detected movie title "${movieTitle}" in text, overriding ID ${possibleId} with known ID ${correctId}`);
                idToUse = correctId;
                movieMatch = movieTitle;
                break;
            }
        }
        
        // Special case for Barbie movie which we already have a flag for
        if (window.barbieMentioned === true && idToUse !== knownMovieIds['barbie']) {
            console.log(`Barbie flag is set, overriding ID ${idToUse} with Barbie ID ${knownMovieIds['barbie']}`);
            idToUse = knownMovieIds['barbie'];
        }
        
        // Store the ID for later use
        console.log(`Setting lastMentionedMovieId to: ${idToUse}`);
        lastMentionedMovieId = idToUse;
        
        // Rest of the function remains unchanged
        // Instead of automatically generating a button, just replace with text that a trailer is available
        // The user can then explicitly ask to see the trailer/details
        const promptText = `
            <div class="movie-action-prompt">
                A trailer is available for this movie. You can say "show me the trailer" or "show details" to view it.
            </div>
        `;
        
        // For backwards compatibility, check if the URL includes a specific parameter
        // that indicates we should show buttons directly
        if (window.location.search.includes('direct_buttons=true')) {
            const buttonHTML = `
                <div class="movie-action-container">
                    <button class="view-details-btn" onclick="showMovieDetails(${idToUse})" style="
                        background-color: #e50914; 
                        text-transform: uppercase;
                        font-weight: 600;
                        padding: 12px 20px;
                        font-size: 14px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto;
                        border-radius: 4px;">
                        <i class="fas fa-play-circle" style="margin-right: 8px;"></i> VIEW DETAILS
                    </button>
                </div>
            `;
            return text.replace(trailerMatch[0], buttonHTML);
        }
        
        return text.replace(trailerMatch[0], promptText);
    }
    
    // Check if Barbie is mentioned in the text
    if (text.includes('Barbie (2023)') || 
        text.toLowerCase().includes('barbie movie')) {
        console.log('Detected Barbie mention in response');
        window.barbieMentioned = true;
        // We can optionally save the movie ID here too
        lastMentionedMovieId = '346698';
    }
    
    // If no marker found, process the text normally
    // Process formatted content - handle special characters that might need escaping
    let processedText = text;
    
    // Check if this is a disambiguation request (from ResponseFormatter)
    if (processedText.includes('<div class=\'disambiguation-request\'>')) {
        // Already formatted, just return it
        return processedText;
    }
    
    // Fix bullet points if they're not in a list
    if (processedText.includes('• ') && !processedText.includes('<ul>')) {
        const lines = processedText.split('\n');
        let inList = false;
        
        for (let i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith('• ')) {
                if (!inList) {
                    // Start a new list before this line
                    lines[i] = '<ul><li>' + lines[i].trim().substring(2) + '</li>';
                    inList = true;
                } else {
                    // Continue the list
                    lines[i] = '<li>' + lines[i].trim().substring(2) + '</li>';
                }
            } else if (inList && lines[i].trim() !== '') {
                // End the list if we have a non-empty line that's not a bullet point
                lines[i-1] += '</ul>';
                inList = false;
            }
        }
        
        // Close any open list at the end
        if (inList) {
            lines[lines.length - 1] += '</ul>';
        }
        
        processedText = lines.join('\n');
    }
    
    // Handle numbered lists if they're not already in <ol> tags
    if (/^\d+\.\s/.test(processedText) && !processedText.includes('<ol>')) {
        const lines = processedText.split('\n');
        let inList = false;
        let lastNumber = 0;
        
        for (let i = 0; i < lines.length; i++) {
            const match = lines[i].trim().match(/^(\d+)\.\s(.+)$/);
            if (match) {
                const number = parseInt(match[1]);
                const content = match[2];
                
                if (!inList) {
                    // Start a new ordered list
                    lines[i] = '<ol start="' + number + '"><li>' + content + '</li>';
                    inList = true;
                    lastNumber = number;
                } else if (number === lastNumber + 1) {
                    // Continue the list with next number
                    lines[i] = '<li>' + content + '</li>';
                    lastNumber = number;
                } else {
                    // End previous list and start a new one with a different start number
                    lines[i-1] += '</ol>';
                    lines[i] = '<ol start="' + number + '"><li>' + content + '</li>';
                    lastNumber = number;
                }
            } else if (inList && lines[i].trim() !== '') {
                // End the list if we have a non-empty line that's not a numbered item
                lines[i-1] += '</ol>';
                inList = false;
            }
        }
        
        // Close any open list at the end
        if (inList) {
            lines[lines.length - 1] += '</ol>';
        }
        
        processedText = lines.join('\n');
    }
    
    // Special handling for movie titles if not already wrapped
    if (!processedText.includes('movie-mention')) {
        // Modified pattern for detecting movie titles - more precise to avoid highlighting contractions
        // 1. Items in double quotes
        // 2. Movie titles with year in parentheses
        // 3. Titles preceded by 🎬 emoji with more than one word (to avoid highlighting contractions)
        const moviePattern = /("([^"]+)"|\b([A-Z][a-zA-Z\s]{2,})\s+\((\d{4})\)|🎬\s+([A-Z][a-zA-Z\s]+\s+[a-zA-Z\s]+))/g;
        
        // Special handling for contractions and possessives - exclude them from highlighting
        const contractionPattern = /'s\b|'t\b|'re\b|'ve\b|'ll\b|'d\b|n't\b/g;
        
        processedText = processedText.replace(moviePattern, (match, p1, p2, p3, p4, p5) => {
            // Skip if it's a contraction
            if (contractionPattern.test(match)) {
                return match;
            }
            
            let title;
            if (p2) title = p2; // Double quotes
            else if (p3) title = p3; // Title with year
            else if (p5) title = p5; // After movie emoji
            
            if (title) {
                if (p4) { // Has year
                    return '🎬 <span class="movie-mention">' + title + ' (' + p4 + ')</span>';
                } else {
                    return '🎬 <span class="movie-mention">' + title + '</span>';
                }
            }
            return match;
        });
    }
    
    // Special handling for director names if not already wrapped
    if (!processedText.includes('director-mention')) {
        const directors = [
            'Christopher Nolan', 'Steven Spielberg', 'Quentin Tarantino',
            'Martin Scorsese', 'Francis Ford Coppola', 'Stanley Kubrick',
            'James Cameron', 'Ridley Scott', 'Tim Burton', 'David Fincher'
        ];
        
        for (const director of directors) {
            const directorRegex = new RegExp('\\b' + director + '\\b', 'g');
            processedText = processedText.replace(directorRegex, 
                '🎮 <span class="director-mention">' + director + '</span>');
        }
    }
    
    // Ensure paragraphs are properly formatted
    if (!processedText.includes('<p>')) {
        const paragraphs = processedText.split('\n\n');
        if (paragraphs.length > 1) {
            processedText = paragraphs.map(p => p.trim() ? '<p>' + p + '</p>' : '').join('');
        }
    }
    
    // Replace any remaining newlines with <br> tags
    processedText = processedText.replace(/\n/g, '<br>');
    
    // Convert emoji unicode to HTML for consistent display
    processedText = addEmojiSpans(processedText);
    
    return processedText;
}

// Function to safely process links in text and return formatted HTML
function processLinks(text) {
    // This function is now deprecated, but kept for backward compatibility
    return processContent(text);
}

// Function to wrap emojis in spans for consistent styling
function addEmojiSpans(text) {
    // Regular expression to match emoji characters
    const emojiRegex = /[\u{1F300}-\u{1F6FF}\u{1F900}-\u{1F9FF}\u{2600}-\u{26FF}\u{2700}-\u{27BF}]/gu;
    
    // Replace emojis with spans
    return text.replace(emojiRegex, (match) => {
        return `<span class="emoji">${match}</span>`;
    });
}

// Create a more engaging typing indicator
function createTypingIndicator() {
    const typingIndicator = document.createElement('div');
    typingIndicator.className = 'typing-indicator';
    
    // Create dots for the typing animation
    for (let i = 0; i < 3; i++) {
        const dot = document.createElement('span');
        typingIndicator.appendChild(dot);
    }
    
    // Add a message that shows the AI is thinking
    const thinkingText = document.createElement('div');
    thinkingText.className = 'thinking-text';
    thinkingText.innerText = 'FilmSage is thinking...';
    typingIndicator.appendChild(thinkingText);
    
    // Start the progress timer for longer responses
    let seconds = 0;
    const progressTimer = setInterval(() => {
        seconds++;
        if (seconds > 2) {
            thinkingText.innerText = `FilmSage is processing your request... ${seconds}s`;
            
            // Add encouraging messages for longer waits
            if (seconds === 10) {
                thinkingText.innerText = 'Almost there! Generating a thoughtful response...';
            } else if (seconds === 20) {
                thinkingText.innerText = 'This is a complex question, creating a detailed answer...';
            } else if (seconds === 30) {
                thinkingText.innerText = 'Thanks for your patience, final touches on your response...';
            }
        }
    }, 1000);
    
    // Store the timer ID so we can clear it later
    typingIndicator.dataset.timerId = progressTimer;
    
    return typingIndicator;
}

// Function to remove the typing indicator and clear its timer
function removeTypingIndicator() {
    const typingIndicator = document.querySelector('.typing-indicator');
    if (typingIndicator && typingIndicator.parentNode) { // Check if indicator exists and is in the DOM
        // Clear the progress timer if it exists
        const timerId = typingIndicator.dataset.timerId;
        if (timerId) {
            clearInterval(parseInt(timerId));
        }
        typingIndicator.remove();
    } else {
        // console.log("Typing indicator already removed or not found."); // Optional logging
    }
}

// Update the initializeChatModal function to clear history when opened
function initializeChatModal() {
    const openChatBtn = document.getElementById('openChatBtn');
    const closeChatBtn = document.getElementById('closeChatBtn');
    const chatOverlay = document.getElementById('chatOverlay');
    const chatBox = document.getElementById('chatBox');
    const userInput = document.getElementById('userInput');
    
    // Make sure the chat overlay is at root level
    if (chatOverlay && chatOverlay.parentNode !== document.body) {
        document.body.appendChild(chatOverlay);
    }
    
    // Function to open the chat modal
    function openChatModal() {
        console.log("Opening chat modal");
        // Make sure it's added to the body before opening
        if (chatOverlay.parentNode !== document.body) {
            document.body.appendChild(chatOverlay);
        }
        
        // Force repaint before adding the active class
        void chatOverlay.offsetWidth;
        
        chatOverlay.classList.add('active');
        setTimeout(() => {
            userInput.focus();
        }, 300);
        
        // Add welcome message if chat is empty
        const chatMessages = document.getElementById('chatMessages');
        if (chatMessages && chatMessages.children.length === 0) {
            addMessage("Hello! I'm FilmSage, your movie assistant. You can ask me about movies, directors, or get personalized recommendations. How can I help you today?", 'chatbot-message');
        }
    }
    
    // Function to close the chat modal
    function closeChatModal() {
        console.log("Closing chat modal");
        chatOverlay.classList.remove('active');
    }
    
    // Event listeners
    if (openChatBtn) {
        console.log("Found open chat button, adding event listener");
        openChatBtn.addEventListener('click', openChatModal);
    }
    
    if (closeChatBtn) {
        closeChatBtn.addEventListener('click', closeChatModal);
    }
    
    // Close on click outside the chat box
    if (chatOverlay) {
        chatOverlay.addEventListener('click', function(event) {
            if (event.target === chatOverlay) {
                closeChatModal();
            }
        });
    }
    
    // Close on Escape key
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && chatOverlay.classList.contains('active')) {
            closeChatModal();
        }
    });
}

document.addEventListener('DOMContentLoaded', function() {
    const sendButton = document.getElementById("sendButton");
    if (sendButton) {
        sendButton.addEventListener("click", function() {
            sendMessage();
        });
    }
    
    // Initialize home page components
    initializeHomePage();
    
    // Rating system
    initializeRatingSystem();
    
    // Load watchlist
    loadWatchlist();
    
    // Initialize trending movies carousel if on home page
    const trendingCarousel = document.getElementById('trendingCarousel');
    if (trendingCarousel) {
        console.log("Found trending carousel, initializing...");
        initializeTrendingMoviesCarousel();
    } else {
        console.log("No trending carousel found on page");
    }
    
    // Initialize the chat modal
    initializeChatModal();
    
    // Apply sidebar states first
    applySidebarStates();
    
    // Set up sidebar toggle button event listeners
    const chatToggleBtn = document.getElementById('chatToggle');
    const watchlistToggleBtn = document.getElementById('watchlistToggle');
    
    if (chatToggleBtn) {
        console.log("Setting up chat toggle button click listener");
        chatToggleBtn.addEventListener('click', function() {
            console.log("Chat toggle button clicked");
            toggleChatSidebar();
        });
    }
    
    if (watchlistToggleBtn) {
        console.log("Setting up watchlist toggle button click listener");
        watchlistToggleBtn.addEventListener('click', function() {
            console.log("Watchlist toggle button clicked");
            toggleWatchlistSidebar();
        });
    }
    
    // Add back event listener for Enter key in chat input
    const userInput = document.getElementById("userInput");
    if (userInput) {
        userInput.addEventListener("keypress", function(event) {
            if (event.key === "Enter") {
                event.preventDefault();
                sendMessage();
            }
        });
    }

    // Ensure the movie details overlay is closed on page load
    const detailsOverlay = document.getElementById('detailsOverlay');
    if (detailsOverlay) {
        detailsOverlay.classList.remove('active');
        console.log("Ensuring movie details overlay is hidden on page load");
    }
    
    // Add our new chat management UI
    setupChatManagement();
    
    // Load existing conversations ONLY via initializeHomePage now
    // // Load existing conversations if the container exists
    // if (document.querySelector('.chat-sidebar') || document.getElementById('chatHistoryContainer')) {
    //     loadChatConversations(); 
    // }
    
    // Add event listener to save conversation before page unload
    window.addEventListener('beforeunload', function() {
        // Only try to save if we have a conversation going
        if (chatHistory && chatHistory.length >= 2) {
            // Use direct save, not debounced for page exit
            saveCurrentConversation();
            
            // Return a string to potentially delay unload slightly
            // (modern browsers won't show this message for security reasons)
            return "Saving your conversation...";
        }
    });
});

// Function to update toggle button positions based on sidebar states
function updateToggleButtonPositions() {
    const chatSidebar = document.getElementById('chatHistorySidebar');
    const watchlistSidebar = document.querySelector('.watchlist-sidebar');
    const chatToggleBtn = document.getElementById('chatToggle');
    const watchlistToggleBtn = document.getElementById('watchlistToggle');
    
    if (chatSidebar && chatToggleBtn) {
        if (chatSidebar.classList.contains('collapsed')) {
            chatToggleBtn.classList.remove('sidebar-visible');
        } else {
            chatToggleBtn.classList.add('sidebar-visible');
        }
    }
    
    if (watchlistSidebar && watchlistToggleBtn) {
        if (watchlistSidebar.classList.contains('collapsed')) {
            watchlistToggleBtn.classList.remove('sidebar-visible');
        } else {
            watchlistToggleBtn.classList.add('sidebar-visible');
        }
    }
}

// New login functionality
function handleLogin(event) {
    event.preventDefault();

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    // Basic input validation
    if (!username || !password) {
        alert("Please fill in all fields");
        return;
    }

    // Placeholder for future authentication logic
    authenticateUser(username, password);
}

function authenticateUser(username, password) {
    // TODO: Add actual authentication logic here
    if (username === "admin" && password === "password") {
        storeUserSession(username);
        redirectToHome();
    } else {
        alert("Invalid credentials. Please try again.");
    }
}

function storeUserSession(username) {
    localStorage.setItem('user', username);
    localStorage.setItem('isLoggedIn', 'true');
}

function redirectToHome() {
    setTimeout(() => {
        window.location.href = "home.html";
    }, 300);
}

// Updated DOMContentLoaded event listener with authentication logic commented out
document.addEventListener('DOMContentLoaded', function() {
    // Authentication and redirect logic temporarily disabled
    /*
    const currentPath = window.location.pathname;
    const isLoginPage = currentPath.endsWith('/login');
    const isHomePage = currentPath.endsWith('home.html');
    const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';

    console.log('Current path:', currentPath);
    console.log('Is login page:', isLoginPage);
    console.log('Is logged in:', isLoggedIn);

    if (isLoginPage) {
        // If on login page and already logged in, redirect to home
        if (isLoggedIn) {
            window.location.href = 'home.html';
        }
    } else if (isHomePage) {
        // If on home page and not logged in, redirect to login
        if (!isLoggedIn) {
            window.location.href = '/login';
        }
    }
    */

    // Always initialize home page components regardless of login status
    initializeHomePage();
    initializeRatingSystem();
    loadWatchlist();
    
    // Initialize trending movies carousel if on home page
    if (document.getElementById('trendingCarousel')) {
        initializeTrendingMoviesCarousel();
    }
});

function initializeHomePage() {
    // Load actual chat history instead of adding example items
    loadChatConversations(); // Keep this one
    
    // Set up search functionality
    setupChatHistorySearch();
}

function toggleSidebar() {
    const chatSidebar = document.getElementById('chatHistorySidebar');
    const watchlistSidebar = document.querySelector('.watchlist-sidebar');
    const content = document.querySelector('.content');
    
    // Toggle chat sidebar visibility
    if (chatSidebar) {
        chatSidebar.classList.toggle('collapsed');
    }
    
    // Toggle watchlist sidebar visibility
    if (watchlistSidebar) {
        watchlistSidebar.classList.toggle('collapsed');
    }
    
    // Adjust content margins
    if (content) {
        content.classList.toggle('full-width');
    }
    
    // Store sidebar state in localStorage
    const isCollapsed = chatSidebar && chatSidebar.classList.contains('collapsed');
    localStorage.setItem('sidebarCollapsed', isCollapsed);
}

function handleLogout() {
    localStorage.removeItem('user');
    localStorage.removeItem('isLoggedIn');
    window.location.href = '/login';
}

function addChatHistoryItem(title, preview, conversationId, timestamp) {
    const historyList = document.getElementById('chatHistoryList'); // Use ID selector
    if (historyList) {
        const historyItem = document.createElement('div');
        historyItem.classList.add('chat-history-item');
        historyItem.dataset.conversationId = conversationId;
        
        // Use the raw timestamp for the data attribute
        const rawTimestamp = timestamp || new Date().toISOString(); // Fallback if timestamp is missing
        const formattedDate = formatTimestamp(rawTimestamp); // Format for initial display
        
        historyItem.innerHTML = `
            <div class="history-item-content">
            <h4>${title}</h4>
            <p>${preview}</p>
            </div>
            <div class="history-item-footer">
                <span class="history-timestamp" data-created-at="${rawTimestamp}"> 
                    ${formattedDate}
                </span>
                <button class="delete-conversation-btn" title="Delete conversation">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `;
        
        // Add click event for loading the conversation
        historyItem.addEventListener('click', (e) => {
            // If the delete button was clicked, don't load the conversation
            if (e.target.closest('.delete-conversation-btn')) {
                e.stopPropagation();
                if (confirm("Are you sure you want to delete this conversation?")) {
                    deleteConversation(conversationId);
                }
                return;
            }
            
            console.log('Loading conversation:', conversationId);
            loadConversation(conversationId);
            
            // On mobile, auto-collapse the sidebar after selection
            if (window.innerWidth <= 768) {
                toggleChatSidebar();
            }
        });
        
        historyList.appendChild(historyItem);
    }
}

// Function to format timestamps nicely
function formatTimestamp(timestamp) {
    if (!timestamp) return '';
    
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffMins < 1) {
        return 'Just now';
    } else if (diffMins < 60) {
        return `${diffMins}m ago`;
    } else if (diffHours < 24) {
        return `${diffHours}h ago`;
    } else if (diffDays < 7) {
        return `${diffDays}d ago`;
    } else {
        // Format as MM/DD/YYYY
        return date.toLocaleDateString();
    }
}

// Function to delete a conversation
function deleteConversation(conversationId) {
    console.log("Deleting conversation:", conversationId);
    
    // Use the correct API endpoint (matching the one used in other chat endpoints)
    fetch(`/api/chat/conversations/${conversationId}`, {
        method: 'DELETE'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`Failed to delete conversation: ${response.status}`);
        }
        
        // Show success toast
        showToast("Conversation deleted successfully");
        
        // Remove the conversation from the UI - ADD CHECK
        const historyItem = document.querySelector(`.chat-history-item[data-conversation-id="${conversationId}"]`);
        if (historyItem && historyItem.parentNode) { // Check if item exists
            historyItem.remove();
            console.log(`Removed conversation item ${conversationId} from UI.`);
        } else {
            console.warn(`Could not find conversation item ${conversationId} in UI to remove.`);
        }
        
        // If we deleted the current conversation, clear the chat
        if (currentConversationId === conversationId) {
            clearChat();
            currentConversationId = null;
            chatHistory = [];
        }
        
        // Refresh the list instead of manually checking for empty list
        loadChatConversations();
        
        console.log("Conversation deleted successfully");
    })
    .catch(error => {
        console.error("Error deleting conversation:", error);
        showToast(`Error deleting conversation: ${error.message}`);
    });
}

// Function to load a conversation
function loadConversation(conversationId) {
    console.log("Loading conversation messages:", conversationId);
    
    // Show loading indicator in chat
    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
        chatMessages.innerHTML = '<div class="loading-indicator">Loading conversation...</div>';
    }
    
    // Set the current conversation ID
    currentConversationId = conversationId;
    
    // Use the correct API endpoint (matching the one used in other chat endpoints)
    fetch(`/api/chat/conversations/${conversationId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to load conversation: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log(`Loaded conversation data:`, data);
            
            // Clear the chat and history
            clearChat();
            chatHistory = [];
            
            // Check if data has messages property or if messages are directly in the response
            const messages = data.messages || [];
            console.log(`Processing ${messages.length} messages`);
            
            // Add each message to the chat
            messages.forEach(message => {
                const isUser = message.role === 'user';
                addMessage(message.content, isUser ? 'user-message' : 'chatbot-message');
                
                // Add to chat history
                chatHistory.push({
                    role: isUser ? 'user' : 'assistant',
                    content: message.content
                });
            });
            
            // Highlight the selected conversation
            const historyItems = document.querySelectorAll('.chat-history-item');
            historyItems.forEach(item => {
                item.classList.remove('active');
                if (item.dataset.conversationId === conversationId.toString()) {
                    item.classList.add('active');
                }
            });
            
            // Update conversation title if available
            if (data.title) {
                const conversationTitle = document.getElementById('currentConversationTitle');
                if (conversationTitle) {
                    conversationTitle.textContent = data.title;
                }
            }
            
            // Open the chat if it's closed
            const chatOverlay = document.getElementById('chatOverlay');
            if (chatOverlay && !chatOverlay.classList.contains('visible')) {
                const openChatBtn = document.getElementById('openChatBtn');
                if (openChatBtn) {
                    openChatBtn.click();
                }
            }
        })
        .catch(error => {
            console.error("Error loading conversation:", error);
            if (chatMessages) {
                chatMessages.innerHTML = `<div class="error-message">Error loading conversation: ${error.message}</div>`;
            }
        });
}

// Function to clear the chat
function clearChat() {
    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
        chatMessages.innerHTML = '';
    }
}

// Add these new functions for watchlist management
function addToWatchlist(movieStr) {
    console.log("addToWatchlist called with raw string:", movieStr); // Log raw input
    // If movieStr is a string (from onclick), parse it
    let movie;
    try {
        movie = typeof movieStr === 'string' ? JSON.parse(movieStr) : movieStr;
        console.log("Parsed movie data for watchlist:", movie);
    } catch (e) {
        console.error("Error parsing movie data for watchlist:", e, "Input string was:", movieStr);
        showToast("Error: Could not process movie data.");
        return;
    }
    
    // Basic validation
    if (!movie || !movie.id || !movie.title) {
        console.error("Invalid movie data provided to addToWatchlist:", movie);
        showToast("Error: Missing movie information.");
        return;
    }
    
    const watchlistItem = {
        movieId: movie.id, // Ensure this matches backend expectation (e.g., Long or String)
        title: movie.title,
        posterPath: movie.posterPath, // Ensure this matches backend expectation
        overview: movie.overview
    };
    
    console.log("Sending POST request to /api/watchlist with payload:", watchlistItem);

    fetch('/api/watchlist', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(watchlistItem)
    })
    .then(response => {
        console.log("POST /api/watchlist response status:", response.status);
        if (!response.ok) {
            // Attempt to read error message from response body
            return response.text().then(text => {
                console.error("Add to watchlist failed. Status:", response.status, "Response:", text);
                // Check for specific error messages if needed
                if (response.status === 409 || (text && text.toLowerCase().includes('already in watchlist'))) {
                    throw new Error('Movie already in watchlist');
                } else {
                    throw new Error(`Failed to add movie (Status: ${response.status})`);
                }
            });
        }
        // If response is OK but doesn't necessarily have JSON body (e.g., 201 Created with no body)
        // Check content type before parsing
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
            return response.json();
        } else {
            return {}; // Return empty object if no JSON body
        }
    })
    .then(data => {
        console.log("Add to watchlist successful. Server response data:", data);
        loadWatchlist(); // Refresh UI
        showToast('Movie added to watchlist!');
    })
    .catch(error => {
        console.error("Error in addToWatchlist fetch chain:", error);
        showToast(error.message || 'Error adding movie to watchlist'); // Show specific error
    });
}

function removeFromWatchlist(movieId) {
    fetch(`/api/watchlist/${movieId}`, {
        method: 'DELETE'
    })
    .then(response => {
        if (response.ok) {
            loadWatchlist();
            showToast('Movie removed from watchlist');
        }
    })
    .catch(error => {
        showToast('Error removing movie from watchlist');
    });
}

function loadWatchlist() {
    const watchlistContent = document.getElementById('watchlistContent');
    
    // Show loading indicator
    if (watchlistContent) {
        watchlistContent.innerHTML = '<p class="loading-indicator">Loading watchlist...</p>';
    } else {
        console.error('Watchlist content element not found');
        return;
    }
    
    fetch('/api/watchlist')
    .then(response => {
        if (!response.ok) {
            throw new Error(`Watchlist API Error: ${response.status}`);
        }
        return response.json();
    })
    .then(items => {
        console.log("Received watchlist items:", items);
        
        // Clear the loading indicator
        watchlistContent.innerHTML = '';
        
        if (!Array.isArray(items)) {
            console.error('Watchlist API did not return an array. Received:', items);
            watchlistContent.innerHTML = '<p class="error-message">Error: Invalid watchlist data received.</p>';
            return;
        }

        if (items.length === 0) {
            watchlistContent.innerHTML = '<p class="empty-message">Your watchlist is empty. Add movies by clicking "Add to Watchlist" when viewing movie details.</p>';
            return;
        }
        
        items.forEach(item => {
            // Create a more visible watchlist entry
            const entryDiv = document.createElement('div');
            entryDiv.classList.add('watchlist-entry');
            entryDiv.dataset.movieId = item.movieId;
            
            // Create a more visible entry with better text contrast
            entryDiv.innerHTML = `
                <div class="thumbnail-container">
                    <img src="https://image.tmdb.org/t/p/w500${item.posterPath}" 
                         alt="${item.title}"
                         onerror="this.src='${DEFAULT_POSTER_PLACEHOLDER}'">
                    <div class="play-button" onclick="removeFromWatchlist(${item.movieId})">
                        <i class="fas fa-times"></i>
                    </div>
                </div>
                <div class="watchlist-entry-content">
                <h3>${item.title}</h3>
                    <p>${item.overview ? (item.overview.length > 100 ? item.overview.substring(0, 100) + '...' : item.overview) : 'No overview available'}</p>
                    <button class="view-details-btn" onclick="openMovieDetailsPopup('${item.movieId}')">
                        <i class="fas fa-info-circle"></i> View Details
                    </button>
                </div>
            `;
            watchlistContent.appendChild(entryDiv);
            
            // Make the entire entry clickable to view details
            entryDiv.addEventListener('click', function(e) {
                // Don't trigger if clicking the remove button
                if (!e.target.closest('.play-button') && !e.target.closest('.view-details-btn')) {
                    openMovieDetailsPopup(item.movieId);
                }
            });
        });
    })
    .catch(error => {
        console.error('Error loading watchlist:', error);
        watchlistContent.innerHTML = `<p class="error-message">Error loading watchlist: ${error.message}</p>`;
    });
}

// Add a simple toast notification function
function showToast(message) {
    const toast = document.createElement('div');
    toast.classList.add('toast');
    toast.textContent = message;
    
    // Add improved styling
    toast.style.position = 'fixed';
    toast.style.bottom = '20px';
    toast.style.right = '20px';
    toast.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
    toast.style.color = 'white';
    toast.style.padding = '10px 20px';
    toast.style.borderRadius = '4px';
    toast.style.zIndex = '10000';
    toast.style.transition = 'opacity 0.3s, transform 0.3s';
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(20px)';
    toast.style.boxShadow = '0 4px 8px rgba(0, 0, 0, 0.2)';
    toast.style.fontSize = '14px';
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = '1';
        toast.style.transform = 'translateY(0)';
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(20px)';
            setTimeout(() => {
                toast.remove();
            }, 300);
        }, 5000); // Show for 5 seconds instead of 3
    }, 100);
}

function showMovieDetails(movieId) {
    // Check if movieId is valid
    if (!movieId) {
        console.error('Cannot show movie details: No movie ID provided');
        showToast('Error: No movie ID available');
        return;
    }

    // Known movie mappings - keep in sync with processContent
    const knownMovieIds = {
        'barbie': '346698',
        'oppenheimer': '872585',
        'dune': '438631',
        'dune part two': '693134',
        'the batman': '414906',
        'inception': '27205',
        'interstellar': '157336',
        'the dark knight': '155'
    };

    // Handle cases where we're talking about Barbie specifically
    const pageUrl = window.location.href.toLowerCase();
    if ((pageUrl.includes('barbie') || window.barbieMentioned === true) && movieId !== knownMovieIds['barbie']) {
        console.log(`Overriding requested movie ID ${movieId} with Barbie ID (${knownMovieIds['barbie']}) based on context`);
        movieId = knownMovieIds['barbie'];
    }

    // --- Set the last mentioned movie ID --- 
    console.log(`Setting lastMentionedMovieId to: ${movieId}`);
    lastMentionedMovieId = movieId;
    
    // Use the detailsOverlay instead of directly modifying modal display
    const detailsOverlay = document.getElementById('detailsOverlay');
    const chatBox = document.querySelector('.chat-box');
    
    if (detailsOverlay) {
        detailsOverlay.classList.add('active');
        if (chatBox) chatBox.style.display = 'none';  // Hide chat interface
        document.body.classList.add('modal-open');
    }
    console.log("Showing details for movie:", movieId); // Debug log
    
    fetch(`/api/movies/${movieId}/details`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`Movie details not found (Status: ${response.status})`);
            }
            return response.json();
        })
        .then(movie => {
            // Update modal content with movie details (Existing code)
            document.getElementById('modalMoviePoster').src = 
                movie.poster_path ? `https://image.tmdb.org/t/p/w500${movie.poster_path}` : DEFAULT_POSTER_PLACEHOLDER;
            document.getElementById('modalMovieTitle').textContent = movie.title;
            document.getElementById('modalMovieYear').textContent = 
                movie.release_date ? movie.release_date.split('-')[0] : 'N/A';
            document.getElementById('modalMovieRating').textContent = 
                `⭐ ${movie.vote_average ? movie.vote_average.toFixed(1) : 'N/A'}/10`; // Added toFixed(1)
            document.getElementById('modalMovieOverview').textContent = movie.overview;
            
            // Update genres (Existing code)
            const genresContainer = document.getElementById('modalMovieGenres');
            genresContainer.innerHTML = movie.genres
                .map(genre => `<span class="genre-tag">${genre.name}</span>`)
                .join('');
            
            // Update cast (Existing code)
            const castContainer = document.getElementById('modalMovieCast');
            castContainer.innerHTML = movie.credits && movie.credits.cast && movie.credits.cast.length > 0
                ? movie.credits.cast
                    .slice(0, 6)
                    .map(actor => `
                        <div class="cast-member">
                                <img src="${actor.profile_path ? 'https://image.tmdb.org/t/p/w200' + actor.profile_path : DEFAULT_PROFILE_PLACEHOLDER}" 
                                 alt="${actor.name}"
                                 onerror="this.src='${DEFAULT_PROFILE_PLACEHOLDER}'">
                            <div class="actor-name">${actor.name}</div>
                            <div class="character-name">${actor.character}</div>
                        </div>
                    `)
                    .join('')
                : '<p>No cast information available</p>';
            
            // Update trailer (Existing code)
            const trailerContainer = document.getElementById('modalMovieTrailer');
            const videos = movie.videos && movie.videos.results ? movie.videos.results : [];
            const trailer = videos.find(video => video.type === 'Trailer' && video.site === 'YouTube');
            
            if (trailer) {
                trailerContainer.innerHTML = `
                    <h4>Trailer</h4>
                    <div class="trailer-container">
                        <iframe src="https://www.youtube.com/embed/${trailer.key}" 
                                frameborder="0" 
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                allowfullscreen>
                        </iframe>
                    </div>
                `;
            } else {
                trailerContainer.innerHTML = '<p>No trailer available</p>';
            }
            
            // Update similar movies (Existing code)
            const similarContainer = document.getElementById('modalSimilarMovies');
            const similarResults = movie.similar && movie.similar.results && Array.isArray(movie.similar.results) 
                ? movie.similar.results
                : [];
            
            if (similarResults.length > 0) {
                similarContainer.innerHTML = similarResults
                    .slice(0, 6)
                    .map(similar => `
                        <div class="similar-movie" onclick="showMovieDetails(${similar.id})">
                                <img src="${similar.poster_path ? 'https://image.tmdb.org/t/p/w200' + similar.poster_path : DEFAULT_POSTER_PLACEHOLDER}" 
                                     alt="${similar.title}" 
                                     onerror="this.src='${DEFAULT_POSTER_PLACEHOLDER}'">
                            <div class="movie-title">${similar.title}</div>
                                <div class="movie-year">${similar.release_date ? similar.release_date.split('-')[0] : 'N/A'}</div>
                        </div>
                    `)
                    .join('');
            } else {
                similarContainer.innerHTML = '<p>No similar movies found</p>';
            }
            
            // Initialize rating system (Existing code)
            document.getElementById('submitRating').dataset.movieId = movie.id;
            initializeRatingSystem();
            loadMovieRatings(movie.id);

            // **** NEW: Add "Add to Watchlist" Button (Refactored) ****
            // Prepare a movie object for the watchlist with needed fields
            const movieForWatchlist = {
                id: movie.id,
                title: movie.title,
                posterPath: movie.poster_path, // Use poster_path from TMDB response
                overview: movie.overview
            };
            
            // Find or create the actions container
            let actionsContainer = document.getElementById('modalActions');
            if (!actionsContainer) {
                console.warn("#modalActions container not found in HTML, creating dynamically.");
                actionsContainer = document.createElement('div');
                actionsContainer.id = 'modalActions';
                actionsContainer.style.marginTop = '20px'; 
                actionsContainer.style.textAlign = 'center'; 
                // Try to insert after overview or before cast (as before)
                const overviewElement = document.getElementById('modalMovieOverview');
                const castElement = document.querySelector('.movie-cast'); 
                if (overviewElement && overviewElement.parentNode) {
                    overviewElement.parentNode.insertBefore(actionsContainer, overviewElement.nextSibling);
                } else if (castElement && castElement.parentNode) {
                     castElement.parentNode.insertBefore(actionsContainer, castElement);
                } else {
                     const modalContent = document.querySelector('#movieModal .modal-content');
                     if(modalContent) modalContent.appendChild(actionsContainer);
                }
            }
            
            // Clear previous buttons/listeners in the container
            actionsContainer.innerHTML = ''; 

            // Create the button element
            const watchlistBtn = document.createElement('button');
            watchlistBtn.className = 'add-watchlist-btn';
            watchlistBtn.innerHTML = '<i class="fas fa-plus-circle"></i> Add to Watchlist';
            
            // Store the raw JSON data in a data attribute
            watchlistBtn.dataset.movieData = JSON.stringify(movieForWatchlist);

            // Add event listener to the button
            watchlistBtn.addEventListener('click', function() {
                console.log("Add to Watchlist button clicked.");
                // Retrieve the data from the attribute
                const movieDataString = this.dataset.movieData;
                // Call the existing function which expects a string (and parses it)
                addToWatchlist(movieDataString);
            });

            // Apply styles
            watchlistBtn.style.padding = '10px 15px';
            watchlistBtn.style.backgroundColor = '#e50914';
            watchlistBtn.style.color = 'white';
            watchlistBtn.style.border = 'none';
            watchlistBtn.style.borderRadius = '4px';
            watchlistBtn.style.cursor = 'pointer';
            watchlistBtn.style.fontSize = '14px';
            
            // Append the button to the container
            actionsContainer.appendChild(watchlistBtn);
        })
        .catch(error => {
            console.error('Error fetching movie details:', error);
            showToast(`Error loading movie details: ${error.message}`);
            
            // Close the modal and return to chat if details cannot be fetched
            backToChat();
        });
}

function backToChat() {
    const detailsOverlay = document.getElementById('detailsOverlay');
    const chatBox = document.querySelector('.chat-box');
    
    if (detailsOverlay) {
        detailsOverlay.classList.remove('active');
        if (chatBox) chatBox.style.display = 'block';  // Show chat interface
        document.body.classList.remove('modal-open');
    }
}

// Add these functions to handle ratings
function initializeRatingSystem() {
    const stars = document.querySelectorAll('.stars i');
    let currentRating = 0;

    // --- Remove existing listeners for stars first (optional but good practice) ---
    stars.forEach(star => {
        const newStar = star.cloneNode(true); // Clone to remove listeners
        star.parentNode.replaceChild(newStar, star);
    });

    // --- Re-query stars after cloning and attach listeners --- 
    const newStars = document.querySelectorAll('.stars i');
    newStars.forEach(star => {
        star.addEventListener('mouseover', function() {
            const rating = this.dataset.rating;
            highlightStars(rating, newStars); // Pass the star collection
        });

        star.addEventListener('mouseout', function() {
            highlightStars(currentRating, newStars); // Pass the star collection
        });

        star.addEventListener('click', function() {
            currentRating = this.dataset.rating;
            highlightStars(currentRating, newStars); // Pass the star collection
        });
    });

    // --- Remove existing listener from submit button by cloning --- 
    const submitButton = document.getElementById('submitRating');
    const newSubmitButton = submitButton.cloneNode(true); // Clone the button
    submitButton.parentNode.replaceChild(newSubmitButton, submitButton); // Replace original with clone

    // --- Attach listener to the NEW button --- 
    newSubmitButton.addEventListener('click', function() {
        // Reset rating if needed or read from stars
        const selectedStar = document.querySelector('.stars i.fas'); // Find the last selected star
        if (selectedStar) {
            currentRating = parseInt(selectedStar.dataset.rating); // Update currentRating from UI
        } else {
            currentRating = 0; // Or handle as error if no star is selected
        }

        if (currentRating === 0) {
            showToast('Please select a rating (click on a star)');
            return;
        }

        const movieId = this.dataset.movieId;
        const review = document.getElementById('reviewText').value;

        submitRating(movieId, currentRating, review);
        
        // Optionally reset the rating UI after submission
        highlightStars(0, newStars);
        currentRating = 0;
        document.getElementById('reviewText').value = ''; // Clear review text
    });
}

// Update highlightStars to accept the stars collection
function highlightStars(rating, stars) {
    // const stars = document.querySelectorAll('.stars i'); // No need to re-query
    stars.forEach(star => {
        const starRating = star.dataset.rating;
        star.classList.remove('fas', 'far');
        star.classList.add(starRating <= rating ? 'fas' : 'far');
    });
}

function submitRating(movieId, rating, review) {
    fetch('/api/ratings', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            movieId: movieId,
            rating: rating,
            review: review
        })
    })
    .then(response => response.json())
    .then(data => {
        showToast('Rating submitted successfully!');
        loadMovieRatings(movieId);
    })
    .catch(error => {
        showToast('Error submitting rating');
    });
}

function loadMovieRatings(movieId) {
    fetch(`/api/ratings/${movieId}`)
    .then(response => response.json())
    .then(data => {
        // Update average rating
        document.getElementById('modalAverageRating').textContent = 
            data.averageRating.toFixed(1);
        document.getElementById('modalRatingCount').textContent = 
            data.totalRatings;

        // Update reviews
        const reviewsContainer = document.getElementById('movieReviews');
        reviewsContainer.innerHTML = data.ratings
            .map(rating => `
                <div class="review">
                    <div class="review-header">
                        <div class="review-stars">
                            ${getStarIcons(rating.rating)}
                        </div>
                        <div class="review-author">
                            by ${rating.username}
                        </div>
                    </div>
                    ${rating.review ? `<div class="review-text">${rating.review}</div>` : ''}
                </div>
            `)
            .join('');
    })
    .catch(error => {
        console.error('Error loading ratings:', error);
    });
}

function getStarIcons(rating) {
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
}

// Add these functions somewhere appropriate in the file
let currentSlide = 0;
let slideshowInterval;
let trendingMovies = [];

async function fetchTrendingMovies() {
    try {
        console.log("Fetching movies for carousel...");
        const response = await fetch('/api/movies/trending');
        console.log("API response status:", response.status);
        
        if (!response.ok) {
            console.error("Failed to fetch movies:", response.status, response.statusText);
            throw new Error('Failed to fetch movies');
        }
        
        const data = await response.json();
        console.log("Received movies data:", data);
        
        if (!data || data.length === 0) {
            console.warn("No movies returned from API");
            return [];
        }
        
        // Log each movie's data for debugging
        data.forEach((movie, index) => {
            console.log(`Movie ${index + 1}:`, {
                title: movie.title,
                id: movie.id,
                posterPath: movie.posterPath,
                hasValidPoster: !!movie.posterPath
            });
        });
        
        return data;
    } catch (error) {
        console.error('Error fetching movies:', error);
        return [];
    }
}

async function initializeTrendingMoviesCarousel() {
    try {
        console.log("Initializing movie carousel...");
        const carouselContainer = document.getElementById('trendingCarousel');
        
        if (!carouselContainer) {
            console.warn("Carousel container not found");
            return;
        }
        
        // Show loading state
        carouselContainer.innerHTML = `
            <div class="carousel-slide active">
                <div class="carousel-content">
                    <h3 class="carousel-title">Loading movies...</h3>
                </div>
            </div>
        `;
        
        // Fetch movies
        const movies = await fetchTrendingMovies();
        
        if (!movies || movies.length === 0) {
            console.warn("No movies available for carousel");
            carouselContainer.innerHTML = `
                <div class="carousel-slide active">
                    <div class="carousel-content">
                        <h3 class="carousel-title">No movies available at the moment</h3>
                        <p>Please try again later</p>
                    </div>
                </div>
            `;
            return;
        }
        
        console.log(`Building carousel with ${movies.length} movies`);
        
        // Build carousel slides
        buildCarouselSlides(movies);
        
        // Set up indicators
        setupCarouselIndicators(movies.length);
        
        // Set up controls
        setupCarouselControls();
        
        // Show first slide
        showSlide(0);
        
        // Start automatic slideshow
        startSlideshow();
        
        console.log("Carousel initialization complete");
    } catch (error) {
        console.error('Error initializing movie carousel:', error);
        
        // Show error state in carousel
        if (carouselContainer) {
            carouselContainer.innerHTML = `
                <div class="carousel-slide active">
                    <div class="carousel-content">
                        <h3 class="carousel-title">Unable to load movies</h3>
                        <p>Please try again later</p>
                    </div>
                </div>
            `;
        }
    }
}

// Create a more visually appealing fallback SVG
function createFallbackImage(movieTitle) {
    return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
        <svg xmlns="http://www.w3.org/2000/svg" width="500" height="750" viewBox="0 0 500 750">
            <defs>
                <linearGradient id="bgGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" stop-color="#1a1a1a" />
                    <stop offset="100%" stop-color="#333333" />
                </linearGradient>
            </defs>
            <rect width="500" height="750" fill="url(#bgGradient)"/>
            <rect x="125" y="200" width="250" height="150" rx="15" fill="#222" stroke="#e50914" stroke-width="2"/>
            <circle cx="175" cy="250" r="25" fill="#444" stroke="#e50914" stroke-width="2"/>
            <circle cx="325" cy="250" r="25" fill="#444" stroke="#e50914" stroke-width="2"/>
            <rect x="150" y="300" width="200" height="25" rx="5" fill="#333"/>
            <text x="250" y="450" font-family="Arial" font-size="30" fill="#e50914" text-anchor="middle" font-weight="bold">No Image Available</text>
            <text x="250" y="490" font-family="Arial" font-size="22" fill="#ffffff" text-anchor="middle">${movieTitle}</text>
            <text x="250" y="520" font-family="Arial" font-size="16" fill="#aaaaaa" text-anchor="middle">FilmSage Movie Database</text>
        </svg>
    `);
}

function buildCarouselSlides(movies) {
    const carouselContainer = document.getElementById('trendingCarousel');
    carouselContainer.innerHTML = '';
    
    if (!movies || movies.length === 0) {
        const errorSlide = document.createElement('div');
        errorSlide.className = 'carousel-slide error-slide active';
        errorSlide.innerHTML = `
            <div class="carousel-content" style="top: 50%; transform: translateY(-50%); text-align: center;">
                <h3 class="carousel-title">Unable to load trending movies</h3>
                <p>Please check your connection or try again later.</p>
            </div>
        `;
        carouselContainer.appendChild(errorSlide);
        return;
    }
    
    movies.forEach((movie, index) => {
        const slide = document.createElement('div');
        slide.className = 'carousel-slide';
        slide.dataset.index = index;
        
        // Use our enhanced fallback image generator
        const fallbackImage = createFallbackImage(movie.title);
        
        let posterUrl = fallbackImage;
        
        // Check both posterPath and poster_path properties (API might return either format)
        const posterPath = movie.posterPath || movie.poster_path;
        
        if (posterPath) {
            // Check if the posterPath already contains the full URL
            if (posterPath.startsWith('http')) {
                posterUrl = posterPath;
            } else {
                // Construct the full TMDB URL
                posterUrl = `https://image.tmdb.org/t/p/w500${posterPath}`;
            }
        }
            
        console.log(`Movie ${index}: ${movie.title}, Poster path: ${posterPath}, Full URL: ${posterUrl}`);
            
        const year = movie.releaseDate || movie.release_date ? 
            (movie.releaseDate || movie.release_date).split('-')[0] : 'N/A';
        
        slide.innerHTML = `
            <div class="carousel-image-container">
                <img class="carousel-image" 
                     src="${posterUrl}" 
                     alt="${movie.title}" 
                     onerror="this.onerror=null; this.src='${fallbackImage}';">
            </div>
            <div class="carousel-content">
                <h3 class="carousel-title">${movie.title}</h3>
                <p class="carousel-overview">${movie.overview || 'No overview available'}</p>
                <div class="carousel-meta">
                    <span class="carousel-rating">⭐ ${movie.rating || movie.vote_average ? (movie.rating || movie.vote_average).toFixed(1) : 'N/A'}</span>
                    <span class="carousel-year">${year}</span>
                </div>
                <button class="view-details-btn" onclick="showMovieDetails(${movie.id})">View Details</button>
            </div>
        `;
        
        carouselContainer.appendChild(slide);
    });
}

function setupCarouselIndicators(slideCount) {
    const indicatorsContainer = document.getElementById('carouselIndicators');
    indicatorsContainer.innerHTML = '';
    
    for (let i = 0; i < slideCount; i++) {
        const indicator = document.createElement('div');
        indicator.className = 'carousel-indicator';
        indicator.dataset.index = i;
        
        indicator.addEventListener('click', () => {
            stopSlideshow();
            showSlide(i);
            startSlideshow();
        });
        
        indicatorsContainer.appendChild(indicator);
    }
}

function setupCarouselControls() {
    const prevButton = document.getElementById('carouselPrev');
    const nextButton = document.getElementById('carouselNext');
    
    prevButton.addEventListener('click', () => {
        stopSlideshow();
        showPrevSlide();
        startSlideshow();
    });
    
    nextButton.addEventListener('click', () => {
        stopSlideshow();
        showNextSlide();
        startSlideshow();
    });
}

function showSlide(index) {
    const slides = document.querySelectorAll('.carousel-slide');
    const indicators = document.querySelectorAll('.carousel-indicator');
    
    if (slides.length === 0) return;
    
    // Hide all slides
    slides.forEach(slide => {
        slide.classList.remove('active');
    });
    
    // Deactivate all indicators
    indicators.forEach(indicator => {
        indicator.classList.remove('active');
    });
    
    // Show selected slide
    currentSlide = index;
    slides[currentSlide].classList.add('active');
    indicators[currentSlide].classList.add('active');
}

function showNextSlide() {
    const slides = document.querySelectorAll('.carousel-slide');
    if (slides.length === 0) return;
    
    currentSlide = (currentSlide + 1) % slides.length;
    showSlide(currentSlide);
}

function showPrevSlide() {
    const slides = document.querySelectorAll('.carousel-slide');
    if (slides.length === 0) return;
    
    currentSlide = (currentSlide - 1 + slides.length) % slides.length;
    showSlide(currentSlide);
}

function startSlideshow() {
    stopSlideshow(); // Clear any existing interval
    slideshowInterval = setInterval(showNextSlide, 5000); // Change slide every 5 seconds
}

function stopSlideshow() {
    if (slideshowInterval) {
        clearInterval(slideshowInterval);
    }
}

// Replace with these new separate toggle functions
function toggleChatSidebar() {
    console.log("toggleChatSidebar function called");
    const chatSidebar = document.getElementById('chatHistorySidebar');
    
    if (chatSidebar) {
        const wasCollapsed = chatSidebar.classList.contains('collapsed');
        chatSidebar.classList.toggle('collapsed');
        
        // Store chat sidebar state in localStorage
        const isCollapsed = chatSidebar.classList.contains('collapsed');
        console.log("Chat sidebar collapsed state changed from", wasCollapsed, "to", isCollapsed);
        localStorage.setItem('chatSidebarCollapsed', isCollapsed);
        
        // Update the toggle button position based on sidebar state
        const chatToggle = document.getElementById('chatToggle');
        if (chatToggle) {
            if (isCollapsed) {
                chatToggle.classList.remove('sidebar-visible');
            } else {
                chatToggle.classList.add('sidebar-visible');
            }
        }
        
        // Update content margins based on both sidebars
        updateContentMargins();
    } else {
        console.error("Chat history sidebar element not found");
    }
}

function toggleWatchlistSidebar() {
    console.log("toggleWatchlistSidebar function called");
    const watchlistSidebar = document.querySelector('.watchlist-sidebar');
    
    if (watchlistSidebar) {
        const wasCollapsed = watchlistSidebar.classList.contains('collapsed');
        watchlistSidebar.classList.toggle('collapsed');
        
        // Store watchlist sidebar state in localStorage
        const isCollapsed = watchlistSidebar.classList.contains('collapsed');
        console.log("Watchlist sidebar collapsed state changed from", wasCollapsed, "to", isCollapsed);
        localStorage.setItem('watchlistSidebarCollapsed', isCollapsed);
        
        // Update the toggle button position based on sidebar state
        const watchlistToggle = document.getElementById('watchlistToggle');
        if (watchlistToggle) {
            if (isCollapsed) {
                watchlistToggle.classList.remove('sidebar-visible');
            } else {
                watchlistToggle.classList.add('sidebar-visible');
            }
        }
        
        // Update content margins based on both sidebars
        updateContentMargins();

        // If watchlist is now visible, ensure content is loaded
        if (!isCollapsed) {
            loadWatchlist();
        }
    } else {
        console.error("Watchlist sidebar element not found");
    }
}

function updateContentMargins() {
    console.log("Updating content margins");
    const content = document.querySelector('.content');
    const chatSidebar = document.getElementById('chatHistorySidebar');
    const watchlistSidebar = document.querySelector('.watchlist-sidebar');
    
    if (!content) {
        console.error("Content element not found");
        return;
    }
    
        // Reset classes
        content.classList.remove('full-width', 'chat-collapsed', 'watchlist-collapsed', 'both-collapsed');
        
        // Determine which sidebars are collapsed
        const isChatCollapsed = chatSidebar && chatSidebar.classList.contains('collapsed');
        const isWatchlistCollapsed = watchlistSidebar && watchlistSidebar.classList.contains('collapsed');
    
    console.log("Sidebar states - Chat collapsed:", isChatCollapsed, "Watchlist collapsed:", isWatchlistCollapsed);
        
        // Apply appropriate class based on sidebars state
        if (isChatCollapsed && isWatchlistCollapsed) {
            content.classList.add('both-collapsed');
        } else if (isChatCollapsed) {
            content.classList.add('chat-collapsed');
        } else if (isWatchlistCollapsed) {
            content.classList.add('watchlist-collapsed');
        }
    
    // Directly set margins for more precise control
    const leftMargin = isChatCollapsed ? '0' : '300px';
    const rightMargin = isWatchlistCollapsed ? '0' : '300px';
    
    content.style.marginLeft = leftMargin;
    content.style.marginRight = rightMargin;
    console.log("Set content margins to - Left:", leftMargin, "Right:", rightMargin);
}

// Ensure updateContentMargins is called when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Add this near the end of the existing DOMContentLoaded listener
    setTimeout(updateContentMargins, 100); // Slight delay to ensure all DOM elements are ready
});

// NEW Function to fetch and display trailer and providers
async function displayMovieTrailerAndProviders(containerElement) {
    const movieId = containerElement.dataset.movieId;
    if (!movieId) return;

    containerElement.innerHTML = ''; // Clear loading message

    try {
        // Fetch trailer
        const trailerResponse = await fetch(`/api/movies/${movieId}/trailer`);
        let trailerKey = null;
        if (trailerResponse.ok) {
            const trailerData = await trailerResponse.json();
            trailerKey = trailerData.trailerKey;
        }

        // Fetch providers
        const providersResponse = await fetch(`/api/movies/${movieId}/providers`);
        let providersData = null;
        if (providersResponse.ok) {
            providersData = await providersResponse.json();
        }

        // Create trailer embed if key exists
        if (trailerKey) {
            const trailerDiv = document.createElement('div');
            trailerDiv.className = 'trailer-container';
            trailerDiv.innerHTML = `
                <h4>Trailer</h4>
                <iframe 
                    width="100%" 
                    height="315" 
                    src="https://www.youtube.com/embed/${trailerKey}" 
                    title="YouTube video player" 
                    frameborder="0" 
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowfullscreen>
                </iframe>
            `;
            containerElement.appendChild(trailerDiv);
        }

        // Create providers section if data exists
        if (providersData && providersData.link) {
            const providersDiv = document.createElement('div');
            providersDiv.className = 'providers-container';
            let providersHtml = `<h4>Where to Watch</h4>`;
            providersHtml += `<a href="${providersData.link}" target="_blank" class="justwatch-link">View on JustWatch</a>`;
            
            const providerTypes = { 
                flatrate: "Stream", 
                rent: "Rent", 
                buy: "Buy" 
            };
            
            providersHtml += '<div class="provider-options">';
            for (const type in providerTypes) {
                if (providersData[type] && providersData[type].length > 0) {
                    providersHtml += `<div class="provider-category"><h5>${providerTypes[type]}:</h5><div class="provider-logos">`;
                    providersData[type].forEach(provider => {
                         providersHtml += `
                             <img src="https://image.tmdb.org/t/p/w45${provider.logo_path}" 
                                  alt="${provider.provider_name}" 
                                  title="${provider.provider_name}" 
                                  class="provider-logo">
                         `;
                    });
                    providersHtml += '</div></div>';
                }
            }
             providersHtml += '</div>';

            providersDiv.innerHTML = providersHtml;
            containerElement.appendChild(providersDiv);
        } else if (!trailerKey) {
             // If neither trailer nor providers found
             containerElement.innerHTML = 'Sorry, no trailer or watch options found for this movie.';
        }

    } catch (error) {
        console.error(`Error fetching movie extras for ID ${movieId}:`, error);
        containerElement.innerHTML = 'Error loading trailer and providers.';
    }
}

// --- ADDED: Event listeners for chat overlay --- 
document.addEventListener('DOMContentLoaded', () => {
    const openChatBtn = document.getElementById('openChatBtn');
    const closeChatBtn = document.getElementById('closeChatBtn');
    const chatOverlay = document.getElementById('chatOverlay');
    const chatInput = document.getElementById('userInput'); // Or 'chat-input'

    if (openChatBtn && chatOverlay) {
        openChatBtn.addEventListener('click', () => {
            console.log("Open chat button clicked");
            chatOverlay.classList.add('visible');
            // Optionally focus the input field when opened
            if (chatInput) {
                chatInput.focus();
            }
        });
    }

    if (closeChatBtn && chatOverlay) {
        closeChatBtn.addEventListener('click', () => {
            console.log("Close chat button clicked");
            chatOverlay.classList.remove('visible');
        });
    }

    // Optional: Close chat when clicking outside the chat box
    if (chatOverlay) {
        chatOverlay.addEventListener('click', (event) => {
            // Check if the click was outside the chat-box itself
            if (event.target === chatOverlay) {
                 console.log("Clicked outside chat box");
                 chatOverlay.classList.remove('visible');
            }
        });
    }
    
    // Handle Enter key in chat input
    if (chatInput) {
        chatInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                sendMessage();
            }
        });
    }
    
    // --- Initial setup calls --- 
    loadTrendingMovies(); 
    loadWatchlist();
    applySidebarStates();
    updateContentMargins(); // Initial margin update
    
    // Add listeners for sidebar toggles if they exist
    const chatToggle = document.getElementById('chatToggle');
    const watchlistToggle = document.getElementById('watchlistToggle');
    if (chatToggle) {
        chatToggle.addEventListener('click', toggleChatSidebar);
    }
    if (watchlistToggle) {
        watchlistToggle.addEventListener('click', toggleWatchlistSidebar);
    }
});

function applySidebarStates() {
     const chatSidebar = document.getElementById('chatHistorySidebar');
     const watchlistSidebar = document.querySelector('.watchlist-sidebar');
    const chatToggle = document.getElementById('chatToggle');
    const watchlistToggle = document.getElementById('watchlistToggle');
     
     const chatCollapsed = localStorage.getItem('chatSidebarCollapsed') === 'true';
     const watchlistCollapsed = localStorage.getItem('watchlistSidebarCollapsed') === 'true';
     
    // Apply chat sidebar state
    if (chatSidebar) {
        if (chatCollapsed) {
         chatSidebar.classList.add('collapsed');
            if (chatToggle) chatToggle.classList.remove('sidebar-visible');
        } else {
            chatSidebar.classList.remove('collapsed');
            if (chatToggle) chatToggle.classList.add('sidebar-visible');
        }
    }
    
    // Apply watchlist sidebar state
    if (watchlistSidebar) {
        if (watchlistCollapsed) {
         watchlistSidebar.classList.add('collapsed');
            if (watchlistToggle) watchlistToggle.classList.remove('sidebar-visible');
        } else {
            watchlistSidebar.classList.remove('collapsed');
            if (watchlistToggle) watchlistToggle.classList.add('sidebar-visible');
            // Load watchlist content if sidebar is visible
            loadWatchlist();
        }
    }
    
    // Update content margins
    updateContentMargins();
}

// --- NEW: Functions to handle the details popup overlay ---
function openMovieDetailsPopup(movieId) {
  const detailsOverlay = document.getElementById('detailsOverlay');
  if (!detailsOverlay) {
      console.error("Details overlay element not found!");
      return;
  }

  console.log(`Opening details popup for movie ID: ${movieId}`);
  
  // *** Crucial Step: Fetch and Populate Details ***
  // We need to make sure the content inside the #movieModal div 
  // (which is inside #detailsOverlay) gets updated. 
  // We can REUSE the showMovieDetails logic, but we need to ensure it 
  // targets the correct elements within the *now potentially hidden* modal structure.
  // The existing showMovieDetails might work if it correctly finds elements by ID,
  // but we might need to adjust it if it relied on the modal being directly visible.
  
  // For now, let's assume showMovieDetails correctly updates the modal's content 
  // even if the parent overlay is hidden initially.
  showMovieDetails(movieId); // Call existing function to populate content

  // Make the overlay visible
  detailsOverlay.classList.add('active');

  // Dim the chat overlay if it's open
  const chatOverlay = document.getElementById('chatOverlay');
  if (chatOverlay && chatOverlay.classList.contains('visible')) {
    chatOverlay.style.opacity = '0.3'; // Dim it
    chatOverlay.style.pointerEvents = 'none'; // Prevent interaction
  }
}

function closeMovieDetailsPopup() {
  const detailsOverlay = document.getElementById('detailsOverlay');
  if (detailsOverlay) {
    console.log("Closing details popup");
    detailsOverlay.classList.remove('active');
  }
  
  // Restore chat overlay if it was dimmed
  const chatOverlay = document.getElementById('chatOverlay');
  if (chatOverlay) {
    chatOverlay.style.opacity = '1';
    chatOverlay.style.pointerEvents = 'auto';
  }
  
  // Clear the last mentioned movie ID when popup is closed?
  // lastMentionedMovieId = null; // Optional: Decide if you want to reset this
}
// --- END: Details popup functions ---

// --- Define loadTrendingMovies if it doesn't exist ---
// This prevents the Reference Error 
if (typeof loadTrendingMovies !== 'function') {
    function loadTrendingMovies() {
        console.log("loadTrendingMovies called but not fully implemented");
        // This is a stub function to prevent console errors
        // Implement actual functionality as needed
    }
}

// Function to extract movie title from "show details for X" pattern
function extractMovieTitleFromShowDetailsCommand(message) {
    const lowerMsg = message.toLowerCase().trim();
    // Only trigger if the command is exactly one of these phrases
    if (lowerMsg === 'show details' ||
        lowerMsg === 'show me details' ||
        lowerMsg === 'show trailer' ||
        lowerMsg === 'show me trailer') {
        return true;
    }
    // Otherwise, do not extract a new movie title and let the default lastMentionedMovieId remain
    return false;
}

// Add a flag to prevent concurrent loading
let isLoadingConversations = false;

/**
 * Load chat conversations from the server and display them in the sidebar
 */
function loadChatConversations() {
    // Prevent concurrent loads
    if (isLoadingConversations) {
        console.log("loadChatConversations skipped: already in progress.");
        return;
    }
    isLoadingConversations = true;
    console.log("loadChatConversations started...");

    // Get the chat history container
    const chatHistoryList = document.getElementById('chatHistoryList');
    if (!chatHistoryList) {
        console.warn('Chat history container not found');
        isLoadingConversations = false; // Release lock
        return;
    }
    
    // Clear the list (except for the "no conversations" message)
    const noConversationsDiv = chatHistoryList.querySelector('.no-conversations');
    console.log("Clearing chat history list before loading.");
    chatHistoryList.innerHTML = ''; 
    if (noConversationsDiv) {
        chatHistoryList.appendChild(noConversationsDiv);
        noConversationsDiv.style.display = 'none'; // Hide initially
    }
    
    // Show loading indicator
    const loadingDiv = document.createElement('div');
    loadingDiv.className = 'loading-conversations';
    loadingDiv.textContent = 'Loading conversations...';
    chatHistoryList.appendChild(loadingDiv);
    
    // Fetch conversations from the server
    fetch('/api/chat/conversations', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`Failed to fetch conversations (Status: ${response.status})`);
        }
        return response.json();
    })
    .then(conversations => {
        // Remove loading indicator
        if (loadingDiv && loadingDiv.parentNode === chatHistoryList) {
            chatHistoryList.removeChild(loadingDiv);
        } 
        
        console.log(`Loaded ${conversations ? conversations.length : 0} conversations from server`, conversations);
        
        if (conversations && conversations.length > 0) {
            if (noConversationsDiv) {
                noConversationsDiv.style.display = 'none';
            }
            conversations.sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));
            conversations.forEach(conversation => {
                const title = conversation.title || `Chat ${conversation.id} (${new Date(conversation.createdAt).toLocaleDateString()})`;
                console.log(`Adding conversation: ID=${conversation.id}, Title=${title}, Updated=${conversation.updatedAt}`);
                addChatHistoryItem(
                    title,
                    conversation.preview || 'No preview available',
                    conversation.id,
                    conversation.updatedAt // Use updatedAt for display? Or createdAt? Let's use updatedAt for sorting, createdAt for initial display
                );
            });
        } else {
            console.log("No conversations found");
            if (noConversationsDiv) {
                noConversationsDiv.style.display = 'block';
            } else {
                const newNoConversationsDiv = document.createElement('div');
                newNoConversationsDiv.className = 'no-conversations';
                newNoConversationsDiv.textContent = 'No saved conversations yet';
                chatHistoryList.appendChild(newNoConversationsDiv);
            }
        }
    })
    .catch(error => {
        console.error('Error loading conversations:', error);
        if (loadingDiv && loadingDiv.parentNode === chatHistoryList) {
            chatHistoryList.removeChild(loadingDiv);
        }
        const errorDiv = document.createElement('div');
        errorDiv.className = 'conversation-error';
        errorDiv.textContent = `Error loading conversations: ${error.message}`;
        // Avoid adding multiple error messages
        if (!chatHistoryList.querySelector('.conversation-error')) {
            chatHistoryList.appendChild(errorDiv);
        }
    })
    .finally(() => {
        isLoadingConversations = false; // Release the lock
        console.log("loadChatConversations finished.");
    });
}

// Add search functionality for chat history
function setupChatHistorySearch() {
    const searchInput = document.getElementById('searchChat');
    if (searchInput) {
        searchInput.addEventListener('input', function(e) {
            const searchTerm = e.target.value.trim().toLowerCase();
            
            if (searchTerm.length === 0) {
                // If search is cleared, load all conversations
                loadChatConversations();
                return;
            }
            
            // If search term is at least 2 characters, perform search
            if (searchTerm.length >= 2) {
                searchChatHistory(searchTerm);
            }
        });
    }
}

// Function to search chat history
function searchChatHistory(term) {
    console.log("Searching chat history for:", term);
    
    const historyList = document.getElementById('chatHistoryList'); // Use ID selector
    if (!historyList) {
        console.error("Chat history list element not found");
        return;
    }
    
    historyList.innerHTML = '<p class="loading-message">Searching...</p>';
    
    fetch(`/api/chat-history/search?term=${encodeURIComponent(term)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`Search failed: ${response.status}`);
            }
            return response.json();
        })
        .then(conversations => {
            console.log(`Found ${conversations.length} conversations matching "${term}"`);
            historyList.innerHTML = '';
            
            if (conversations.length === 0) {
                historyList.innerHTML = `<p class="empty-message">No conversations found matching "${term}"</p>`;
                return;
            }
            
            // Add each conversation to the list
            conversations.forEach(conversation => {
                addChatHistoryItem(conversation.title, conversation.preview, conversation.id, new Date(conversation.updatedAt));
            });
        })
        .catch(error => {
            console.error("Error searching conversations:", error);
            historyList.innerHTML = `<p class="error-message">Error searching: ${error.message}</p>`;
        });
}

// Update the initializeHomePage function to load conversations and setup search
function initializeHomePage() {
    // Load actual chat history instead of adding example items
    loadChatConversations(); // Keep this one
    
    // Set up search functionality
    setupChatHistorySearch();
}

// Add these functions after the existing chat-related functions

/**
 * Start a new chat conversation
 * This clears the current chat history and conversation ID
 */
function startNewChat() {
    console.log("Starting a new chat...");
    
    // Save the current conversation if it has at least 1 message
    if (chatHistory && chatHistory.length >= 1) {
        console.log(`Attempting to save current conversation (ID: ${currentConversationId || 'new'}, length: ${chatHistory.length})`);
        showToast("Saving conversation before starting new chat");
        
        // Wait for save to complete before proceeding
        saveCurrentConversation()
            .then(saved => {
                console.log(`Save operation finished. Result: ${saved}. Proceeding to clear chat.`);
                // Clear and reset chat regardless of save result, but log the outcome
                if (!saved) {
                    console.warn("Conversation save failed or was skipped, but starting new chat anyway.");
                }
                clearChatAndStartNew();
                // Refresh is handled within saveCurrentConversation's success path
            })
            .catch(error => {
                console.error("Error occurred during the save attempt:", error);
                // Still clear and start new chat even if save failed
                showToast("Error saving previous chat, starting new one.");
                clearChatAndStartNew();
            });
    } else {
        console.log("No conversation content to save. Starting new chat immediately.");
        clearChatAndStartNew();
        // No save happened, so no automatic refresh needed here unless desired
        // Optionally, refresh here if you want the list updated even when no save occurs
        // loadChatConversations(); 
    }
    
    // Helper function to clear chat and start a new one
    function clearChatAndStartNew() {
        // Clear the chat messages display
        clearChat();
        
        // Reset conversation variables
        chatHistory = [];
        currentConversationId = null;
        lastMentionedMovieId = null;
        
        // Add a welcome message for the new conversation
        addMessage("New chat started. How can I help you today?", "chatbot-message");
        
        // Update UI to reflect new conversation
        const conversationTitle = document.getElementById('currentConversationTitle');
        if (conversationTitle) {
            conversationTitle.textContent = 'New Conversation';
        }
        
        console.log('Started new chat conversation');
        showToast("New conversation started");
    }
}

/**
 * Simple function to save the current conversation
 * This is a simpler approach that eliminates potential sources of errors
 */
function saveCurrentConversation() {
    // Skip if there are no messages (Allow saving if at least 1 message exists)
    if (!chatHistory || chatHistory.length < 1) { 
        console.log(`No conversation to save (chatHistory length: ${chatHistory ? chatHistory.length : 0}). Minimum 1 required.`);
        return Promise.resolve(false); // Return promise that resolves to false to indicate no save
    }
    
    // Show status
    updateSaveStatus('saving');
    
    // Create a simple title from timestamp if no ID exists yet
    let title = currentConversationId ? null : "Chat " + new Date().toLocaleTimeString();
    
    // For existing conversations, try to find the first user message
    if (!currentConversationId) {
        for (const msg of chatHistory) {
            if (msg.role === 'user') {
                const content = msg.content.substring(0, 30);
                title = content + (content.length >= 30 ? "..." : "");
                break;
            }
        }
    }
    
    // If title is still empty, set a default
    if (!title && !currentConversationId) {
        title = "New Chat (" + new Date().toLocaleString() + ")";
    }
    
    // Filter out extraneous system prompt messages from assistant responses.
    const messagesToSave = chatHistory.filter(msg => {
        if (msg.role === 'assistant' && msg.content.includes("You are FilmSage, an intelligent movie recommendation assistant")) {
            console.log("Excluding system prompt message from conversation save.");
            return false;
        }
        return true;
    });

    // Create payload using the filtered messages.
    const payload = {
        messages: messagesToSave
    };
    
    // Add title for new conversations only
    if (title) {
        payload.title = title;
    }
    
    // Set endpoint based on whether we're creating or updating
    const url = currentConversationId 
        ? `/api/chat/conversations/${currentConversationId}`
        : '/api/chat/conversations';
        
    const method = currentConversationId ? 'PUT' : 'POST';
    
    console.log(`${method}ing conversation to ${url} with title: ${title || 'existing title'}`);
    
    // Return a promise that resolves when the save is complete
    return fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
    .then(response => {
        if (!response.ok) {
            // Log the response body if available for more context on error
            return response.text().then(text => {
                throw new Error(`Server returned ${response.status}. Response: ${text}`);
            });
        }
        return response.json();
    })
    .then(data => {
        console.log(`Save request successful (${method}). Server response:`, data);
        
        // Save the ID for new conversations (only if method was POST and ID is received)
        if (method === 'POST' && data && data.id) {
            if (!currentConversationId) {
                currentConversationId = data.id;
                console.log(`Assigned new currentConversationId: ${currentConversationId} from server response.`);
            } else {
                console.warn(`Received new conversation ID ${data.id} from POST, but currentConversationId (${currentConversationId}) already exists. Not overwriting.`);
            }
        } else if (method === 'POST') {
            console.warn("Save (POST) successful, but no ID received in server response data:", data);
        }
        
        // Update save status
        updateSaveStatus('saved');
        
        // Refresh the conversation list AFTER successful save
        console.log("Save successful, triggering conversation list refresh.");
        if (typeof loadChatConversations === 'function') {
            // Add a small delay to allow potential DB updates to settle
            setTimeout(() => {
                console.log("Executing delayed loadChatConversations().");
                loadChatConversations();
            }, 300); // 300ms delay
        }
        
        return true; // Return true to indicate successful save
    })
    .catch(error => {
        console.error("Error saving conversation:", error);
        updateSaveStatus('error', 'Failed to save');
        showToast("Error saving: " + error.message);
        
        return false; // Return false to indicate failed save
    });
}

/**
 * Add UI elements for conversation management
 * Call this from the document ready handler
 */
function setupChatManagement() {
    // Create the new chat button in the appropriate place
    const chatHeader = document.querySelector('.chat-header') || 
                      document.querySelector('.modal-header') ||
                      document.querySelector('.chat-container h2');
    
    if (chatHeader) {
        // Create a new chat button (only if it doesn't exist already)
        if (!document.getElementById('newChatButton')) {
            const newChatButton = document.createElement('button');
            newChatButton.id = 'newChatButton';
            newChatButton.className = 'new-chat-btn';
            newChatButton.innerHTML = '<i class="fas fa-plus"></i> New Chat';
            newChatButton.onclick = startNewChat;
            
            // Style the button
            newChatButton.style.backgroundColor = '#e50914';
            newChatButton.style.color = 'white';
            newChatButton.style.border = 'none';
            newChatButton.style.borderRadius = '4px';
            newChatButton.style.padding = '8px 12px';
            newChatButton.style.marginLeft = '10px';
            newChatButton.style.cursor = 'pointer';
            
            // Add it to the header
            chatHeader.appendChild(newChatButton);
            console.log('Added new chat button to the interface');
        }
    } else {
        console.warn('Could not find chat header to add new chat button');
    }
    
    // Initialize save status indicator
    addSaveStatusIndicator();
    
    // Add a manual save button in the chat input area
    addManualSaveButton();
}

// Function to check login status when page loads
function checkLoginStatus() {
    // Wait until document is fully loaded to avoid timing issues
    window.addEventListener('load', function() {
        setTimeout(() => {
            // With session-based auth, we don't need to check for token
            // This function is now mostly a placeholder but we'll leave it for now
            console.log("Login status check: Using session-based authentication");
            
            // We could implement a fetch to a server endpoint that returns login status
            // if needed in the future
        }, 2000); // Wait 2 seconds to ensure page is fully loaded
    });
}

// Call the login check function
checkLoginStatus();

// Add a function to manually save on demand
function addManualSaveButton() {
    // Check if we already have a save button first
    if (document.getElementById('manualSaveButton')) {
        return; // Button already exists
    }
    
    const chatInput = document.querySelector('.chat-input');
    if (!chatInput) return;
    
    const saveButton = document.createElement('button');
    saveButton.id = 'manualSaveButton';
    saveButton.innerHTML = '<i class="fas fa-save"></i>';
    saveButton.title = 'Save conversation';
    saveButton.classList.add('manual-save-button');
    
    // Style the button
    saveButton.style.position = 'absolute';
    saveButton.style.right = '60px'; // Position it to the left of the send button
    saveButton.style.top = '50%';
    saveButton.style.transform = 'translateY(-50%)';
    saveButton.style.background = 'none';
    saveButton.style.border = 'none';
    saveButton.style.color = '#777';
    saveButton.style.fontSize = '16px';
    saveButton.style.cursor = 'pointer';
    saveButton.style.transition = 'color 0.2s';
    
    saveButton.addEventListener('mouseover', function() {
        this.style.color = '#e50914';
    });
    
    saveButton.addEventListener('mouseout', function() {
        this.style.color = '#777';
    });
    
    saveButton.addEventListener('click', function() {
        if (chatHistory && chatHistory.length >= 2) {
            saveCurrentConversation();
            showToast('Saving conversation...');
        } else {
            showToast('Nothing to save yet');
        }
    });
    
    chatInput.appendChild(saveButton);
}

/**
 * Handle the chat form submission
 */
function handleChatSubmit(event) {
    event.preventDefault();
    const userInput = document.getElementById('user-input');
    const message = userInput.value;
    
    if (!message.trim()) {
        return;
    }
    
    // Clear the input field
    userInput.value = '';
    
    // Add the user message to the chat history
    addMessageToChatHistory('user', message);
    
    // Scroll to the bottom of the chat
    scrollToBottom();
    
    // Get the context information
    const contextInfo = {
        currentUrl: window.location.href,
        chatHistory: chatHistory
    };
    
    // Add the typing indicator
    addTypingIndicator();
    
    // Send the message to the server
    fetch('/api/chat/message', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            message: message,
            contextInfo: contextInfo
        })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.status);
        }
        return response.json();
    })
    .then(data => {
        removeTypingIndicator();
        
        // Check if the response is a search result
        if (data.searchResults && data.searchResults.length > 0) {
            // Display the search results
            addMessageToChatHistory('assistant', 'Here are some movies that match your search:');
            displaySearchResults(data.searchResults);
        } else {
            // Add the assistant's message to the chat history
            addMessageToChatHistory('assistant', data.message);
        }
        
        // Scroll to the bottom of the chat
        scrollToBottom();
        
        // Save conversation after each exchange is complete
        saveCurrentConversation();
    })
    .catch(error => {
        console.error('Error:', error);
        removeTypingIndicator();
        
        // Add an error message to the chat history
        addMessageToChatHistory('assistant', 'Sorry, I encountered an error processing your request. Please try again later.');
        
        // Scroll to the bottom of the chat
        scrollToBottom();
    });
}

// Add a debounce utility function at the top of the file
function debounce(func, wait) {
    let timeout;
    return function() {
        const context = this;
        const args = arguments;
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(context, args), wait);
    };
}

// Add a debounced version of the save function
const debouncedSaveConversation = debounce(function() {
    saveCurrentConversation();
}, 1500); // 1.5 seconds delay

// Setup auto-save when leaving the page
(function() {
    // Wait for the document to be fully loaded
    window.addEventListener('load', function() {
        // Set up the beforeunload event to save conversations when leaving
        window.addEventListener('beforeunload', function() {
            // Only try to save if we have a conversation going
            if (chatHistory && chatHistory.length >= 2) {
                // Use direct save, not debounced for page exit
                saveCurrentConversation();
                
                // Return a string to potentially delay unload slightly
                // (modern browsers won't show this message for security reasons)
                return "Saving your conversation...";
            }
        });
    });
})();

// Function to debug authentication status
function checkAuthenticationStatus() {
    // With session-based authentication, we don't need to check for tokens
    console.log('Using session-based authentication - authentication status will be determined by the server');
    return true;
}

/**
 * Add a save status indicator to the chat header
 */
function addSaveStatusIndicator() {
    // Find the chat header
    const chatHeader = document.querySelector('.chat-header') || 
                      document.querySelector('.modal-header');
    
    if (!chatHeader) return;
    
    // Check if indicator already exists
    let indicator = document.getElementById('saveStatusIndicator');
    if (indicator) return;
    
    // Create the indicator
    indicator = document.createElement('span');
    indicator.id = 'saveStatusIndicator';
    indicator.className = 'save-status';
    indicator.innerHTML = '<i class="fas fa-check-circle"></i> Saved';
    
    // Style the indicator
    indicator.style.fontSize = '12px';
    indicator.style.marginLeft = 'auto';
    indicator.style.color = '#8bc34a';
    indicator.style.opacity = '0';
    indicator.style.transition = 'opacity 0.3s';
    indicator.style.marginRight = '15px';
    
    // Add it to the header
    chatHeader.appendChild(indicator);
}

/**
 * Update the save status indicator
 * @param {string} status - 'saving', 'saved', 'error', or 'hidden'
 * @param {string} message - Optional message to show
 */
function updateSaveStatus(status, message = '') {
    // Make sure the indicator exists
    addSaveStatusIndicator();
    
    const indicator = document.getElementById('saveStatusIndicator');
    if (!indicator) return;
    
    // Show the indicator
    indicator.style.opacity = '1';
    
    // Update based on status
    switch (status) {
        case 'saving':
            indicator.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
            indicator.style.color = '#2196f3';
            break;
        case 'saved':
            indicator.innerHTML = '<i class="fas fa-check-circle"></i> Saved';
            indicator.style.color = '#8bc34a';
            // Hide after a delay
            setTimeout(() => {
                indicator.style.opacity = '0';
            }, 2000);
            break;
        case 'error':
            indicator.innerHTML = `<i class="fas fa-exclamation-circle"></i> ${message || 'Error'}`;
            indicator.style.color = '#f44336';
            // Hide after a longer delay
            setTimeout(() => {
                indicator.style.opacity = '0';
            }, 4000);
            break;
        case 'hidden':
            indicator.style.opacity = '0';
            break;
    }
}

// Add an event listener to ensure the New Chat button is properly connected
document.addEventListener('DOMContentLoaded', function() {
    // Check for both possible button IDs
    const newChatBtn = document.getElementById('newChatBtn') || document.getElementById('newChatButton');
    if (newChatBtn) {
        console.log("New Chat button found, attaching event listener.");
        newChatBtn.addEventListener("click", function() {
            startNewChat();
        });
    } else {
        console.warn("New Chat button not found. It will be created by setupChatManagement().");
    }
});

// Function to periodically update relative timestamps
function updateTimestamps() {
    // console.log("Updating timestamps..."); // Optional: Log when update runs
    const timestampSpans = document.querySelectorAll('#chatHistoryList .history-timestamp[data-created-at]');
    
    timestampSpans.forEach(span => {
        const rawTimestamp = span.dataset.createdAt;
        if (rawTimestamp) {
            const newFormattedTime = formatTimestamp(rawTimestamp);
            // Only update if the text content actually changed (e.g., "Just now" -> "1m ago")
            if (span.textContent.trim() !== newFormattedTime) {
                span.textContent = newFormattedTime;
            }
        }
    });
}

// Start the timestamp update interval (e.g., every 60 seconds)
setInterval(updateTimestamps, 60 * 1000);

// --- Existing Code At End of File --- //

// --- NEW: Profile Dropdown Logic --- //
document.addEventListener('DOMContentLoaded', function() {
    const profileIcon = document.getElementById('profileIcon');
    const profileDropdown = document.getElementById('profileDropdown');

    if (profileIcon && profileDropdown) {
        profileIcon.addEventListener('click', function(event) {
            event.stopPropagation(); // Prevent click from immediately closing dropdown
            profileDropdown.classList.toggle('active');
        });

        // Close dropdown if clicking outside of it
        document.addEventListener('click', function(event) {
            if (!profileIcon.contains(event.target) && !profileDropdown.contains(event.target)) {
                profileDropdown.classList.remove('active');
            }
        });
    } else {
        console.warn("Profile icon or dropdown elements not found.");
    }
    
    // Add the handleLogout function if it doesn't exist
    if (typeof window.handleLogout !== 'function') {
        window.handleLogout = function() {
            // Perform logout - typically involves redirecting to Spring Security's logout URL
            console.log("Logout button clicked. Redirecting to /logout endpoint...");
            // Redirect to the Spring Security configured logout URL (which then redirects to login)
            window.location.href = '/logout'; // <<< CORRECT: Redirects to /logout endpoint
            // NOTE: Spring Security handles the actual session invalidation and redirect to login page
            // based on the configuration in SecurityConfig.java
        }
    }
});