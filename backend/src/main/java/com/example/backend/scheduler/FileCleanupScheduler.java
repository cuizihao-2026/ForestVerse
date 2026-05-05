package com.example.backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Component
public class FileCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(FileCleanupScheduler.class);

    private static final String CHAT_IMAGES_DIR = "uploads/chat-images";

    // 保留天数：7天
    private static final long RETENTION_DAYS = 7;

    @Async("scheduledTaskExecutor")
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupOldFiles() {
        logger.info("开始执行聊天图片清理任务...");
        try {
            String projectRoot = System.getProperty("user.dir");
            Path chatImagesPath = Paths.get(projectRoot, CHAT_IMAGES_DIR);

            if (!Files.exists(chatImagesPath)) {
                logger.info("聊天图片目录不存在，跳过清理任务");
                return;
            }

            AtomicLong totalSize = new AtomicLong(0);
            Instant cutoffTime = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);

            long deletedCount;
            try (Stream<Path> paths = Files.walk(chatImagesPath)) {
                deletedCount = paths.filter(Files::isRegularFile)
                        .filter(path -> {
                            try {
                                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                                Instant lastModified = attrs.lastModifiedTime().toInstant();
                                
                                // 只清理超过7天的聊天图片
                                return lastModified.isBefore(cutoffTime);
                            } catch (IOException e) {
                                logger.error("读取文件属性失败: " + path, e);
                                return false;
                            }
                        })
                        .mapToLong(path -> {
                            try {
                                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                                long size = attrs.size();
                                Files.delete(path);
                                logger.info("已删除聊天图片: " + path);
                                totalSize.addAndGet(size);
                                return 1;
                            } catch (IOException e) {
                                logger.error("删除文件失败: " + path, e);
                                return 0;
                            }
                        })
                        .sum();
            }

            logger.info("聊天图片清理任务完成！共删除 {} 个文件，释放空间: {:.2f} MB", 
                    deletedCount, totalSize.get() / (1024.0 * 1024.0));

        } catch (Exception e) {
            logger.error("聊天图片清理任务执行失败", e);
        }
    }
}
