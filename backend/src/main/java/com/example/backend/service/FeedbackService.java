package com.example.backend.service;

import com.example.backend.entity.Feedback;
import com.example.backend.entity.FeedbackMessage;

import java.util.List;

public interface FeedbackService {
    List<Feedback> getUserFeedbacks(Long userId);
    List<Feedback> getAllFeedbacks();
    Feedback getFeedbackById(Long id);
    void createFeedback(Feedback feedback);
    void updateFeedback(Feedback feedback);
    void updateFeedbackStatus(Long id, String status);

    List<FeedbackMessage> getMessages(Long feedbackId);
    void sendMessage(Long feedbackId, Long senderId, Boolean isAdmin, String content);
    void markMessagesAsRead(Long feedbackId, Long userId);
    Integer countUnreadMessages(Long feedbackId, Long userId);
}
