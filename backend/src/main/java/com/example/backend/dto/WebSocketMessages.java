package com.example.backend.dto;

import com.example.backend.entity.User;

import java.util.List;

public class WebSocketMessages {

    public static class NotificationMessage {
        private String type;
        private User data;
        private String reason;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public User getData() {
            return data;
        }

        public void setData(User data) {
            this.data = data;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class OnlineCountMessage {
        private String type;
        private int onlineCount;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getOnlineCount() {
            return onlineCount;
        }

        public void setOnlineCount(int onlineCount) {
            this.onlineCount = onlineCount;
        }
    }

    public static class PingMessage {
        private String type;
        private Long userId;
        private Long timestamp;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class PongMessage {
        private String type;
        private Long timestamp;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class OnlineUsersMessage {
        private String type;
        private List<Long> onlineUsers;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<Long> getOnlineUsers() {
            return onlineUsers;
        }

        public void setOnlineUsers(List<Long> onlineUsers) {
            this.onlineUsers = onlineUsers;
        }
    }
}
