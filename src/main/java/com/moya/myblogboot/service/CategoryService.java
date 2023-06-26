package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Category;
import com.moya.myblogboot.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
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
        Category category = Category.builder().name(categoryName).build();
        Long id = categoryRepository.create(category);
        if(id > 0){
            return "success";
        }else{
            return "failed";
        }
    }

    public Boolean deleteCategory(Long categoryId) {
        Category category = findCategory(categoryId);
        try {
            categoryRepository.removeCategory(category);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Category findCategory(Long categoryId) {
        return categoryRepository.findOne(categoryId).orElseThrow(()
                -> new IllegalStateException("해당 카테고리는 존재하지 않습니다."));
    }
}
