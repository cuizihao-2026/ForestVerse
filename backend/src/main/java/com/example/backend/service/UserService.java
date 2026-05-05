package com.example.backend.service;

import com.example.backend.dto.UserStats;
import com.example.backend.entity.User;
import java.util.List;

public interface UserService {
    User register(User user);

    User login(String username, String password);

    User findByUsername(String username);

    User findByEmail(String email);

    User findById(Long id);

    boolean verifyUserCredentials(String username, String email);

    void resetPassword(String username, String email, String newPassword);

    List<User> findAllUsers();

    int countUsers();

    void updateUserRole(Long userId, String role);

    void updateUserStatus(Long userId, String status);

    User updateUser(User user);
    
    void changePassword(Long userId, String oldPassword, String newPassword);
    
    void deleteUser(Long userId);
    
    UserStats getUserStats();
}