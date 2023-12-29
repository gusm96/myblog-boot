package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.*;
import java.time.LocalDateTime;

public interface BoardService {

    BoardListResDto retrieveBoardList(int page);

    BoardListResDto retrieveBoardListByCategory(String categoryName, int page);

    BoardListResDto retrieveBoardListBySearch(SearchType searchType, String searchContents, int page);

    Board retrieveBoardById(Long boardId);

    BoardResDtoV2 retrieveBoardDetail(Long boardId);

    BoardListResDto retrieveDeletedBoards(int page);

    Long uploadBoard(BoardReqDto boardReqDto, Long memberId);

    Long editBoard(Long memberId, Long boardId, BoardReqDto boardReqDto);

    void undeleteBoard(Long boardId);

    boolean deleteBoard(Long boardId, Long memberId);

    void deletePermanently(Long boardId);

    void deletePermanently(LocalDateTime thresholdDate);

}

