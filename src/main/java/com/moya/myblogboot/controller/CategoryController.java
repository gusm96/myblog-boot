package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.Category;
import com.moya.myblogboot.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/api/v1/management/categories")
    public ResponseEntity getCategoryList(){
        return ResponseEntity.ok(categoryService.categoryList());
    }
    @PostMapping("/api/v1/management/new-category")
    public ResponseEntity newCategory(@RequestBody String categoryName) {
        return ResponseEntity.ok(categoryService.createCategory(categoryName));
    }
}
