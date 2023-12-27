package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

public interface BoardService {

    BoardListResDto retrieveBoardList(int page);

    BoardListResDto retrieveBoardListByCategory(String categoryName, int page);

    BoardListResDto retrieveBoardListBySearch(SearchType searchType, String searchContents, int page);
    BoardDetailResDto boardToResponseDto(Long boardId);

    Long editBoard(Long memberId, Long boardId, BoardReqDto boardReqDto);

    boolean deleteBoard(Long boardId, Long memberId);

    Long uploadBoard(BoardReqDto boardReqDto, Long memberId);

    Board retrieveBoardById(Long boardId);

    BoardResDtoV2 retrieveBoardDetail(Long boardId);

    void deletePermanently(LocalDateTime thresholdDate);

    BoardListResDto retrieveDeletedBoards(int page);

    void undeleteBoard(Long boardId);
}

