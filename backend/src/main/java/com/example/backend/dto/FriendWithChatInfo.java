package com.example.backend.dto;

import com.example.backend.entity.User;

public class FriendWithChatInfo {
    private User friend;
    private String lastMessage;
    private String lastMessageTime;
    private Long lastTimestamp;
    private int unreadCount;

    public FriendWithChatInfo() {
    }

    public FriendWithChatInfo(User friend, String lastMessage, String lastMessageTime, Long lastTimestamp, int unreadCount) {
        this.friend = friend;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.lastTimestamp = lastTimestamp;
        this.unreadCount = unreadCount;
    }

    public User getFriend() {
        return friend;
    }

    public void setFriend(User friend) {
        this.friend = friend;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Long getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(Long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
