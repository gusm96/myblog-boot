package com.moya.myblogboot.repository.implementation;


import com.moya.myblogboot.domain.post.PostStatus;
import com.moya.myblogboot.domain.category.CategoriesResDto;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.repository.CategoryQuerydslRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

import static com.moya.myblogboot.domain.post.QPost.*;
import static com.moya.myblogboot.domain.category.QCategory.*;

@Repository
@RequiredArgsConstructor
public class CategoryQuerydslRepositoryImpl implements CategoryQuerydslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<CategoriesResDto> findCategoriesWithViewPosts() {
        // fetchJoin + 컬렉션 → 게시글 수만큼 Category 행이 중복 생성됨
        // distinct()로 Hibernate 레벨 중복 제거 (SQL DISTINCT + 인메모리 de-duplication)
        List<Category> categories = queryFactory
                .selectFrom(category)
                .distinct()
                .leftJoin(category.posts, post).fetchJoin()
                .where(category.posts.size().gt(0).and(post.postStatus.eq(PostStatus.VIEW)))
                .fetch();
        return categories.stream().map(category -> CategoriesResDto.builder().category(category).build()).collect(Collectors.toList());
    }

    @Override
    public List<CategoriesResDto> findAllDto () {
        List<Category> categories = queryFactory
                .selectFrom(category)
                .distinct()
                .leftJoin(category.posts, post).fetchJoin()
                .fetch();
        return categories.stream().map(category -> CategoriesResDto.builder().category(category).build()).collect(Collectors.toList());
    }
}
