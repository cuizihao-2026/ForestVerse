package com.example.backend.mapper;

import com.example.backend.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FeedbackMapper {

    List<Feedback> findByUserId(Long userId);

    List<Feedback> findAll();

    Feedback findById(Long id);

    void insert(Feedback feedback);

    void update(Feedback feedback);

    void updateStatus(Long id, String status);
}
