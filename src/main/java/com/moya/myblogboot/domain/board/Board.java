package com.moya.myblogboot.domain.board;

import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "longtext")
    private String content;
    private LocalDateTime uploadDate;
    private LocalDateTime editDate;
    private LocalDateTime deleteDate;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageFile> imageFiles =  new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private BoardStatus boardStatus; // VIEW, HIDE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    /*생성 메서드*/
    @Builder
    public Board(String title, String content, Category category, Member member) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.member = member;
        this.uploadDate = LocalDateTime.now();
        this.boardStatus = BoardStatus.VIEW;
    }

    // 게시글 수정
    public void updateBoard(Category category, String title, String content ) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.editDate = LocalDateTime.now();
    }

    // 게시글 숨김 상태 수정
    public void updateBoardStatus(BoardStatus boardStatus) {
        this.boardStatus = boardStatus;
    }

    public void addComment (Comment comment){
        this.comments.add(comment);
    }
    public void removeComment(Comment comment) {
        this.comments.remove(comment);
    }

    public void addImageFile(ImageFile file) {
        this.imageFiles.add(file);
    }
    public void removeImageFile(ImageFile file) {
        this.imageFiles.remove(file);
    }
    public void removeCategory() {
        this.category = null;
    }

    public void setDeleteDate(){
        this.deleteDate = LocalDateTime.now();
    }
}
