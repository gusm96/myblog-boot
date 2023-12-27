package com.moya.myblogboot.domain.category;


import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriesResDto {
    private Long id;
    private String name;
    private int boardsCount;
}
