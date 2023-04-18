package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Board;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BoardRepository implements BoardRepositoryInf {

    private final EntityManager em;

    @Override
    public Long upload(Board board) {
        em.persist(board);
        return board.getBidx();
    }

    @Override
    public Optional<Board> findOne(long idx) {
       Board board =  em.find(Board.class, idx);
        return Optional.ofNullable(board);
    }

    @Override
    public List<Board> findAll(int board_type) {
        List<Board> boards = em.createQuery(
                "select b.id, b.title, b.upload_date from Board b where b.board_type=:idx"
                        , Board.class)
                .setParameter("idx", board_type)
                .getResultList();
        return boards;
    }

    @Override
    public List<Board> findRecentPosts() {
        List<Board> boards = em.createQuery(
                "select b.id, b.title, b.upload_date from Board b order by b.upload_date desc limit 20"
                , Board.class
        ).getResultList();
        return boards;
    }
}
