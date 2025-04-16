package com.filmsage.services;

import com.filmsage.models.Rating;
import com.filmsage.models.Rating.ModerationStatus;
import com.filmsage.repositories.RatingRepository;
import com.filmsage.repositories.UserRepository;
import com.filmsage.repositories.WatchlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.Query; // Need this for custom queries later

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // User Stats
        stats.put("totalUsers", userRepository.count());
        // TODO: Add more user stats later (e.g., new users this week, using createdAt)
        // TODO: Implement logic to update and retrieve lastLogin stats

        // Rating Stats
        long totalRatings = ratingRepository.count();
        // Need countByStatus method in RatingRepository
        long pendingReviews = ratingRepository.countByStatus(ModerationStatus.PENDING);
        long approvedReviews = ratingRepository.countByStatus(ModerationStatus.APPROVED);
        long rejectedReviews = ratingRepository.countByStatus(ModerationStatus.REJECTED);
        stats.put("totalRatings", totalRatings);
        stats.put("pendingReviews", pendingReviews);
        stats.put("approvedReviews", approvedReviews);
        stats.put("rejectedReviews", rejectedReviews);
        
        // Calculate overall average rating (only based on approved)
        Double averageRating = ratingRepository.findAll().stream()
                .filter(r -> r.getStatus() == ModerationStatus.APPROVED)
                .mapToDouble(Rating::getRating)
                .average()
                .orElse(0.0);
        stats.put("averageRating", averageRating);

        // Watchlist Stats
        stats.put("totalWatchlistItems", watchlistRepository.count());
        // TODO: Add top watchlist movies later (requires aggregation query)

        return stats;
    }
    
    // --- Add more methods for detailed analytics --- 
    
    // Example: Method to get top rated movies
    // public List<Map<String, Object>> getTopRatedMovies(int limit) {
    //     // Needs more complex query or processing on RatingRepository
    //     // Example: SELECT movieId, AVG(rating) FROM ratings WHERE status = 'APPROVED' GROUP BY movieId ORDER BY AVG(rating) DESC LIMIT :limit
    //     return List.of(); 
    // }

    // Example: Method to get most watchlisted movies
    // public List<Map<String, Object>> getMostWatchlistedMovies(int limit) {
        // Needs aggregation query on WatchlistRepository
        // Example: SELECT movieId, COUNT(*) as count FROM watchlist GROUP BY movieId ORDER BY count DESC LIMIT :limit
    //     return List.of();
    // }
}