package com.example.backend.controller;

import com.example.backend.entity.Notification;
import com.example.backend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/list")
    public ResponseEntity<?> getNotifications(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<Notification> notifications = notificationService.getUnreadNotificationsByUserId(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok("标记成功");
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok("全部标记成功");
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete-read")
    public ResponseEntity<?> deleteReadNotifications(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            notificationService.deleteReadByUserId(userId);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteById(id);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
