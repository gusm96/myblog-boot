package com.moya.myblogboot.service.implementation;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;


    private final AmazonS3 amazonS3;

    private String randomImageName(String originImageName) {
        String random = UUID.randomUUID().toString();
        return random + originImageName;
    }

    @Override
    public ImageFileDto saveImageFile(MultipartFile file) {
            try{
                // 파일 저장
                String fileName = file.getOriginalFilename();
                log.info("fileName = {}", fileName);
                String ext = fileName.substring(fileName.lastIndexOf("."));
                log.info("ext = {}", ext);
                String randomName = randomImageName(fileName);
                log.info("randomName = {}", randomName);

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType("image/" + ext);
                metadata.setContentLength(file.getSize());
                amazonS3.putObject(new PutObjectRequest(
                        bucketName, randomName, file.getInputStream(), metadata
                ).withCannedAcl(CannedAccessControlList.PublicRead));

                return ImageFileDto.builder()
                        .fileName(randomName)
                        .filePath(amazonS3.getUrl(bucketName, randomName).toString())
                        .build();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    @Override
    public boolean deleteImageFile(String imageFileName) {
        try {
            amazonS3.deleteObject(bucketName, imageFileName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 삭제를 실패 했습니다.");
        }
    }
}
