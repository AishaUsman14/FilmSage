package com.filmsage.controllers;

import com.filmsage.models.Rating;
import com.filmsage.models.Rating.ModerationStatus;
import com.filmsage.repositories.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    @Autowired
    private RatingRepository ratingRepository;

    @PostMapping
    public ResponseEntity<?> addRating(@RequestBody Rating rating, Authentication authentication) {
        
        String username = getUsernameFromAuthentication(authentication);
        if (username == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        rating.setUsername(username);

        // Check if user has already rated this movie
        Rating existingRating = ratingRepository.findByMovieIdAndUsername(
            rating.getMovieId(),
            rating.getUsername()
        );

        if (existingRating != null) {
            existingRating.setRating(rating.getRating());
            existingRating.setReview(rating.getReview());
            existingRating.setStatus(ModerationStatus.PENDING);
            rating = existingRating;
        } else {
            rating.setStatus(ModerationStatus.PENDING);
        }

        Rating savedRating = ratingRepository.save(rating);
        return ResponseEntity.ok(savedRating);
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<Map<String, Object>> getMovieRatings(@PathVariable Long movieId) {
        // Fetch only APPROVED ratings
        List<Rating> approvedRatings = ratingRepository.findByMovieId(movieId).stream()
                .filter(r -> r.getStatus() == ModerationStatus.APPROVED)
                .collect(Collectors.toList());

        // Calculate average rating based ONLY on approved ratings
        Double averageRating = approvedRatings.stream()
                .mapToDouble(Rating::getRating)
                .average()
                .orElse(0.0);

        Map<String, Object> response = new HashMap<>();
        response.put("ratings", approvedRatings);
        response.put("averageRating", averageRating);
        response.put("totalRatings", approvedRatings.size());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRating(@PathVariable Long id, Authentication authentication) {
        String username = getUsernameFromAuthentication(authentication);
        Rating rating = ratingRepository.findById(id).orElse(null);

        if (rating == null) {
            return ResponseEntity.notFound().build();
        }

        if (username == null || !rating.getUsername().equals(username)) {
             return ResponseEntity.status(403).body("Forbidden: You can only delete your own ratings");
        }

        ratingRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private String getUsernameFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        return null;
    }
} 