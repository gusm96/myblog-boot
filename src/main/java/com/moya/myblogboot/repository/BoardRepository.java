package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardStatus;
import com.moya.myblogboot.domain.category.Category;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> ,BoardQuerydslRepository {

    @Query("select distinct b from Board b " +
            "join fetch b.member " +
            "left join fetch b.boardLikes boardLike " +
            "left join fetch boardLike.member " +
            "join fetch b.category where b.id = :boardId ")
    Optional<Board> findById(@Param("boardId") Long boardId);

    @Query("SELECT b FROM Board b WHERE b.boardStatus = :boardStatus")
    Page<Board> findAll(@Param("boardStatus") BoardStatus boardStatus,Pageable pageable);

    @Query("select b from Board b where b.category.name = : category")
    Page<Board> findAllByCategory(String category, PageRequest pageRequest);

    @Query("select b from Board b where b.deleteDate is not null")
    Page<Board> findByDeletionStatus(PageRequest pageRequest);
}
