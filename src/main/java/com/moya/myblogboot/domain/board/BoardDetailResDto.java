package com.moya.myblogboot.domain.board;


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
    private LocalDateTime creatDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private BoardStatus boardStatus;


    @Builder
    public BoardDetailResDto(BoardForRedis boardForRedis) {
        this.id = boardForRedis.getId();
        this.title = boardForRedis.getTitle();
        this.content = boardForRedis.getContent();
        this.views = boardForRedis.getViews() + boardForRedis.getUpdateViews();
        this.likes = (long) boardForRedis.getLikes().size();
        this.creatDate = boardForRedis.getCreateDate();
        this.updateDate = boardForRedis.getUpdateDate();
        this.deleteDate = boardForRedis.getDeleteDate();
        this.boardStatus = boardForRedis.getBoardStatus();
    }
}
