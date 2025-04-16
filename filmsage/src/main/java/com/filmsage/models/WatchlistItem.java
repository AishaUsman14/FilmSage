package com.filmsage.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist")
public class WatchlistItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long movieId;
    private String title;
    private String posterPath;
    
    @Column(columnDefinition = "TEXT") // Allow longer text for overview
    private String overview;
    
    private LocalDateTime addedAt;
    
    // For now, we'll just store username since we haven't implemented full user system
    private String username;
    
    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
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
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getPosterPath() {
        return posterPath;
    }
    
    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }
    
    public String getOverview() {
        return overview;
    }
    
    public void setOverview(String overview) {
        this.overview = overview;
    }
    
    public LocalDateTime getAddedAt() {
        return addedAt;
    }
    
    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
} 