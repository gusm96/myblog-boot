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
        String key = MEMBER_BOARD_LIKE_KEY + memberId;
        redisTemplate.opsForSet().add(key, boardId);
    }

    @Override
    public boolean isMember(Long memberId, Long boardId) {
        String key = MEMBER_BOARD_LIKE_KEY + memberId;
        return redisTemplate.opsForSet().isMember(key, boardId);
    }

    @Override
    public void delete(Long memberId, Long boardId) {
        String key = MEMBER_BOARD_LIKE_KEY + memberId;
        redisTemplate.opsForSet().remove(key, boardId);
    }
}
