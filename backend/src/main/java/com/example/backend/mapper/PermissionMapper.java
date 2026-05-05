package com.example.backend.mapper;

import com.example.backend.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PermissionMapper {
    List<Permission> findAll();
}
