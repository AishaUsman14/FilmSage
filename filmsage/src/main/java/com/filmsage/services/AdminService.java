package com.filmsage.services;

import com.filmsage.models.User;
import com.filmsage.models.Role;
import com.filmsage.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AdminService {
    
    @Autowired
    private UserRepository userRepository;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public void suspendUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(false);
        userRepository.save(user);
    }
    
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
    }
    
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    public void assignRole(Long userId, Role.RoleType roleType) {
        // Implementation for assigning roles
    }
} 