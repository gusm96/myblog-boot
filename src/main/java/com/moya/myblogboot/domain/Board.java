package com.moya.myblogboot.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Board {
    @Id
    private Long idx;
    private String title;
    private String content;
    private String upload_date;
    private String edit_date;
    private int views;
    private int like;
    private int board_type;
}
