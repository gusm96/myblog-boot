package com.moya.myblogboot.repository.implementation;


import com.moya.myblogboot.domain.board.BoardStatus;
import com.moya.myblogboot.domain.category.CategoriesResDto;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.repository.CategoryQuerydslRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

import static com.moya.myblogboot.domain.board.QBoard.*;
import static com.moya.myblogboot.domain.category.QCategory.*;

@Repository
@RequiredArgsConstructor
public class CategoryQuerydslRepositoryImpl implements CategoryQuerydslRepository {
    private final EntityManager em;
    private JPAQueryFactory jpaQueryFactory;

    @Override
    public List<CategoriesResDto> findCategoriesWithViewBoards() {
        jpaQueryFactory = new JPAQueryFactory(em);
        List<Category> categories = jpaQueryFactory
                .selectFrom(category)
                .leftJoin(category.boards, board).fetchJoin()
                .where(category.boards.size().gt(0).and(board.boardStatus.eq(BoardStatus.VIEW)))
                .fetch();
        return categories.stream().map(category -> CategoriesResDto.builder().category(category).build()).collect(Collectors.toList());
    }

    @Override
    public List<CategoriesResDto> findAllDto () {
        jpaQueryFactory = new JPAQueryFactory(em);
        List<Category> categories = jpaQueryFactory
                .selectFrom(category)
                .leftJoin(category.boards, board).fetchJoin()
                .fetch();
        return categories.stream().map(category -> CategoriesResDto.builder().category(category).build()).collect(Collectors.toList());
    }
}
