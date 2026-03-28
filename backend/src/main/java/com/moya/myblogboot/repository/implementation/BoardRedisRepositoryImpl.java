package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.dto.board.BoardForRedis;
import com.moya.myblogboot.repository.BoardRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.moya.myblogboot.domain.keys.RedisKey.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BoardRedisRepositoryImpl implements BoardRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final long CACHE_TTL_SECONDS = 60 * 60L; // 1시간

    @Override
    public Set<Long> getKeys(String pattern) {
        return redisTemplate.execute((RedisCallback<Set<Long>>) connection -> {
            Set<Long> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next());
                    keys.add(Long.parseLong(key.split(":")[1]));
                }
            }
            return keys;
        });
    }

    @Override
    public void saveClientIp(String key) {
        redisTemplate.opsForValue().set(key, "", 24, TimeUnit.HOURS);
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
        redisTemplate.expire(viewsKey, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
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
        redisTemplate.expire(likesKey, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
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
        redisTemplate.expire(likesKey, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
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

    @Override
    public boolean isDuplicateBoardViewCount(String key) {
        return redisTemplate.hasKey(key);
    }

    private BoardForRedis getBoardForRedis(String key) {
        return (BoardForRedis) redisTemplate.opsForValue().get(key);
    }

    private void setBoardForRedis(String key, BoardForRedis boardForRedis) {
        redisTemplate.opsForValue().set(key, boardForRedis, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
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