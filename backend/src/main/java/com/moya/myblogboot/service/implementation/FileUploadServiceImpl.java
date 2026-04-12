package com.moya.myblogboot.service.implementation;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.dto.file.ImageFileDto;
import com.moya.myblogboot.exception.custom.ImageDeleteFailException;
import com.moya.myblogboot.exception.custom.ImageUploadFailException;
import com.moya.myblogboot.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private static final Map<String, String> ALLOWED_MIME_TYPES = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "gif",  "image/gif",
            "webp", "image/webp"
    );

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    @Override
    public ImageFileDto upload(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new ImageUploadFailException();
        }

        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex < 0) {
            throw new ImageUploadFailException();
        }
        String ext = originalName.substring(dotIndex + 1).toLowerCase();
        String mimeType = ALLOWED_MIME_TYPES.get(ext);
        if (mimeType == null) {
            throw new ImageUploadFailException();
        }

        String storedName = UUID.randomUUID() + "." + ext;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(mimeType);
            metadata.setContentLength(file.getSize());
            amazonS3.putObject(new PutObjectRequest(
                    bucketName, storedName, file.getInputStream(), metadata));
            log.info("AWS S3 이미지 업로드 {}", storedName);
            return ImageFileDto.builder()
                    .fileName(storedName)
                    .filePath(amazonS3.getUrl(bucketName, storedName).toString())
                    .build();
        } catch (IOException e) {
            log.error("이미지 업로드 실패, {}", e.getMessage());
            throw new ImageUploadFailException();
        }
    }

    @Override
    public void delete(String imageFileName) {
        try {
            amazonS3.deleteObject(bucketName, imageFileName);
            log.info("이미지 삭제 FileName = {}", imageFileName);
        } catch (Exception e) {
            log.error("이미지 삭제 실패 {}", e.getMessage());
            throw new ImageDeleteFailException();
        }
    }

    @Override
    public void deleteFiles(List<ImageFile> imageFiles) {
        try {
            imageFiles.forEach(imageFile ->
                    amazonS3.deleteObject(bucketName, imageFile.getFileName()));
        } catch (Exception e) {
            log.error("이미지 삭제 실패 {}", e.getMessage());
            throw new ImageDeleteFailException();
        }
    }
}
