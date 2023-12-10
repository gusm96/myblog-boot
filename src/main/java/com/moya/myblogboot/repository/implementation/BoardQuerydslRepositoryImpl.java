package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.QBoard;
import com.moya.myblogboot.domain.board.SearchType;
import com.moya.myblogboot.repository.BoardQuerydslRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public Page<Board> findBySearchType (PageRequest pageRequest, SearchType searchType, String searchContents){

       /* queryFactory = new JPAQueryFactory(em);
        Page<Board> boards = queryFactory.selectFrom(board)
                    .where(searchType == SearchType.TITLE ? board.title.like(searchContents) : board.content.like(searchContents))
                */
        return null;
    }
}
