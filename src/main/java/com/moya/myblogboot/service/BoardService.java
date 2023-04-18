package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    public List<Board> getBoardList(String type, int page){
        List<Board> list = null;

        return list;
    }

    public Board getBoard(int bidx){
        Board board = null;

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
    public long newPost(Board board) {
        long bidx = 0;
        bidx = boardRepository.upload(board);
        return bidx;
    }

    @Transactional
    public List<Board> getRecentPosts() {
        List<Board> list = null;
        // 최근 게시글 20개를 return 한다.

        return list;
    }
}
