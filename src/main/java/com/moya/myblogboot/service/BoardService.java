package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.*;
import com.moya.myblogboot.exception.ExpiredTokenException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.utils.JwtUtil;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final CategoryService categoryService;
    private final BoardRepository boardRepository;
    private final AdminRepository adminRepository;
    public static final int LIMIT = 5;
    // 모든 게시글 찾기
    public List<BoardResDto> getBoardList(int page) {
        List<Board> boardList = null;
        boardList = boardRepository.findAll(pagination(page), LIMIT);
        List<BoardResDto> resultList = boardList.stream().map(BoardResDto::of).toList();
        return resultList;
    }

    // 해당 카테고리 모든 게시글 가져오기
    public List<BoardResDto> getAllBoardsInThatCategory(String category, int page){
        List<Board> boardList = boardRepository.findAllBoardsInThatCategory(category, pagination(page), LIMIT);
        List<BoardResDto> resultList = boardList.stream().map(BoardResDto::of).toList();
        return resultList;
    }
    // 선택한 게시글 가져오기
    public BoardResDto getBoard(Long boardId){
        Board board = findBoard(boardId);
        BoardResDto boardResDto = BoardResDto.builder().board(board).build();
        return boardResDto;
    }
    // 게시글 수정
    @Transactional
    public Long editBoard(Long boardId, BoardReqDto boardReqDto){
        Board board = findBoard(boardId);
        Category category = categoryService.findCategory(boardReqDto.getCategory());
        board.updateBoard(category, boardReqDto.getTitle(), boardReqDto.getContent());
        // 변경 감지
        return board.getId();
    }
    // 게시글 삭제
    @Transactional
    public Boolean deleteBoard(Long boardId){
        try {
            Board board = findBoard(boardId);
            boardRepository.removeBoard(board);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    // 게시글 업로드
    @Transactional
    public long uploadBoard(BoardReqDto boardReqDto, String adminName) {
        Admin admin = adminRepository.findById(adminName).orElseThrow(() ->
                new NoResultException("해당 관리자는 존재하지 않습니다."));
        Category category = categoryService.findCategory(boardReqDto.getCategory());
        Board board = boardReqDto.toEntity(category, admin);
        return boardRepository.upload(board);
    }

    public Board findBoard(Long boardId) {
        return boardRepository.findOne(boardId).orElseThrow(
                () -> new IllegalStateException("해당 게시글이 존재하지 않습니다.")
        );
    }

    public List<BoardResDto> getSearchBoards(SearchType searchType, String searchContents, int page) {
        List<Board> list = boardRepository.findBySearch(searchType, searchContents, pagination(page), LIMIT);
        List<BoardResDto> resultList = list.stream().map(BoardResDto::of).toList();
        return resultList;
    }

    // 페이지값
    private int pagination (int page){
        if (page == 1) return 0;
        return (page - 1) * LIMIT;
    }
}
