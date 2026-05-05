package com.example.backend.service.impl;

import com.example.backend.config.WebsiteSettings;
import com.example.backend.service.EmailService;
import com.example.backend.service.SettingsService;
import com.example.backend.utils.EmailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private EmailUtils emailUtils;

    @Autowired
    private SettingsService settingsService;

    // 缓存邮箱验证码，key为邮箱，value为验证码
    private final ConcurrentHashMap<String, String> codeCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void sendVerificationCode(String email) {
        WebsiteSettings settings = settingsService.getSettings();
        
        // 检查邮箱功能是否启用
        if (!settings.isEmailEnabled()) {
            throw new RuntimeException("邮箱功能未启用");
        }

        // 生成验证码
        String code = emailUtils.generateVerificationCode();

        logger.info("发送注册验证码到: {}", email);

        try {
            // 发送验证码到邮箱
            emailUtils.sendVerificationCode(email, code);
            logger.info("邮件发送成功到: {}", email);
        } catch (Exception e) {
            logger.error("邮件发送失败到: {}, 错误: {}", email, e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }

        // 存储验证码到缓存
        codeCache.put(email, code);

        // 设置验证码过期时间
        long expireTime = settings.getVerificationCodeExpireMinutes();
        scheduler.schedule(() -> codeCache.remove(email), expireTime, TimeUnit.MINUTES);
    }

    @Override
    public void sendResetPasswordCode(String email) {
        WebsiteSettings settings = settingsService.getSettings();
        
        // 检查邮箱功能是否启用
        if (!settings.isEmailEnabled()) {
            throw new RuntimeException("邮箱功能未启用");
        }

        // 生成验证码
        String code = emailUtils.generateVerificationCode();

        logger.info("发送重置密码验证码到: {}", email);

        try {
            // 发送重置密码验证码到邮箱
            emailUtils.sendResetPasswordCode(email, code);
            logger.info("重置密码邮件发送成功到: {}", email);
        } catch (Exception e) {
            logger.error("重置密码邮件发送失败到: {}, 错误: {}", email, e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }

        // 存储验证码到缓存
        codeCache.put(email, code);

        // 设置验证码过期时间
        long expireTime = settings.getVerificationCodeExpireMinutes();
        scheduler.schedule(() -> codeCache.remove(email), expireTime, TimeUnit.MINUTES);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        WebsiteSettings settings = settingsService.getSettings();
        
        // 如果邮箱功能未启用，则直接返回 true（跳过验证）
        if (!settings.isEmailEnabled()) {
            return true;
        }
        
        // 从缓存中获取验证码
        String cachedCode = codeCache.get(email);

        // 验证验证码是否正确
        return cachedCode != null && cachedCode.equals(code);
    }
}
