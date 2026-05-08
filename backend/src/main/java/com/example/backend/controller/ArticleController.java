package com.example.backend.controller;

import com.example.backend.dto.ArticleDTO;
import com.example.backend.dto.PageResponse;
import com.example.backend.entity.Article;
import com.example.backend.entity.User;
import com.example.backend.service.ArticleService;
import com.example.backend.service.RolePermissionService;
import com.example.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/article")
public class ArticleController {

    private static final Logger logger = LoggerFactory.getLogger(ArticleController.class);

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserService userService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @GetMapping("/list")
    public ResponseEntity<?> getArticles(@RequestParam(required = false) Integer status, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "review:manage")) {
                return ResponseEntity.status(403).body("权限不足");
            }
            List<Article> articles;
            if (status != null) {
                articles = articleService.getArticlesByStatus(status);
            } else {
                articles = articleService.getAllArticles();
            }
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllArticles(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("当前用户不存在");
            }

            // 检查是否有 article:manage 权限
            if (!rolePermissionService.hasPermission(currentUser.getRole(), "article:manage")) {
                return ResponseEntity.status(403).body("需要管理员权限");
            }

            List<Article> articles = articleService.getAllArticlesWithAuthor();
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all/paginated")
    public ResponseEntity<?> getAllArticlesPaginated(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("当前用户不存在");
            }

            // 检查是否有 article:manage 权限
            if (!rolePermissionService.hasPermission(currentUser.getRole(), "article:manage")) {
                return ResponseEntity.status(403).body("需要管理员权限");
            }

            Integer statusValue = null;
            if (status != null && !status.isEmpty()) {
                try {
                    statusValue = Integer.parseInt(status);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            PageResponse<Article> articles = articleService.getAllArticlesWithAuthorPaginated(page, size, keyword, statusValue);
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/published")
    public ResponseEntity<?> getPublishedArticles() {
        try {
            List<Article> articles = articleService.getPublishedArticles();
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/published/paginated")
    public ResponseEntity<?> getPublishedArticlesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            PageResponse<Article> articles = articleService.getPublishedArticlesPaginated(page, size);
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchArticles(@RequestParam(required = false) String keyword) {
        try {
            List<Article> articles = articleService.searchPublishedArticles(keyword);
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search/paginated")
    public ResponseEntity<?> searchArticlesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword) {
        try {
            PageResponse<Article> articles = articleService.searchPublishedArticlesPaginated(page, size, keyword);
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyArticles(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "article:create")) {
                return ResponseEntity.status(403).body("权限不足");
            }
            List<Article> articles = articleService.getArticlesByUserId(userId);
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getArticleDetail(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Boolean hasArticleManagePermission = false;
            if (userId != null) {
                User currentUser = userService.findById(userId);
                if (currentUser != null) {
                    hasArticleManagePermission = rolePermissionService.hasPermission(currentUser.getRole(), "article:manage");
                }
            }
            ArticleDTO article = articleService.getArticleDetail(id, userId, hasArticleManagePermission);
            articleService.incrementViews(id);
            return ResponseEntity.ok(article);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createArticle(HttpServletRequest request, @RequestBody ArticleDTO articleDTO) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body("请先登录");
            }
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "article:create")) {
                return ResponseEntity.status(403).body("权限不足");
            }
            ArticleDTO article = articleService.createArticle(userId, articleDTO);
            return ResponseEntity.ok(article);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateArticle(@PathVariable Long id, @RequestBody ArticleDTO articleDTO, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null) {
                return ResponseEntity.status(401).body("请先登录");
            }
            // 检查是否有 article:create 权限
            if (!rolePermissionService.hasPermission(currentUser.getRole(), "article:create")) {
                return ResponseEntity.status(403).body("权限不足");
            }
            // 检查是否有 article:manage 权限或者是文章作者
            Article article = articleService.getArticleById(id);
            if (article == null) {
                return ResponseEntity.badRequest().body("文章不存在");
            }
            boolean hasManagePermission = rolePermissionService.hasPermission(currentUser.getRole(), "article:manage");
            if (!hasManagePermission && !article.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body("只能编辑自己的文章");
            }
            ArticleDTO updatedArticle = articleService.updateArticle(id, userId, articleDTO);
            return ResponseEntity.ok(updatedArticle);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "review:manage")) {
                return ResponseEntity.status(403).body("权限不足");
            }
            articleService.updateStatus(id, request.getStatus(), request.getRejectReason());
            return ResponseEntity.ok("状态更新成功");
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeArticle(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body("请先登录");
            }
            articleService.likeArticle(id, userId);
            return ResponseEntity.ok("点赞成功");
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<?> unlikeArticle(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body("请先登录");
            }
            articleService.unlikeArticle(id, userId);
            return ResponseEntity.ok("取消点赞成功");
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my/liked")
    public ResponseEntity<?> getMyLikedArticles(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<Article> articles = articleService.getLikedArticles(userId);
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArticle(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("当前用户不存在");
            }

            // 检查是否有 article:create 权限
            if (!rolePermissionService.hasPermission(currentUser.getRole(), "article:create")) {
                return ResponseEntity.status(403).body("权限不足");
            }

            Article article = articleService.getArticleById(id);
            if (article == null) {
                return ResponseEntity.badRequest().body("文章不存在");
            }

            // 检查是否有 article:manage 权限或者是文章作者
            boolean hasManagePermission = rolePermissionService.hasPermission(currentUser.getRole(), "article:manage");
            if (!hasManagePermission && !article.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body("只能删除自己的文章");
            }

            articleService.deleteArticle(id);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    static class StatusUpdateRequest {
        private Integer status;
        private String rejectReason;

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getRejectReason() {
            return rejectReason;
        }

        public void setRejectReason(String rejectReason) {
            this.rejectReason = rejectReason;
        }
    }
}
