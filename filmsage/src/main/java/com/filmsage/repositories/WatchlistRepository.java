package com.filmsage.repositories;

import com.filmsage.models.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByUsernameOrderByAddedAtDesc(String username);
    boolean existsByUsernameAndMovieId(String username, Long movieId);
    void deleteByUsernameAndMovieId(String username, Long movieId);
} 