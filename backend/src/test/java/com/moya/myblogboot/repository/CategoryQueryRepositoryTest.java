package com.moya.myblogboot.repository;


import com.moya.myblogboot.domain.category.CategoriesResDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.moya.myblogboot.domain.category.QCategory.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class CategoryQueryRepositoryTest {

    @Autowired
    EntityManager em;

    private JPAQueryFactory jpaQueryFactory;

    @BeforeEach
    void before (){
        jpaQueryFactory = new JPAQueryFactory(em);
    }

    @Test
    @DisplayName("카테고리 DTO 조회")
    void 카테고리_DTO_조회() {
        // given
        List<CategoriesResDto> categoriesResDtoList =
                jpaQueryFactory.select(Projections.fields(CategoriesResDto.class,
                                category.id,
                                category.name,
                                category.boards.size().as("boardsCount")))
                        .from(category)
                        .groupBy(category.id)
                        .fetch();
        // when
        for (CategoriesResDto c : categoriesResDtoList) {
            System.out.println(c.getName());
            System.out.println(c.getBoardsCount());
        }
        // then
    }
}
