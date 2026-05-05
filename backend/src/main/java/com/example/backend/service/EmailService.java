package com.example.backend.service;

public interface EmailService {
    // 发送注册验证码
    void sendVerificationCode(String email);

    // 发送重置密码验证码
    void sendResetPasswordCode(String email);

    // 验证邮箱验证码
    boolean verifyCode(String email, String code);
}
