package com.filmsage.services;

import com.filmsage.models.Movie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
public class MovieService {
    
    private static final Logger logger = LoggerFactory.getLogger(MovieService.class);
    
    @Value("${tmdb.api.token}")
    private String apiToken;
    
    @Value("${tmdb.api.key}")
    private String apiKey;
    
    @Value("${tmdb.api.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Simple in-memory cache for frequently accessed data
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY = 1000 * 60 * 15; // 15 minutes
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    @Cacheable("trendingMovies")
    public List<Movie> getTrendingMovies() {
        try {
            // Check cache first
            String cacheKey = "trending-movies";
            if (cache.containsKey(cacheKey) && 
                (System.currentTimeMillis() - cacheTimestamps.get(cacheKey) < CACHE_EXPIRY)) {
                logger.info("Using cached trending movies");
                return (List<Movie>) cache.get(cacheKey);
            }
            
            // Debug info
            logger.info("Fetching trending movies from TMDB API");
            
            // Build URL with API key as parameter
            String url = baseUrl + "/trending/movie/week?api_key=" + apiKey;
            logger.info("TMDB URL: {}", url);
            
            // Make request
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
                logger.info("Retrieved {} trending movies from TMDB", results != null ? results.size() : 0);
                
                // If no results, try popular movies as fallback
                if (results == null || results.isEmpty()) {
                    logger.warn("No trending movies found, trying popular movies instead");
                    return getPopularMovies();
                }
                
                // Map TMDB movie objects to our Movie domain objects
                List<Movie> movies = results.stream().map(this::convertToMovie)
                    .limit(10)
                    .collect(Collectors.toList());
                
                // Store in cache
                cache.put(cacheKey, movies);
                cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                
                return movies;
            } else {
                logger.error("Failed to get trending movies. Status: {}", response.getStatusCode());
                return getPopularMovies();
            }
        } catch (Exception e) {
            logger.error("Error fetching trending movies: {}", e.getMessage(), e);
            return getPopularMovies();
        }
    }

