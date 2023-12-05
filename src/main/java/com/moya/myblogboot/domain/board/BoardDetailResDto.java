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
    private LocalDateTime uploadDate;
    private LocalDateTime editDate;
    private List<CommentResDto> comments;

    @Builder
    public BoardDetailResDto(Board board, Long likes) {
        this.id = board.getId();
        this.category = board.getCategory().getName();
        this.title = board.getTitle();
        this.content = board.getContent();
        this.likes = likes;
        this.uploadDate = board.getUploadDate();
        this.editDate = board.getEditDate();
        this.comments = board.getComments().stream()
                .filter(comment -> comment.getParent() == null)
                .map(CommentResDto::of)
                .collect(Collectors.toList());
    }
}
