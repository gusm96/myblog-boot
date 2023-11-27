package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.ImageFile;

public interface ImageFileRepository {
    ImageFile save(ImageFile file);
}
