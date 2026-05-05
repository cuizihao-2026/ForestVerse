package com.example.backend.service;

import com.example.backend.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    ChatMessage sendMessage(Long senderId, Long receiverId, String content);
    List<ChatMessage> getConversation(Long userId1, Long userId2);
    List<ChatMessage> getConversationPaginated(Long userId1, Long userId2, int page, int size);
    int countConversation(Long userId1, Long userId2);
    void markAsRead(Long senderId, Long receiverId);
    int countUnread(Long receiverId, Long senderId);
}
