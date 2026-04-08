package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardStatus;
import com.moya.myblogboot.domain.category.Category;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardQuerydslRepository {

    @Query("select b from Board b " +
            "join fetch b.member " +
            "join fetch b.category where b.id = :boardId")
    Optional<Board> findById(@Param("boardId") Long boardId);

    @Query(value = "select b from Board b join fetch b.member join fetch b.category where b.boardStatus = :boardStatus",
            countQuery = "select count(b) from Board b where b.boardStatus = :boardStatus")
    Page<Board> findAll(@Param("boardStatus") BoardStatus boardStatus, Pageable pageable);

    @Query(value = "select b from Board b join fetch b.member join fetch b.category " +
            "where b.category.name = :categoryName and b.boardStatus = 'VIEW'",
            countQuery = "select count(b) from Board b where b.category.name = :categoryName and b.boardStatus = 'VIEW'")
    Page<Board> findAllByCategoryName(@Param("categoryName") String categoryName, PageRequest pageRequest);

    @Query(value = "select b from Board b join fetch b.member join fetch b.category " +
            "where b.deleteDate is not null",
            countQuery = "select count(b) from Board b where b.deleteDate is not null")
    Page<Board> findByDeletionStatus(PageRequest pageRequest);
}
