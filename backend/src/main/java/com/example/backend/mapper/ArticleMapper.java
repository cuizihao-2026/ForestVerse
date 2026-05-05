package com.example.backend.mapper;

import com.example.backend.entity.Article;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ArticleMapper {
    
    Article findById(Long id);
    
    List<Article> findByUserId(Long userId);
    
    List<Article> findByStatus(Integer status);
    
    List<Article> findPublished();
    
    List<Article> findAll();

    List<Article> findAllWithAuthor();
    
    List<Article> searchPublished(String keyword);

    List<Article> findAllWithAuthorPaginated(@Param("offset") int offset, @Param("size") int size, @Param("keyword") String keyword, @Param("status") Integer status);

    long countAllWithAuthor(@Param("keyword") String keyword, @Param("status") Integer status);

    List<Article> findPublishedPaginated(@Param("offset") int offset, @Param("size") int size);

    long countPublished();

    List<Article> searchPublishedPaginated(@Param("offset") int offset, @Param("size") int size, @Param("keyword") String keyword);

    long countSearchPublished(@Param("keyword") String keyword);

    int insert(Article article);
    
    int update(Article article);
    
    int delete(Long id);
    
    int softDelete(Long id);
    
    int incrementViews(Long id);
    
    int incrementLikes(Long id);
    
    int decrementLikes(Long id);
    
    int incrementComments(Long id);
    
    int decrementComments(Long id);
    
    int count();
    
    int countByStatus(Integer status);
}
