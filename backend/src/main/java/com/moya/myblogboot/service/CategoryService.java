package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.category.CategoriesResDto;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.category.CategoryResDto;

import java.util.List;

public interface CategoryService {
    String create(String categoryName);

    List<CategoryResDto> retrieveAll();

    String update(Long categoryId, String modifiedCategoryName);

    String delete(Long categoryId);

    Category retrieve(Long categoryId);

    List<CategoriesResDto> retrieveAllWithViewBoards();

    List<CategoriesResDto> retrieveDto();
}
