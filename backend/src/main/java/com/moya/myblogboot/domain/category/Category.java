package com.moya.myblogboot.domain.category;

import com.moya.myblogboot.domain.board.Board;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;
    private String name;
    @OneToMany(mappedBy = "category")
    private List<Board> boards = new ArrayList<>();

    // 생성 메서드
    @Builder
    public Category(String name) {
        this.name = name;
    }

    // 수정 메서드
    public void editCategory(String name){
        this.name = name;
    }
    // Category 삭제 시에 Board의 Category 정보를 null로 설정
    @PreRemove
    private void removeBoards() {
        for (Board board : boards) {
            board.removeCategory();
        }
    }
    public void addBoard(Board board) {
        this.boards.add(board);
    }
}
