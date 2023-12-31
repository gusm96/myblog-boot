package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    // S3 이미지 파일 업로드
    @PostMapping("/api/v1/images")
    public ResponseEntity<ImageFileDto> requestUploadImageFile(@RequestParam("image") MultipartFile file) {
        return ResponseEntity.ok().body(fileUploadService.saveImageFile(file));
    }

    // S3 이미지 파일 삭제
    @DeleteMapping("/api/v1/images")
    public ResponseEntity<?> requestDeleteImageFile(@RequestBody @Valid ImageFileDto imageFileDto) {
        fileUploadService.deleteImageFile(imageFileDto.getFileName());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
