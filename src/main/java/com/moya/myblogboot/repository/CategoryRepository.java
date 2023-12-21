package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.category.Category;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryQuerydslRepository {

    @Query("select distinct c from Category c left join fetch c.boards where c.id = :id")
    Optional<Category> findById(@Param("id") Long id);
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
}
