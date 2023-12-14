package com.moya.myblogboot.domain.board;

import com.moya.myblogboot.domain.comment.CommentResDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardDetailResDto {
    private Long id;
    private String category;
    private String title;
    private String content;
    private Long likes;
    private Long views;
    private LocalDateTime uploadDate;
    private LocalDateTime editDate;

    @Builder
    public BoardDetailResDto(Board board, Long likes, Long views) {
        this.id = board.getId();
        this.category = board.getCategory().getName();
        this.title = board.getTitle();
        this.content = board.getContent();
        this.likes = likes;
        this.views = views;
        this.uploadDate = board.getUploadDate();
        this.editDate = board.getEditDate();
    }
}
