package com.example.backend.service.impl;

import com.example.backend.entity.Category;
import com.example.backend.mapper.CategoryMapper;
import com.example.backend.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> findAll() {
        return categoryMapper.findAllOrdered();
    }

    @Override
    public Category findById(Long id) {
        return categoryMapper.findById(id);
    }

    @Override
    public Category create(String name, Integer sortOrder) {
        Category category = new Category();
        category.setName(name);
        category.setSortOrder(sortOrder != null ? sortOrder : 0);
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public Category update(Long id, String name, Integer sortOrder) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        category.setName(name);
        category.setSortOrder(sortOrder != null ? sortOrder : category.getSortOrder());
        categoryMapper.update(category);
        return category;
    }

    @Override
    public void delete(Long id) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        categoryMapper.delete(id);
    }
}
