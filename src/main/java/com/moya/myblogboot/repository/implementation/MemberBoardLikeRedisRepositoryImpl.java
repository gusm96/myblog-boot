package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.MemberBoardLikeRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberBoardLikeRedisRepositoryImpl implements MemberBoardLikeRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String MEMBER_BOARD_LIKE_KEY = "memberBoardLike:";
    @Override
    public void save(Long memberId, Long boardId) {
        redisTemplate.opsForSet().add(MEMBER_BOARD_LIKE_KEY + memberId, boardId);
    }

    @Override
    public boolean isMember(Long memberId, Long boardId) {
        return redisTemplate.opsForSet().isMember(MEMBER_BOARD_LIKE_KEY + memberId, boardId);
    }

    @Override
    public void delete(Long memberId, Long boardId) {
        redisTemplate.opsForSet().remove(MEMBER_BOARD_LIKE_KEY + memberId, boardId);
    }
}