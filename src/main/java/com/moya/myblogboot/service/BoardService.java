package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.*;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final CategoryRepository categoryRepository;

    public static final int LIMIT = 20;
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
        Board board = boardRepository.findOne(boardId).orElseThrow(
                () -> new IllegalStateException("해당 게시글이 존재하지 않습니다.")
        );
        BoardResDto boardResDto = BoardResDto.builder().board(board).build();
        return boardResDto;
    }
    // 게시글 수정
    @Transactional
    public Long editBoard(Long boardId, BoardReqDto boardReqDto){
        Board board = boardRepository.findOne(boardId).orElseThrow(
                ()-> new IllegalStateException("해당 게시글이 존재하지 않습니다.")
                );
        Category category = categoryRepository.findOne(boardReqDto.getCategory()).orElseThrow(
                () -> new IllegalStateException("존재하지 않는 카테고리 입니다."));
        board.updateBoard(category, boardReqDto.getTitle(), boardReqDto.getContent());
        // 변경 감지
        return board.getId();
    }
    // 게시글 삭제
    @Transactional
    public String deleteBoard(Long boardId){
        String result = "";

        return result;
    }
    // 게시글 업로드
    @Transactional
    public long uploadBoard(BoardReqDto boardReqDto) {
        Category category = categoryRepository.findOne(boardReqDto.getCategory()).orElseThrow(() -> new IllegalStateException("존재하지 않는 카테고리 입니다."));
        Board board = boardReqDto.toEntity(category);
        return boardRepository.upload(board);
    }

    private int pagination (int page){
        if (page == 1) return 0;
        return (page - 1) * LIMIT;
    }
}
