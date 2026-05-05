package com.example.backend.mapper;

import com.example.backend.entity.ArticleContent;

public interface ArticleContentMapper {
    
    ArticleContent findByArticleId(Long articleId);
    
    int insert(ArticleContent articleContent);
    
    int update(ArticleContent articleContent);
    
    int deleteByArticleId(Long articleId);
}
