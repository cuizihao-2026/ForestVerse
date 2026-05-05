package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.config.WebsiteSettings;
import com.example.backend.service.SettingsService;
import com.example.backend.service.UserService;
import com.example.backend.utils.EmailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = {"http://localhost:8020", "http://127.0.0.1:8020"})
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @Autowired
    private SettingsService settingsService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailUtils emailUtils;

    @GetMapping
    public ResponseEntity<?> getSettings(jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            
            if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
                return ResponseEntity.status(403).body("您没有权限访问此接口");
            }
            
            return ResponseEntity.ok(settingsService.getSettings());
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(jakarta.servlet.http.HttpServletRequest request, @RequestBody WebsiteSettings settings) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            
            if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
                return ResponseEntity.status(403).body("您没有权限修改此设置");
            }
            
            WebsiteSettings updatedSettings = settingsService.updateSettings(settings);
            return ResponseEntity.ok(updatedSettings);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // 获取单个配置项
    @GetMapping("/{key}")
    public ResponseEntity<?> getSetting(jakarta.servlet.http.HttpServletRequest request, @PathVariable String key) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            
            if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
                return ResponseEntity.status(403).body("您没有权限访问此接口");
            }
            
            // 兼容旧键名
            if ("heartbeatInterval".equals(key)) {
                key = "heartbeatRate";
            }
            
            Object value = settingsService.getSetting(key);
            if (value == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("key", key);
            result.put("value", value);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // 更新单个配置项
    @PutMapping("/{key}")
    public ResponseEntity<?> updateSetting(jakarta.servlet.http.HttpServletRequest request, @PathVariable String key, @RequestBody Map<String, Object> body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            
            if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
                return ResponseEntity.status(403).body("您没有权限修改此设置");
            }
            
            // 兼容旧键名
            if ("heartbeatInterval".equals(key)) {
                key = "heartbeatRate";
            }
            
            Object value = body.get("value");
            settingsService.setSetting(key, value);
            
            Map<String, Object> result = new HashMap<>();
            result.put("key", key);
            result.put("value", value);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // 发送测试邮件
    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(jakarta.servlet.http.HttpServletRequest request, @RequestBody Map<String, String> body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            
            if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
                return ResponseEntity.status(403).body("您没有权限访问此接口");
            }
            
            String to = body.get("to");
            if (to == null || to.isEmpty()) {
                return ResponseEntity.badRequest().body("收件人邮箱不能为空");
            }
            
            String subject = "邮件服务测试";
            String content = "这是一封测试邮件，说明你的邮件服务配置正确！";
            emailUtils.sendEmail(to, subject, content);
            
            return ResponseEntity.ok("测试邮件发送成功");
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body("测试邮件发送失败: " + e.getMessage());
        }
    }
    
    // 公开接口：获取邮箱功能是否启用
    @GetMapping("/public/email-enabled")
    public ResponseEntity<?> getEmailEnabled() {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            Map<String, Object> result = new HashMap<>();
            result.put("emailEnabled", settings.isEmailEnabled());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // 公开接口：获取所有验证配置
    @GetMapping("/public/security-config")
    public ResponseEntity<?> getSecurityConfig() {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            Map<String, Object> result = new HashMap<>();
            result.put("emailEnabled", settings.isEmailEnabled());
            result.put("captchaRegisterEnabled", settings.isCaptchaRegisterEnabled());
            result.put("captchaForgotPasswordEnabled", settings.isCaptchaForgotPasswordEnabled());
            result.put("captchaLoginEnabled", settings.isCaptchaLoginEnabled());
            result.put("captchaNoiseCount", settings.getCaptchaNoiseCount());
            result.put("captchaLineCount", settings.getCaptchaLineCount());
            result.put("captchaCodeLength", settings.getCaptchaCodeLength());
            result.put("captchaCaseSensitive", settings.isCaptchaCaseSensitive());
            result.put("captchaRotateEnabled", settings.isCaptchaRotateEnabled());
            result.put("articleAuditEnabled", settings.isArticleAuditEnabled());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/public/heartbeat-config")
    public ResponseEntity<?> getHeartbeatConfig() {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            Map<String, Object> result = new HashMap<>();
            result.put("heartbeatEnabled", settings.isHeartbeatEnabled());
            result.put("heartbeatRate", settings.getHeartbeatRate());
            result.put("heartbeatTimeout", settings.getHeartbeatTimeout());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/public/site-config")
    public ResponseEntity<?> getSiteConfig() {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            Map<String, Object> result = new HashMap<>();
            result.put("siteTitle", settings.getSiteTitle());
            result.put("siteIcon", settings.getSiteIcon());
            result.put("siteFavicon", settings.getSiteFavicon());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/public/ai-config")
    public ResponseEntity<?> getAiConfig() {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            Map<String, Object> result = new HashMap<>();
            result.put("aiProvider", settings.getAiProvider());
            result.put("aiReadingEnabled", settings.isAiReadingEnabled());
            result.put("aiArticleAuditEnabled", settings.isAiArticleAuditEnabled());
            result.put("aiCommentAuditEnabled", settings.isAiCommentAuditEnabled());
            result.put("aiArticleAutoAuditEnabled", settings.isAiArticleAutoAuditEnabled());
            result.put("aiCommentAutoAuditEnabled", settings.isAiCommentAutoAuditEnabled());
            // 不返回API密钥等敏感信息
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
