package com.example.backend.controller;

import com.example.backend.dto.PageResponse;
import com.example.backend.entity.Comment;
import com.example.backend.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    @Autowired
    private CommentService commentService;
    
    @Autowired
    private com.example.backend.service.UserService userService;
    
    @Autowired
    private com.example.backend.service.RolePermissionService rolePermissionService;

    @GetMapping("/article/{articleId}")
    public ResponseEntity<List<Comment>> getCommentsByArticle(@PathVariable Long articleId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(commentService.getCommentsByArticleIdForUser(articleId, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comment> getComment(@PathVariable Long id) {
        Comment comment = commentService.getCommentById(id);
        if (comment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(comment);
    }

    @PostMapping
    public ResponseEntity<Comment> createComment(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }
            
            // 检查评论权限
            com.example.backend.entity.User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }
            
            Set<String> permissions = rolePermissionService.getPermissionsByRoleName(user.getRole());
            if (!permissions.contains("comment:use")) {
                return ResponseEntity.status(403).build();
            }
            
            Long articleId = Long.valueOf(payload.get("articleId").toString());
            String content = (String) payload.get("content");

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (content.length() > 300) {
                return ResponseEntity.badRequest().build();
            }

            Long parentId = payload.get("parentId") != null ? Long.valueOf(payload.get("parentId").toString()) : null;
            Long replyToUserId = payload.get("replyToUserId") != null ? Long.valueOf(payload.get("replyToUserId").toString()) : null;

            Comment comment = commentService.createComment(articleId, userId, content, parentId, replyToUserId);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }
            
            Comment comment = commentService.getCommentById(id);
            if (comment == null || !comment.getUserId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }

            String content = payload.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (content.length() > 300) {
                return ResponseEntity.badRequest().build();
            }

            Comment updatedComment = commentService.updateComment(id, content);
            return ResponseEntity.ok(updatedComment);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }
            
            Comment comment = commentService.getCommentById(id);
            if (comment == null) {
                return ResponseEntity.notFound().build();
            }
            
            Boolean isAdmin = (Boolean) request.getAttribute("isAdmin");
            if (!comment.getUserId().equals(userId) && !Boolean.TRUE.equals(isAdmin)) {
                return ResponseEntity.status(403).build();
            }
            
            commentService.deleteComment(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveComment(@PathVariable Long id, HttpServletRequest request) {
        try {
            Boolean isAdmin = (Boolean) request.getAttribute("isAdmin");
            if (!Boolean.TRUE.equals(isAdmin)) {
                return ResponseEntity.status(403).build();
            }
            
            commentService.approveComment(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectComment(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            Boolean isAdmin = (Boolean) request.getAttribute("isAdmin");
            if (!Boolean.TRUE.equals(isAdmin)) {
                return ResponseEntity.status(403).build();
            }
            
            commentService.rejectComment(id, payload.get("reason"));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<List<Comment>> getPendingComments(HttpServletRequest request) {
        try {
            Boolean isAdmin = (Boolean) request.getAttribute("isAdmin");
            if (!Boolean.TRUE.equals(isAdmin)) {
                return ResponseEntity.status(403).build();
            }
            
            return ResponseEntity.ok(commentService.getPendingComments());
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Comment>> getAllComments(HttpServletRequest request) {
        try {
            Boolean isAdmin = (Boolean) request.getAttribute("isAdmin");
            if (!Boolean.TRUE.equals(isAdmin)) {
                return ResponseEntity.status(403).build();
            }
            
            return ResponseEntity.ok(commentService.getAllComments());
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/all/paginated")
    public ResponseEntity<?> getAllCommentsPaginated(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            Boolean isAdmin = (Boolean) request.getAttribute("isAdmin");
            if (!Boolean.TRUE.equals(isAdmin)) {
                return ResponseEntity.status(403).build();
            }
            Integer statusValue = null;
            if (status != null && !status.isEmpty()) {
                try {
                    statusValue = Integer.parseInt(status);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            PageResponse<Comment> comments = commentService.getAllCommentsPaginated(page, size, keyword, statusValue);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<Comment>> getAllCommentsAdmin(HttpServletRequest request) {
        return getAllComments(request);
    }

    @GetMapping("/article/{articleId}/count")
    public ResponseEntity<Integer> getCommentCount(@PathVariable Long articleId) {
        return ResponseEntity.ok(commentService.getApprovedCommentCountByArticleId(articleId));
    }
}
