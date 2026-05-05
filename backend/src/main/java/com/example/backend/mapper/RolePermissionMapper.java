package com.example.backend.mapper;

import com.example.backend.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RolePermissionMapper {
    List<RolePermission> findAll();
    RolePermission findById(Long id);
    RolePermission findByRoleName(String roleName);
    int insert(RolePermission rolePermission);
    int update(RolePermission rolePermission);
    int deleteById(Long id);
}
