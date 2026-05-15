package com.example.backend.service;

import com.example.backend.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> findAll();

    Category findById(Long id);

    Category create(String name, Integer sortOrder);

    Category update(Long id, String name, Integer sortOrder);

    void delete(Long id);
}
