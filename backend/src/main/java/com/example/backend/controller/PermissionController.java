package com.example.backend.controller;

import com.example.backend.entity.Permission;
import com.example.backend.mapper.PermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/permissions")
public class PermissionController {

    @Autowired
    private PermissionMapper permissionMapper;

    @GetMapping
    public List<Permission> getAllPermissions() {
        return permissionMapper.findAll();
    }
}
