package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Board;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardService {

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

    public String newPost(Board board) {
        String result = "";

        return result;
    }
}
