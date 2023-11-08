package com.moya.myblogboot.domain.board;


import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

@Data
@RedisHash(value = "boardLikeCount")
public class BoardLikeCount {
    @Id
    private Long id;
    private Long count;

    @Builder
    public BoardLikeCount(Long id) {
        this.id = id;
        this.count = 0L;
    }
    public void increment() {
        this.count++;
    }

    public void decrement() {
        if(this.count > 0) this.count--;
    }
}

