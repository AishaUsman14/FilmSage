package com.filmsage.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Movie {
    @Id
    private Long id;
    private String title;
    @JsonProperty("poster_path")
    private String posterPath;
    @JsonProperty("release_date")
    private String releaseDate;
    private String overview;
    @JsonProperty("vote_average")
    private Double rating;

    @ManyToMany
    @JoinTable(
        name = "movie_actors",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    private Set<Actor> actors = new HashSet<>();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getFullPosterPath() {
        return "https://image.tmdb.org/t/p/w500" + posterPath;
    }
} 