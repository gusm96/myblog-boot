package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    // 이미지 파일 업로드
    @PostMapping("/api/v1/images")
    public ResponseEntity<ImageFileDto> requestUploadImageFile(@RequestParam("image") MultipartFile file) {
        return ResponseEntity.ok().body(fileUploadService.saveImageFile(file));
    }

    @DeleteMapping("/api/v1/images")
    public ResponseEntity<?> requestDeleteImageFile(@RequestBody @Valid ImageFileDto imageFileDto) {
        // 경로로 이미지 찾아서 삭제.
        return ResponseEntity.ok().body(fileUploadService.deleteImageFile(imageFileDto));
    }
}
