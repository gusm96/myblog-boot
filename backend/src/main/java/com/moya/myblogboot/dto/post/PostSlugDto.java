package com.moya.myblogboot.dto.post;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostSlugDto {
    private String slug;
    private LocalDateTime updateDate;
}
