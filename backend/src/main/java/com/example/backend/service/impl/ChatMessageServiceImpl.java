package com.example.backend.service.impl;

import com.example.backend.config.WebSocketHandler;
import com.example.backend.entity.ChatMessage;
import com.example.backend.mapper.ChatMessageMapper;
import com.example.backend.mapper.FriendshipMapper;
import com.example.backend.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private FriendshipMapper friendshipMapper;

    @Autowired
    @Lazy
    private WebSocketHandler webSocketHandler;

    @Override
    @Transactional
    public ChatMessage sendMessage(Long senderId, Long receiverId, String content) {
        // 检查是否是好友
        if (friendshipMapper.findByUserAndFriend(senderId, receiverId) == null) {
            throw new RuntimeException("你们还不是好友，不能发送消息");
        }

        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        message.setIsRead(false);
        chatMessageMapper.insert(message);

        // 通过WebSocket推送消息
        webSocketHandler.sendChatMessage(message);

        return message;
    }

    @Override
    public List<ChatMessage> getConversation(Long userId1, Long userId2) {
        return chatMessageMapper.findByConversation(userId1, userId2);
    }

    @Override
    public List<ChatMessage> getConversationPaginated(Long userId1, Long userId2, int page, int size) {
        int offset = (page - 1) * size;
        return chatMessageMapper.findByConversationPaginated(userId1, userId2, offset, size);
    }

    @Override
    public int countConversation(Long userId1, Long userId2) {
        return chatMessageMapper.countConversation(userId1, userId2);
    }

    @Override
    public void markAsRead(Long senderId, Long receiverId) {
        chatMessageMapper.markAsRead(senderId, receiverId);
    }

    @Override
    public int countUnread(Long receiverId, Long senderId) {
        return chatMessageMapper.countUnread(receiverId, senderId);
    }
}
