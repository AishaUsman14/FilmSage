package com.filmsage.controllers;

import com.filmsage.models.WatchlistItem;
import com.filmsage.repositories.WatchlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    @Autowired
    private WatchlistRepository watchlistRepository;

    @GetMapping
    public ResponseEntity<List<WatchlistItem>> getWatchlist(Authentication authentication) {
        // Handle cases where the user might not be authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            // Return an empty list for unauthenticated users
            return ResponseEntity.ok(List.of()); 
        }
        String username = authentication.getName();
        return ResponseEntity.ok(watchlistRepository.findByUsernameOrderByAddedAtDesc(username));
    }

    @PostMapping
    public ResponseEntity<?> addToWatchlist(@RequestBody WatchlistItem item, Authentication authentication) {
        String username = authentication.getName();
        
        // Check if movie already exists in watchlist
        if (watchlistRepository.existsByUsernameAndMovieId(username, item.getMovieId())) {
            return ResponseEntity.badRequest().body("Movie already in watchlist");
        }
        
        item.setUsername(username);
        WatchlistItem saved = watchlistRepository.save(item);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<?> removeFromWatchlist(@PathVariable Long movieId, Authentication authentication) {
        String username = authentication.getName();
        watchlistRepository.deleteByUsernameAndMovieId(username, movieId);
        return ResponseEntity.ok().build();
    }
} 