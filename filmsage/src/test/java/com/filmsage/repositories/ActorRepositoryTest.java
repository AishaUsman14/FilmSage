package com.filmsage.repositories;

import com.filmsage.models.Actor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class ActorRepositoryTest {

    @Autowired
    private ActorRepository actorRepository;

    @Test
    void findByNameContainingIgnoreCase_ShouldReturnMatchingActors() {
        // Create test actor
        Actor actor = new Actor();
        actor.setName("Morgan Freeman");
        actor.setBiography("Famous actor");
        actor.setBirthDate(LocalDate.of(1937, 6, 1));
        actorRepository.save(actor);

        // Test search
        List<Actor> found = actorRepository.findByNameContainingIgnoreCase("morgan");
        
        assertNotNull(found);
        assertFalse(found.isEmpty());
        assertEquals("Morgan Freeman", found.get(0).getName());
    }

    @Test
    void findByNameContainingIgnoreCase_ShouldReturnEmptyList_WhenNoMatch() {
        List<Actor> found = actorRepository.findByNameContainingIgnoreCase("xyz123");
        
        assertNotNull(found);
        assertTrue(found.isEmpty());
    }
} 