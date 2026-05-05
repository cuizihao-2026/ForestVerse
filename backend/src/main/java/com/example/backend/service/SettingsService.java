package com.example.backend.service;

import com.example.backend.config.WebsiteSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class SettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    private WebsiteSettings currentSettings;
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = CONFIG_DIR + "/website-settings.yaml";
    private final ObjectMapper objectMapper;

    public SettingsService() {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        loadSettingsFromFile();
    }

    private void loadSettingsFromFile() {
        try {
            // 优先检查外部配置文件
            File externalFile = new File(CONFIG_FILE);
            if (externalFile.exists()) {
                currentSettings = objectMapper.readValue(externalFile, WebsiteSettings.class);
                return;
            }

            // 外部文件不存在，加载类路径下的默认配置
            ClassPathResource resource = new ClassPathResource(CONFIG_FILE);
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    String content = new String(FileCopyUtils.copyToByteArray(inputStream), StandardCharsets.UTF_8);
                    currentSettings = objectMapper.readValue(content, WebsiteSettings.class);
                    
                    // 复制到外部配置文件，方便后续修改
                    if (!externalFile.getParentFile().exists()) {
                        externalFile.getParentFile().mkdirs();
                    }
                    objectMapper.writeValue(externalFile, currentSettings);
                }
            } else {
                // 默认配置
                currentSettings = createDefaultSettings();
            }
        } catch (Exception e) {
            logger.error("加载配置文件失败: " + e.getMessage());
            currentSettings = createDefaultSettings();
        }
    }

    private WebsiteSettings createDefaultSettings() {
        WebsiteSettings settings = new WebsiteSettings();
        settings.setHeartbeatEnabled(true);
        settings.setHeartbeatRate(30);
        settings.setHeartbeatTimeout(90);
        settings.setEmailEnabled(false);
        settings.setEmailFrom("");
        settings.setVerificationCodeLength(6);
        settings.setVerificationCodeExpireMinutes(10);
        settings.setSmtpHost("");
        settings.setSmtpPort(465);
        settings.setSmtpUsername("");
        settings.setSmtpPassword("");
        settings.setSmtpSsl(true);
        settings.setCaptchaRegisterEnabled(true);
        settings.setCaptchaForgotPasswordEnabled(true);
        settings.setCaptchaLoginEnabled(true);
        settings.setCaptchaNoiseCount(150);
        settings.setCaptchaLineCount(5);
        settings.setCaptchaCodeLength(4);
        settings.setCaptchaCaseSensitive(false);
        settings.setCaptchaRotateEnabled(false);
        settings.setArticleAuditEnabled(false);
        settings.setCommentAuditEnabled(false);
        settings.setSiteTitle("");
        settings.setSiteIcon("");
        settings.setSiteFavicon("");
        settings.setAiProvider("deepseek");
        settings.setAiApiKey("");
        settings.setAiBaseUrl("https://api.deepseek.com");
        settings.setAiModel("deepseek-chat");
        settings.setAiSystemPrompt("");
        settings.setAiArticleAuditPrompt("");
        settings.setAiCommentAuditPrompt("");
        settings.setAiReadingEnabled(false);
        settings.setAiArticleAuditEnabled(false);
        settings.setAiCommentAuditEnabled(false);
        settings.setAiArticleAutoAuditEnabled(false);
        settings.setAiCommentAutoAuditEnabled(false);
        settings.setBackupEnabled(false);
        settings.setBackupTime("02:00");
        settings.setBackupRetentionEnabled(false);
        settings.setBackupRetentionCount(7);
        return settings;
    }

    private void saveSettingsToFile() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            objectMapper.writeValue(configFile, currentSettings);
        } catch (IOException e) {
            logger.error("保存配置文件失败: " + e.getMessage());
        }
    }

    public WebsiteSettings getSettings() {
        return currentSettings;
    }

    public WebsiteSettings updateSettings(WebsiteSettings newSettings) {
        // 更新心跳配置
        if (newSettings.getHeartbeatRate() >= 15 && newSettings.getHeartbeatRate() <= 60) {
            currentSettings.setHeartbeatRate(newSettings.getHeartbeatRate());
        }
        
        int minTimeout = currentSettings.getHeartbeatRate() + 10;
        if (newSettings.getHeartbeatTimeout() >= minTimeout && newSettings.getHeartbeatTimeout() <= 180) {
            currentSettings.setHeartbeatTimeout(newSettings.getHeartbeatTimeout());
        }
        
        currentSettings.setHeartbeatEnabled(newSettings.isHeartbeatEnabled());
        
        // 更新邮箱配置
        currentSettings.setEmailEnabled(newSettings.isEmailEnabled());
        if (newSettings.getEmailFrom() != null && !newSettings.getEmailFrom().isEmpty()) {
            currentSettings.setEmailFrom(newSettings.getEmailFrom());
        }
        if (newSettings.getVerificationCodeLength() >= 4 && newSettings.getVerificationCodeLength() <= 12) {
            currentSettings.setVerificationCodeLength(newSettings.getVerificationCodeLength());
        }
        if (newSettings.getVerificationCodeExpireMinutes() >= 1 && newSettings.getVerificationCodeExpireMinutes() <= 60) {
            currentSettings.setVerificationCodeExpireMinutes(newSettings.getVerificationCodeExpireMinutes());
        }
        
        // 更新 SMTP 配置
        if (newSettings.getSmtpHost() != null && !newSettings.getSmtpHost().isEmpty()) {
            currentSettings.setSmtpHost(newSettings.getSmtpHost());
        }
        if (newSettings.getSmtpPort() > 0) {
            currentSettings.setSmtpPort(newSettings.getSmtpPort());
        }
        if (newSettings.getSmtpUsername() != null) {
            currentSettings.setSmtpUsername(newSettings.getSmtpUsername());
        }
        if (newSettings.getSmtpPassword() != null) {
            currentSettings.setSmtpPassword(newSettings.getSmtpPassword());
        }
        currentSettings.setSmtpSsl(newSettings.isSmtpSsl());
        
        // 更新验证码配置
        currentSettings.setCaptchaRegisterEnabled(newSettings.isCaptchaRegisterEnabled());
        currentSettings.setCaptchaForgotPasswordEnabled(newSettings.isCaptchaForgotPasswordEnabled());
        currentSettings.setCaptchaLoginEnabled(newSettings.isCaptchaLoginEnabled());
        
        // 更新验证码详细配置
        if (newSettings.getCaptchaNoiseCount() >= 0 && newSettings.getCaptchaNoiseCount() <= 500) {
            currentSettings.setCaptchaNoiseCount(newSettings.getCaptchaNoiseCount());
        }
        if (newSettings.getCaptchaLineCount() >= 0 && newSettings.getCaptchaLineCount() <= 20) {
            currentSettings.setCaptchaLineCount(newSettings.getCaptchaLineCount());
        }
        if (newSettings.getCaptchaCodeLength() >= 4 && newSettings.getCaptchaCodeLength() <= 8) {
            currentSettings.setCaptchaCodeLength(newSettings.getCaptchaCodeLength());
        }
        currentSettings.setCaptchaCaseSensitive(newSettings.isCaptchaCaseSensitive());
        currentSettings.setCaptchaRotateEnabled(newSettings.isCaptchaRotateEnabled());
        
        // 更新文章审核配置
        currentSettings.setArticleAuditEnabled(newSettings.isArticleAuditEnabled());
        
        // 更新评论审核配置
        currentSettings.setCommentAuditEnabled(newSettings.isCommentAuditEnabled());
        
        // 更新网站基本设置
        if (newSettings.getSiteTitle() != null) {
            currentSettings.setSiteTitle(newSettings.getSiteTitle());
        }
        if (newSettings.getSiteIcon() != null) {
            currentSettings.setSiteIcon(newSettings.getSiteIcon());
        }
        if (newSettings.getSiteFavicon() != null) {
            currentSettings.setSiteFavicon(newSettings.getSiteFavicon());
        }

        // 更新 AI 设置
        if (newSettings.getAiProvider() != null) {
            currentSettings.setAiProvider(newSettings.getAiProvider());
        }
        if (newSettings.getAiApiKey() != null) {
            currentSettings.setAiApiKey(newSettings.getAiApiKey());
        }
        if (newSettings.getAiBaseUrl() != null && !newSettings.getAiBaseUrl().isEmpty()) {
            currentSettings.setAiBaseUrl(newSettings.getAiBaseUrl());
        }
        if (newSettings.getAiModel() != null) {
        currentSettings.setAiModel(newSettings.getAiModel());
    }
    if (newSettings.getAiSystemPrompt() != null) {
            currentSettings.setAiSystemPrompt(newSettings.getAiSystemPrompt());
        }
        if (newSettings.getAiArticleAuditPrompt() != null) {
            currentSettings.setAiArticleAuditPrompt(newSettings.getAiArticleAuditPrompt());
        }
        if (newSettings.getAiCommentAuditPrompt() != null) {
            currentSettings.setAiCommentAuditPrompt(newSettings.getAiCommentAuditPrompt());
        }

        currentSettings.setAiReadingEnabled(newSettings.isAiReadingEnabled());
        currentSettings.setAiArticleAuditEnabled(newSettings.isAiArticleAuditEnabled());
        currentSettings.setAiCommentAuditEnabled(newSettings.isAiCommentAuditEnabled());
        currentSettings.setAiArticleAutoAuditEnabled(newSettings.isAiArticleAutoAuditEnabled());
        currentSettings.setAiCommentAutoAuditEnabled(newSettings.isAiCommentAutoAuditEnabled());
        
        // 更新备份配置
        currentSettings.setBackupEnabled(newSettings.isBackupEnabled());
        if (newSettings.getBackupTime() != null) {
            currentSettings.setBackupTime(newSettings.getBackupTime());
        }
        currentSettings.setBackupRetentionEnabled(newSettings.isBackupRetentionEnabled());
        currentSettings.setBackupRetentionCount(newSettings.getBackupRetentionCount());

        saveSettingsToFile();
        return currentSettings;
    }
    
    // 获取单个配置项
    public Object getSetting(String key) {
        switch (key) {
            case "heartbeatEnabled":
                return currentSettings.isHeartbeatEnabled();
            case "heartbeatRate":
                return currentSettings.getHeartbeatRate();
            case "heartbeatTimeout":
                return currentSettings.getHeartbeatTimeout();
            case "emailEnabled":
                return currentSettings.isEmailEnabled();
            case "emailFrom":
                return currentSettings.getEmailFrom();
            case "verificationCodeLength":
                return currentSettings.getVerificationCodeLength();
            case "verificationCodeExpireMinutes":
                return currentSettings.getVerificationCodeExpireMinutes();
            case "smtpHost":
                return currentSettings.getSmtpHost();
            case "smtpPort":
                return currentSettings.getSmtpPort();
            case "smtpUsername":
                return currentSettings.getSmtpUsername();
            case "smtpPassword":
                return currentSettings.getSmtpPassword();
            case "smtpSsl":
                return currentSettings.isSmtpSsl();
            case "captchaRegisterEnabled":
                return currentSettings.isCaptchaRegisterEnabled();
            case "captchaForgotPasswordEnabled":
                return currentSettings.isCaptchaForgotPasswordEnabled();
            case "captchaLoginEnabled":
                return currentSettings.isCaptchaLoginEnabled();
            case "captchaNoiseCount":
                return currentSettings.getCaptchaNoiseCount();
            case "captchaLineCount":
                return currentSettings.getCaptchaLineCount();
            case "captchaCodeLength":
                return currentSettings.getCaptchaCodeLength();
            case "captchaCaseSensitive":
                return currentSettings.isCaptchaCaseSensitive();
            case "captchaRotateEnabled":
                return currentSettings.isCaptchaRotateEnabled();
            case "articleAuditEnabled":
                return currentSettings.isArticleAuditEnabled();
            case "commentAuditEnabled":
                return currentSettings.isCommentAuditEnabled();
            case "siteTitle":
                return currentSettings.getSiteTitle();
            case "siteIcon":
                return currentSettings.getSiteIcon();
            case "siteFavicon":
                return currentSettings.getSiteFavicon();
            case "aiProvider":
                return currentSettings.getAiProvider();
            case "aiApiKey":
                return currentSettings.getAiApiKey();
            case "aiBaseUrl":
                return currentSettings.getAiBaseUrl();
            case "aiModel":
                return currentSettings.getAiModel();
            case "aiSystemPrompt":
                return currentSettings.getAiSystemPrompt();
            case "aiArticleAuditPrompt":
                return currentSettings.getAiArticleAuditPrompt();
            case "aiCommentAuditPrompt":
                return currentSettings.getAiCommentAuditPrompt();
            case "aiReadingEnabled":
                return currentSettings.isAiReadingEnabled();
            case "aiArticleAuditEnabled":
                return currentSettings.isAiArticleAuditEnabled();
            case "aiCommentAuditEnabled":
                return currentSettings.isAiCommentAuditEnabled();
            case "aiArticleAutoAuditEnabled":
                return currentSettings.isAiArticleAutoAuditEnabled();
            case "aiCommentAutoAuditEnabled":
                return currentSettings.isAiCommentAutoAuditEnabled();
            case "backupEnabled":
                return currentSettings.isBackupEnabled();
            case "backupTime":
                return currentSettings.getBackupTime();
            case "backupRetentionEnabled":
                return currentSettings.isBackupRetentionEnabled();
            case "backupRetentionCount":
                return currentSettings.getBackupRetentionCount();
            default:
                return null;
        }
    }
    
    // 设置单个配置项
    public void setSetting(String key, Object value) {
        switch (key) {
            case "heartbeatEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setHeartbeatEnabled((Boolean) value);
                }
                break;
            case "heartbeatRate":
                if (value instanceof Integer) {
                    Integer intValue = (Integer) value;
                    if (intValue >= 15 && intValue <= 60) {
                        currentSettings.setHeartbeatRate(intValue);
                        int minTimeout = intValue + 10;
                        if (currentSettings.getHeartbeatTimeout() < minTimeout) {
                            currentSettings.setHeartbeatTimeout(Math.min(minTimeout, 180));
                        }
                    }
                }
                break;
            case "heartbeatTimeout":
                if (value instanceof Integer) {
                    Integer intValue = (Integer) value;
                    int minTimeout = currentSettings.getHeartbeatRate() + 10;
                    if (intValue >= minTimeout && intValue <= 180) {
                        currentSettings.setHeartbeatTimeout(intValue);
                    }
                }
                break;
            case "emailEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setEmailEnabled((Boolean) value);
                }
                break;
            case "emailFrom":
                if (value instanceof String) {
                    currentSettings.setEmailFrom((String) value);
                }
                break;
            case "verificationCodeLength":
                if (value instanceof Integer) {
                    Integer intValue = (Integer) value;
                    if (intValue >= 4 && intValue <= 12) {
                        currentSettings.setVerificationCodeLength(intValue);
                    }
                }
                break;
            case "verificationCodeExpireMinutes":
                if (value instanceof Integer) {
                    Integer intValue = (Integer) value;
                    if (intValue >= 1 && intValue <= 60) {
                        currentSettings.setVerificationCodeExpireMinutes(intValue);
                    }
                }
                break;
            case "smtpHost":
                if (value instanceof String) {
                    currentSettings.setSmtpHost((String) value);
                }
                break;
            case "smtpPort":
                if (value instanceof Integer) {
                    currentSettings.setSmtpPort((Integer) value);
                }
                break;
            case "smtpUsername":
                if (value instanceof String) {
                    currentSettings.setSmtpUsername((String) value);
                }
                break;
            case "smtpPassword":
                if (value instanceof String) {
                    currentSettings.setSmtpPassword((String) value);
                }
                break;
            case "smtpSsl":
                if (value instanceof Boolean) {
                    currentSettings.setSmtpSsl((Boolean) value);
                }
                break;
            case "captchaRegisterEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setCaptchaRegisterEnabled((Boolean) value);
                }
                break;
            case "captchaForgotPasswordEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setCaptchaForgotPasswordEnabled((Boolean) value);
                }
                break;
            case "captchaLoginEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setCaptchaLoginEnabled((Boolean) value);
                }
                break;
            case "captchaNoiseCount":
                if (value instanceof Integer) {
                    Integer intValue = (Integer) value;
                    if (intValue >= 0 && intValue <= 500) {
                        currentSettings.setCaptchaNoiseCount(intValue);
                    }
                }
                break;
            case "captchaLineCount":
                if (value instanceof Integer) {
                    Integer intValue = (Integer) value;
                    if (intValue >= 0 && intValue <= 20) {
                        currentSettings.setCaptchaLineCount(intValue);
                    }
                }
                break;
            case "captchaCodeLength":
                if (value instanceof Integer) {
                    Integer intValue = (Integer) value;
                    if (intValue >= 4 && intValue <= 8) {
                        currentSettings.setCaptchaCodeLength(intValue);
                    }
                }
                break;
            case "captchaCaseSensitive":
                if (value instanceof Boolean) {
                    currentSettings.setCaptchaCaseSensitive((Boolean) value);
                }
                break;
            case "captchaRotateEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setCaptchaRotateEnabled((Boolean) value);
                }
                break;
            case "articleAuditEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setArticleAuditEnabled((Boolean) value);
                }
                break;
            case "commentAuditEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setCommentAuditEnabled((Boolean) value);
                }
                break;
            case "siteTitle":
                if (value instanceof String) {
                    currentSettings.setSiteTitle((String) value);
                }
                break;
            case "siteIcon":
                if (value instanceof String) {
                    currentSettings.setSiteIcon((String) value);
                }
                break;
            case "siteFavicon":
                if (value instanceof String) {
                    currentSettings.setSiteFavicon((String) value);
                }
                break;
            case "aiProvider":
                if (value instanceof String) {
                    currentSettings.setAiProvider((String) value);
                }
                break;
            case "aiApiKey":
                if (value instanceof String) {
                    currentSettings.setAiApiKey((String) value);
                }
                break;
            case "aiBaseUrl":
                if (value instanceof String) {
                    currentSettings.setAiBaseUrl((String) value);
                }
                break;
            case "aiModel":
                if (value instanceof String) {
                    currentSettings.setAiModel((String) value);
                }
                break;
            case "aiSystemPrompt":
                if (value instanceof String) {
                    currentSettings.setAiSystemPrompt((String) value);
                }
                break;
            case "aiArticleAuditPrompt":
                if (value instanceof String) {
                    currentSettings.setAiArticleAuditPrompt((String) value);
                }
                break;
            case "aiCommentAuditPrompt":
                if (value instanceof String) {
                    currentSettings.setAiCommentAuditPrompt((String) value);
                }
                break;
            case "aiReadingEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setAiReadingEnabled((Boolean) value);
                }
                break;
            case "aiArticleAuditEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setAiArticleAuditEnabled((Boolean) value);
                }
                break;
            case "aiCommentAuditEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setAiCommentAuditEnabled((Boolean) value);
                }
                break;
            case "aiArticleAutoAuditEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setAiArticleAutoAuditEnabled((Boolean) value);
                }
                break;
            case "aiCommentAutoAuditEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setAiCommentAutoAuditEnabled((Boolean) value);
                }
                break;
            case "backupEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setBackupEnabled((Boolean) value);
                }
                break;
            case "backupTime":
                if (value instanceof String) {
                    currentSettings.setBackupTime((String) value);
                }
                break;
            case "backupRetentionEnabled":
                if (value instanceof Boolean) {
                    currentSettings.setBackupRetentionEnabled((Boolean) value);
                }
                break;
            case "backupRetentionCount":
                if (value instanceof Integer) {
                    currentSettings.setBackupRetentionCount((Integer) value);
                } else if (value instanceof Long) {
                    currentSettings.setBackupRetentionCount(((Long) value).intValue());
                }
                break;
        }
        saveSettingsToFile();
    }
}
