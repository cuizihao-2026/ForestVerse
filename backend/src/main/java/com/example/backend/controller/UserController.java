package com.example.backend.controller;

import com.example.backend.config.WebSocketHandler;
import com.example.backend.dto.ChangePasswordRequest;
import com.example.backend.dto.RoleUpdateRequest;
import com.example.backend.dto.UserStatusUpdateRequest;
import com.example.backend.entity.User;
import com.example.backend.entity.RolePermission;
import com.example.backend.service.UserService;
import com.example.backend.service.TokenService;
import com.example.backend.service.RolePermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private RolePermissionService rolePermissionService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("获取用户信息失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/permissions")
    public ResponseEntity<?> getPermissions(jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }
            Set<String> permissions = rolePermissionService.getPermissionsByRoleName(user.getRole());
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            logger.error("获取用户权限失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/role-info")
    public ResponseEntity<?> getRoleInfo(jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }
            RolePermission rolePermission = rolePermissionService.findByRoleName(user.getRole());
            return ResponseEntity.ok(rolePermission);
        } catch (Exception e) {
            logger.error("获取角色信息失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User user) {
        try {
            User currentUser = userService.findById(user.getId());
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }

            if (user.getNickname() != null) {
                currentUser.setNickname(user.getNickname());
            }
            if (user.getBio() != null) {
                currentUser.setBio(user.getBio());
            }
            if (user.getAvatar() != null) {
                currentUser.setAvatar(user.getAvatar());
            }

            User updatedUser = userService.updateUser(currentUser);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.error("更新用户信息失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(request.getUserId(), request.getOldPassword(), request.getNewPassword());
            tokenService.clearActiveToken(request.getUserId());
            return ResponseEntity.ok("密码修改成功，请重新登录");
        } catch (Exception e) {
            logger.error("修改密码失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.findAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("获取所有用户失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/role/{userId}")
    public ResponseEntity<?> updateUserRole(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable Long userId,
            @RequestBody RoleUpdateRequest roleRequest) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(currentUserId);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("当前用户不存在");
            }

            User targetUser = userService.findById(userId);
            if (targetUser == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }
            if ("SUPER_ADMIN".equals(targetUser.getRole())) {
                return ResponseEntity.badRequest().body("无法修改超级管理员的角色");
            }

            if ("SUPER_ADMIN".equals(roleRequest.getRole())) {
                return ResponseEntity.badRequest().body("无法将用户设置为超级管理员");
            }

            if ("ADMIN".equals(roleRequest.getRole()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
                return ResponseEntity.badRequest().body("您没有权限任命管理员");
            }

            userService.updateUserRole(userId, roleRequest.getRole());
            return ResponseEntity.ok("角色更新成功");
        } catch (Exception e) {
            logger.error("更新用户角色失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/status/{userId}")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long userId, @RequestBody UserStatusUpdateRequest request) {
        try {
            userService.updateUserStatus(userId, request.getStatus());
            return ResponseEntity.ok("状态更新成功");
        } catch (Exception e) {
            logger.error("更新用户状态失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(jakarta.servlet.http.HttpServletRequest request, @PathVariable Long userId) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(currentUserId);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("当前用户不存在");
            }

            User targetUser = userService.findById(userId);
            if (targetUser == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }

            if (currentUserId.equals(userId)) {
                if ("SUPER_ADMIN".equals(targetUser.getRole())) {
                    return ResponseEntity.badRequest().body("超级管理员账户无法注销");
                }
            } else {
                if (!"ADMIN".equals(currentUser.getRole()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
                    return ResponseEntity.badRequest().body("您没有权限删除其他用户");
                }

                if ("SUPER_ADMIN".equals(targetUser.getRole()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
                    return ResponseEntity.badRequest().body("您没有权限删除超级管理员");
                }

                if ("ADMIN".equals(targetUser.getRole()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
                    return ResponseEntity.badRequest().body("您没有权限删除管理员");
                }
            }

            String reason = currentUserId.equals(userId) ? "SELF_DELETE" : "ADMIN_DELETE";
            webSocketHandler.sendUserDeletedNotification(userId, reason);

            tokenService.clearActiveToken(userId);

            userService.deleteUser(userId);
            return ResponseEntity.ok("用户删除成功");
        } catch (Exception e) {
            logger.error("删除用户失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
