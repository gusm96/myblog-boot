package com.moya.myblogboot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardLikeService {

    public void likeBoard(Long boardId, Long guestId) {
        String key = "board_likes: " + boardId;

    }
}
