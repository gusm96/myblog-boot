package com.moya.myblogboot.service;

import com.moya.myblogboot.dto.category.CategoriesResDto;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.dto.category.CategoryResDto;

import java.util.List;

public interface CategoryService {
    void create(String categoryName);

    List<CategoryResDto> retrieveAll();

    void update(Long categoryId, String modifiedCategoryName);

    void delete(Long categoryId);

    Category retrieve(Long categoryId);

    List<CategoriesResDto> retrieveAllWithViewPosts();

    List<CategoriesResDto> retrieveDto();
}
