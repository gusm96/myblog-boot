package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.category.CategoriesResDto;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.category.CategoryResDto;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResDto> getCategoryList() {
        return categoryRepository.findAll().stream().map(CategoryResDto::of).toList();
    }
    @Override
    @Transactional
    public String createCategory(String categoryName){
        // Category 중복 검사
        if(categoryRepository.existsByName(categoryName)){
            throw new DuplicateKeyException("이미 존재하는 카테고리입니다.");
        }
        Category category = Category.builder().name(categoryName).build();
        try {
            categoryRepository.save(category);
            return "카테고리가 정상적으로 등록되었습니다.";
        } catch (Exception e) {
            log.error("카테고리 등록 실패.");
            throw new PersistenceException("카테고리 등록을 실패했습니다.");
        }
    }

    @Override
    public List<CategoriesResDto> retrieveCategoriesDto() {
        return categoryRepository.findAllDto();
    }

    @Override
    @Transactional
    public String updateCategory(Long categoryId, String modifiedCategoryName) {
        Category category = retrieveCategoryById(categoryId);
        try {
            category.editCategory(modifiedCategoryName);
            return category.getName();
        } catch (Exception e) {
            log.error("카테고리 수정 실패");
            throw new RuntimeException("카테고리 수정 중 오류가 발생했습니다.");
        }
    }
    // 카테고리 삭제
    @Override
    @Transactional
    public String deleteCategory(Long categoryId) {
        Category category = retrieveCategoryById(categoryId);
        if(category.getBoards().size() > 0) {
            throw new RuntimeException("등록된 게시글이 존재해 삭제할 수 없습니다.");
        }
        try {
            categoryRepository.delete(category);
            return "카테고리가 삭제되었습니다.";
        } catch (Exception e) {
            log.error("카테고리 삭제 실패");
            throw new RuntimeException("카테고리 삭제 중 오류가 발생했습니다.");
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
