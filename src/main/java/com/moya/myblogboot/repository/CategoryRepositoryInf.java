package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.category.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryInf {

    Long create(Category category);

    Optional<Category> findOne(Long categoryId);

    Optional<Category> findByName(String categoryName);
    List<Category> categories();

    void removeCategory(Category category);
}
