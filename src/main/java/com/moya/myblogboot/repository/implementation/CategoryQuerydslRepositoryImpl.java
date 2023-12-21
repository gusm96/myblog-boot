package com.moya.myblogboot.repository.implementation;


import com.moya.myblogboot.domain.category.CategoriesResDto;
import com.moya.myblogboot.repository.CategoryQuerydslRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.moya.myblogboot.domain.category.QCategory.*;

@Repository
@RequiredArgsConstructor
public class CategoryQuerydslRepositoryImpl implements CategoryQuerydslRepository {
    private final EntityManager em;
    private JPAQueryFactory jpaQueryFactory;

    @Override
    public List<CategoriesResDto> findAllDto() {
        jpaQueryFactory = new JPAQueryFactory(em);
        return jpaQueryFactory.select(Projections.fields(CategoriesResDto.class,
                        category.id,
                        category.name,
                        category.boards.size().as("boardsCount"))
                ).from(category)
                .fetch();
    }
}
