package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardDto;
import com.moya.myblogboot.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;

    public static final int LIMIT = 3;

    // 해당 카테고리 모든 게시글 가져오기
    public List<Board> getAllBoardsInThatCategory(String category, int page){
        List<Board> list = boardRepository.findAllBoardsInThatCategory(category, pagination(page), LIMIT);
        return list;
    }
    // 선택한 게시글 가져오기
    public Board getBoard(Long boardId){
        Board board = boardRepository.findOne(boardId).orElseThrow(
                () -> new IllegalStateException("해당 게시글이 존재하지 않습니다.")
        );
        return board;
    }
    @Transactional
    public Long editBoard(Long boardId, BoardDto boardDto){
        Board board = boardRepository.findOne(boardId).orElseThrow(
                ()-> new IllegalStateException("해당 게시글이 존재하지 않습니다.")
                );

        return board.getId();
    }
    @Transactional
    public int deleteBoard(int bidx){
        int result = 0;

        return result;
    }

    @Transactional
    public long newPost(BoardDto boardDto, Admin admin) {
        Board board= Board.builder()
                .admin(admin)
                .title(boardDto.getTitle())
                .content(boardDto.getContent())
                .category(boardDto.getCategory()).build();
        return boardRepository.upload(board);
    }

    public List<Board> getBoardList(int page) {
        List<Board> list = null;
        list = boardRepository.findAll(pagination(page), LIMIT);
        return list;
    }

    private int pagination (int page){
        if (page == 1) return 0;
        return (page - 1) * LIMIT;
    }
}
