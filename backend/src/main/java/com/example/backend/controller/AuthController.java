package com.example.backend.controller;

import com.example.backend.config.WebsiteSettings;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.ResetPasswordRequest;
import com.example.backend.entity.User;
import com.example.backend.service.EmailService;
import com.example.backend.service.HumanVerificationService;
import com.example.backend.service.SettingsService;
import com.example.backend.service.TokenService;
import com.example.backend.service.UserService;
import com.example.backend.utils.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private HumanVerificationService humanVerificationService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SettingsService settingsService;

    // 登录接口
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword());

            String token = jwtUtils.generateToken(user.getId(), user.getUsername());
            
            // 获取token过期时间
            long expiryTime = jwtUtils.getExpirationFromToken(token);
            
            // 先清除旧token，再保存为该用户的活跃token和过期时间
            tokenService.clearActiveToken(user.getId());
            tokenService.setActiveToken(user.getId(), token, expiryTime);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("user", user);
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // 注册接口
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            
            // 验证邮箱验证码（仅当邮箱功能启用时）
            if (settings.isEmailEnabled()) {
                if (!emailService.verifyCode(request.getEmail(), request.getEmailCaptcha())) {
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", false);
                    response.put("message", "邮箱验证码错误");

                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }

            // 创建用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setEmail(request.getEmail());

            User registeredUser = userService.register(user);

            // 注册成功，返回用户信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("user", registeredUser);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // 注册失败，返回错误信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // 发送邮箱验证码接口
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> request) {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            
            // 检查邮箱功能是否启用
            if (!settings.isEmailEnabled()) {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", false);
                response.put("message", "邮箱功能未启用");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            String email = request.get("email");

            // 发送邮箱验证码
            emailService.sendVerificationCode(email);

            // 发送成功，返回成功信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "验证码已发送到邮箱");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 发送失败，返回错误信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "验证码发送失败：" + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 验证邮箱验证码接口
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            
            // 检查邮箱功能是否启用
            if (!settings.isEmailEnabled()) {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", false);
                response.put("message", "邮箱功能未启用");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            String email = request.get("email");
            String code = request.get("code");

            // 验证邮箱验证码
            boolean isValid = emailService.verifyCode(email, code);

            // 返回验证结果
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", isValid);
            response.put("message", isValid ? "验证码正确" : "验证码错误");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 验证失败，返回错误信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "验证失败：" + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 获取人机验证验证码接口
    @GetMapping("/captcha")
    public ResponseEntity<?> getCaptcha() {
        try {
            // 生成人机验证
            Map<String, Object> verification = humanVerificationService.generateVerification();

            // 返回验证信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("data", verification);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 生成失败，返回错误信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "验证码生成失败：" + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 验证人机验证码接口
    @PostMapping("/verify-captcha")
    public ResponseEntity<?> verifyCaptcha(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String code = request.get("code");

            // 验证人机验证码
            boolean isValid = humanVerificationService.verifyCode(token, code);

            // 返回验证结果
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", isValid);
            response.put("message", isValid ? "验证通过" : "验证码错误");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 验证失败，返回错误信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "验证失败：" + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 验证用户名和邮箱匹配接口
    @PostMapping("/verify-user")
    public ResponseEntity<?> verifyUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");

            // 验证用户名和邮箱是否匹配
            boolean isValid = userService.verifyUserCredentials(username, email);

            // 返回验证结果
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", isValid);
            response.put("message", isValid ? "用户名和邮箱匹配" : "用户名和邮箱不匹配");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 验证失败，返回错误信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "验证失败：" + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 验证用户名和邮箱唯一性接口
    @PostMapping("/verify-unique")
    public ResponseEntity<?> verifyUnique(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");

            // 检查用户名是否已存在
            if (userService.findByUsername(username) != null) {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", false);
                response.put("message", "用户名已存在");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 检查邮箱是否已存在
            if (userService.findByEmail(email) != null) {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", false);
                response.put("message", "邮箱已被注册");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 验证通过，返回成功信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "用户名和邮箱可用");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 验证失败，返回错误信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "验证失败：" + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 发送找回密码验证码接口
    @PostMapping("/send-reset-code")
    public ResponseEntity<?> sendResetCode(@RequestBody Map<String, String> request) {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            
            // 检查邮箱功能是否启用
            if (!settings.isEmailEnabled()) {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", false);
                response.put("message", "邮箱功能未启用");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            String username = request.get("username");
            String email = request.get("email");

            // 验证用户名和邮箱是否匹配
            if (!userService.verifyUserCredentials(username, email)) {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", false);
                response.put("message", "用户名和邮箱不匹配");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 发送重置密码验证码
            emailService.sendResetPasswordCode(email);

            // 发送成功，返回成功信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "验证码已发送到邮箱");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 发送失败，返回错误信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "验证码发送失败：" + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 重置密码接口
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            
            // 验证邮箱验证码（仅当邮箱功能启用时）
            if (settings.isEmailEnabled()) {
                if (!emailService.verifyCode(request.getEmail(), request.getEmailCaptcha())) {
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", false);
                    response.put("message", "邮箱验证码错误");

                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }

            // 重置密码
            userService.resetPassword(request.getUsername(), request.getEmail(), request.getNewPassword());

            // 重置成功，返回成功信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "密码重置成功");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // 重置失败，返回错误信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            // 重置失败，返回错误信息
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "重置失败：" + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 登出接口
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Long userId = jwtUtils.getUserIdFromToken(token);
                    if (userId != null) {
                        tokenService.clearActiveToken(userId);
                    }
                } catch (Exception e) {
                    // 解析token失败不影响登出
                }
            }

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "登出成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "登出失败：" + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
