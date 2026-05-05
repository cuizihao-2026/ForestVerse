package com.example.backend.scheduler;

import com.example.backend.config.WebsiteSettings;
import com.example.backend.service.BackupService;
import com.example.backend.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class BackupScheduler implements SchedulingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(BackupScheduler.class);

    @Autowired
    private BackupService backupService;

    @Autowired
    private SettingsService settingsService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                this::checkAndBackup,
                triggerContext -> {
                    WebsiteSettings settings = settingsService.getSettings();
                    
                    if (!settings.isBackupEnabled()) {
                        return null; // 备份关闭，不执行
                    }
                    
                    String backupTime = settings.getBackupTime();
                    String cron = convertTimeToCron(backupTime);
                    
                    return new CronTrigger(cron).nextExecution(triggerContext);
                }
        );
    }

    private String convertTimeToCron(String timeStr) {
        LocalTime time = LocalTime.parse(timeStr);
        return String.format("0 %d %d * * ?", time.getMinute(), time.getHour());
    }

    @Async("scheduledTaskExecutor")
    public void checkAndBackup() {
        WebsiteSettings settings = settingsService.getSettings();
        
        try {
            logger.info("执行自动备份任务...");
            backupService.createBackup();
            
            if (settings.isBackupRetentionEnabled()) {
                backupService.cleanOldBackups(settings.getBackupRetentionCount());
            }
        } catch (Exception e) {
            logger.error("自动备份失败", e);
        }
    }
}
