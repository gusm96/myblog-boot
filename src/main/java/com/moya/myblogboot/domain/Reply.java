package com.moya.myblogboot.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import static jakarta.persistence.FetchType.*;

@Entity
@Getter
public class Reply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long id;
    private String content;
    private LocalDateTime write_date;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "id")
    private Admin admin;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "board_id")
    private Board board;
}
