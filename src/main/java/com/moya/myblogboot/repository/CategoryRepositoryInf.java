package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryInf {

    Long create(Category category);

    Optional<Category> findOne(String categoryName);

    List<Category> categories();

}
