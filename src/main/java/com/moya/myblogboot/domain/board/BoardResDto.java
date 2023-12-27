package com.moya.myblogboot.domain.board;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardResDto {
    private Long id;
    private String title;
    private String content;
    private String category;
    private Long likes;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private BoardStatus boardStatus;

    @Builder
    public BoardResDto(Board board, Long likes) {
        this.id = board.getId();
        this.title = board.getTitle();
        this.content = board.getContent();
        this.category = board.getCategory().getName();
        this.likes = likes;
        this.createDate = board.getCreateDate();
        this.updateDate = board.getUpdateDate();
        this.deleteDate = board.getDeleteDate();
        this.boardStatus = board.getBoardStatus();
    }

    // List<Board> to List<BoardResDto>
    public static BoardResDto of(Board board, Long likes) {
        return BoardResDto.builder()
                .board(board)
                .likes(likes)
                .build();
    }
}
