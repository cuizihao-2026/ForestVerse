package com.example.backend.controller;

import com.example.backend.entity.Category;
import com.example.backend.entity.User;
import com.example.backend.service.CategoryService;
import com.example.backend.service.RolePermissionService;
import com.example.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() {
        try {
            List<Category> categories = categoryService.findAll();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/admin/categories")
    public ResponseEntity<?> getCategoriesForAdmin(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "article.class")) {
                return ResponseEntity.status(403).body("权限不足");
            }
            List<Category> categories = categoryService.findAll();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/admin/categories")
    public ResponseEntity<?> createCategory(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "article.class")) {
                return ResponseEntity.status(403).body("权限不足");
            }

            String name = (String) body.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("分类名称不能为空");
            }
            Integer sortOrder = body.get("sortOrder") != null ? (Integer) body.get("sortOrder") : 0;

            Category category = categoryService.create(name.trim(), sortOrder);
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/admin/categories/{id}")
    public ResponseEntity<?> updateCategory(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "article.class")) {
                return ResponseEntity.status(403).body("权限不足");
            }

            String name = (String) body.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("分类名称不能为空");
            }
            Integer sortOrder = body.get("sortOrder") != null ? (Integer) body.get("sortOrder") : 0;

            Category category = categoryService.update(id, name.trim(), sortOrder);
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/admin/categories/{id}")
    public ResponseEntity<?> deleteCategory(HttpServletRequest request, @PathVariable Long id) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User currentUser = userService.findById(userId);
            if (currentUser == null || !rolePermissionService.hasPermission(currentUser.getRole(), "article.class")) {
                return ResponseEntity.status(403).body("权限不足");
            }

            categoryService.delete(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
