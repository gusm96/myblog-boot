package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.domain.member.Member;

import java.util.List;

public interface BoardService {

    BoardListResDto retrieveBoardList(int page);

    BoardListResDto retrieveBoardListByCategory(Category category, int page);

    BoardListResDto retrieveBoardListBySearch(SearchType searchType, String searchContents, int page);

    BoardResDto retrieveBoardResponseById(Long boardId);

    Long editBoard(Long memberId, Long boardId, String modifiedTitle, String modifiedContent, Category modifiedCategory);

    boolean deleteBoard(Long boardId, Long memberId);

    Long uploadBoard(BoardReqDto boardReqDto, Member member, Category category);

    Long addLikeToBoard(Long memberId, Long boardId);

    boolean checkBoardLikedStatus(Long memberId, Long boardId);

    Long deleteBoardLike(Long memberId, Long boardId);

    Board retrieveBoardById(Long boardId);

    void saveImageFile(List<ImageFileDto> images, Board board);

    Long addBoardLikeVersion2(Long boardId, Member member);
}

