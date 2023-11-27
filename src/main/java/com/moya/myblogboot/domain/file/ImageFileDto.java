package com.moya.myblogboot.domain.file;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.ImageFile;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageFileDto {
    private String fileName;
    private String filePath;

    public ImageFile toEntity(Board board){
        return ImageFile.builder()
                .fileName(this.fileName)
                .filePath(this.filePath)
                .board(board)
                .build();
    }
}
