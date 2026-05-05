package com.example.backend.mapper;

import com.example.backend.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NotificationMapper {

    List<Notification> findByUserId(Long userId);

    List<Notification> findUnreadByUserId(Long userId);

    void insert(Notification notification);

    void markAsRead(Long id);

    void markAllAsRead(Long userId);

    void deleteReadByUserId(Long userId);

    void deleteById(Long id);
}
