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
import jakarta.validation.Valid;
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
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(request.getUserId(), request.getOldPassword(), request.getNewPassword());
            tokenService.clearActiveToken(request.getUserId());
            return ResponseEntity.ok("密码修改成功，请重新登录");
        } catch (Exception e) {
            logger.error("修改密码失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 用户DTO，包含用户信息和权限
    static class UserWithPermissions {
        private User user;
        private Set<String> permissions;

        public UserWithPermissions(User user, Set<String> permissions) {
            this.user = user;
            this.permissions = permissions;
        }

        public User getUser() { return user; }
        public Set<String> getPermissions() { return permissions; }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.findAllUsers();
            List<UserWithPermissions> usersWithPermissions = users.stream()
                .map(user -> new UserWithPermissions(user, rolePermissionService.getPermissionsByRoleName(user.getRole())))
                .toList();
            return ResponseEntity.ok(usersWithPermissions);
        } catch (Exception e) {
            logger.error("获取所有用户失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/role-permissions")
    public ResponseEntity<?> getAllRolePermissions() {
        try {
            List<RolePermission> rolePermissions = rolePermissionService.findAll();
            return ResponseEntity.ok(rolePermissions);
        } catch (Exception e) {
            logger.error("获取角色权限失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 检查是否可以管理目标用户
    private boolean canManageUser(User currentUser, User targetUser) {
        // 用户不能管理自己
        if (targetUser.getId().equals(currentUser.getId())) return false;
        
        // 获取当前用户和目标用户的权限
        Set<String> currentPermissions = rolePermissionService.getPermissionsByRoleName(currentUser.getRole());
        Set<String> targetPermissions = rolePermissionService.getPermissionsByRoleName(targetUser.getRole());
        
        // 目标用户有 user.supermanage 权限，没人能管理/显示（包括其他 user.supermanage 用户）
        if (targetPermissions.contains("user.supermanage")) return false;
        
        // 有 user.supermanage 权限的用户可以管理其他人
        if (currentPermissions.contains("user.supermanage")) return true;
        
        // 当前用户有 user:manage 权限，只能管理没有 user:manage 的用户
        if (currentPermissions.contains("user:manage")) {
            return !targetPermissions.contains("user:manage");
        }
        
        return false;
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

            // 检查是否有修改角色的权限
            boolean hasUserManage = rolePermissionService.hasPermission(currentUser.getRole(), "user:manage");
            boolean hasUserSuperManage = rolePermissionService.hasPermission(currentUser.getRole(), "user.supermanage");
            
            if (!hasUserManage && !hasUserSuperManage) {
                return ResponseEntity.status(403).body("您没有权限修改角色");
            }

            User targetUser = userService.findById(userId);
            if (targetUser == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }

            // 检查是否可以管理该用户
            if (!canManageUser(currentUser, targetUser)) {
                return ResponseEntity.status(403).body("您没有权限管理该用户");
            }

            // 获取目标角色的权限
            Set<String> targetRolePermissions = rolePermissionService.getPermissionsByRoleName(roleRequest.getRole());
            
            // 任何人都不能设置为有 user.supermanage 权限的角色
            if (targetRolePermissions.contains("user.supermanage")) {
                return ResponseEntity.status(403).body("您没有权限设置该角色");
            }

            // 只有 user.supermanage 权限的用户，才能设置为有 user:manage 权限的角色
            if (targetRolePermissions.contains("user:manage")) {
                if (!hasUserSuperManage) {
                    return ResponseEntity.status(403).body("您没有权限设置该角色");
                }
            }

            userService.updateUserRole(userId, roleRequest.getRole());
            return ResponseEntity.ok("角色更新成功");
        } catch (Exception e) {
            logger.error("更新用户角色失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/status/{userId}")
    public ResponseEntity<?> updateUserStatus(jakarta.servlet.http.HttpServletRequest request, 
                                             @PathVariable Long userId, 
                                             @RequestBody UserStatusUpdateRequest statusRequest) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(currentUserId);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("当前用户不存在");
            }

            // 检查是否有 user:manage 或 user.supermanage 权限
            boolean hasUserManage = rolePermissionService.hasPermission(currentUser.getRole(), "user:manage");
            boolean hasUserSuperManage = rolePermissionService.hasPermission(currentUser.getRole(), "user.supermanage");
            
            if (!hasUserManage && !hasUserSuperManage) {
                return ResponseEntity.status(403).body("您没有权限修改用户状态");
            }

            User targetUser = userService.findById(userId);
            if (targetUser == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }

            // 检查是否可以管理该用户
            if (!canManageUser(currentUser, targetUser)) {
                return ResponseEntity.status(403).body("您没有权限管理该用户");
            }

            userService.updateUserStatus(userId, statusRequest.getStatus());
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
                // 用户删除自己的账户
            } else {
                // 检查是否有 user:manage 或 user.supermanage 权限
                boolean hasUserManage = rolePermissionService.hasPermission(currentUser.getRole(), "user:manage");
                boolean hasUserSuperManage = rolePermissionService.hasPermission(currentUser.getRole(), "user.supermanage");
                
                if (!hasUserManage && !hasUserSuperManage) {
                    return ResponseEntity.status(403).body("您没有权限删除其他用户");
                }
                
                // 检查是否可以管理该用户
                if (!canManageUser(currentUser, targetUser)) {
                    return ResponseEntity.status(403).body("您没有权限删除该用户");
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
