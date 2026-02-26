package com.moya.myblogboot.dto.board;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardForRedis {

    private Long id;
    private String title;
    private String content;
    private Long views;
    private Long updateViews;
    private Long likes;
    private Long updateLikes;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private BoardStatus boardStatus;


    @Builder
    public BoardForRedis(Board board) {
        this.id = board.getId();
        this.title = board.getTitle();
        this.content = board.getContent();
        this.views = board.getViews();
        this.updateViews = 0L;
        this.likes = board.getLikes() == null ? 0L : board.getLikes();
        this.updateLikes = 0L;
        this.createDate = board.getCreateDate();
        this.updateDate = board.getUpdateDate();
        this.deleteDate = board.getDeleteDate();
        this.boardStatus = board.getBoardStatus();
    }

    public void incrementViews() {
        this.views++;
    }

    public void setUpdateViews(Long updateViews) {
        this.updateViews = updateViews;
    }

    public void setUpdateLikes(Long updateLikes) {
        this.updateLikes = updateLikes;
    }

    public Long totalViews() {
        return this.views + this.updateViews;
    }

    public Long totalLikes() {
        return this.likes + this.updateLikes;
    }

    public void update(Board board) {
        this.title = board.getTitle();
        this.content = board.getContent();
        this.updateDate = board.getUpdateDate();
        this.deleteDate = board.getDeleteDate();
        this.boardStatus = board.getBoardStatus();
    }
}

