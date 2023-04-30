package com.moya.myblogboot.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "board")
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bidx;
    private int aidx;
    @Column(name = "btidx")
    @Setter
    private int board_type;
    @Setter
    private String title;
    @Setter
    private String content;
    private int hits;
    private String upload_date;
    @Setter
    private String edit_date;
    private String delete_date;
    @Setter
    private boolean board_status;

    @Builder
    public Board(int aidx, int board_type, String title, String content) {
        this.aidx = aidx;
        this.board_type = board_type;
        this.title = title;
        this.content = content;
    }
}
