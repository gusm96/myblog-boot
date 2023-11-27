package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.file.ImageFileDto;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    ImageFileDto saveImageFile(MultipartFile file);
}
