package com.moya.myblogboot.dto.board;


import com.moya.myblogboot.domain.board.BoardStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardDetailResDto {

    private Long id;
    private String title;
    private String content;
    private Long views;
    private Long likes;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private BoardStatus boardStatus;


    @Builder
    public BoardDetailResDto(BoardForRedis boardForRedis) {
        this.id = boardForRedis.getId();
        this.title = boardForRedis.getTitle();
        this.content = boardForRedis.getContent();
        this.views = boardForRedis.totalViews();
        this.likes = boardForRedis.totalLikes();
        this.createDate = boardForRedis.getCreateDate();
        this.updateDate = boardForRedis.getUpdateDate();
        this.deleteDate = boardForRedis.getDeleteDate();
        this.boardStatus = boardForRedis.getBoardStatus();
    }
}
