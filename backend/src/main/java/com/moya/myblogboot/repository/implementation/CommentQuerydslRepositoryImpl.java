package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.repository.CommentQuerydslRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

import static com.moya.myblogboot.domain.comment.QComment.*;

@Repository
@RequiredArgsConstructor
public class CommentQuerydslRepositoryImpl implements CommentQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommentResDto> findAllByPostId(Long postId) {
        List<Comment> comments = queryFactory.selectDistinct(comment1)
                .from(comment1)
                .leftJoin(comment1.child).fetchJoin()
                .where(comment1.post.id.eq(postId))
                .where(comment1.parent.isNull())
                .orderBy(comment1.createDate.desc())
                .fetch();
        return comments.stream().map(CommentResDto::of).collect(Collectors.toList());
    }

    @Override
    public List<CommentResDto> findChildByParentId(Long parentId) {
        List<Comment> comments = queryFactory.selectDistinct(comment1)
                .from(comment1)
                .leftJoin(comment1.parent).fetchJoin()
                .where(comment1.parent.id.eq(parentId))
                .orderBy(comment1.createDate.asc())
                .fetch();

        return comments.stream().map(CommentResDto::of).collect(Collectors.toList());
    }
}
