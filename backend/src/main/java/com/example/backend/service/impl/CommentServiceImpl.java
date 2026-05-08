package com.example.backend.service.impl;

import com.example.backend.config.WebSocketHandler;
import com.example.backend.dto.PageResponse;
import com.example.backend.entity.Comment;
import com.example.backend.mapper.CommentMapper;
import com.example.backend.mapper.ArticleMapper;
import com.example.backend.service.CommentService;
import com.example.backend.service.NotificationService;
import com.example.backend.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    @Lazy
    private WebSocketHandler webSocketHandler;

    @Autowired
    private CommentAuditService commentAuditService;

    @Override
    public List<Comment> getCommentsByArticleId(Long articleId) {
        return commentMapper.findByArticleId(articleId);
    }

    @Override
    public List<Comment> getApprovedCommentsByArticleId(Long articleId) {
        return commentMapper.findByArticleIdAndStatus(articleId, 2);
    }

    @Override
    public List<Comment> getCommentsByArticleIdForUser(Long articleId, Long userId) {
        if (userId == null) {
            return getApprovedCommentsByArticleId(articleId);
        }
        return commentMapper.findByArticleIdForUser(articleId, userId);
    }

    @Override
    public Comment getCommentById(Long id) {
        return commentMapper.findById(id);
    }

    @Override
    public Comment createComment(Long articleId, Long userId, String content, Long parentId, Long replyToUserId) {
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        
        // 根据设置决定初始状态
        var settings = settingsService.getSettings();
        if (settings.isCommentAuditEnabled()) {
            comment.setStatus(1); // 待审核
        } else {
            comment.setStatus(2); // 直接通过
        }

        commentMapper.insert(comment);
        
        // 只有不需要审核时才立即增加评论数
        if (!settings.isCommentAuditEnabled()) {
            articleMapper.incrementComments(articleId);
            sendCommentNotification(comment, articleId);
            webSocketHandler.broadcastCommentApproved(articleId, comment.getId());
        }

        // 评论全自动审核
        if (settings.isAiCommentAuditEnabled() && settings.isAiCommentAutoAuditEnabled() && comment.getStatus() == 1) {
            commentAuditService.asyncAuditComment(comment.getId(), content);
        }
        
        return commentMapper.findById(comment.getId());
    }

    @Override
    public Comment updateComment(Long commentId, String content) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        comment.setContent(content);
        commentMapper.update(comment);

        // 评论全自动审核 - 更新评论时如果状态是待审核，也触发
        var settings = settingsService.getSettings();
        if (settings.isAiCommentAuditEnabled() && settings.isAiCommentAutoAuditEnabled() && comment.getStatus() == 1) {
            commentAuditService.asyncAuditComment(commentId, content);
        }

        return commentMapper.findById(commentId);
    }

    @Override
    public void deleteComment(Long commentId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        commentMapper.deleteById(commentId);
        articleMapper.decrementComments(comment.getArticleId());
    }

    @Override
    public void approveComment(Long commentId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        comment.setStatus(2);
        commentMapper.update(comment);
        
        // 审核通过时才增加评论数
        articleMapper.incrementComments(comment.getArticleId());
        
        sendCommentNotification(comment, comment.getArticleId());
        
        // 广播评论审核通过通知给所有在线用户
        webSocketHandler.broadcastCommentApproved(comment.getArticleId(), commentId);
    }

    @Override
    public void rejectComment(Long commentId, String rejectReason) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        comment.setStatus(3);
        comment.setRejectReason(rejectReason);
        commentMapper.update(comment);
        
        // 发送驳回通知
        String message = "您的评论审核未通过";
        if (rejectReason != null && !rejectReason.trim().isEmpty()) {
            message += "，驳回原因：" + rejectReason;
        }
        notificationService.sendNotification(comment.getUserId(), "error", "评论审核未通过", message);
        
        // 给评论发布者发送WebSocket消息，让他刷新
        webSocketHandler.sendCommentRejected(comment.getUserId(), comment.getArticleId(), commentId);
    }

    @Override
    public int getCommentCountByArticleId(Long articleId) {
        return commentMapper.countByArticleId(articleId);
    }

    @Override
    public int getApprovedCommentCountByArticleId(Long articleId) {
        return commentMapper.countByArticleIdAndStatus(articleId, 2);
    }

    @Override
    public List<Comment> getPendingComments() {
        return commentMapper.findByStatus(1);
    }

    @Override
    public List<Comment> getAllComments() {
        return commentMapper.findAll();
    }

    @Override
    public PageResponse<Comment> getAllCommentsPaginated(int page, int size, String keyword, Integer status) {
        int offset = page * size;
        List<Comment> comments = commentMapper.findAllPaginated(offset, size, keyword, status);
        long totalElements = commentMapper.countAll(keyword, status);
        return new PageResponse<>(comments, page, size, totalElements);
    }

    private void sendCommentNotification(Comment comment, Long articleId) {
        var article = articleMapper.findById(articleId);
        if (article != null && !article.getUserId().equals(comment.getUserId())) {
            String message = "您的文章《" + article.getTitle() + "》收到了新评论";
            notificationService.sendNotification(article.getUserId(), "info", "新评论", message);
        }

        if (comment.getReplyToUserId() != null && !comment.getReplyToUserId().equals(comment.getUserId())) {
            notificationService.sendNotification(
                comment.getReplyToUserId(), 
                "info", 
                "收到回复", 
                "有人回复了您的评论"
            );
        }
    }
}
