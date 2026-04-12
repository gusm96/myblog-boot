package com.moya.myblogboot.repository;

import com.moya.myblogboot.dto.category.CategoriesResDto;

import java.util.List;

public interface CategoryQuerydslRepository {
    List<CategoriesResDto> findAllDto();

    List<CategoriesResDto> findCategoriesWithViewPosts();
}
