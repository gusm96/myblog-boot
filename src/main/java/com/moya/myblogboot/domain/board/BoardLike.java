package com.moya.myblogboot.domain.board;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("BoardLike")
public class BoardLike {
    @Id
    private String id;
    private Long boardId;
    private Long guestId;
}
