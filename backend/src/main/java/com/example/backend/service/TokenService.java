package com.example.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    // userId -> 当前活跃的token
    private final Cache<Long, String> activeTokens;
    // userId -> token过期时间戳
    private final Map<Long, Long> tokenExpiryMap = new ConcurrentHashMap<>();

    public TokenService() {
        this.activeTokens = Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(100_000)
                .removalListener((Long key, String value, RemovalCause cause) -> {
                    if (cause == RemovalCause.EXPIRED) {
                        logger.info("用户 " + key + " 的token已过期");
                        tokenExpiryMap.remove(key);
                    }
                })
                .build();
    }

    /**
     * 保存用户的活跃token和过期时间
     */
    public void setActiveToken(Long userId, String token, long expiryTime) {
        activeTokens.put(userId, token);
        tokenExpiryMap.put(userId, expiryTime);
    }

    /**
     * 旧版本方法（保留兼容）
     */
    public void setActiveToken(Long userId, String token) {
        activeTokens.put(userId, token);
    }

    /**
     * 验证token是否是用户当前活跃的token
     */
    public boolean isTokenActive(Long userId, String token) {
        String activeToken = activeTokens.getIfPresent(userId);
        return activeToken != null && token.equals(activeToken);
    }

    /**
     * 刷新token（校验旧token）
     */
    public String refreshToken(Long userId, String oldToken) {
        String storedToken = activeTokens.getIfPresent(userId);
        if (storedToken == null) {
            throw new RuntimeException("token不存在");
        }
        if (!storedToken.equals(oldToken)) {
            throw new RuntimeException("旧token无效");
        }
        return storedToken;
    }

    /**
     * 获取用户的活跃token
     */
    public String getActiveToken(Long userId) {
        return activeTokens.getIfPresent(userId);
    }
    
    /**
     * 获取token过期时间
     */
    public Long getTokenExpiry(Long userId) {
        return tokenExpiryMap.get(userId);
    }

    /**
     * 获取所有在线用户ID
     */
    public Iterable<Long> getAllUserIds() {
        return tokenExpiryMap.keySet();
    }

    /**
     * 清除用户的活跃token
     */
    public void clearActiveToken(Long userId) {
        activeTokens.invalidate(userId);
        tokenExpiryMap.remove(userId);
    }
}
