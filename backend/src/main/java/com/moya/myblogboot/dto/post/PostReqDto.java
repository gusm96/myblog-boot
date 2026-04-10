package com.moya.myblogboot.dto.post;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.domain.post.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReqDto {
    @NotBlank(message = "제목을 입력하세요.")
    @Size(min = 2, max = 45, message = "제목은 2글자 이상 45글자 이하로 작성해야합니다.")
    private String title;
    @NotBlank(message = "내용을 입력하세요.")
    private String content;
    private Long category;
    private List<ImageFileDto> images;

    public Post toEntity(Category category, Admin admin) {
        return Post.builder()
                .admin(admin)
                .category(category)
                .title(this.title)
                .content(this.content)
                .build();
    }
}
