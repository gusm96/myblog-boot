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
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private Long likes;

    @Builder
    public BoardResDto(Board board, Long likes) {
        this.id = board.getId();
        this.title = board.getTitle();
        this.content = board.getContent();
        this.category = board.getCategory().getName();
        this.createDate = board.getCreateDate();
        this.updateDate = board.getUpdateDate();
        this.likes = likes;
    }

    // List<Board> to List<BoardResDto>
    public static BoardResDto of(Board board, Long likes) {
        return BoardResDto.builder()
                .board(board)
                .likes(likes)
                .build();
    }
}
