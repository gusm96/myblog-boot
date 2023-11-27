package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.ImageFile;
import com.moya.myblogboot.domain.file.ImageFileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${file.upload-dir}")
    private String fileUploadPath;

    @Override
    public ImageFileDto saveImageFile(MultipartFile file) {
        try {
            // 파일 저장
            byte[] bytes = file.getBytes();
            String fileName = file.getOriginalFilename();
            Path path = Paths.get(fileUploadPath + fileName);
            Files.write(path, bytes);
            // 파일의 위치(주소) 반환
            String filePath = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/images/")
                    .path(fileName)
                    .toUriString();

            return ImageFileDto.builder()
                    .fileName(fileName)
                    .filePath(filePath)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 파일 저장을 실패했습니다.");
        }
    }

}
