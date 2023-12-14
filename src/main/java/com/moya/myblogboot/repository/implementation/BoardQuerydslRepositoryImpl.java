package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.SearchType;
import com.moya.myblogboot.repository.BoardQuerydslRepository;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static com.moya.myblogboot.domain.board.QBoard.*;

@Repository
@RequiredArgsConstructor
public class BoardQuerydslRepositoryImpl implements BoardQuerydslRepository {
    private final EntityManager em;
    private JPAQueryFactory queryFactory;

    @Override
    public void deleteWithinPeriod(LocalDateTime deleteDate) {
        queryFactory = new JPAQueryFactory(em);
        queryFactory.delete(board)
                .where(board.deleteDate.loe(deleteDate))
                .execute();
    }

    @Override
    public QueryResults<Board> findBySearchType (int page, int limit , SearchType searchType, String contents){
        queryFactory = new JPAQueryFactory(em);
        return queryFactory.selectFrom(board)
                .where(searchType == SearchType.TITLE ? board.title.eq(contents) : board.content.like(contents))
                .orderBy(board.uploadDate.desc())
                .offset(page)
                .limit(limit)
                .fetchResults();
    }
}
