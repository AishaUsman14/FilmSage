package com.filmsage.repositories;

import com.filmsage.models.Rating;
import com.filmsage.models.Rating.ModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByMovieId(Long movieId);
    Rating findByMovieIdAndUsername(Long movieId, String username);
    
    List<Rating> findByStatus(ModerationStatus status);

    long countByStatus(ModerationStatus status);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.movieId = :movieId")
    Double averageRatingByMovieId(@Param("movieId") Long movieId);
} 