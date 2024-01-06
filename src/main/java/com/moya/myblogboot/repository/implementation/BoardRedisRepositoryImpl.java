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
        String key = KEY + boardId;
        BoardForRedis boardForRedis = getBoardForRedis(key);
        if (boardForRedis == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(boardForRedis);
        }
    }

    @Override
    public BoardForRedis incrementViews(BoardForRedis boardForRedis) {
        String key = KEY + boardForRedis.getId();
        String viewsKey = key + VIEWS_KEY;
        Long updateViews = redisTemplate.opsForValue().increment(viewsKey);
        boardForRedis.setUpdateViews(updateViews);
        // 수정된 데이터 저장.
        setBoardForRedis(key, boardForRedis);
        return boardForRedis;
    }

    @Override
    public BoardForRedis save(Board board) {
        String key = KEY + board.getId();
        Set<Long> memberIds = board.getBoardLikes().stream().map(boardLike
                -> boardLike.getMember().getId()).collect(Collectors.toSet());
        BoardForRedis boardForRedis = BoardForRedis.builder().board(board).memberIds(memberIds).build();
        setBoardForRedis(key, boardForRedis);
        return boardForRedis;
    }

    @Override
    public void delete(Long boardId) {
        String key = KEY + boardId;
        String viewsKey = key + VIEWS_KEY;
        redisTemplate.delete(key);
        redisTemplate.delete(viewsKey);
    }

    @Override
    public boolean existsMember(Long boardId, Long memberId) {
        String key = KEY + boardId;
        BoardForRedis boardForRedis = getBoardForRedis(key);
        return boardForRedis.getLikes().contains(memberId);
    }

    @Override
    public Long addLike(Long boardId, Long memberId) {
        String key = KEY + boardId;
        BoardForRedis boardForRedis = getBoardForRedis(key);
        boardForRedis.addLike(memberId);
        setBoardForRedis(key, boardForRedis);
        return (long) boardForRedis.getLikes().size();
    }

    @Override
    public Long deleteMembers(Long boardId, Long memberId) {
        String key = KEY + boardId;
        BoardForRedis boardForRedis = getBoardForRedis(key);
        boardForRedis.cancelLike(memberId);
        setBoardForRedis(key, boardForRedis);
        return (long) boardForRedis.getLikes().size();
    }

    @Override
    public void update(Board board) {
        String key = KEY + board.getId();
        BoardForRedis boardForRedis = getBoardForRedis(key);
        if (boardForRedis != null) {
            boardForRedis.update(board);
            setBoardForRedis(key, boardForRedis);
        }else{
            save(board);
        }
    }
    private BoardForRedis getBoardForRedis(String key) {
        return (BoardForRedis) redisTemplate.opsForValue().get(key);
    }
    private void setBoardForRedis(String key, BoardForRedis boardForRedis) {
        redisTemplate.opsForValue().set(key, boardForRedis);
    }
}