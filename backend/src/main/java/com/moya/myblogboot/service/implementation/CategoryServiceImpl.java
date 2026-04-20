package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.event.CategoryChangeEvent;
import com.moya.myblogboot.dto.category.CategoriesResDto;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.dto.category.CategoryResDto;
import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.DuplicateException;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<CategoryResDto> retrieveAll() {
        return categoryRepository.findAll().stream().map(CategoryResDto::of).toList();
    }

    @Override
    @Transactional
    public void create(String categoryName) {
        if (categoryRepository.existsByName(categoryName)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_CATEGORY);
        }
        Category category = Category.builder().name(categoryName).build();
        Category saved = categoryRepository.save(category);
        eventPublisher.publishEvent(new CategoryChangeEvent(this, "CREATED", saved.getId()));
    }

    @Override
    public List<CategoriesResDto> retrieveAllWithViewPosts() {
        return categoryRepository.findCategoriesWithViewPosts();
    }

    @Override
    public List<CategoriesResDto> retrieveDto() {
        return categoryRepository.findAllDto();
    }

    @Override
    @Transactional
    public void update(Long categoryId, String modifiedCategoryName) {
        Category category = retrieve(categoryId);
        if (categoryRepository.existsByName(modifiedCategoryName)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_CATEGORY);
        }
        category.editCategory(modifiedCategoryName);
        eventPublisher.publishEvent(new CategoryChangeEvent(this, "UPDATED", categoryId));
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        Category category = retrieve(categoryId);
        if (!category.getPosts().isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_POSTS);
        }
        categoryRepository.delete(category);
        eventPublisher.publishEvent(new CategoryChangeEvent(this, "DELETED", categoryId));
    }

    @Override
    public Category retrieve(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(()
                -> new EntityNotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
