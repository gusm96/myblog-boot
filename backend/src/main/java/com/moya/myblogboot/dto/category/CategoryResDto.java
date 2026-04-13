package com.moya.myblogboot.dto.category;

import com.moya.myblogboot.domain.category.Category;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryResDto {
    private Long id;
    private String name;

    public static CategoryResDto of(Category cateGory) {
        return CategoryResDto.builder().id(cateGory.getId()).name(cateGory.getName()).build();
    }
}
