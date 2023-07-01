package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.Category;
import com.moya.myblogboot.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // 카테고리 리스트
    @GetMapping("/api/v1/management/categories")
    public ResponseEntity<List> getCategoryList() {
        return ResponseEntity.ok(categoryService.categoryList());
    }

    // 카테고리 작성
    @PostMapping("/api/v1/management/category")
    public ResponseEntity<String> newCategory(@RequestBody Map<String,String> categortMap) {
        return ResponseEntity.ok(categoryService.createCategory(categortMap.get("category")));
    }

    // 카테고리 수정
    @PutMapping("/api/v1/management/category/{categoryId}")
    public ResponseEntity<Long> editCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(0L);
    }

    // 카테고리 삭제
    @DeleteMapping("/api/v1/management/category/{categoryId}")
    public ResponseEntity<Boolean> deleteCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.deleteCategory(categoryId));
    }
}
