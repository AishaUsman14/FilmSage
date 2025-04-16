package com.filmsage.config;

import com.filmsage.models.User;
import com.filmsage.models.Role;
import com.filmsage.repositories.UserRepository;
import com.filmsage.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create roles if they don't exist
        if (roleRepository.count() == 0) {
            Role userRole = new Role();
            userRole.setName(Role.RoleType.ROLE_USER);
            roleRepository.save(userRole);

            Role adminRole = new Role();
            adminRole.setName(Role.RoleType.ROLE_ADMIN);
            roleRepository.save(adminRole);

            Role modRole = new Role();
            modRole.setName(Role.RoleType.ROLE_MODERATOR);
            roleRepository.save(modRole);
        }

        // Create test users if they don't exist
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@filmsage.com");
            admin.getRoles().add(roleRepository.findByName(Role.RoleType.ROLE_ADMIN));
            userRepository.save(admin);

            User user1 = new User();
            user1.setUsername("user1");
            user1.setPassword(passwordEncoder.encode("user123"));
            user1.setEmail("user1@example.com");
            user1.getRoles().add(roleRepository.findByName(Role.RoleType.ROLE_USER));
            userRepository.save(user1);

            User user2 = new User();
            user2.setUsername("user2");
            user2.setPassword("user123");
            user2.setEmail("user2@example.com");
            user2.getRoles().add(roleRepository.findByName(Role.RoleType.ROLE_USER));
            userRepository.save(user2);
        }
    }
} 