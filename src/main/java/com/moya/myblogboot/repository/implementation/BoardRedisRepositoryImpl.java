package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardForRedis;
import com.moya.myblogboot.repository.BoardRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class BoardRedisRepositoryImpl implements BoardRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY = "board:";
    private static final String VIEWS_KEY = ":views";

    @Override
    public Set<Long> getKeysValues(String key) {
        Set<String> keys = redisTemplate.keys(key + "*");
        Set<Long> values = new HashSet<>();
        for (String k : keys) {
            // 'likes:' 부분을 잘라내고 나머지 부분을 Long으로 변환하여 리스트에 추가
            String resultKey = k.split(":")[1];
            values.add(Long.parseLong(resultKey));
        }
        return values;
    }

    @Override
    public Optional<BoardForRedis> findOne(Long boardId) {
        String key = getKey(boardId);
        BoardForRedis boardForRedis = getBoardForRedis(key);
        if (boardForRedis == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(boardForRedis);
        }
    }

    @Override
    public BoardForRedis incrementViews(BoardForRedis board) {
        String key = getKey(board.getId());
        String viewsKey = getViewsKey(key);
        Long updateViews = redisTemplate.opsForValue().increment(viewsKey);
        board.setUpdateViews(updateViews);
        // 수정된 데이터 저장.
        setBoardForRedis(key, board);
        return board;
    }

    @Override
    public BoardForRedis save(Board board) {
        String key = getKey(board.getId());
        Set<Long> memberIds = board.getBoardLikes().stream().map(boardLike
                -> boardLike.getMember().getId()).collect(Collectors.toSet());
        BoardForRedis boardForRedis = BoardForRedis.builder().board(board).memberIds(memberIds).build();
        setBoardForRedis(key, boardForRedis);
        return boardForRedis;
    }

    @Override
    public void delete(BoardForRedis board) {
        String key = getKey(board.getId());
        String viewsKey = getViewsKey(key);
        redisTemplate.delete(key);
        redisTemplate.delete(viewsKey);
    }

    @Override
    public boolean existsMember(Long boardId, Long memberId) {
        String key = getKey(boardId);
        BoardForRedis boardForRedis = getBoardForRedis(key);
        return boardForRedis.getLikes().contains(memberId);
    }

    @Override
    public void update(BoardForRedis board) {
        String key = getKey(board.getId());
        setBoardForRedis(key, board);
    }

    private BoardForRedis getBoardForRedis(String key) {
        return (BoardForRedis) redisTemplate.opsForValue().get(key);
    }

    private void setBoardForRedis(String key, BoardForRedis boardForRedis) {
        redisTemplate.opsForValue().set(key, boardForRedis);
    }

    private String getKey(Long boardId) {
        return KEY + boardId;
    }

    private String getViewsKey(String key) {
        return key + VIEWS_KEY;
    }
}