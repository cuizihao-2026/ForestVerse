package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.entity.Article;
import com.example.backend.entity.ArticleContent;
import com.example.backend.entity.Comment;
import com.example.backend.entity.User;
import com.example.backend.mapper.ArticleContentMapper;
import com.example.backend.mapper.ArticleMapper;
import com.example.backend.mapper.CommentMapper;
import com.example.backend.service.AIService;
import com.example.backend.service.ArticleService;
import com.example.backend.service.CommentService;
import com.example.backend.service.RolePermissionService;
import com.example.backend.service.UserService;
import com.example.backend.service.impl.AIServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = {"http://localhost:8020", "http://127.0.0.1:8020"})
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Autowired
    private AIService aiService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleContentMapper articleContentMapper;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RolePermissionService rolePermissionService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Map<String, String>>> chat(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("请先登录"));
            }
            
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "ai:assist")) {
                return ResponseEntity.status(403).body(ApiResponse.error("权限不足"));
            }

            String userMessage = (String) requestBody.get("message");
            String articleContent = (String) requestBody.get("articleContent");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> history = (List<Map<String, String>>) requestBody.get("history");

            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("消息不能为空"));
            }

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> 
                aiService.chat(userMessage, articleContent, history), 
                VIRTUAL_THREAD_EXECUTOR
            );
            
            String aiResponse = future.get();

            Map<String, String> data = new HashMap<>();
            data.put("message", aiResponse);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("请求失败: " + e.getMessage()));
        }
    }

    @PostMapping("/audit/{articleId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> auditArticle(@PathVariable Long articleId, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("请先登录"));
            }
            
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "review:manage")) {
                return ResponseEntity.status(403).body(ApiResponse.error("权限不足"));
            }

            Article article = articleMapper.findById(articleId);
            if (article == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("文章不存在"));
            }

            ArticleContent content = articleContentMapper.findByArticleId(articleId);
            String articleContent = content != null ? content.getContent() : "";

            if (aiService instanceof AIServiceImpl) {
                AIServiceImpl.AuditResult result = ((AIServiceImpl) aiService).auditArticle(article.getTitle(), articleContent);
                
                Map<String, Object> data = new HashMap<>();
                data.put("approved", result.approved);
                data.put("reason", result.reason);
                
                if (result.approved) {
                    articleService.updateStatus(articleId, 3, null);
                    data.put("action", "文章已自动发布");
                } else {
                    articleService.updateStatus(articleId, 2, result.reason);
                    data.put("action", "文章已自动拒绝");
                }
                
                return ResponseEntity.ok(ApiResponse.success(data));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("AI服务实现错误"));
            }
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("审核失败: " + e.getMessage()));
        }
    }

    @PostMapping("/audit/comment/{commentId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> auditComment(@PathVariable Long commentId, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("请先登录"));
            }
            
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "review:manage")) {
                return ResponseEntity.status(403).body(ApiResponse.error("权限不足"));
            }

            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("评论不存在"));
            }

            if (aiService instanceof AIServiceImpl) {
                AIServiceImpl.AuditResult result = ((AIServiceImpl) aiService).auditComment(comment.getContent());
                
                Map<String, Object> data = new HashMap<>();
                data.put("approved", result.approved);
                data.put("reason", result.reason);
                
                if (result.approved) {
                    commentService.approveComment(commentId);
                    data.put("action", "评论已自动通过");
                } else {
                    commentService.rejectComment(commentId, result.reason);
                    data.put("action", "评论已自动拒绝");
                }
                
                return ResponseEntity.ok(ApiResponse.success(data));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("AI服务实现错误"));
            }
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("审核失败: " + e.getMessage()));
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queryBalance(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("请先登录"));
            }
            
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "ai:assist")) {
                return ResponseEntity.status(403).body(ApiResponse.error("权限不足"));
            }

            Map<String, Object> result = aiService.queryBalance();
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("查询余额失败: " + e.getMessage()));
        }
    }
    
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        CompletableFuture.runAsync(() -> {
            try {
                Long userId = (Long) request.getAttribute("userId");
                if (userId == null) {
                    emitter.send(SseEmitter.event().data("请先登录"));
                    emitter.complete();
                    return;
                }
                
                User currentUser = userService.findById(userId);
                if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "ai:assist")) {
                    emitter.send(SseEmitter.event().data("权限不足"));
                    emitter.complete();
                    return;
                }

                String userMessage = (String) requestBody.get("message");
                String articleContent = (String) requestBody.get("articleContent");
                @SuppressWarnings("unchecked")
                List<Map<String, String>> history = (List<Map<String, String>>) requestBody.get("history");

                if (userMessage == null || userMessage.trim().isEmpty()) {
                    emitter.send(SseEmitter.event().data("消息不能为空"));
                    emitter.complete();
                    return;
                }

                aiService.streamChat(userMessage, articleContent, history, new AIService.StreamResponseHandler() {
                    @Override
                    public void onChunk(String chunk) {
                        try {
                            emitter.send(SseEmitter.event().data(chunk));
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onComplete() {
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        try {
                            emitter.send(SseEmitter.event().data("请求失败: " + error.getMessage()));
                        } catch (Exception ignored) {}
                        emitter.complete();
                    }
                });

            } catch (Exception e) {
                logger.error("操作失败: " + e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().data("请求失败: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        }, VIRTUAL_THREAD_EXECUTOR);

        return emitter;
    }
}
