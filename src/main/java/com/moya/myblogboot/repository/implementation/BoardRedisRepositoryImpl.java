package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardForRedis;
import com.moya.myblogboot.repository.BoardRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.moya.myblogboot.domain.keys.RedisKey.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BoardRedisRepositoryImpl implements BoardRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Set<Long> getKeys(String pattern) {
        //
        RedisKeyCommands keyCommands = redisTemplate.getRequiredConnectionFactory().getConnection().keyCommands();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
        Cursor<byte[]> cursor = keyCommands.scan(options);
        Set<Long> keys = new HashSet<>();
        while (cursor.hasNext()) {
            String key = new String(cursor.next());
            keys.add(Long.parseLong(key.split(":")[1]));
        }
        return keys;
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
    public BoardForRedis incrementLikes(BoardForRedis board) {
        String key = getKey(board.getId());
        String likesKey = getLikesKey(key);
        Long updateLikes = redisTemplate.opsForValue().increment(likesKey);
        board.setUpdateLikes(updateLikes);
        // 수정된 데이터 저장.
        setBoardForRedis(key, board);
        return board;
    }

    @Override
    public BoardForRedis decrementLikes(BoardForRedis board) {
        String key = getKey(board.getId());
        String likesKey = getLikesKey(key);
        Long updateLikes = redisTemplate.opsForValue().decrement(likesKey);
        board.setUpdateLikes(updateLikes);
        // 수정된 데이터 저장.
        setBoardForRedis(key, board);
        return board;
    }

    @Override
    public BoardForRedis save(Board board) {
        String key = getKey(board.getId());
        BoardForRedis boardForRedis = BoardForRedis.builder().board(board).build();
        setBoardForRedis(key, boardForRedis);
        return boardForRedis;
    }

    @Override
    public void update(BoardForRedis board) {
        String key = getKey(board.getId());
        setBoardForRedis(key, board);
    }

    @Override
    public void delete(BoardForRedis board) {
        String key = getKey(board.getId());
        String viewsKey = getViewsKey(key);
        String likesKey = getLikesKey(key);
        redisTemplate.delete(key);
        redisTemplate.delete(viewsKey);
        redisTemplate.delete(likesKey);
    }

    private BoardForRedis getBoardForRedis(String key) {
        return (BoardForRedis) redisTemplate.opsForValue().get(key);
    }

    private void setBoardForRedis(String key, BoardForRedis boardForRedis) {
        redisTemplate.opsForValue().set(key, boardForRedis);
    }

    private String getKey(Long boardId) {
        return BOARD_KEY + boardId;
    }

    private String getViewsKey(String key) {
        return key + BOARD_VIEWS_KEY;
    }

    private String getLikesKey(String key) {
        return key + BOARD_LIKES_KEY;
    }

}