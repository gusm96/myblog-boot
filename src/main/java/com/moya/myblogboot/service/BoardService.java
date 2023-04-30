package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardReq;
import com.moya.myblogboot.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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

        Board board = boardRepository.findOne(idx).orElseThrow(
                () -> new IllegalStateException("해당 게시글이 존재하지 않습니다.")
        );

        return board;
    }
    @Transactional
    public Long editBoard(long idx, BoardReq boardReq) {
        Date editDate = new Date();
        System.out.println(editDate.toString());
        Board board = boardRepository.findOne(idx).orElseThrow(
                () -> new IllegalStateException("해당 게시글이 존재하지 않습니다.")
        );
        // 변경감지
        board.setBoard_type(boardReq.getBoard_type());
        board.setTitle(boardReq.getTitle());
        board.setContent(boardReq.getContent());
        return board.getBidx();
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
