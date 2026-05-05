package com.example.backend.mapper;

import com.example.backend.entity.User;
import java.util.List;

public interface UserMapper {
    // 根据用户名查询用户
    User findByUsername(String username);

    // 根据邮箱查询用户
    User findByEmail(String email);

    // 插入新用户
    int insert(User user);

    // 根据ID查询用户
    User findById(Long id);

    // 更新用户信息
    int update(User user);

    // 删除用户
    int delete(Long id);

    // 根据用户名和邮箱更新密码
    int updatePassword(String username, String email, String password);

    // 查询所有用户
    List<User> findAll();

    // 统计用户数量
    int count();
    
    // 统计活跃用户数量
    int countActiveUsers();
    
    // 搜索用户
    List<User> searchUsers(String keyword, Long excludeUserId);
}