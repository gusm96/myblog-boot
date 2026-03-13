package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.SearchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface BoardQuerydslRepository {

    List<Board> findByDeleteDate(LocalDateTime deleteDate);
    Page<Board> findBySearchType(Pageable pageable, SearchType searchType, String contents);
}
