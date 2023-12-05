package com.moya.myblogboot.domain.board;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardListResDto {
    private List<BoardResDto> list = new ArrayList<>();
    private int totalPage;

    @Builder
    public BoardListResDto(List<BoardResDto> list, int totalPage){
        this.list = list;
        this.totalPage = totalPage;
    }
}
