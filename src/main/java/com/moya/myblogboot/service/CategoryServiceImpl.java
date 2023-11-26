package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.category.CategoryReqDto;
import com.moya.myblogboot.domain.category.CategoryResDto;
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
public class CategoryServiceImpl implements CategoryService{
    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResDto> getCategoryList() {
        return categoryRepository.categories().stream().map(category -> CategoryResDto.of(category)).toList();
    }
    @Override
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
    @Override
    public Long updateCategory(Long categoryId, String modifiedCategoryName) {
        Category category = retrieveCategoryById(categoryId);
        category.editCategory(modifiedCategoryName);
        return category.getId();
    }
    // 카테고리 삭제
    @Override
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
    @Override
    public Category retrieveCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(()
                -> new EntityNotFoundException("해당 카테고리를 찾을 수 없습니다."));
    }
    @Override
    public Category retrieveCategoryByName(String categoryName) {
        return categoryRepository.findByName(categoryName).orElseThrow(()
                -> new EntityNotFoundException("해당 카테고리를 찾을 수 없습니다."));
    }
}
