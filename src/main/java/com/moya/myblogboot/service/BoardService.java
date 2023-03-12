package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.BoardDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardService {

    public List<BoardDTO> getBoardList(String type, int page){
        List<BoardDTO> list = null;

        return list;
    }

    public BoardDTO getBoard(int bidx){
        BoardDTO board = null;

        return board;
    }

    public int editBoard(BoardDTO board) {
        int result = 0;

        return result;
    }

    public int deleteBoard(int bidx){
        int result = 0;

        return result;
    }

    public String newPost(BoardDTO board) {
        String result = "";

        return result;
    }
}
