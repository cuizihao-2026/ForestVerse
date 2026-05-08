package com.example.backend.mapper;

import com.example.backend.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper {
    int insert(ChatMessage message);
    
    List<ChatMessage> findByConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    List<ChatMessage> findByConversationPaginated(@Param("userId1") Long userId1, @Param("userId2") Long userId2, @Param("offset") int offset, @Param("limit") int limit);
    
    int countConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    List<ChatMessage> findByUserId(@Param("userId") Long userId);
    
    int markAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
    
    int countUnread(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);
    
    List<Map<String, Object>> getLatestMessagesForFriends(@Param("userId") Long userId, @Param("friendIds") List<Long> friendIds);
    
    List<Map<String, Object>> getUnreadCountsForFriends(@Param("userId") Long userId, @Param("friendIds") List<Long> friendIds);

    int updateContentByContent(@Param("oldContent") String oldContent, @Param("newContent") String newContent);

    List<ChatMessage> findByContent(@Param("content") String content);
}
