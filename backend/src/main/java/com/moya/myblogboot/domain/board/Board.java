package com.moya.myblogboot.domain.board;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.base.BaseTimeEntity;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.file.ImageFile;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "longtext")
    private String content;
    private Long views = 0L;
    private Long likes = 0L;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageFile> imageFiles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private BoardStatus boardStatus = BoardStatus.VIEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Builder
    public Board(String title, String content, Category category, Admin admin) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.admin = admin;
    }

    public void updateBoard(Category category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.update();
    }

    public void updateBoardStatus(BoardStatus boardStatus) {
        this.boardStatus = boardStatus;
    }

    public void addComment(Comment comment) {
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

    public void updateViews(Long views) {
        this.views = views;
    }

    public void updateLikes(Long likes) {
        this.likes = likes;
    }

    public void deleteBoard() {
        this.delete();
        this.boardStatus = BoardStatus.HIDE;
    }

    public void undeleteBoard() {
        this.undelete();
        this.boardStatus = BoardStatus.VIEW;
    }
}
