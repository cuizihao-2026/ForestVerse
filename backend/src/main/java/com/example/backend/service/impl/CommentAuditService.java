package com.example.backend.service.impl;

import com.example.backend.service.AIService;
import com.example.backend.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CommentAuditService {

    private static final Logger logger = LoggerFactory.getLogger(CommentAuditService.class);

    @Autowired
    private AIService aiService;

    @Autowired
    @Lazy
    private CommentService commentService;

    @Async
    public void asyncAuditComment(Long commentId, String content) {
        try {
            if (aiService instanceof AIServiceImpl) {
                AIServiceImpl.AuditResult result = ((AIServiceImpl) aiService).auditComment(content);

                if (result.approved) {
                    commentService.approveComment(commentId);
                } else {
                    commentService.rejectComment(commentId, result.reason);
                }
            }
        } catch (Exception e) {
            logger.error("AI自动评论审核失败: {}", e.getMessage(), e);
        }
    }
}
