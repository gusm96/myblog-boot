package com.moya.myblogboot.dto.file;

import com.moya.myblogboot.domain.file.ImageFile;

import com.moya.myblogboot.domain.post.Post;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageFileDto {
    private String fileName;
    private String filePath;

    public ImageFile toEntity(Post post){
        return ImageFile.builder()
                .fileName(this.fileName)
                .filePath(this.filePath)
                .post(post)
                .build();
    }
}
