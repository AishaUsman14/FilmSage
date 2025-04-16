package com.filmsage.services;

import com.filmsage.models.User;
import com.filmsage.models.Role;
import com.filmsage.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
    }

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser));
        
        List<User> users = adminService.getAllUsers();
        
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("testuser", users.get(0).getUsername());
    }

    @Test
    void suspendUser_ShouldDisableUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        adminService.suspendUser(1L);
        
        assertFalse(testUser.isEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void activateUser_ShouldEnableUser() {
        testUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        adminService.activateUser(1L);
        
        assertTrue(testUser.isEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void deleteUser_ShouldCallRepository() {
        adminService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void suspendUser_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> {
            adminService.suspendUser(1L);
        });
    }
} 