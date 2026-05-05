package com.example.backend.service.impl;

import com.example.backend.entity.RolePermission;
import com.example.backend.mapper.RolePermissionMapper;
import com.example.backend.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RolePermissionServiceImpl implements RolePermissionService {

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public List<RolePermission> findAll() {
        return rolePermissionMapper.findAll();
    }

    @Override
    public RolePermission findById(Long id) {
        return rolePermissionMapper.findById(id);
    }

    @Override
    public RolePermission findByRoleName(String roleName) {
        return rolePermissionMapper.findByRoleName(roleName);
    }

    @Override
    public RolePermission create(RolePermission rolePermission) {
        rolePermissionMapper.insert(rolePermission);
        return rolePermissionMapper.findById(rolePermission.getId());
    }

    @Override
    public RolePermission update(Long id, RolePermission rolePermission) {
        RolePermission existing = rolePermissionMapper.findById(id);
        if (existing != null) {
            existing.setPermissionNames(rolePermission.getPermissionNames());
            existing.setDescription(rolePermission.getDescription());
            rolePermissionMapper.update(existing);
            return rolePermissionMapper.findById(id);
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        rolePermissionMapper.deleteById(id);
    }

    @Override
    public Set<String> getPermissionsByRoleName(String roleName) {
        RolePermission rolePermission = rolePermissionMapper.findByRoleName(roleName);
        if (rolePermission == null || rolePermission.getPermissionNames() == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(rolePermission.getPermissionNames().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
