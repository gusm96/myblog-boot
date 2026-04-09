package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.category.CategoriesResDto;
import com.moya.myblogboot.domain.category.CategoryReqDto;
import com.moya.myblogboot.domain.category.CategoryResDto;
import com.moya.myblogboot.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/api/v1/categories")
    public ResponseEntity<List<CategoryResDto>> getCategoryList() {
        return ResponseEntity.ok(categoryService.retrieveAll());
    }

    @GetMapping("/api/v2/categories")
    public ResponseEntity<List<CategoriesResDto>> getCategoryListV2() {
        return ResponseEntity.ok().body(categoryService.retrieveAllWithViewBoards());
    }

    @GetMapping("/api/v1/categories-management")
    public ResponseEntity<List<CategoriesResDto>> getCategoryListForAdmin() {
        return ResponseEntity.ok().body(categoryService.retrieveDto());
    }

    @PostMapping("/api/v1/categories")
    public ResponseEntity<Void> newCategory(@RequestBody @Valid CategoryReqDto categoryReqDto) {
        categoryService.create(categoryReqDto.getCategoryName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/v1/categories/{categoryId}")
    public ResponseEntity<Void> editCategory(@PathVariable Long categoryId, @RequestBody @Valid CategoryReqDto categoryReqDto) {
        categoryService.update(categoryId, categoryReqDto.getCategoryName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/v1/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ResponseEntity.ok().build();
    }
}
