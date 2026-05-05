package com.example.backend.mapper;

import com.example.backend.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendshipMapper {
    int insert(Friendship friendship);
    int deleteByUserAndFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);
    List<Friendship> findByUserId(Long userId);
    Friendship findByUserAndFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);
    int updateGroup(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("groupName") String groupName);
    int updateRemark(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("remark") String remark);
}
