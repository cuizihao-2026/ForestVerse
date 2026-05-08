package com.example.backend.service.impl;

import com.example.backend.config.WebSocketHandler;
import com.example.backend.config.WebsiteSettings;
import com.example.backend.dto.ArticleDTO;
import com.example.backend.dto.PageResponse;
import com.example.backend.entity.Article;
import com.example.backend.entity.ArticleContent;
import com.example.backend.entity.ArticleLike;
import com.example.backend.entity.User;
import com.example.backend.mapper.ArticleContentMapper;
import com.example.backend.mapper.ArticleLikeMapper;
import com.example.backend.mapper.ArticleMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.ArticleService;
import com.example.backend.service.NotificationService;
import com.example.backend.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleContentMapper articleContentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    @Lazy
    private WebSocketHandler webSocketHandler;

    @Autowired
    private ArticleAuditService articleAuditService;

    @Override
    @Cacheable(value = "article", key = "#id")
    public ArticleDTO getArticleDetail(Long id) {
        return getArticleDetail(id, null, false);
    }

    public ArticleDTO getArticleDetail(Long id, Long currentUserId) {
        return getArticleDetail(id, currentUserId, false);
    }

    public ArticleDTO getArticleDetail(Long id, Long currentUserId, Boolean hasManagePermission) {
        Article article = articleMapper.findById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        // 检查文章状态：只有已发布的文章可以被访问，除非是作者本人或者有管理权限
        if (article.getStatus() != 3) { // 3 表示已发布
            if (currentUserId == null || !currentUserId.equals(article.getUserId())) {
                if (!Boolean.TRUE.equals(hasManagePermission)) {
                    throw new RuntimeException("文章尚未发布或已被驳回");
                }
            }
        }

        ArticleContent content = articleContentMapper.findByArticleId(id);

        ArticleDTO dto = new ArticleDTO();
        dto.setId(article.getId());
        dto.setUserId(article.getUserId());
        dto.setTitle(article.getTitle());
        dto.setCover(article.getCover());
        dto.setStatus(article.getStatus());
        dto.setRejectReason(article.getRejectReason());
        dto.setCategory(article.getCategory());
        dto.setTags(article.getTags());
        dto.setViews(article.getViews());
        dto.setLikes(article.getLikes());
        dto.setComments(article.getComments());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        dto.setPublishedAt(article.getPublishedAt());

        if (content != null) {
            dto.setContent(content.getContent());
        }

        User author = userMapper.findById(article.getUserId());
        if (author != null) {
            dto.setAuthorName(author.getNickname() != null && !author.getNickname().isEmpty()
                ? author.getNickname() : author.getUsername());
            dto.setAuthorAvatar(author.getAvatar());
        }

        if (currentUserId != null) {
            dto.setIsLiked(articleLikeMapper.findByArticleIdAndUserId(id, currentUserId) != null);
        } else {
            dto.setIsLiked(false);
        }

        return dto;
    }

    @Override
    public Article getArticleById(Long id) {
        return articleMapper.findById(id);
    }

    @Override
    public List<Article> getArticlesByUserId(Long userId) {
        return articleMapper.findByUserId(userId);
    }

    @Override
    public List<Article> getArticlesByStatus(Integer status) {
        return articleMapper.findByStatus(status);
    }

    @Override
    public List<Article> getPublishedArticles() {
        return articleMapper.findPublished();
    }

    @Override
    public List<Article> getAllArticles() {
        return articleMapper.findAll();
    }

    @Override
    public List<Article> getAllArticlesWithAuthor() {
        return articleMapper.findAllWithAuthor();
    }

    @Override
    public PageResponse<Article> getAllArticlesWithAuthorPaginated(int page, int size, String keyword, Integer status) {
        int offset = page * size;
        List<Article> articles = articleMapper.findAllWithAuthorPaginated(offset, size, keyword, status);
        long totalElements = articleMapper.countAllWithAuthor(keyword, status);
        return new PageResponse<>(articles, page, size, totalElements);
    }

    @Override
    @Cacheable(value = "articleList", key = "'published:' + #page + ':' + #size")
    public PageResponse<Article> getPublishedArticlesPaginated(int page, int size) {
        int offset = page * size;
        List<Article> articles = articleMapper.findPublishedPaginated(offset, size);
        long totalElements = articleMapper.countPublished();
        return new PageResponse<>(articles, page, size, totalElements);
    }

    @Override
    @Cacheable(value = "articleList", key = "'search:' + #page + ':' + #size + ':' + #keyword")
    public PageResponse<Article> searchPublishedArticlesPaginated(int page, int size, String keyword) {
        int offset = page * size;
        List<Article> articles = articleMapper.searchPublishedPaginated(offset, size, keyword);
        long totalElements = articleMapper.countSearchPublished(keyword);
        return new PageResponse<>(articles, page, size, totalElements);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"article", "articleList"}, allEntries = true)
    public ArticleDTO createArticle(Long userId, ArticleDTO articleDTO) {
        // 验证标题长度
        if (articleDTO.getTitle() == null || articleDTO.getTitle().trim().isEmpty()) {
            throw new RuntimeException("请输入文章标题");
        }
        if (articleDTO.getTitle().length() > 100) {
            throw new RuntimeException("标题长度不能超过100字");
        }

        WebsiteSettings settings = settingsService.getSettings();
        
        Article article = new Article();
        article.setUserId(userId);
        article.setTitle(articleDTO.getTitle());
        article.setCover(articleDTO.getCover() != null ? articleDTO.getCover() : "");
        
        // 判断是否启用文章审核
        if (settings.isArticleAuditEnabled()) {
            // 启用审核时，新文章默认为待审核状态
            article.setStatus(1);
        } else {
            // 未启用审核时，直接发布
            article.setStatus(3);
            article.setPublishedAt(LocalDateTime.now());
        }
        
        article.setCategory(articleDTO.getCategory() != null ? articleDTO.getCategory() : "");
        article.setTags(articleDTO.getTags() != null ? articleDTO.getTags() : "");

        articleMapper.insert(article);

        if (articleDTO.getContent() != null && !articleDTO.getContent().trim().isEmpty()) {
            ArticleContent content = new ArticleContent();
            content.setArticleId(article.getId());
            content.setContent(articleDTO.getContent());
            articleContentMapper.insert(content);
        }

        // 文章全自动审核
        if (settings.isAiArticleAuditEnabled() && settings.isAiArticleAutoAuditEnabled() && article.getStatus() == 1) {
            articleAuditService.asyncAuditArticle(article.getId(), article.getTitle(), articleDTO.getContent());
        }

        // 直接构造 ArticleDTO 而不是调用 getArticleDetail（后者会检查状态导致报错）
        ArticleDTO dto = new ArticleDTO();
        dto.setId(article.getId());
        dto.setUserId(article.getUserId());
        dto.setTitle(article.getTitle());
        dto.setCover(article.getCover());
        dto.setStatus(article.getStatus());
        dto.setCategory(article.getCategory());
        dto.setTags(article.getTags());
        dto.setViews(article.getViews());
        dto.setLikes(article.getLikes());
        dto.setComments(article.getComments());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        dto.setPublishedAt(article.getPublishedAt());
        dto.setContent(articleDTO.getContent());
        
        // 获取作者信息
        User author = userMapper.findById(article.getUserId());
        if (author != null) {
            dto.setAuthorName(author.getNickname() != null && !author.getNickname().isEmpty()
                    ? author.getNickname()
                    : author.getUsername());
            dto.setAuthorAvatar(author.getAvatar());
        }
        
        dto.setIsLiked(false);
        
        return dto;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "article", key = "#articleId"),
        @CacheEvict(value = "articleList", allEntries = true)
    })
    public ArticleDTO updateArticle(Long articleId, Long userId, ArticleDTO articleDTO) {
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        // 验证用户是否是文章作者
        if (!article.getUserId().equals(userId)) {
            throw new RuntimeException("您没有权限编辑这篇文章");
        }

        // 验证标题长度
        if (articleDTO.getTitle() != null) {
            if (articleDTO.getTitle().trim().isEmpty()) {
                throw new RuntimeException("请输入文章标题");
            }
            if (articleDTO.getTitle().length() > 100) {
                throw new RuntimeException("标题长度不能超过100字");
            }
            article.setTitle(articleDTO.getTitle());
        }
        if (articleDTO.getCover() != null) {
            article.setCover(articleDTO.getCover());
        }
        if (articleDTO.getStatus() != null) {
          Integer oldStatus = article.getStatus();
          int newStatus = articleDTO.getStatus();
          
          WebsiteSettings settings = settingsService.getSettings();
          if (settings.isArticleAuditEnabled() && newStatus == 3) {
            newStatus = 1;
          }
          
          article.setStatus(newStatus);
          if (newStatus == 3 && oldStatus != 3) {
            article.setPublishedAt(LocalDateTime.now());
          }
          
          // 如果文章从已发布状态变更为其他状态，广播下架通知
          if (oldStatus == 3 && newStatus != 3) {
            webSocketHandler.broadcastArticleUnpublished(articleId, article.getTitle(), newStatus);
          }
        }
        if (articleDTO.getCategory() != null) {
            article.setCategory(articleDTO.getCategory());
        }
        if (articleDTO.getTags() != null) {
            article.setTags(articleDTO.getTags());
        }

        articleMapper.update(article);

        String content = null;
        if (articleDTO.getContent() != null) {
            ArticleContent articleContent = articleContentMapper.findByArticleId(articleId);
            if (articleContent == null) {
                articleContent = new ArticleContent();
                articleContent.setArticleId(articleId);
                articleContent.setContent(articleDTO.getContent());
                articleContentMapper.insert(articleContent);
            } else {
                articleContent.setContent(articleDTO.getContent());
                articleContentMapper.update(articleContent);
            }
            content = articleDTO.getContent();
        } else {
            ArticleContent articleContent = articleContentMapper.findByArticleId(articleId);
            if (articleContent != null) {
                content = articleContent.getContent();
            }
        }

        // 文章全自动审核 - 更新文章时如果状态变为待审核，也触发
        WebsiteSettings settings = settingsService.getSettings();
        if (settings.isAiArticleAuditEnabled() && settings.isAiArticleAutoAuditEnabled() && article.getStatus() == 1) {
            articleAuditService.asyncAuditArticle(articleId, article.getTitle(), content);
        }

        return getArticleDetail(articleId, userId, false);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"article", "articleList"}, allEntries = true)
    public void deleteArticle(Long articleId) {
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        
        Long userId = article.getUserId();
        String articleTitle = article.getTitle();
        
        // 如果文章已发布，广播下架通知
        if (article.getStatus() == 3) {
            webSocketHandler.broadcastArticleUnpublished(articleId, articleTitle, 0);
        }
        
        articleContentMapper.deleteByArticleId(articleId);
        articleMapper.softDelete(articleId);
        
        // 发送通知给原作者
        notificationService.sendNotification(
            userId,
            "warning",
            "文章已删除",
            "您的文章《" + articleTitle + "》已被管理员删除"
        );
    }

    @Override
    public void incrementViews(Long articleId) {
        articleMapper.incrementViews(articleId);
    }

    @Override
  @Transactional
  public void likeArticle(Long articleId, Long userId) {
    Article article = articleMapper.findById(articleId);
    if (article == null) {
      throw new RuntimeException("文章不存在");
    }
    // 只能点赞已发布的文章
    if (article.getStatus() != 3) {
      throw new RuntimeException("只能点赞已发布的文章");
    }
    ArticleLike existing = articleLikeMapper.findByArticleIdAndUserId(articleId, userId);
    if (existing != null) {
      throw new RuntimeException("已经点过赞了");
    }
    ArticleLike like = new ArticleLike();
    like.setArticleId(articleId);
    like.setUserId(userId);
    articleLikeMapper.insert(like);
    // 更新文章表的点赞数
    articleMapper.incrementLikes(articleId);
  }

    @Override
    @Transactional
    public void unlikeArticle(Long articleId, Long userId) {
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        int rows = articleLikeMapper.deleteByArticleIdAndUserId(articleId, userId);
        if (rows == 0) {
            throw new RuntimeException("尚未点赞");
        }
        // 更新文章表的点赞数
        articleMapper.decrementLikes(articleId);
    }

    @Override
    public boolean isLiked(Long articleId, Long userId) {
        return articleLikeMapper.findByArticleIdAndUserId(articleId, userId) != null;
    }

    @Override
    public void incrementComments(Long articleId) {
        articleMapper.incrementComments(articleId);
    }

    @Override
    public void decrementComments(Long articleId) {
        articleMapper.decrementComments(articleId);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "article", key = "#articleId"),
        @CacheEvict(value = "articleList", allEntries = true)
    })
    public void updateStatus(Long articleId, Integer status, String rejectReason) {
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        Integer oldStatus = article.getStatus();
        article.setStatus(status);
        article.setRejectReason(rejectReason);

        if (status == 3 && oldStatus != 3) {
            article.setPublishedAt(LocalDateTime.now());
        }

        articleMapper.update(article);

        // 发送通知
        if (oldStatus == 1) { // 从待审核状态变更
            if (status == 3) { // 审核通过
                notificationService.sendNotification(
                    article.getUserId(),
                    "success",
                    "文章审核通过",
                    "恭喜！您的文章《" + article.getTitle() + "》已通过审核并成功发布"
                );
                // 广播文章审核通过通知给所有在线用户
                webSocketHandler.broadcastArticleApproved(articleId);
            } else if (status == 2) { // 审核驳回
                String message = "您的文章《" + article.getTitle() + "》审核未通过";
                if (rejectReason != null && !rejectReason.trim().isEmpty()) {
                    message += "，驳回原因：" + rejectReason;
                }
                notificationService.sendNotification(
                    article.getUserId(),
                    "warning",
                    "文章审核驳回",
                    message
                );
            }
        }

        // 如果文章从已发布状态变更为其他状态，广播下架通知
        if (oldStatus == 3 && status != 3) {
            webSocketHandler.broadcastArticleUnpublished(articleId, article.getTitle(), status);
        }
    }

    @Override
    public List<Article> getLikedArticles(Long userId) {
        return articleLikeMapper.findLikedPublishedArticlesByUserId(userId);
    }

    @Override
    public List<Article> searchPublishedArticles(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return articleMapper.findPublished();
        }
        return articleMapper.searchPublished(keyword.trim());
    }
}
