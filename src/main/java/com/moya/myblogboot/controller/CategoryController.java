package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.category.CategoryReqDto;
import com.moya.myblogboot.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // 카테고리 리스트
    @GetMapping("/api/v1/categories")
    public ResponseEntity<List> getCategoryList() {
        return ResponseEntity.ok(categoryService.getCategoryList());
    }

    // 카테고리 작성
    @PostMapping("/api/v1/categories")
    public ResponseEntity<String> newCategory(@RequestBody @Valid CategoryReqDto categoryReqDto) {
        return ResponseEntity.ok(categoryService.createCategory(categoryReqDto.getCategoryName()));
    }

    // 카테고리 수정
    @PutMapping("/api/v1/categories/{categoryId}")
    public ResponseEntity<String> editCategory(@PathVariable Long categoryId, @RequestBody @Valid CategoryReqDto categoryReqDto ) {
        return ResponseEntity.ok().body(categoryService.updateCategory(categoryId, categoryReqDto.getCategoryName()));
    }

    // 카테고리 삭제
    @DeleteMapping("/api/v1/categories/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.deleteCategory(categoryId));
    }
}
