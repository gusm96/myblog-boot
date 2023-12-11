package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.*;

public interface BoardService {

    BoardListResDto retrieveBoardList(int page);

    BoardListResDto retrieveBoardListByCategory(String categoryName, int page);

    BoardListResDto retrieveBoardListBySearch(SearchType searchType, String searchContents, int page);

    BoardDetailResDto boardToResponseDto(Long boardId);

    Long editBoard(Long memberId, Long boardId, BoardReqDto boardReqDto);

    boolean deleteBoard(Long boardId, Long memberId);

    Long uploadBoard(BoardReqDto boardReqDto, Long memberId);

    Board retrieveBoardById(Long boardId);
}

