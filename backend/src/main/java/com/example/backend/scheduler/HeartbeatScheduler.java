package com.example.backend.scheduler;

import com.example.backend.config.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatScheduler.class);

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Async("scheduledTaskExecutor")
    @Scheduled(fixedDelay = 5000) // 每5秒执行一次
    public void checkHeartbeats() {
        try {
            sessionManager.checkHeartbeats();
        } catch (Exception e) {
            logger.error("心跳检查任务执行失败", e);
        }
    }

    @Async("scheduledTaskExecutor")
    @Scheduled(fixedDelay = 30 * 60 * 1000) // 每30分钟执行一次
    public void checkTokenAndRefresh() {
        try {
            sessionManager.checkTokenAndRefresh();
        } catch (Exception e) {
            logger.error("Token检查和刷新任务执行失败", e);
        }
    }
}
