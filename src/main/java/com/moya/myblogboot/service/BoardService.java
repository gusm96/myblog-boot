package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardReq;
import com.moya.myblogboot.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    public static final int LIMIT = 3;

    public List<Board> getAllPostsOfThatType(int type, int page){
        List<Board> list = boardRepository.findAllPostsOfThatType(type, pagination(page), LIMIT);

        return list;
    }

    public Board getBoard(Long idx){

        Board board = boardRepository.findOne(idx).orElseThrow();

        return board;
    }

    public int editBoard(Board board) {
        int result = 0;

        return result;
    }

    public int deleteBoard(int bidx){
        int result = 0;

        return result;
    }

    @Transactional
    public long newPost(BoardReq boardReq) {
        long bidx = 0;
        Board board = Board.builder()
                // Token으로 Admin idx값 추출
                // .aidx(boardReq.getAidx())
                .board_type(boardReq.getBoard_type())
                .title(boardReq.getTitle())
                .content(boardReq.getContent())
                .build();
        bidx = boardRepository.upload(board);
        return bidx;
    }

    @Transactional
    public List<Board> getAllPost(int page) {
        List<Board> list = null;
        list = boardRepository.findAllPosts(pagination(page), LIMIT);
        return list;
    }

    private int pagination (int page){
        if (page == 1) return 0;
        return (page - 1) * LIMIT;
    }
}
