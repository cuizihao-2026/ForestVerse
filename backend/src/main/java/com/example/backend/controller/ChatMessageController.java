package com.example.backend.controller;

import com.example.backend.entity.ChatMessage;
import com.example.backend.service.ChatMessageService;
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
@RequestMapping("/api/chat")
public class ChatMessageController {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageController.class);

    @Autowired
    private ChatMessageService chatMessageService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            Long receiverId = Long.valueOf(body.get("receiverId").toString());
            String content = body.get("content").toString();
            
            // 验证消息长度
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("消息内容不能为空");
            }
            if (content.length() > 1000) {
                throw new IllegalArgumentException("消息长度不能超过1000字");
            }
            
            ChatMessage message = chatMessageService.sendMessage(currentUserId, receiverId, content);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", message);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("发送消息失败: " + e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/conversation/{friendId}")
    public ResponseEntity<?> getConversation(
            HttpServletRequest request,
            @PathVariable Long friendId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            List<ChatMessage> messages = chatMessageService.getConversationPaginated(currentUserId, friendId, page, size);
            int total = chatMessageService.countConversation(currentUserId, friendId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", messages);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取聊天记录失败: " + e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/mark-read/{friendId}")
    public ResponseEntity<?> markAsRead(
            HttpServletRequest request,
            @PathVariable Long friendId) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            chatMessageService.markAsRead(friendId, currentUserId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "已标记为已读");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("标记已读失败: " + e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/unread/{friendId}")
    public ResponseEntity<?> countUnread(
            HttpServletRequest request,
            @PathVariable Long friendId) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            int count = chatMessageService.countUnread(currentUserId, friendId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", count);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取未读消息数失败: " + e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
