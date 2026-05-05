package com.example.backend.service.impl;

import com.example.backend.config.WebSocketHandler;
import com.example.backend.entity.Notification;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    @Lazy
    private WebSocketHandler webSocketHandler;

    @Override
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationMapper.findByUserId(userId);
    }

    @Override
    public List<Notification> getUnreadNotificationsByUserId(Long userId) {
        return notificationMapper.findUnreadByUserId(userId);
    }

    @Override
    public void sendNotification(Long userId, String type, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        
        // 发送WebSocket提醒给用户
        webSocketHandler.sendNewNotification(userId);
    }

    @Override
    public void markAsRead(Long id) {
        notificationMapper.markAsRead(id);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }

    @Override
    public void deleteReadByUserId(Long userId) {
        notificationMapper.deleteReadByUserId(userId);
    }

    @Override
    public void deleteById(Long id) {
        notificationMapper.deleteById(id);
    }
}
