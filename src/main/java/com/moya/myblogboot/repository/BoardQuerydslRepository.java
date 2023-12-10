package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.SearchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

public interface BoardQuerydslRepository {

    void deleteWithinPeriod(LocalDateTime deleteDate);
    Page<Board> findBySearchType(PageRequest pageRequest, SearchType searchType, String searchContents);
}
