package com.moya.myblogboot.service.implementation;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.domain.file.ImageFileDto;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    @Override
    public ImageFileDto upload(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String ext = fileName.substring(fileName.lastIndexOf("."));
            String randomFileName = randomImageName(fileName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/" + ext);
            metadata.setContentLength(file.getSize());
            amazonS3.putObject(new PutObjectRequest(
                    bucketName, randomFileName, file.getInputStream(), metadata
            ).withCannedAcl(CannedAccessControlList.PublicRead));
            log.info("AWS S3 이미지 업로드 {}", randomFileName);
            return ImageFileDto.builder()
                    .fileName(randomFileName)
                    .filePath(amazonS3.getUrl(bucketName, randomFileName).toString())
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
            imageFiles.stream().forEach(imageFile ->
                    amazonS3.deleteObject(bucketName, imageFile.getFileName()));
        } catch (Exception e) {
            log.error("이미지 삭제 실패 {}", e.getMessage());
            throw new ImageDeleteFailException();
        }
    }

    private String randomImageName(String originImageName) {
        String random = UUID.randomUUID().toString();
        return random + originImageName;
    }
}