    @Cacheable("popularMovies")
    public List<Movie> getPopularMovies() {
        try {
            // Check cache first
            String cacheKey = "popular-movies";
            if (cache.containsKey(cacheKey) && 
                (System.currentTimeMillis() - cacheTimestamps.get(cacheKey) < CACHE_EXPIRY)) {
                logger.info("Using cached popular movies");
                return (List<Movie>) cache.get(cacheKey);
            }
            
            logger.info("Fetching popular movies as fallback");
            
            // Build URL with API key parameter
            String url = baseUrl + "/movie/popular?api_key=" + apiKey;
            logger.info("Popular movies URL: {}", url);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
                logger.info("Found {} popular movies", results != null ? results.size() : 0);
                
                if (results == null || results.isEmpty()) {
                    // As a last resort, use hardcoded sample movies
                    logger.warn("No popular movies returned, using hardcoded sample movies");
                    return getSampleMovies();
                }
                
                List<Movie> movies = results.stream()
                    .map(this::convertToMovie)
                    .limit(10)
                    .collect(Collectors.toList());
                    
                // Store in cache
                cache.put(cacheKey, movies);
                cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                
                return movies;
            } else {
                logger.error("Failed to get popular movies: {}", response.getStatusCode());
                return getSampleMovies();
            }
        } catch (Exception e) {
            logger.error("Error fetching popular movies: {}", e.getMessage(), e);
            return getSampleMovies();
        }
    }
    
    // Add a method to provide sample movies when all API calls fail
    private List<Movie> getSampleMovies() {
        logger.info("Using sample movies as last resort");
        
        // Create a few sample movies with realistic data
        Movie movie1 = new Movie();
        movie1.setId(1L);
        movie1.setTitle("The Matrix");
        movie1.setOverview("A computer hacker learns from mysterious rebels about the true nature of his reality and his role in the war against its controllers.");
        movie1.setRating(8.7);
        movie1.setReleaseDate("1999-03-31");
        
        Movie movie2 = new Movie();
        movie2.setId(2L);
        movie2.setTitle("Inception");
        movie2.setOverview("A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.");
        movie2.setRating(8.3);
        movie2.setReleaseDate("2010-07-16");
        
        Movie movie3 = new Movie();
        movie3.setId(3L);
        movie3.setTitle("The Shawshank Redemption");
        movie3.setOverview("Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.");
        movie3.setRating(9.3);
        movie3.setReleaseDate("1994-09-23");
        
        Movie movie4 = new Movie();
        movie4.setId(4L);
        movie4.setTitle("Pulp Fiction");
        movie4.setOverview("The lives of two mob hitmen, a boxer, a gangster and his wife, and a pair of diner bandits intertwine in four tales of violence and redemption.");
        movie4.setRating(8.9);
        movie4.setReleaseDate("1994-10-14");
        
        Movie movie5 = new Movie();
        movie5.setId(5L);
        movie5.setTitle("The Dark Knight");
        movie5.setOverview("When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.");
        movie5.setRating(9.0);
        movie5.setReleaseDate("2008-07-18");
        
        return List.of(movie1, movie2, movie3, movie4, movie5);
    }

    /**
     * Enhanced search method that uses natural language processing to extract search terms
     * @param query The user's query which may contain natural language
     * @return A list of movies matching the query
     */
    @Cacheable("movieSearch")
    public List<Movie> searchMovies(String query) {
        try {
            // Check cache first with normalized query
            String normalizedQuery = query.toLowerCase().trim();
            String cacheKey = "search-" + normalizedQuery.replaceAll("[^a-z0-9]", "-");
            if (cache.containsKey(cacheKey) && 
                (System.currentTimeMillis() - cacheTimestamps.get(cacheKey) < CACHE_EXPIRY)) {
                logger.info("Using cached search results for query: {}", normalizedQuery);
                return (List<Movie>) cache.get(cacheKey);
            }
            
            // Extract just the search terms, removing words like "search" and "movies"
            String cleanQuery = normalizedQuery
                .replace("search", "")
                .replace("find", "")
                .replace("looking for", "")
                .replace("movies", "")
                .replace("movie", "")
                .replace("about", "")
                .replace("tell me", "")
                .replace("show me", "")
                .replace("can you", "")
                .replace("please", "")
                .trim();
                
            logger.info("Searching for movies with cleaned query: {}", cleanQuery);
            
            // If search term is empty, use "popular" as default
            if (cleanQuery.isEmpty()) {
                cleanQuery = "popular";
                logger.info("Empty search query, using 'popular' as default");
            }

            List<Movie> searchResults = performTMDBSearch(cleanQuery);
            
            // If direct search yields no results, try discovering by genre
            if (searchResults.isEmpty()) {
                logger.info("No results from direct search, trying genre discovery");
                searchResults = searchMoviesByGenre(cleanQuery);
            }
            
            // If still no results, try searching by person (actor/director)
            if (searchResults.isEmpty()) {
                logger.info("No results from genre search, trying person search");
                searchResults = searchMoviesByPerson(cleanQuery);
            }
            
            // If we still have no results, return popular movies
            if (searchResults.isEmpty()) {
                logger.info("No results found through any method, returning popular movies");
                return getPopularMovies();
            }
            
            // Store results in cache
            cache.put(cacheKey, searchResults);
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());
            
            return searchResults;
        } catch (Exception e) {
            logger.error("Error searching movies: {}", e.getMessage(), e);
            return getPopularMovies(); // Fallback to popular movies
        }
    }
    
    /**
     * Perform a direct search against the TMDB API
     */
    private List<Movie> performTMDBSearch(String query) {
        try {
            String url = baseUrl + "/search/movie?api_key=" + apiKey + "&query=" + query + "&include_adult=false";
            logger.info("Performing TMDB search: {}", url);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
                if (results != null) {
                    logger.info("Found {} results for query \'{}\'", results.size(), query);
                    return results.stream()
                        .map(this::convertToMovie)
                        .collect(Collectors.toList());
                }
            } else {
                logger.warn("TMDB search failed for query \'{}\'. Status: {}", query, response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error during TMDB search for query \'{}\': {}", query, e.getMessage(), e);
        }
        return Collections.emptyList();
    }
    
    /**
     * Discover movies by genre
     */
    private List<Movie> searchMoviesByGenre(String query) {
        // Simple genre matching for now
        String genreQuery = query.toLowerCase().contains("sci-fi") ? "science fiction" : query;
        // Can implement more sophisticated genre mapping or search by genre ID later
        logger.info("Attempting genre discovery for query: {}", genreQuery);
        
        // TMDB Discover endpoint allows filtering by genre, keywords etc.
        // This requires mapping the query to a genre ID first.
        // For simplicity, we'll skip advanced genre discovery for now.
        return Collections.emptyList();
    }
    
    /**
     * Search for movies by person (actor, director, etc.)
     */
    private List<Movie> searchMoviesByPerson(String query) {
        try {
            // 1. Search for the person to get their ID
            String personSearchUrl = baseUrl + "/search/person?api_key=" + apiKey + "&query=" + query;
            logger.info("Searching for person: {}", personSearchUrl);
            ResponseEntity<Map> personResponse = restTemplate.getForEntity(personSearchUrl, Map.class);
            
            if (personResponse.getStatusCode().is2xxSuccessful() && personResponse.getBody() != null) {
                List<Map<String, Object>> personResults = (List<Map<String, Object>>) personResponse.getBody().get("results");
                if (personResults != null && !personResults.isEmpty()) {
                    // Get the first matching person's ID
                    Integer personId = (Integer) personResults.get(0).get("id");
                    logger.info("Found person ID {} for query \'{}\'", personId, query);
                    
                    // 2. Discover movies associated with that person ID
                    String discoverUrl = baseUrl + "/discover/movie?api_key=" + apiKey + "&with_people=" + personId + "&sort_by=popularity.desc";
                    logger.info("Discovering movies by person ID: {}", discoverUrl);
                    ResponseEntity<Map> movieResponse = restTemplate.getForEntity(discoverUrl, Map.class);
                    
                    if (movieResponse.getStatusCode().is2xxSuccessful() && movieResponse.getBody() != null) {
                        List<Map<String, Object>> movieResults = (List<Map<String, Object>>) movieResponse.getBody().get("results");
                        if (movieResults != null) {
                             logger.info("Found {} movies associated with person ID {}", movieResults.size(), personId);
                            return movieResults.stream()
                                .map(this::convertToMovie)
                                .collect(Collectors.toList());
                        }
                    } else {
                        logger.warn("Failed to discover movies for person ID {}. Status: {}", personId, movieResponse.getStatusCode());
                    }
                } else {
                    logger.info("No person found for query: {}", query);
                }
            } else {
                 logger.warn("Person search failed for query \'{}\'. Status: {}", query, personResponse.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error searching movies by person \'{}\': {}", query, e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private Movie convertToMovie(Map<String, Object> movieData) {
        Movie movie = new Movie();
        
        try {
            // Set ID
            Object idObj = movieData.get("id");
            if (idObj != null) {
                movie.setId(((Number) idObj).longValue());
            }
            
            // Set title
            String title = (String) movieData.get("title");
            movie.setTitle(title != null ? title : "Untitled");
            
            // Set poster path - properly handle this to avoid undefined
            String posterPath = (String) movieData.get("poster_path");
            movie.setPosterPath(posterPath); // This can be null/undefined but we handle that in the frontend
            logger.debug("Movie: {}, Poster path from API: {}", movie.getTitle(), posterPath);
            
            // Set release date
            movie.setReleaseDate((String) movieData.get("release_date"));
            
            // Set overview
            String overview = (String) movieData.get("overview");
            movie.setOverview(overview != null ? overview : "No overview available");
            
            // Set rating - handle if vote_average is null
            Object voteAverage = movieData.get("vote_average");
            if (voteAverage != null) {
                movie.setRating(((Number) voteAverage).doubleValue());
            } else {
                movie.setRating(0.0);
            }
        } catch (Exception e) {
            logger.error("Error converting movie data: {}", e.getMessage(), e);
        }
        
        return movie;
    }

    @Cacheable("movieDetails")
    public Map<String, Object> getMovieDetails(long movieId) {
        try {
            // Check cache first
            String cacheKey = "details-" + movieId;
            if (cache.containsKey(cacheKey) && 
                (System.currentTimeMillis() - cacheTimestamps.get(cacheKey) < CACHE_EXPIRY)) {
                logger.info("Using cached details for movie ID: {}", movieId);
                return (Map<String, Object>) cache.get(cacheKey);
            }
            
            // Append credits, videos, release_dates, and similar movies to the request
            String url = baseUrl + "/movie/" + movieId + "?api_key=" + apiKey + "&append_to_response=credits,videos,release_dates,similar";
            logger.info("Fetching details for movie ID: {}", movieId);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully retrieved details for movie ID: {}", movieId);
                Map<String, Object> details = response.getBody();
                
                // Store in cache
                cache.put(cacheKey, details);
                cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                
                return details;
            } else {
                logger.error("Failed to get details for movie ID {}. Status: {}", movieId, response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            logger.error("Error fetching movie details for ID {}: {}", movieId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Fetches the YouTube key for the official trailer of a movie.
     *
     * @param movieId The TMDB ID of the movie.
     * @return The YouTube video key as a String, or null if not found.
     */
    public String getMovieTrailerKey(long movieId) {
        String cacheKey = "trailer-" + movieId;
        if (cache.containsKey(cacheKey) &&
            (System.currentTimeMillis() - cacheTimestamps.get(cacheKey) < CACHE_EXPIRY)) {
            logger.info("Using cached trailer key for movie ID: {}", movieId);
            return (String) cache.get(cacheKey);
        }

        String url = baseUrl + "/movie/" + movieId + "/videos?api_key=" + apiKey;
        logger.info("Fetching videos for movie ID: {}", movieId);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> videos = (List<Map<String, Object>>) response.getBody().get("results");
                if (videos != null) {
                    // Find the official trailer on YouTube
                    for (Map<String, Object> video : videos) {
                        boolean isOfficial = (Boolean) video.getOrDefault("official", false);
                        String type = (String) video.get("type");
                        String site = (String) video.get("site");

                        if (isOfficial && "Trailer".equalsIgnoreCase(type) && "YouTube".equalsIgnoreCase(site)) {
                            String key = (String) video.get("key");
                            logger.info("Found official YouTube trailer key '{}' for movie ID {}", key, movieId);
                            cache.put(cacheKey, key);
                            cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                            return key;
                        }
                    }
                    // Fallback: find any trailer if no official one exists
                     for (Map<String, Object> video : videos) {
                         String type = (String) video.get("type");
                         String site = (String) video.get("site");
                         if ("Trailer".equalsIgnoreCase(type) && "YouTube".equalsIgnoreCase(site)) {
                             String key = (String) video.get("key");
                             logger.info("Found non-official YouTube trailer key '{}' for movie ID {}", key, movieId);
                             cache.put(cacheKey, key); // Cache fallback as well
                             cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                             return key;
                         }
                     }
                }
            } else {
                logger.warn("Failed to get videos for movie ID {}. Status: {}", movieId, response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error fetching videos for movie ID {}: {}", movieId, e.getMessage(), e);
        }

        logger.warn("No suitable trailer found for movie ID {}", movieId);
        cache.put(cacheKey, null); // Cache null to avoid repeated failed lookups
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        return null;
    }

    /**
     * Fetches watch provider information (streaming, rent, buy) for a movie in the US region.
     *
     * @param movieId The TMDB ID of the movie.
     * @return A Map containing provider information structured by type (flatrate, rent, buy),
     *         or an empty map if no providers are found or an error occurs.
     */
    public Map<String, Object> getMovieWatchProviders(long movieId) {
        String cacheKey = "providers-" + movieId;
        if (cache.containsKey(cacheKey) &&
            (System.currentTimeMillis() - cacheTimestamps.get(cacheKey) < CACHE_EXPIRY)) {
            logger.info("Using cached watch providers for movie ID: {}", movieId);
            return (Map<String, Object>) cache.get(cacheKey);
        }

        String url = baseUrl + "/movie/" + movieId + "/watch/providers?api_key=" + apiKey;
        logger.info("Fetching watch providers for movie ID: {}", movieId);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> results = (Map<String, Object>) response.getBody().get("results");
                if (results != null && results.containsKey("US")) {
                    // Extract US providers
                    Map<String, Object> usProviders = (Map<String, Object>) results.get("US");
                    logger.info("Found watch providers for US region for movie ID {}", movieId);
                    
                    // We only need specific fields, create a cleaned map
                    Map<String, Object> providerData = new HashMap<>();
                    providerData.put("link", usProviders.get("link")); // Link to JustWatch page
                    
                    if (usProviders.containsKey("flatrate")) {
                         providerData.put("flatrate", usProviders.get("flatrate"));
                    }
                     if (usProviders.containsKey("rent")) {
                         providerData.put("rent", usProviders.get("rent"));
                     }
                     if (usProviders.containsKey("buy")) {
                         providerData.put("buy", usProviders.get("buy"));
                     }

                    cache.put(cacheKey, providerData);
                    cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                    return providerData;
                } else {
                     logger.info("No watch providers found for US region for movie ID {}", movieId);
                }
            } else {
                logger.warn("Failed to get watch providers for movie ID {}. Status: {}", movieId, response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error fetching watch providers for movie ID {}: {}", movieId, e.getMessage(), e);
        }

        logger.warn("No watch provider data retrieved for movie ID {}", movieId);
        cache.put(cacheKey, Collections.emptyMap()); // Cache empty map
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        return Collections.emptyMap();
    }
} 