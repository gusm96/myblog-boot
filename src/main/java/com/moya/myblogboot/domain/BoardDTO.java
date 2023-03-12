package com.moya.myblogboot.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardDTO {
    private int bidx;
    private String title;
    private String content;
    private String upload_date;
    private String edit_date;
    private int views;
    private int like;
    private int board_type;
}
