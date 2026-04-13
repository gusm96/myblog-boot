package com.moya.myblogboot.dto.post;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostListResDto {
    private List<PostResDto> list = new ArrayList<>();
    private int totalPage;

    @Builder
    public PostListResDto(List<PostResDto> list, int totalPage) {
        this.list = list;
        this.totalPage = totalPage;
    }
}
