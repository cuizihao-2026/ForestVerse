package com.example.backend.service.impl;

import com.example.backend.config.WebsiteSettings;
import com.example.backend.service.HumanVerificationService;
import com.example.backend.service.SettingsService;
import com.example.backend.utils.CaptchaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class HumanVerificationServiceImpl implements HumanVerificationService {

    @Autowired
    private SettingsService settingsService;

    @Autowired
    @Lazy
    private HumanVerificationServiceImpl self;

    // 缓存验证令牌和验证码，key为令牌，value为验证码
    private final ConcurrentHashMap<String, String> verificationCache = new ConcurrentHashMap<>();

    // 缓存过期时间（5分钟）
    private static final long EXPIRE_TIME = TimeUnit.MINUTES.toMillis(5);

    @Override
    public Map<String, Object> generateVerification() {
        WebsiteSettings settings = settingsService.getSettings();
        
        // 生成验证码
        String code = CaptchaUtils.generateCode(settings.getCaptchaCodeLength());

        // 生成验证码图片
        String imageBase64 = CaptchaUtils.generateImage(
            code,
            settings.getCaptchaNoiseCount(),
            settings.getCaptchaLineCount(),
            settings.isCaptchaRotateEnabled()
        );

        // 生成验证令牌
        String token = UUID.randomUUID().toString();

        // 存储令牌和验证码到缓存
        verificationCache.put(token, code);

        // 设置令牌过期时间
        self.expireVerificationToken(token);

        // 返回令牌和验证码图片
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("image", "data:image/png;base64," + imageBase64);

        return result;
    }

    @Override
    public boolean verifyCode(String token, String code) {
        WebsiteSettings settings = settingsService.getSettings();
        
        // 从缓存中获取验证码
        String cachedCode = verificationCache.get(token);

        // 验证验证码是否正确
        boolean isValid;
        if (settings.isCaptchaCaseSensitive()) {
            isValid = cachedCode != null && cachedCode.equals(code);
        } else {
            isValid = cachedCode != null && cachedCode.equalsIgnoreCase(code);
        }

        // 验证后移除令牌，防止重复使用
        if (isValid) {
            verificationCache.remove(token);
        }

        return isValid;
    }

    @Async
    public void expireVerificationToken(String token) {
        try {
            Thread.sleep(EXPIRE_TIME);
            verificationCache.remove(token);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
