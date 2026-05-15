package com.example.backend.controller;

import com.example.backend.entity.Feedback;
import com.example.backend.entity.FeedbackMessage;
import com.example.backend.entity.User;
import com.example.backend.service.FeedbackService;
import com.example.backend.service.UserService;
import com.example.backend.service.RolePermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private UserService userService;

    @Autowired
    private RolePermissionService rolePermissionService;

    private boolean hasFeedbackManagePermission(User user) {
        return rolePermissionService.hasPermission(user.getRole(), "feedback.manage");
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyFeedbacks(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<Feedback> feedbacks = feedbackService.getUserFeedbacks(userId);
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            logger.error("获取反馈列表失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllFeedbacks(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (!hasFeedbackManagePermission(currentUser)) {
                return ResponseEntity.status(403).body("权限不足");
            }
            List<Feedback> feedbacks = feedbackService.getAllFeedbacks();
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            logger.error("获取全部反馈失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFeedbackDetail(@PathVariable Long id) {
        try {
            Feedback feedback = feedbackService.getFeedbackById(id);
            if (feedback == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            logger.error("获取反馈详情失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createFeedback(@RequestBody Feedback feedback, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            feedback.setUserId(userId);
            feedbackService.createFeedback(feedback);
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            logger.error("提交反馈失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (!hasFeedbackManagePermission(currentUser)) {
                return ResponseEntity.status(403).body("权限不足");
            }
            String status = body.get("status");
            feedbackService.updateFeedbackStatus(id, status);
            return ResponseEntity.ok("状态更新成功");
        } catch (Exception e) {
            logger.error("更新状态失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            feedbackService.markMessagesAsRead(id, userId);
            List<FeedbackMessage> messages = feedbackService.getMessages(id);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("获取消息失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            Boolean isAdmin = (Boolean) request.getAttribute("isAdmin");
            if (isAdmin != null && isAdmin) {
                if (!hasFeedbackManagePermission(currentUser)) {
                    return ResponseEntity.status(403).body("权限不足");
                }
            }
            if (isAdmin == null) isAdmin = false;
            String content = body.get("content");
            feedbackService.sendMessage(id, userId, isAdmin, content);
            return ResponseEntity.ok("发送成功");
        } catch (Exception e) {
            logger.error("发送消息失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/unread-count")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Integer count = feedbackService.countUnreadMessages(id, userId);
            Map<String, Integer> result = new HashMap<>();
            result.put("count", count);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取未读数失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
