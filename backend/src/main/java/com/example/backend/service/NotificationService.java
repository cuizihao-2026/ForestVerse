package com.example.backend.service;

import com.example.backend.entity.Notification;

import java.util.List;

public interface NotificationService {
    List<Notification> getNotificationsByUserId(Long userId);
    List<Notification> getUnreadNotificationsByUserId(Long userId);
    void sendNotification(Long userId, String type, String title, String message);
    void markAsRead(Long id);
    void markAllAsRead(Long userId);
    void deleteReadByUserId(Long userId);
    void deleteById(Long id);
}
