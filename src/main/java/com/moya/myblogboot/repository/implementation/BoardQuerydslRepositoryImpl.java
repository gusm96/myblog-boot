package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.board.QBoard;
import com.moya.myblogboot.repository.BoardQuerydslRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class BoardQuerydslRepositoryImpl implements BoardQuerydslRepository {
    private final EntityManager em;
    private JPAQueryFactory queryFactory;

    @Override
    public void deleteWithinPeriod(LocalDateTime deleteDate) {
        queryFactory = new JPAQueryFactory(em);
        queryFactory.delete(QBoard.board)
                .where(QBoard.board.deleteDate.loe(deleteDate))
                .execute();
    }
}
