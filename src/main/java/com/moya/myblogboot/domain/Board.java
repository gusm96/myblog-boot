package com.moya.myblogboot.domain;

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
    private String title;
    @Lob
    private String content;
    private Long hits;
    private LocalDateTime upload_date;
    private LocalDateTime edit_date;
    private LocalDateTime delete_date;

    @Enumerated(EnumType.STRING)
    private BoardStatus boardStatus; // VIEW, HIDE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "board")
    private List<Reply> replies = new ArrayList<>();

    /*생성 메서드*/
    @Builder
    public Board(String title, String content, Admin admin, Category category) {
        this.title = title;
        this.content = content;
        this.admin = admin;
        this.category = category;
        category.getBoards().add(this); // 양방향 연관관계를 위한 메서드
        this.upload_date = LocalDateTime.now();
    }
    /*비즈니스 로직*/
    // Update Board
    public void updateBoard(Category category, String title, String content) {
        category.getBoards().remove(this);
        this.category = category;
        category.getBoards().add(this);
        this.title = title;
        this.content = content;
        this.edit_date = LocalDateTime.now();
    }
    // 좋아요 증가
    public void hitsCount(){
        // 우선 단순로직으로...
        this.hits++;
    }

}
