package com.example.backend.mapper;

import com.example.backend.entity.Category;
import java.util.List;

public interface CategoryMapper {
    Category findById(Long id);

    List<Category> findAll();

    List<Category> findAllOrdered();

    void insert(Category category);

    void update(Category category);

    void delete(Long id);
}
