package com.moya.myblogboot.domain.board;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("BoardLike")
public class BoardLike {
    @Id
    private String id;
    private Long boardId;
    private Long guestId;
}
