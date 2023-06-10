package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Category;
import com.moya.myblogboot.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> categoryList() {
        return categoryRepository.categories();
    }

    public String createCategory(String categoryName){
        Category category = Category.builder().name(categoryName).build();
        Long id = categoryRepository.create(category);
        if(id > 0){
            return "success";
        }else{
            return "failed";
        }
    }
}
