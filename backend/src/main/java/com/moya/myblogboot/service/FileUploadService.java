package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.domain.file.ImageFileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileUploadService {

    ImageFileDto upload(MultipartFile file);

    void delete(String imageFileName);
    void deleteFiles(List<ImageFile> imageFiles);
}
