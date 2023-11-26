package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.ImageFile;
import com.moya.myblogboot.repository.ImageFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {
    private final ImageFileRepository imageFileRepository;

    @Value("${file.upload-dir}")
    private String fileUploadPath;

    @Override
    public String saveImageFIle(MultipartFile file) {
        try {
            // 파일 저장
            byte[] bytes = file.getBytes();
            String fileName = file.getOriginalFilename();
            Path path = Paths.get(fileUploadPath + fileName);
            Files.write(path, bytes);

            // 파일의 위치(주소) 반환
            return path.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 파일 저장을 실패했습니다.");
        }
    }
}
