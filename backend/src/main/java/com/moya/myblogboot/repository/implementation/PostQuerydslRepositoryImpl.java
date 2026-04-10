package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.SearchType;
import com.moya.myblogboot.repository.PostQuerydslRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.moya.myblogboot.domain.post.QPost.*;

@Repository
@RequiredArgsConstructor
public class PostQuerydslRepositoryImpl implements PostQuerydslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Post> findByDeleteDate(LocalDateTime deleteDate) {
        return queryFactory.selectFrom(post)
                .leftJoin(post.imageFiles).fetchJoin()
                .where(post.deleteDate.loe(deleteDate))
                .fetch();
    }

    @Override
    public Page<Post> findBySearchType(Pageable pageable, SearchType searchType, String contents) {
        BooleanExpression condition = searchType == SearchType.TITLE
                ? post.title.contains(contents)
                : post.content.contains(contents);

        List<Post> posts = queryFactory.selectFrom(post)
                .join(post.admin).fetchJoin()
                .join(post.category).fetchJoin()
                .where(condition)
                .orderBy(post.updateDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = queryFactory.select(post.count())
                .from(post)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(posts, pageable, count != null ? count : 0L);
    }
}
