package com.moya.myblogboot.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

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
    @Lob
    @Column(nullable = false)
    private String content;
    private LocalDateTime upload_date;
    private LocalDateTime edit_date;
    private LocalDateTime delete_date;

    @Enumerated(EnumType.STRING)
    private BoardStatus boardStatus; // VIEW, HIDE

    @ManyToOne // 대부분 Category를 함께 조회 하기때문에 FetchType 을 Default 값으로 둠 (EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reply> replies = new ArrayList<>();

    /*생성 메서드*/
    @Builder
    public Board(String title, String content, Category category) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.upload_date = LocalDateTime.now();
        this.boardStatus = BoardStatus.VIEW;
    }


    // 게시글 수정
    public void updateBoard(Category category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.edit_date = LocalDateTime.now();
    }

    // 게시글 숨김 상태 수정
    public void updateBoardStatus(BoardStatus boardStatus) {
        this.boardStatus = boardStatus;
    }
}
