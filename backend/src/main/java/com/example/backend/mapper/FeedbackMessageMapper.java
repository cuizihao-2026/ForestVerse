package com.example.backend.mapper;

import com.example.backend.entity.FeedbackMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FeedbackMessageMapper {

    List<FeedbackMessage> findByFeedbackId(Long feedbackId);

    void insert(FeedbackMessage message);

    void markAsRead(Long feedbackId, Long userId);

    Integer countUnread(Long feedbackId, Long userId);
}
