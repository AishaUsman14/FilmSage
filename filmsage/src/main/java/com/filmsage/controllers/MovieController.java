package com.filmsage.controllers;

import com.filmsage.models.Movie;
import com.filmsage.services.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @GetMapping("/search")
    public ResponseEntity<List<Movie>> searchMovies(@RequestParam String query) {
        List<Movie> movies = movieService.searchMovies(query);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<Map<String, Object>> getMovieDetails(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieDetails(id));
    }

    /**
     * Endpoint to get the trailer key for a specific movie.
     * Returns a 404 if no trailer key is found.
     */
    @GetMapping("/{id}/trailer")
    public ResponseEntity<Map<String, String>> getMovieTrailer(@PathVariable Long id) {
        String trailerKey = movieService.getMovieTrailerKey(id);
        if (trailerKey != null) {
            Map<String, String> response = Map.of("trailerKey", trailerKey);
            return ResponseEntity.ok(response);
        } else {
            // Return 404 Not Found if no trailer key exists
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Trailer not found"));
        }
    }

    /**
     * Endpoint to get watch providers (streaming, rent, buy) for a specific movie.
     * Focuses on US region providers.
     * Returns a 404 if no provider data is found.
     */
    @GetMapping("/{id}/providers")
    public ResponseEntity<Map<String, Object>> getMovieProviders(@PathVariable Long id) {
        Map<String, Object> providers = movieService.getMovieWatchProviders(id);
        if (!providers.isEmpty()) {
            return ResponseEntity.ok(providers);
        } else {
            // Return 404 Not Found if no provider data exists for the US region
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Watch providers not found for US region"));
        }
    }

    @GetMapping("/trending")
    public ResponseEntity<List<Movie>> getTrendingMovies() {
        try {
            // First try to get trending movies
            List<Movie> trendingMovies = movieService.getTrendingMovies();
            
            if (!trendingMovies.isEmpty()) {
                return ResponseEntity.ok(trendingMovies);
            }
            
            // If trending movies are empty, search for any popular movies as fallback
            List<Movie> fallbackMovies = movieService.searchMovies("popular");
            if (!fallbackMovies.isEmpty()) {
                // Limit to 10 movies if we got more
                List<Movie> limitedMovies = fallbackMovies.size() > 10 
                    ? fallbackMovies.subList(0, 10) 
                    : fallbackMovies;
                return ResponseEntity.ok(limitedMovies);
            }
            
            // If still no movies, try a general search
            fallbackMovies = movieService.searchMovies("movie");
            if (!fallbackMovies.isEmpty()) {
                List<Movie> limitedMovies = fallbackMovies.size() > 10 
                    ? fallbackMovies.subList(0, 10) 
                    : fallbackMovies;
                return ResponseEntity.ok(limitedMovies);
            }
            
            // If all attempts fail, return empty list
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            // Log the error but return an empty list
            e.printStackTrace(); // For debugging
            return ResponseEntity.ok(List.of());
        }
    }
} 