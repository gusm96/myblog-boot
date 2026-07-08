package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.TagQuerydslRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.moya.myblogboot.domain.post.QPost.post;
import static com.moya.myblogboot.domain.tag.QPostTag.postTag;
import static com.moya.myblogboot.domain.tag.QTag.tag;

@Repository
@RequiredArgsConstructor
public class TagQuerydslRepositoryImpl implements TagQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findTagIdsWithMismatchedPostCount() {
        return queryFactory.select(tag.id)
                .from(tag)
                .fetch()
                .stream()
                .filter(tagId -> countActivePostsByTagId(tagId) != findPostCount(tagId))
                .toList();
    }

    @Override
    public int countActivePostsByTagId(Long tagId) {
        Long count = queryFactory.select(postTag.count())
                .from(postTag)
                .join(postTag.post, post)
                .where(postTag.tag.id.eq(tagId)
                        .and(post.deleteDate.isNull()))
                .fetchOne();
        return count == null ? 0 : count.intValue();
    }

    private int findPostCount(Long tagId) {
        Integer count = queryFactory.select(tag.postCount)
                .from(tag)
                .where(tag.id.eq(tagId))
                .fetchOne();
        return count == null ? 0 : count;
    }
}
