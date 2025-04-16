package com.filmsage.controllers;

import com.filmsage.models.User;
import com.filmsage.models.Role;
import com.filmsage.models.Rating;
import com.filmsage.models.Rating.ModerationStatus;
import com.filmsage.services.AdminService;
import com.filmsage.repositories.RatingRepository;
import com.filmsage.services.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long userId) {
        adminService.suspendUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long userId) {
        adminService.activateUser(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/role")
    public ResponseEntity<?> assignRole(@PathVariable Long userId, @RequestBody Map<String, String> roleMap) {
        Role.RoleType roleType = Role.RoleType.valueOf(roleMap.get("role"));
        adminService.assignRole(userId, roleType);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reviews/pending")
    public ResponseEntity<List<Rating>> getPendingReviews() {
        List<Rating> pendingReviews = ratingRepository.findByStatus(ModerationStatus.PENDING);
        return ResponseEntity.ok(pendingReviews);
    }

    @PutMapping("/reviews/{id}/approve")
    public ResponseEntity<Void> approveReview(@PathVariable Long id) {
        return updateReviewStatus(id, ModerationStatus.APPROVED);
    }

    @PutMapping("/reviews/{id}/reject")
    public ResponseEntity<Void> rejectReview(@PathVariable Long id) {
        return updateReviewStatus(id, ModerationStatus.REJECTED);
    }

    private ResponseEntity<Void> updateReviewStatus(Long id, ModerationStatus newStatus) {
        Rating rating = ratingRepository.findById(id)
                .orElse(null);

        if (rating == null) {
            return ResponseEntity.notFound().build();
        }

        rating.setStatus(newStatus);
        ratingRepository.save(rating);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = analyticsService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
} 