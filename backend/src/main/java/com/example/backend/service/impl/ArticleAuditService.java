package com.example.backend.service.impl;

import com.example.backend.service.AIService;
import com.example.backend.service.ArticleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ArticleAuditService {

    private static final Logger logger = LoggerFactory.getLogger(ArticleAuditService.class);

    @Autowired
    private AIService aiService;

    @Autowired
    @Lazy
    private ArticleService articleService;

    @Async
    public void asyncAuditArticle(Long articleId, String title, String content) {
        try {
            if (aiService instanceof AIServiceImpl) {
                AIServiceImpl.AuditResult result = ((AIServiceImpl) aiService).auditArticle(title, content);

                if (result.approved) {
                    articleService.updateStatus(articleId, 3, null);
                } else {
                    articleService.updateStatus(articleId, 2, result.reason);
                }
            }
        } catch (Exception e) {
            logger.error("AI自动文章审核失败: {}", e.getMessage(), e);
        }
    }
}
