package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.category.CategoriesResDto;

import java.util.List;

public interface CategoryQuerydslRepository {
    List<CategoriesResDto> findAllDto();

    List<CategoriesResDto> findCategoriesWithViewBoards();
}
