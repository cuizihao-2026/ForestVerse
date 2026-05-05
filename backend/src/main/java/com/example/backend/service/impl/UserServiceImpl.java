package com.example.backend.service.impl;

import com.example.backend.config.WebSocketHandler;
import com.example.backend.dto.UserStats;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.mapper.ArticleMapper;
import com.example.backend.mapper.CommentMapper;
import com.example.backend.service.UserService;
import com.example.backend.service.TokenService;
import com.example.backend.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private WebSocketHandler webSocketHandler;
    
    @Autowired
    private TokenService tokenService;

    private static final String[] NICKNAME_PREFIXES = { "可爱", "聪明", "勇敢", "帅气", "温柔", "善良", "乐观", "活泼", "冷静", "热情",
            "真诚", "稳重", "调皮", "大方", "细心" };
    private static final String[] NICKNAME_SUFFIXES = { "小熊", "小猫", "小狗", "小兔", "小鸟", "小鱼", "小鹿", "小虎", "小鹰", "小龙",
            "星星", "月光", "阳光", "云朵", "彩虹" };

    @Override
    public User register(User user) {
        if (userMapper.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        if (userMapper.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        user.setNickname(generateRandomNickname());
        user.setPassword(PasswordUtils.encode(user.getPassword()));

        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        if (user.getStatus() == null || user.getStatus().isEmpty()) {
            user.setStatus("ACTIVE");
        }
        if (user.getAvatar() == null) {
            user.setAvatar("");
        }
        if (user.getBio() == null) {
            user.setBio("");
        }

        userMapper.insert(user);

        return user;
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!PasswordUtils.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if ("BANNED".equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }

        return user;
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public boolean verifyUserCredentials(String username, String email) {
        User user = userMapper.findByUsername(username);
        return user != null && email != null && user.getEmail() != null && user.getEmail().equals(email);
    }

    @Override
    public void resetPassword(String username, String email, String newPassword) {
        if (!verifyUserCredentials(username, email)) {
            throw new RuntimeException("用户名和邮箱不匹配");
        }

        String encodedPassword = PasswordUtils.encode(newPassword);
        userMapper.updatePassword(username, email, encodedPassword);
    }

    @Override
    public List<User> findAllUsers() {
        return userMapper.findAll();
    }

    @Override
    public int countUsers() {
        return userMapper.count();
    }

    @Override
    public void updateUserRole(Long userId, String role) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setRole(role);
        userMapper.update(user);
        webSocketHandler.sendUserUpdateNotification(user);
    }

    @Override
    public void updateUserStatus(Long userId, String status) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(status);
        userMapper.update(user);
        webSocketHandler.sendUserUpdateNotification(user);
        
        // 如果用户被封禁，清除他们的 token
        if ("BANNED".equals(status)) {
            tokenService.clearActiveToken(userId);
        }
    }

    @Override
    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        userMapper.update(user);
        User updatedUser = userMapper.findById(user.getId());
        webSocketHandler.sendUserUpdateNotification(updatedUser);
        return updatedUser;
    }
    
    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // 根据用户ID查找用户
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码是否正确
        if (!PasswordUtils.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        
        // 验证新旧密码是否相同
        if (PasswordUtils.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }
        
        // 对新密码进行加密
        String encodedPassword = PasswordUtils.encode(newPassword);
        
        // 更新用户密码
        user.setPassword(encodedPassword);
        userMapper.update(user);
    }
    
    @Override
    public void deleteUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        userMapper.delete(userId);
    }
    
    @Override
    public UserStats getUserStats() {
        UserStats stats = new UserStats();
        stats.setTotalUsers(userMapper.count());
        stats.setOnlineUsers(webSocketHandler.getOnlineCount());
        stats.setTotalArticles(articleMapper.count());
        stats.setPublishedArticles(articleMapper.countByStatus(3));
        stats.setPendingArticles(articleMapper.countByStatus(1));
        stats.setPendingComments(commentMapper.countByStatus(1));
        stats.setPendingCount(stats.getPendingArticles() + stats.getPendingComments());
        return stats;
    }

    private String generateRandomNickname() {
        Random random = new Random();
        String prefix = NICKNAME_PREFIXES[random.nextInt(NICKNAME_PREFIXES.length)];
        String suffix = NICKNAME_SUFFIXES[random.nextInt(NICKNAME_SUFFIXES.length)];
        int number = random.nextInt(1000);
        return prefix + suffix + number;
    }
}