package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.SearchType;
import com.moya.myblogboot.repository.BoardQuerydslRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.moya.myblogboot.domain.board.QBoard.*;

@Repository
@RequiredArgsConstructor
public class BoardQuerydslRepositoryImpl implements BoardQuerydslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Board> findByDeleteDate(LocalDateTime deleteDate) {
        return queryFactory.selectFrom(board)
                .leftJoin(board.imageFiles).fetchJoin()
                .where(board.deleteDate.loe(deleteDate))
                .fetch();
    }

    @Override
    public Page<Board> findBySearchType(Pageable pageable, SearchType searchType, String contents) {
        BooleanExpression condition = searchType == SearchType.TITLE
                ? board.title.contains(contents)
                : board.content.contains(contents);

        List<Board> boards = queryFactory.selectFrom(board)
                .where(condition)
                .orderBy(board.updateDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = queryFactory.select(board.count())
                .from(board)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(boards, pageable, count != null ? count : 0L);
    }
}
