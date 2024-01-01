package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.domain.file.ImageFileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileUploadService {

    ImageFileDto saveImageFile(MultipartFile file);

    void deleteImageFile(String imageFileName);
    void deleteImageFile(List<ImageFile> imageFiles);
}
