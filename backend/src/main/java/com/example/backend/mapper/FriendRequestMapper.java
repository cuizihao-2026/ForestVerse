package com.example.backend.mapper;

import com.example.backend.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendRequestMapper {
    int insert(FriendRequest request);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    FriendRequest findById(Long id);
    List<FriendRequest> findByReceiverId(Long receiverId);
    List<FriendRequest> findBySenderId(Long senderId);
    FriendRequest findBySenderAndReceiver(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
