package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.category.CategoryResDto;

import java.util.List;

public interface CategoryService {
    String createCategory(String categoryName);

    List<CategoryResDto> getCategoryList();

    Long updateCategory(Long categoryId, String modifiedCategoryName);

    String deleteCategory(Long categoryId);

    Category retrieveCategoryById(Long categoryId);

    Category retrieveCategoryByName(String categoryName);

}
