package com.moya.myblogboot.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
}
