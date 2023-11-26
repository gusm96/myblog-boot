package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final BoardService boardService;

    // 이미지 파일 업로드
    @PostMapping("/api/v1/images")
    public ResponseEntity<String> requestUploadImageFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok().body(fileUploadService.saveImageFIle(file));
    }

    @DeleteMapping("/api/v1/images")
    public ResponseEntity<?> requestDeleteImageFile(@RequestParam("file_path") String filePath) {
        // 경로로 이미지 찾아서 삭제.
        return ResponseEntity.ok().body("");
    }
}
