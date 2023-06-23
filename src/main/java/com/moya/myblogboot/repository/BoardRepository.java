package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardResDto;
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
        return board.getId();
    }

    @Override
    public Optional<Board> findOne(Long id) {
       Board board =  em.find(Board.class, id);
        return Optional.ofNullable(board);
    }

    @Override
    public List<Board> findAllBoardsInThatCategory(String categoryName,int offset, int limit) {
        List<Board> boards = em.createQuery(
                        "select b from Board b " +
                                "where b.category.name=:categoryName " +
                                "order by b.upload_date desc "
                        , Board.class)
                .setParameter("categoryName", categoryName)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
        return boards;
    }

    @Override
    public List<Board> findAll(int offset, int limit) {
        List<Board> boards = em.createQuery("select b from Board b order by b.upload_date desc "
                        , Board.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
        return boards;
    }

    @Override
    public String deleteBoard(Long boardId) {
        String result = "";
        return result;
    }
}
