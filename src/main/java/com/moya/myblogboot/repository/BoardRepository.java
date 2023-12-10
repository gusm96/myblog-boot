package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.SearchType;
import com.moya.myblogboot.domain.category.Category;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> ,BoardQuerydslRepository {

    @Query("select distinct b from Board b " +
            "join fetch b.category " +
            "left join fetch b.comments where b.id = :boardId")
    Optional<Board> findById(@Param("boardId") Long boardId);

    Page<Board> findAll(Pageable pageable);

    Page<Board> findAllByCategory(Category category, PageRequest pageRequest);


}
