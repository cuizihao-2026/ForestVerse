package com.example.backend.service;

import com.example.backend.entity.RolePermission;

import java.util.List;
import java.util.Set;

public interface RolePermissionService {
    List<RolePermission> findAll();
    RolePermission findById(Long id);
    RolePermission findByRoleName(String roleName);
    RolePermission create(RolePermission rolePermission);
    RolePermission update(Long id, RolePermission rolePermission);
    void delete(Long id);
    Set<String> getPermissionsByRoleName(String roleName);
}
