package com.example.backend.service;

import com.example.backend.dto.PageResponse;
import com.example.backend.entity.Comment;

import java.util.List;

public interface CommentService {
    
    List<Comment> getCommentsByArticleId(Long articleId);
    
    List<Comment> getApprovedCommentsByArticleId(Long articleId);
    
    List<Comment> getCommentsByArticleIdForUser(Long articleId, Long userId);
    
    Comment getCommentById(Long id);
    
    Comment createComment(Long articleId, Long userId, String content, Long parentId, Long replyToUserId);
    
    Comment updateComment(Long commentId, String content);
    
    void deleteComment(Long commentId);
    
    void approveComment(Long commentId);
    
    void rejectComment(Long commentId, String rejectReason);
    
    int getCommentCountByArticleId(Long articleId);
    
    int getApprovedCommentCountByArticleId(Long articleId);
    
    List<Comment> getPendingComments();
    
    List<Comment> getAllComments();

    PageResponse<Comment> getAllCommentsPaginated(int page, int size, String keyword, Integer status);
}
