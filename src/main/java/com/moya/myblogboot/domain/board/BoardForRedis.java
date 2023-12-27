package com.moya.myblogboot.domain.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardForRedis {

    private Long id;
    private String title;
    private String content;
    private Long views;
    private Long updateViews;
    private Set<Long> likes = new HashSet<>();
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private BoardStatus boardStatus;


    @Builder
    public BoardForRedis(Board board, Set<Long> memberIds) {
        this.id = board.getId();
        this.title= board.getTitle();
        this.content = board.getContent();
        this.views = board.getViews();
        this.likes.addAll(memberIds);
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

    public void addLike(Long memberId) {
        this.likes.add(memberId);
    }

    public void cancelLike(Long memberId) {
        this.likes.remove(memberId);
    }

    public void update(Board board) {
        this.title = board.getTitle();
        this.content = board.getContent();
        this.updateDate= board.getUpdateDate();
        this.deleteDate = board.getDeleteDate();
        this.boardStatus = board.getBoardStatus();
    }
}

