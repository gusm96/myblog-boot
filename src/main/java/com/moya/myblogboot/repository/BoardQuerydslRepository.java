package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.SearchType;
import com.querydsl.core.QueryResults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

public interface BoardQuerydslRepository {

    void deleteWithinPeriod(LocalDateTime deleteDate);
    QueryResults<Board> findBySearchType(int page, int limit, SearchType searchType, String searchContents);
}
