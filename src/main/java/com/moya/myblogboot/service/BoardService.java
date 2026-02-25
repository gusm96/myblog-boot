package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.dto.board.BoardDetailResDto;
import com.moya.myblogboot.dto.board.BoardForRedis;
import com.moya.myblogboot.dto.board.BoardListResDto;
import com.moya.myblogboot.dto.board.BoardReqDto;

import java.time.LocalDateTime;

public interface BoardService {

    BoardListResDto retrieveAll(int page);

    BoardListResDto retrieveAllByCategory(String categoryName, int page);

    BoardListResDto retrieveAllBySearched(SearchType searchType, String searchContents, int page);

    Board findById(Long boardId);

    BoardDetailResDto getBoardDetail(Long boardId);

    BoardDetailResDto getBoardDetailAndIncrementViews(Long boardId);

    BoardForRedis getBoardFromCache(Long boardId);

    BoardListResDto retrieveAllDeleted(int page);

    Long write(BoardReqDto boardReqDto, Long memberId);

    Long edit(Long memberId, Long boardId, BoardReqDto boardReqDto);

    void undelete(Long boardId, Long memberId);

    void delete(Long boardId, Long memberId);

    void deletePermanently(Long boardId);

    void deletePermanently(LocalDateTime thresholdDate);

    boolean isDuplicateBoardViewCount(String key);
}

