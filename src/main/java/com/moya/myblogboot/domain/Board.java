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
    private int board_type;
    private String title;
    private String content;
    private int hits;
    private String upload_date;
    private String edit_date;
    private String delete_date;
    private boolean board_status;

    @Builder
    public Board(int aidx, int board_type, String title, String content) {
        this.aidx = aidx;
        this.board_type = board_type;
        this.title = title;
        this.content = content;
    }
    public Board(long bidx, String title, String upload_date) {
        this.bidx = bidx;
        this.title = title;
        this.upload_date = upload_date;
    }

}
