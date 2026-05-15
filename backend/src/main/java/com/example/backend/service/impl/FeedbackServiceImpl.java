package com.example.backend.service.impl;

import com.example.backend.entity.Feedback;
import com.example.backend.entity.FeedbackMessage;
import com.example.backend.mapper.FeedbackMapper;
import com.example.backend.mapper.FeedbackMessageMapper;
import com.example.backend.service.FeedbackService;
import com.example.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private FeedbackMessageMapper feedbackMessageMapper;

    @Autowired
    private NotificationService notificationService;

    @Override
    public List<Feedback> getUserFeedbacks(Long userId) {
        return feedbackMapper.findByUserId(userId);
    }

    @Override
    public List<Feedback> getAllFeedbacks() {
        return feedbackMapper.findAll();
    }

    @Override
    public Feedback getFeedbackById(Long id) {
        return feedbackMapper.findById(id);
    }

    @Override
    public void createFeedback(Feedback feedback) {
        feedback.setStatus("PENDING");
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
        feedbackMapper.insert(feedback);
    }

    @Override
    public void updateFeedback(Feedback feedback) {
        feedback.setUpdatedAt(LocalDateTime.now());
        feedbackMapper.update(feedback);
    }

    @Override
    public void updateFeedbackStatus(Long id, String status) {
        feedbackMapper.updateStatus(id, status);
        
        Feedback feedback = feedbackMapper.findById(id);
        if (feedback != null) {
            String title = "反馈状态更新";
            String message = "您的反馈\"" + feedback.getTitle() + "\"状态已更新为：" + getStatusText(status);
            notificationService.sendNotification(feedback.getUserId(), "SYSTEM", title, message);
        }
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "待处理";
            case "PROCESSING": return "处理中";
            case "RESOLVED": return "已解决";
            case "CLOSED": return "已关闭";
            default: return status;
        }
    }

    @Override
    public List<FeedbackMessage> getMessages(Long feedbackId) {
        return feedbackMessageMapper.findByFeedbackId(feedbackId);
    }

    @Override
    public void sendMessage(Long feedbackId, Long senderId, Boolean isAdmin, String content) {
        FeedbackMessage message = new FeedbackMessage();
        message.setFeedbackId(feedbackId);
        message.setSenderId(senderId);
        message.setIsAdmin(isAdmin);
        message.setContent(content);
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        feedbackMessageMapper.insert(message);

        Feedback feedback = feedbackMapper.findById(feedbackId);
        if (feedback != null) {
            Long notifyUserId = isAdmin ? feedback.getUserId() : null;
            if (notifyUserId != null) {
                String title = "反馈有新回复";
                String msg = "您的反馈\"" + feedback.getTitle() + "\"收到了新回复";
                notificationService.sendNotification(notifyUserId, "SYSTEM", title, msg);
            }
        }
    }

    @Override
    public void markMessagesAsRead(Long feedbackId, Long userId) {
        feedbackMessageMapper.markAsRead(feedbackId, userId);
    }

    @Override
    public Integer countUnreadMessages(Long feedbackId, Long userId) {
        return feedbackMessageMapper.countUnread(feedbackId, userId);
    }
}
