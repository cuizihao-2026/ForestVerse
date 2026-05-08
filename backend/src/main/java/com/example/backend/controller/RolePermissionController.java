package com.example.backend.controller;

import com.example.backend.entity.RolePermission;
import com.example.backend.entity.User;
import com.example.backend.service.RolePermissionService;
import com.example.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
public class RolePermissionController {

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private UserService userService;

    private boolean hasRoleManagePermission(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return false;
        User user = userService.findById(userId);
        if (user == null) return false;
        return rolePermissionService.hasPermission(user.getRole(), "role:manage");
    }

    @GetMapping
    public ResponseEntity<?> getAllRoles(HttpServletRequest request) {
        if (!hasRoleManagePermission(request)) {
            return ResponseEntity.status(403).body("权限不足");
        }
        List<RolePermission> roles = rolePermissionService.findAll();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleById(@PathVariable Long id, HttpServletRequest request) {
        if (!hasRoleManagePermission(request)) {
            return ResponseEntity.status(403).body("权限不足");
        }
        RolePermission role = rolePermissionService.findById(id);
        if (role == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(role);
    }

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody RolePermission rolePermission, HttpServletRequest request) {
        if (!hasRoleManagePermission(request)) {
            return ResponseEntity.status(403).body("权限不足");
        }
        
        // 检查角色名是否已存在
        RolePermission existing = rolePermissionService.findByRoleName(rolePermission.getRoleName());
        if (existing != null) {
            return ResponseEntity.badRequest().body("角色名已存在");
        }
        
        RolePermission created = rolePermissionService.create(rolePermission);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody RolePermission rolePermission, HttpServletRequest request) {
        if (!hasRoleManagePermission(request)) {
            return ResponseEntity.status(403).body("权限不足");
        }
        
        RolePermission updated = rolePermissionService.update(id, rolePermission);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id, HttpServletRequest request) {
        if (!hasRoleManagePermission(request)) {
            return ResponseEntity.status(403).body("权限不足");
        }
        
        rolePermissionService.delete(id);
        return ResponseEntity.ok().build();
    }
}
