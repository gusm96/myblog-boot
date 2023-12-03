package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.category.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    void save(Category category);

    Optional<Category> findById(Long categoryId);

    Optional<Category> findByName(String categoryName);
    List<Category> categories();

    void removeCategory(Category category);
}
