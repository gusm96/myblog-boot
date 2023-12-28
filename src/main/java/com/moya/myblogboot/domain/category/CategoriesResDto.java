package com.moya.myblogboot.domain.category;


import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoriesResDto {
    private Long id;
    private String name;
    private int boardsCount;


    @Builder
    public CategoriesResDto (Category category){
        this.id = category.getId();
        this.name = category.getName();
        this.boardsCount = category.getBoards().size();
    }
}
