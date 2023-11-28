package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.file.ImageFile;

public interface ImageFileRepository {
    ImageFile save(ImageFile file);
}
