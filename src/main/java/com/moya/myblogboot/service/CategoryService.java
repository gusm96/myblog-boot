package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.repository.CategoryRepository;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> categoryList() {
        return categoryRepository.categories();
    }
    @Transactional
    public String createCategory(String categoryName){
        Category category = Category.builder().name(categoryName).build();
        Long id = categoryRepository.create(category);
        if(id > 0){
            return "success";
        }else{
            return "failed";
        }
    }

    @Transactional
    public Boolean deleteCategory(Long categoryId) {
        Category category = retrieveCategoryById(categoryId);
        try {
            categoryRepository.removeCategory(category);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Category retrieveCategoryById(Long categoryId) {
        return categoryRepository.findOne(categoryId).orElseThrow(()
                -> new NoSuchElementException("해당 카테고리는 존재하지 않습니다."));
    }
}
