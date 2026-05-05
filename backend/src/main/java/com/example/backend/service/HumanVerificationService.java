package com.example.backend.service;

import java.util.Map;

public interface HumanVerificationService {
    // 生成人机验证令牌和验证码
    Map<String, Object> generateVerification();

    // 验证人机验证码
    boolean verifyCode(String token, String code);
}
