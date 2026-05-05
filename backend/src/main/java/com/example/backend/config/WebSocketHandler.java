package com.example.backend.config;

import com.example.backend.dto.WebSocketMessages.NotificationMessage;
import com.example.backend.dto.WebSocketMessages.OnlineCountMessage;
import com.example.backend.dto.WebSocketMessages.OnlineUsersMessage;
import com.example.backend.dto.WebSocketMessages.PingMessage;
import com.example.backend.dto.WebSocketMessages.PongMessage;
import com.example.backend.entity.ChatMessage;
import com.example.backend.entity.User;
import com.example.backend.service.TokenService;
import com.example.backend.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private WebSocketSessionManager sessionManager;

    private final ObjectMapper objectMapper;

    public WebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        logger.info("WebSocketHandler 已初始化，心跳检查和Token刷新任务已通过 HeartbeatScheduler 启动");
    }

    // 保存待认证的会话
    private final Map<String, WebSocketSession> pendingSessions = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 先接受连接，等待认证消息
        pendingSessions.put(session.getId(), session);
        logger.info("WebSocket连接已建立，等待认证消息");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // 检查是否是待认证会话
        if (pendingSessions.containsKey(session.getId())) {
            try {
                // 尝试解析认证消息
                com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(payload);
                if (jsonNode.has("type") && "auth".equals(jsonNode.get("type").asText())) {
                    Long userId = jsonNode.get("userId").asLong();
                    String username = jsonNode.get("username").asText();
                    String token = jsonNode.get("token").asText();

                    if (token == null) {
                        logger.info("WebSocket认证失败：缺少token");
                        session.close(CloseStatus.NOT_ACCEPTABLE);
                        pendingSessions.remove(session.getId());
                        return;
                    }

                    if (!jwtUtils.validateToken(token)) {
                        logger.info("WebSocket认证失败：token无效");
                        session.close(CloseStatus.NOT_ACCEPTABLE);
                        pendingSessions.remove(session.getId());
                        return;
                    }

                    if (!tokenService.isTokenActive(userId, token)) {
                        logger.info("WebSocket认证失败：token不活跃 - 用户: " + username);
                        session.close(CloseStatus.NOT_ACCEPTABLE);
                        pendingSessions.remove(session.getId());
                        return;
                    }

                    // 认证成功
                    pendingSessions.remove(session.getId());
                    sessionManager.addSession(userId, session);

                    // 发送认证成功响应
                    Map<String, Object> authResponse = new java.util.HashMap<>();
                    authResponse.put("type", "auth_success");
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(authResponse)));

                    broadcastOnlineCount();
                    broadcastOnlineUsers();
                    logger.info("WebSocket认证成功 - 用户: " + username);
                    return;
                }
            } catch (Exception e) {
                logger.info("解析认证消息失败: " + e.getMessage());
            }
        }

        // 处理心跳等其他消息（已认证会话）
        try {
            PingMessage pingMessage = objectMapper.readValue(payload, PingMessage.class);
            if ("ping".equals(pingMessage.getType())) {
                Long userId = sessionManager.getUserBySession(session);
                if (userId != null) {
                    sessionManager.updateHeartbeat(userId);

                    PongMessage pongMessage = new PongMessage();
                    pongMessage.setType("pong");
                    pongMessage.setTimestamp(System.currentTimeMillis());
                    String pongJson = objectMapper.writeValueAsString(pongMessage);
                    session.sendMessage(new TextMessage(pongJson));
                }
                return;
            }
        } catch (Exception e) {
            if ("ping".equals(payload)) {
                Long userId = sessionManager.getUserBySession(session);
                if (userId != null) {
                    sessionManager.updateHeartbeat(userId);
                    session.sendMessage(new TextMessage("pong"));
                }
                return;
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        pendingSessions.remove(session.getId());
        sessionManager.removeSession(session);
        broadcastOnlineCount();
        broadcastOnlineUsers();
    }

    private void broadcastOnlineCount() {
        OnlineCountMessage message = new OnlineCountMessage();
        message.setType("ONLINE_COUNT");
        message.setOnlineCount(sessionManager.getOnlineCount());

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            for (WebSocketSession session : sessionManager.getUserSessions().values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("发送在线人数通知失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("序列化在线人数消息失败: " + e.getMessage());
        }
    }

    private void broadcastOnlineUsers() {
        OnlineUsersMessage message = new OnlineUsersMessage();
        message.setType("ONLINE_USERS");
        message.setOnlineUsers(new java.util.ArrayList<>(sessionManager.getUserSessions().keySet()));

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            for (WebSocketSession session : sessionManager.getUserSessions().values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("发送在线用户列表通知失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("序列化在线用户列表消息失败: " + e.getMessage());
        }
    }

    public int getOnlineCount() {
        return sessionManager.getOnlineCount();
    }

    public List<User> getOnlineUsers() {
        return sessionManager.getOnlineUsersList();
    }

    public void sendUserUpdateNotification(User updatedUser) {
        WebSocketSession session = sessionManager.getSession(updatedUser.getId());
        if (session != null && session.isOpen()) {
            try {
                NotificationMessage message = new NotificationMessage();
                message.setType("USER_UPDATED");
                message.setData(updatedUser);
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                sessionManager.updateOnlineUser(updatedUser);
                logger.info("已向用户 " + updatedUser.getUsername() + " 发送更新通知");
            } catch (IOException e) {
                logger.error("发送WebSocket消息失败: " + e.getMessage());
            }
        }
    }

    public void sendUserDeletedNotification(Long userId, String reason) {
        WebSocketSession session = sessionManager.getSession(userId);
        if (session != null && session.isOpen()) {
            try {
                NotificationMessage message = new NotificationMessage();
                message.setType("USER_DELETED");
                message.setReason(reason);
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                User user = sessionManager.getOnlineUsers().get(userId);
                String username = user != null ? user.getUsername() : String.valueOf(userId);
                logger.info("已向用户 " + username + " 发送账户删除通知，原因: " + reason);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                session.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                logger.error("发送WebSocket删除通知失败: " + e.getMessage());
            }
        }
    }

    public void sendNewNotification(Long userId) {
        WebSocketSession session = sessionManager.getSession(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = new java.util.HashMap<>();
                message.put("type", "NEW_NOTIFICATION");
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                User user = sessionManager.getOnlineUsers().get(userId);
                String username = user != null ? user.getUsername() : String.valueOf(userId);
                logger.info("已向用户 " + username + " 发送新通知提醒");
            } catch (IOException e) {
                logger.error("发送新通知提醒失败: " + e.getMessage());
            }
        }
    }

    public void sendFriendRequest(Long receiverId) {
        WebSocketSession session = sessionManager.getSession(receiverId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = new java.util.HashMap<>();
                message.put("type", "NEW_FRIEND_REQUEST");
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                User user = sessionManager.getOnlineUsers().get(receiverId);
                String username = user != null ? user.getUsername() : String.valueOf(receiverId);
                logger.info("已向用户 " + username + " 发送新好友请求提醒");
            } catch (IOException e) {
                logger.error("发送新好友请求提醒失败: " + e.getMessage());
            }
        }
    }

    public void sendFriendAccepted(Long userId) {
        WebSocketSession session = sessionManager.getSession(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = new java.util.HashMap<>();
                message.put("type", "FRIEND_ACCEPTED");
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                User user = sessionManager.getOnlineUsers().get(userId);
                String username = user != null ? user.getUsername() : String.valueOf(userId);
                logger.info("已向用户 " + username + " 发送好友请求被接受提醒");
            } catch (IOException e) {
                logger.error("发送好友请求被接受提醒失败: " + e.getMessage());
            }
        }
    }

    public void sendChatMessage(ChatMessage chatMessage) {
        WebSocketSession session = sessionManager.getSession(chatMessage.getReceiverId());
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = new java.util.HashMap<>();
                message.put("type", "NEW_CHAT_MESSAGE");
                message.put("data", chatMessage);
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                User user = sessionManager.getOnlineUsers().get(chatMessage.getReceiverId());
                String username = user != null ? user.getUsername() : String.valueOf(chatMessage.getReceiverId());
                logger.info("已向用户 " + username + " 发送新聊天消息");
            } catch (IOException e) {
                logger.error("发送聊天消息失败: " + e.getMessage());
            }
        }
    }

    public void broadcastCommentApproved(Long articleId, Long commentId) {
        try {
            Map<String, Object> message = new java.util.HashMap<>();
            message.put("type", "COMMENT_APPROVED");
            message.put("articleId", articleId);
            message.put("commentId", commentId);
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            for (WebSocketSession session : sessionManager.getUserSessions().values()) {
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("发送评论审核通过通知失败: " + e.getMessage());
                    }
                }
            }
            logger.info("已广播评论审核通过通知，文章ID: " + articleId + ", 评论ID: " + commentId);
        } catch (Exception e) {
            logger.error("序列化评论审核通过消息失败: " + e.getMessage());
        }
    }

    public void sendCommentRejected(Long userId, Long articleId, Long commentId) {
        WebSocketSession session = sessionManager.getSession(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = new java.util.HashMap<>();
                message.put("type", "COMMENT_REJECTED");
                message.put("articleId", articleId);
                message.put("commentId", commentId);
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                logger.info("已向用户 " + userId + " 发送评论被拒绝通知，评论ID: " + commentId);
            } catch (IOException e) {
                logger.error("发送评论被拒绝通知失败: " + e.getMessage());
            }
        }
    }

    public void broadcastArticleApproved(Long articleId) {
        try {
            Map<String, Object> message = new java.util.HashMap<>();
            message.put("type", "ARTICLE_APPROVED");
            message.put("articleId", articleId);
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            for (WebSocketSession session : sessionManager.getUserSessions().values()) {
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("发送文章审核通过通知失败: " + e.getMessage());
                    }
                }
            }
            logger.info("已广播文章审核通过通知，文章ID: " + articleId);
        } catch (Exception e) {
            logger.error("序列化文章审核通过消息失败: " + e.getMessage());
        }
    }

    public void broadcastForcedRefresh() {
        try {
            Map<String, Object> message = new java.util.HashMap<>();
            message.put("type", "FORCED_REFRESH");
            message.put("timestamp", System.currentTimeMillis());
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            for (WebSocketSession session : sessionManager.getUserSessions().values()) {
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("发送强制刷新通知失败: " + e.getMessage());
                    }
                }
            }
            logger.info("已广播强制刷新通知，在线用户数: " + sessionManager.getOnlineCount());
        } catch (Exception e) {
            logger.error("序列化强制刷新消息失败: " + e.getMessage());
        }
    }

    public void broadcastArticleUnpublished(Long articleId, String articleTitle, Integer newStatus) {
        try {
            Map<String, Object> message = new java.util.HashMap<>();
            message.put("type", "ARTICLE_UNPUBLISHED");
            message.put("articleId", articleId);
            message.put("articleTitle", articleTitle);
            message.put("newStatus", newStatus);
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            for (WebSocketSession session : sessionManager.getUserSessions().values()) {
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("发送文章下架通知失败: " + e.getMessage());
                    }
                }
            }
            logger.info("已广播文章下架通知，文章ID: " + articleId + ", 标题: " + articleTitle + ", 新状态: " + newStatus);
        } catch (Exception e) {
            logger.error("序列化文章下架消息失败: " + e.getMessage());
        }
    }
}
