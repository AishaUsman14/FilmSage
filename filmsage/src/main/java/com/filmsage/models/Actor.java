package com.filmsage.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "actors")
public class Actor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String biography;
    private LocalDate birthDate;
    private String birthPlace;
    private String imageUrl;
    
    @ManyToMany(mappedBy = "actors")
    private Set<Movie> movies = new HashSet<>();
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Set<Movie> getMovies() {
        return movies;
    }

    public void setMovies(Set<Movie> movies) {
        this.movies = movies;
    }
} 