package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.board.BoardLike;
import com.moya.myblogboot.repository.BoardLikeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BoardLikeRepositoryImpl implements BoardLikeRepository {
    private final EntityManager em;

    @Override
    public void save(BoardLike boardLike) {
        em.persist(boardLike);
    }

    @Override
    public Optional<BoardLike> findByBoardId(Long boardId) {
        try {
            BoardLike boardLike = em.createQuery("select b from BoardLike b where b.board.id =: boardId", BoardLike.class)
                    .setParameter("boardId", boardId).getSingleResult();
            return Optional.ofNullable(boardLike);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
