package com.example.backend.config;

import com.example.backend.entity.User;
import com.example.backend.service.SettingsService;
import com.example.backend.service.TokenService;
import com.example.backend.service.UserService;
import com.example.backend.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    @Lazy
    private UserService userService;

    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<Long, User> onlineUsers = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastHeartbeatTimes = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public WebSocketSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<Long, WebSocketSession> getUserSessions() {
        return userSessions;
    }

    public Map<Long, User> getOnlineUsers() {
        return onlineUsers;
    }

    public Map<Long, Long> getLastHeartbeatTimes() {
        return lastHeartbeatTimes;
    }

    public int getOnlineCount() {
        return userSessions.size();
    }

    public List<User> getOnlineUsersList() {
        return new ArrayList<>(onlineUsers.values());
    }

    public void addSession(Long userId, WebSocketSession session) {
        WebSocketSession existingSession = userSessions.get(userId);
        userSessions.put(userId, session);
        lastHeartbeatTimes.put(userId, System.currentTimeMillis());

        User user = userService.findById(userId);
        if (user != null) {
            onlineUsers.put(userId, user);
            logger.info("用户 " + user.getUsername() + " 已连接");
        } else {
            logger.info("用户 " + userId + " 已连接，但用户信息未找到");
        }

        if (existingSession != null && existingSession.isOpen()) {
            try {
                Map<String, Object> kickMessage = new java.util.HashMap<>();
                kickMessage.put("type", "ACCOUNT_KICKED");
                kickMessage.put("message", "您的账号已在其他地方登录");
                existingSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(kickMessage)));
                Thread.sleep(100);
                existingSession.close(CloseStatus.NORMAL);
            } catch (Exception e) {
                logger.error("关闭旧连接失败: " + e.getMessage(), e);
            }
        }
    }

    public Long getUserBySession(WebSocketSession session) {
        for (Map.Entry<Long, WebSocketSession> entry : userSessions.entrySet()) {
            if (entry.getValue().equals(session)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeSession(WebSocketSession session) {
        Long userId = getUserBySession(session);
        if (userId != null) {
            User user = onlineUsers.get(userId);
            if (user != null) {
                logger.info("用户 " + user.getUsername() + " 连接已关闭");
            } else {
                logger.info("用户 " + userId + " 连接已关闭");
            }
            userSessions.remove(userId);
            onlineUsers.remove(userId);
            lastHeartbeatTimes.remove(userId);
        }
    }

    public void updateHeartbeat(Long userId) {
        lastHeartbeatTimes.put(userId, System.currentTimeMillis());
    }

    public WebSocketSession getSession(Long userId) {
        return userSessions.get(userId);
    }

    public void updateOnlineUser(User user) {
        onlineUsers.put(user.getId(), user);
    }

    public void checkHeartbeats() {
        WebsiteSettings settings = settingsService.getSettings();
        if (!settings.isHeartbeatEnabled()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeoutMillis = settings.getHeartbeatTimeout() * 1000L;

        for (Map.Entry<Long, WebSocketSession> entry : userSessions.entrySet()) {
            Long userId = entry.getKey();
            WebSocketSession session = entry.getValue();
            Long lastHeartbeat = lastHeartbeatTimes.get(userId);

            if (lastHeartbeat != null && (currentTime - lastHeartbeat) > timeoutMillis) {
                try {
                    User user = onlineUsers.get(userId);
                    String username = user != null ? user.getUsername() : String.valueOf(userId);
                    logger.info("用户 " + username + " 心跳超时，关闭连接");
                    session.close(CloseStatus.NORMAL);
                } catch (IOException e) {
                    logger.error("关闭超时连接失败: " + e.getMessage(), e);
                }
            }
        }
    }

    public void checkTokenAndRefresh() {
        long currentTime = System.currentTimeMillis();
        long warningThreshold = 60 * 60 * 1000;

        for (Long userId : tokenService.getAllUserIds()) {
            Long expiryTime = tokenService.getTokenExpiry(userId);
            if (expiryTime == null) {
                continue;
            }

            long remaining = expiryTime - currentTime;

            if (remaining > 0 && remaining < warningThreshold) {
                WebSocketSession session = userSessions.get(userId);

                if (session != null && session.isOpen()) {
                    try {
                        User user = onlineUsers.get(userId);
                        String currentToken = tokenService.getActiveToken(userId);

                        if (currentToken == null) {
                            continue;
                        }

                        if (!jwtUtils.validateToken(currentToken)) {
                            continue;
                        }

                        String username = user != null ? user.getUsername() : jwtUtils.getUsernameFromToken(currentToken);
                        if (username == null) {
                            continue;
                        }

                        String newToken = jwtUtils.generateToken(userId, username);
                        long newExpiryTime = jwtUtils.getExpirationFromToken(newToken);

                        sendNewTokenToClient(userId, newToken);
                        tokenService.setActiveToken(userId, newToken, newExpiryTime);

                        logger.info("用户 " + (username != null ? username : userId) + " token即将过期，用户在线，已刷新");
                    } catch (Exception e) {
                        logger.error("刷新token失败: " + e.getMessage(), e);
                    }
                } else {
                    User user = onlineUsers.get(userId);
                    String username = user != null ? user.getUsername() : String.valueOf(userId);
                    logger.info("用户 " + username + " token即将过期，但用户不在线，等待自然过期");
                }
            }
        }
    }

    private void sendNewTokenToClient(Long userId, String newToken) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = new java.util.HashMap<>();
                message.put("type", "TOKEN_REFRESHED");
                message.put("token", newToken);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                logger.error("发送新token失败: " + e.getMessage(), e);
            }
        }
    }
}
