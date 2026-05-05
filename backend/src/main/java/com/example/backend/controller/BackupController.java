package com.example.backend.controller;

import com.example.backend.config.WebsiteSettings;
import com.example.backend.dto.ApiResponse;
import com.example.backend.service.BackupService;
import com.example.backend.service.SettingsService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final BackupService backupService;
    private final SettingsService settingsService;

    public BackupController(BackupService backupService, SettingsService settingsService) {
        this.backupService = backupService;
        this.settingsService = settingsService;
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<?>> listBackups() {
        try {
            return ResponseEntity.ok(ApiResponse.success(backupService.listBackups()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createBackup() {
        try {
            Map<String, String> result = backupService.createBackup();
            if ("error".equals(result.get("status"))) {
                return ResponseEntity.badRequest().body(ApiResponse.error(result.get("message")));
            }
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String filename) {
        try {
            Path backupPath = Paths.get(System.getProperty("user.dir"), "backups", filename);
            if (!Files.exists(backupPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(backupPath);
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/restore/{filename}")
    public ResponseEntity<ApiResponse<?>> restoreBackup(@PathVariable String filename) {
        try {
            Map<String, Object> result = backupService.restoreBackup(filename);
            if ("error".equals(result.get("status"))) {
                return ResponseEntity.badRequest().body(ApiResponse.error((String) result.get("message")));
            }
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<?>> uploadBackup(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("请选择备份文件"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".zip") && !filename.endsWith(".sql"))) {
                return ResponseEntity.badRequest().body(ApiResponse.error("仅支持 .zip 或 .sql 格式的备份文件"));
            }

            Path backupsPath = Paths.get(System.getProperty("user.dir"), "backups");
            Files.createDirectories(backupsPath);
            Path destFile = backupsPath.resolve(filename);
            
            // 如果文件已存在，添加时间戳避免覆盖
            if (Files.exists(destFile)) {
                String baseName = filename.substring(0, filename.lastIndexOf('.'));
                String extension = filename.substring(filename.lastIndexOf('.'));
                String timestamp = String.valueOf(System.currentTimeMillis());
                destFile = backupsPath.resolve(baseName + "_" + timestamp + extension);
            }
            
            file.transferTo(destFile.toFile());
            return ResponseEntity.ok(ApiResponse.success("备份文件上传成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/restore/upload")
    public ResponseEntity<ApiResponse<?>> restoreFromUpload(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("请选择备份文件"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".zip") && !filename.endsWith(".sql"))) {
                return ResponseEntity.badRequest().body(ApiResponse.error("仅支持 .zip 或 .sql 格式的备份文件"));
            }

            Path tempFile = Files.createTempFile("upload_", "_" + filename);
            file.transferTo(tempFile.toFile());

            Map<String, Object> result = backupService.restoreFromUpload(tempFile);
            if ("error".equals(result.get("status"))) {
                return ResponseEntity.badRequest().body(ApiResponse.error((String) result.get("message")));
            }
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<ApiResponse<?>> deleteBackup(@PathVariable String filename) {
        try {
            boolean deleted = backupService.deleteBackup(filename);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("备份已删除"));
            }
            return ResponseEntity.badRequest().body(ApiResponse.error("备份文件不存在"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<?>> getSettings() {
        WebsiteSettings settings = settingsService.getSettings();
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", settings.isBackupEnabled());
        result.put("backupTime", settings.getBackupTime());
        result.put("enableRetention", settings.isBackupRetentionEnabled());
        result.put("retentionCount", settings.getBackupRetentionCount());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<?>> saveSettings(@RequestBody Map<String, Object> request) {
        WebsiteSettings currentSettings = settingsService.getSettings();
        WebsiteSettings newSettings = new WebsiteSettings();
        
        // 复制当前所有设置
        newSettings.setHeartbeatEnabled(currentSettings.isHeartbeatEnabled());
        newSettings.setHeartbeatRate(currentSettings.getHeartbeatRate());
        newSettings.setHeartbeatTimeout(currentSettings.getHeartbeatTimeout());
        newSettings.setEmailEnabled(currentSettings.isEmailEnabled());
        newSettings.setEmailFrom(currentSettings.getEmailFrom());
        newSettings.setVerificationCodeLength(currentSettings.getVerificationCodeLength());
        newSettings.setVerificationCodeExpireMinutes(currentSettings.getVerificationCodeExpireMinutes());
        newSettings.setSmtpHost(currentSettings.getSmtpHost());
        newSettings.setSmtpPort(currentSettings.getSmtpPort());
        newSettings.setSmtpUsername(currentSettings.getSmtpUsername());
        newSettings.setSmtpPassword(currentSettings.getSmtpPassword());
        newSettings.setSmtpSsl(currentSettings.isSmtpSsl());
        newSettings.setCaptchaRegisterEnabled(currentSettings.isCaptchaRegisterEnabled());
        newSettings.setCaptchaForgotPasswordEnabled(currentSettings.isCaptchaForgotPasswordEnabled());
        newSettings.setCaptchaLoginEnabled(currentSettings.isCaptchaLoginEnabled());
        newSettings.setCaptchaNoiseCount(currentSettings.getCaptchaNoiseCount());
        newSettings.setCaptchaLineCount(currentSettings.getCaptchaLineCount());
        newSettings.setCaptchaCodeLength(currentSettings.getCaptchaCodeLength());
        newSettings.setCaptchaCaseSensitive(currentSettings.isCaptchaCaseSensitive());
        newSettings.setCaptchaRotateEnabled(currentSettings.isCaptchaRotateEnabled());
        newSettings.setArticleAuditEnabled(currentSettings.isArticleAuditEnabled());
        newSettings.setCommentAuditEnabled(currentSettings.isCommentAuditEnabled());
        newSettings.setSiteTitle(currentSettings.getSiteTitle());
        newSettings.setSiteIcon(currentSettings.getSiteIcon());
        newSettings.setSiteFavicon(currentSettings.getSiteFavicon());
        newSettings.setAiProvider(currentSettings.getAiProvider());
        newSettings.setAiApiKey(currentSettings.getAiApiKey());
        newSettings.setAiBaseUrl(currentSettings.getAiBaseUrl());
        newSettings.setAiModel(currentSettings.getAiModel());
        newSettings.setAiSystemPrompt(currentSettings.getAiSystemPrompt());
        newSettings.setAiArticleAuditPrompt(currentSettings.getAiArticleAuditPrompt());
        newSettings.setAiCommentAuditPrompt(currentSettings.getAiCommentAuditPrompt());
        newSettings.setAiReadingEnabled(currentSettings.isAiReadingEnabled());
        newSettings.setAiArticleAuditEnabled(currentSettings.isAiArticleAuditEnabled());
        newSettings.setAiCommentAuditEnabled(currentSettings.isAiCommentAuditEnabled());
        newSettings.setAiArticleAutoAuditEnabled(currentSettings.isAiArticleAutoAuditEnabled());
        newSettings.setAiCommentAutoAuditEnabled(currentSettings.isAiCommentAutoAuditEnabled());
        
        // 更新备份设置
        if (request.containsKey("enabled")) {
            newSettings.setBackupEnabled((Boolean) request.get("enabled"));
        } else {
            newSettings.setBackupEnabled(currentSettings.isBackupEnabled());
        }
        if (request.containsKey("backupTime")) {
            newSettings.setBackupTime((String) request.get("backupTime"));
        } else {
            newSettings.setBackupTime(currentSettings.getBackupTime());
        }
        if (request.containsKey("enableRetention")) {
            newSettings.setBackupRetentionEnabled((Boolean) request.get("enableRetention"));
        } else {
            newSettings.setBackupRetentionEnabled(currentSettings.isBackupRetentionEnabled());
        }
        if (request.containsKey("retentionCount")) {
            Object countObj = request.get("retentionCount");
            if (countObj instanceof Integer) {
                newSettings.setBackupRetentionCount((Integer) countObj);
            } else if (countObj instanceof Long) {
                newSettings.setBackupRetentionCount(((Long) countObj).intValue());
            }
        } else {
            newSettings.setBackupRetentionCount(currentSettings.getBackupRetentionCount());
        }
        
        settingsService.updateSettings(newSettings);
        return ResponseEntity.ok(ApiResponse.success("设置已保存"));
    }
}
