package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.repository.UserViewedBoardRedisRepository;
import com.moya.myblogboot.service.UserViewedBoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserUserViewedBoardServiceImpl implements UserViewedBoardService {
    private final UserViewedBoardRedisRepository userViewedBoardRedisRepository;

    @Override
    public void addViewedBoard(Long userNum, Long boardId) {
        try {
            userViewedBoardRedisRepository.save(userNum, boardId);
        } catch (Exception e) {
            log.error("Redis Store - 조회한 게시글 추가중 오류발생 : {}", e.getMessage());
        }
    }

    @Override
    public boolean isViewedBoard(Long userNum, Long boardId) {
        boolean result = false;
        try {
            result = userViewedBoardRedisRepository.isExists(userNum, boardId);
        } catch (Exception e) {
            log.error("Redis Store - 조회한 게시글 조회중 오류발생 : {}", e.getMessage());
        }
        return result;
    }
}
