package com.example.backend.mapper;

import com.example.backend.entity.Article;
import com.example.backend.entity.ArticleLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleLikeMapper {

    ArticleLike findByArticleIdAndUserId(@Param("articleId") Long articleId, @Param("userId") Long userId);

    int insert(ArticleLike articleLike);

    int deleteByArticleIdAndUserId(@Param("articleId") Long articleId, @Param("userId") Long userId);

    int countByArticleId(Long articleId);

    List<Article> findLikedArticlesByUserId(@Param("userId") Long userId);

    List<Article> findLikedPublishedArticlesByUserId(@Param("userId") Long userId);
}
