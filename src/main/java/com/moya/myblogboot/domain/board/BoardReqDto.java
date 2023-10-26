package com.moya.myblogboot.domain.board;

import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor
public class BoardReqDto {
    @NotBlank(message = "제목을 입력하세요.")
    @Size(min = 2 , max = 45, message = "제목은 2글자 이상 45글자 이하로 작성해야합니다.")
    private String title;
    @NotBlank(message = "내용을 입력하세요.")
    private String content;
    private Long category;

    @Builder
    public BoardReqDto(Long category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public Board toEntity(Category category, Member member){
        return Board.builder()
                .member(member)
                .category(category)
                .title(this.title)
                .content(this.content)
                .build();
    }
}
