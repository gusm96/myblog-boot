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
    public Set<Long> getKeysValues(String key) {
        Set<String> keys = redisTemplate.keys(key+"*");
        Set<Long> values = new HashSet<>();
        for (String k : keys) {
            // 'likes:' 부분을 잘라내고 나머지 부분을 Long으로 변환하여 리스트에 추가
            String resultKey = k.split(":")[1];
            values.add(Long.parseLong(resultKey));
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
    @Override
    public Optional<BoardForRedis> findById(Long boardId) {
        String key = "board:" + boardId;
        String viewsKey = key + ":views";
        BoardForRedis findBoard = (BoardForRedis) redisTemplate.opsForValue().get(key);
        if (findBoard == null)
            return Optional.empty();
        try{
            Long updateViews = redisTemplate.opsForValue().increment(viewsKey);
            findBoard.setUpdateViews(updateViews);
            redisTemplate.opsForValue().set(key, findBoard);
            return Optional.ofNullable(findBoard);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public Optional<BoardForRedis> findOne(Long boardId) {
        String key = "board:" + boardId;
        BoardForRedis boardForRedis = (BoardForRedis) redisTemplate.opsForValue().get(key);
        if(boardForRedis == null){
            return Optional.empty();
        }else {
            return Optional.ofNullable(boardForRedis);
        }
    }

    @Override
    public BoardForRedis save(Board board) {
        String key = "board:" + board.getId();
        String viewsKey = key + ":views";
        Set<Long> memberIds = board.getBoardLikes().stream().map(boardLike -> boardLike.getMember().getId()).collect(Collectors.toSet());
        BoardForRedis boardForRedis = BoardForRedis.builder()
                .board(board)
                .memberIds(memberIds)
                .build();
        try {
            Long updateViews = redisTemplate.opsForValue().increment(viewsKey);
            boardForRedis.setUpdateViews(updateViews);
            redisTemplate.opsForValue().set(key, boardForRedis);
            return boardForRedis;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }


    @Override
    public void delete(Long boardId) {
        String key = "board:" + boardId;
        String viewsKey = key + ":views";
        redisTemplate.delete(key);
        redisTemplate.delete(viewsKey);
    }

    @Override
    public boolean existsMember(Long boardId, Long memberId) {
        String key = "board:" + boardId;
        BoardForRedis boardForRedis = (BoardForRedis) redisTemplate.opsForValue().get(key);
        return boardForRedis.getLikes().contains(memberId);
    }

    @Override
    public Long addLikeV2(Long boardId, Long memberId) {
        String key = "board:" + boardId;
        String viewsKey = "views";
        BoardForRedis boardForRedis = (BoardForRedis) redisTemplate.opsForValue().get(key);
        boardForRedis.addLike(memberId);
        redisTemplate.opsForValue().set(key, boardForRedis);
        return (long) boardForRedis.getLikes().size();
    }

    @Override
    public Long deleteMembers(Long boardId, Long memberId) {
        String key = "board:" + boardId;
        BoardForRedis boardForRedis = (BoardForRedis) redisTemplate.opsForValue().get(key);
        boardForRedis.cancelLike(memberId);
        redisTemplate.opsForValue().set(key, boardForRedis);
        return (long) boardForRedis.getLikes().size();
    }
}