package com.filmsage.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
public class Rating {
    
    // Inner enum for moderation status
    public enum ModerationStatus {
        PENDING,    // Review is awaiting moderation
        APPROVED,   // Review is approved and visible
        REJECTED    // Review is rejected and not visible
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long movieId;
    private String username;
    private double rating;  // 1-5 stars
    private String review;
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING) // Store enum as String in the DB
    private ModerationStatus status = ModerationStatus.PENDING; // Default status
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Getter and Setter for status
    public ModerationStatus getStatus() {
        return status;
    }

    public void setStatus(ModerationStatus status) {
        this.status = status;
    }
} 