package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.BoardRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ConvertingCursor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class BoardRedisRepositoryImpl implements BoardRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BOARD_LIKE_KEY = "likes:";
    private static final String BOARD_VIEWS_KEY = "views:";
    @Override
    public void addLike(Long boardId, Long memberId) {
        String key = BOARD_LIKE_KEY + boardId;
        redisTemplate.opsForSet().add(key, memberId);
    }

    @Override
    public boolean isMember(Long boardId, Long memberId) {
        String key = BOARD_LIKE_KEY + boardId;
        return redisTemplate.opsForSet().isMember(key, memberId);
    }

    @Override
    public void likesCancel(Long boardId, Long memberId) {
        String key = BOARD_LIKE_KEY + boardId;
        redisTemplate.opsForSet().remove(key, memberId);
    }

    @Override
    public Long getLikesCount(Long boardId) {
        String key = BOARD_LIKE_KEY + boardId;
        return (long) redisTemplate.opsForSet().members(key).size();
    }

    @Override
    public List<Long> getKeysValues(String key) {
        Set<String> keys = redisTemplate.keys(key+"*");

        List<Long> values = new ArrayList<>();
        for (String k : keys) {
            // 'likes:' 부분을 잘라내고 나머지 부분을 Long으로 변환하여 리스트에 추가
            values.add(Long.parseLong(k.substring(key.length())));
        }
        return values;
    }

    @Override
    public List<Long> getLikesMembers(Long boardId) {
        String key = BOARD_LIKE_KEY + boardId;
        Set<Object> members = redisTemplate.opsForSet().members(key);

        return members.stream()
                .map(member -> Long.parseLong(member.toString()))
                .collect(Collectors.toList());
    }
    @Override
    public void deleteLikes(Long boardId) {
        String key = BOARD_LIKE_KEY + boardId;
        redisTemplate.delete(key);
    }

    @Override
    public Long getViews(Long boardId) {
        String key = BOARD_VIEWS_KEY + boardId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return Long.parseLong(value.toString());
        }else{
            return null;
        }
    }

    @Override
    public Long viewsIncrement(Long boardId) {
        String key = BOARD_VIEWS_KEY + boardId;
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Long setViews(Long boardId, Long views) {
        String key = BOARD_VIEWS_KEY + boardId;
        redisTemplate.opsForValue().set(key, views);
        return getViews(boardId);
    }

    @Override
    public void deleteViews(Long boardId) {
        String key = BOARD_VIEWS_KEY + boardId;
        redisTemplate.delete(key);
    }
}