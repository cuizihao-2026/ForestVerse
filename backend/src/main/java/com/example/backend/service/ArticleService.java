package com.example.backend.service;

import com.example.backend.dto.ArticleDTO;
import com.example.backend.dto.PageResponse;
import com.example.backend.entity.Article;

import java.util.List;

public interface ArticleService {
    
    ArticleDTO getArticleDetail(Long id);
    ArticleDTO getArticleDetail(Long id, Long currentUserId);
    ArticleDTO getArticleDetail(Long id, Long currentUserId, Boolean hasManagePermission);
    
    Article getArticleById(Long id);
    
    List<Article> getArticlesByUserId(Long userId);
    
    List<Article> getArticlesByStatus(Integer status);
    
    List<Article> getPublishedArticles();
    
    List<Article> getAllArticles();

    List<Article> getAllArticlesWithAuthor();

    PageResponse<Article> getAllArticlesWithAuthorPaginated(int page, int size, String keyword, Integer status);

    PageResponse<Article> getPublishedArticlesPaginated(int page, int size);

    PageResponse<Article> searchPublishedArticlesPaginated(int page, int size, String keyword);

    ArticleDTO createArticle(Long userId, ArticleDTO articleDTO);
    
    ArticleDTO updateArticle(Long articleId, Long userId, ArticleDTO articleDTO);
    
    void deleteArticle(Long articleId);
    
    void incrementViews(Long articleId);

    void likeArticle(Long articleId, Long userId);

    void unlikeArticle(Long articleId, Long userId);

    boolean isLiked(Long articleId, Long userId);

    List<Article> getLikedArticles(Long userId);
    
    void incrementComments(Long articleId);
    
    void decrementComments(Long articleId);
    
    void updateStatus(Long articleId, Integer status, String rejectReason);
    
    List<Article> searchPublishedArticles(String keyword);
}
