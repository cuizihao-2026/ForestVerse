package com.example.backend.mapper;

import com.example.backend.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {
    
    List<Comment> findByArticleId(@Param("articleId") Long articleId);
    
    List<Comment> findByArticleIdAndStatus(@Param("articleId") Long articleId, @Param("status") Integer status);
    
    List<Comment> findByArticleIdForUser(@Param("articleId") Long articleId, @Param("userId") Long userId);
    
    Comment findById(@Param("id") Long id);
    
    int insert(Comment comment);
    
    int update(Comment comment);
    
    int deleteById(@Param("id") Long id);
    
    int countByArticleId(@Param("articleId") Long articleId);
    
    int countByArticleIdAndStatus(@Param("articleId") Long articleId, @Param("status") Integer status);
    
    List<Comment> findByStatus(@Param("status") Integer status);
    
    List<Comment> findAll();
    
    int countByStatus(@Param("status") Integer status);

    List<Comment> findAllPaginated(@Param("offset") int offset, @Param("size") int size, @Param("keyword") String keyword, @Param("status") Integer status);

    long countAll(@Param("keyword") String keyword, @Param("status") Integer status);
}
