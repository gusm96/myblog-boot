package com.moya.myblogboot.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardDto {
    private String title;
    private String content;
    private String category;
}
