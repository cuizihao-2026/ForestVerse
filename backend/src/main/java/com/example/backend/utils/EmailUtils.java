package com.example.backend.utils;

import com.example.backend.config.DynamicMailConfig;
import com.example.backend.config.WebsiteSettings;
import com.example.backend.service.SettingsService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class EmailUtils {
    private final SettingsService settingsService;

    public EmailUtils(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    private WebsiteSettings getSettings() {
        return settingsService.getSettings();
    }

    private JavaMailSender getMailSender() {
        return DynamicMailConfig.createMailSender(getSettings());
    }

    // 生成随机验证码
    public String generateVerificationCode() {
        WebsiteSettings settings = getSettings();
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        int length = settings.getVerificationCodeLength() > 0 
            ? settings.getVerificationCodeLength() 
            : 6;
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    // 发送注册验证码
    public void sendVerificationCode(String toEmail, String code) {
        WebsiteSettings settings = getSettings();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.getEmailFrom());
        message.setTo(toEmail);
        message.setSubject("注册验证码");
        message.setText("您的注册验证码是：" + code + "，有效期为" + 
            settings.getVerificationCodeExpireMinutes() + "分钟。");
        getMailSender().send(message);
    }

    // 发送重置密码验证码
    public void sendResetPasswordCode(String toEmail, String code) {
        WebsiteSettings settings = getSettings();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.getEmailFrom());
        message.setTo(toEmail);
        message.setSubject("重置密码验证码");
        message.setText("您正在重置密码，验证码是：" + code + "，有效期为" + 
            settings.getVerificationCodeExpireMinutes() + "分钟。");
        getMailSender().send(message);
    }
    
    // 发送测试邮件
    public void sendEmail(String toEmail, String subject, String content) {
        WebsiteSettings settings = getSettings();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.getEmailFrom());
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(content);
        getMailSender().send(message);
    }
}
