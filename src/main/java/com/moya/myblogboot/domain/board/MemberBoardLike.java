package com.moya.myblogboot.domain.board;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;


@Getter
@Builder
@AllArgsConstructor
@RedisHash(value = "memberBoardLike")
public class MemberBoardLike {
    @Id
    private Long id;
    @Indexed
    private Long boardId;
}
