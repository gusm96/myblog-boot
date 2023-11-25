package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        // Category 중복 검사
        if(categoryRepository.findByName(categoryName).isPresent()){
            throw new DuplicateKeyException("이미 존재하는 카테고리명입니다.");
        }
        Category category = Category.builder().name(categoryName).build();
        Long result = categoryRepository.create(category);
        if(result > 0){
            return "success";
        }else{
            throw new PersistenceException("카테고리 등록을 실패했습니다.");
        }
    }

    public Long updateCategory(Long categoryId, String categoryName) {
        Category category = retrieveCategoryById(categoryId);
        category.editCategory(categoryName);
        return category.getId();
    }
    // 카테고리 삭제
    @Transactional
    public boolean deleteCategory(Long categoryId) {
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
        return categoryRepository.findById(categoryId).orElseThrow(()
                -> new EntityNotFoundException("해당 카테고리를 찾을 수 없습니다."));
    }

    public Category retrieveCategoryByName(String categoryName) {
        return categoryRepository.findByName(categoryName).orElseThrow(()
                -> new EntityNotFoundException("해당 카테고리를 찾을 수 없습니다."));
    }
}
