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


    // S3에 ImageFile 저장
    @Override
    public ImageFileDto saveImageFile(MultipartFile file) {
            try{
                // 파일 저장
                String fileName = file.getOriginalFilename(); // 오리지널 파일명
                String ext = fileName.substring(fileName.lastIndexOf(".")); // 파일 확장자명
                String randomFileName = randomImageName(fileName); // 이름 중복을 방지하기 위한 랜던 파일명

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType("image/" + ext);
                metadata.setContentLength(file.getSize());
                amazonS3.putObject(new PutObjectRequest(
                        bucketName, randomFileName, file.getInputStream(), metadata
                ).withCannedAcl(CannedAccessControlList.PublicRead));
                return ImageFileDto.builder()
                        .fileName(randomFileName)
                        .filePath(amazonS3.getUrl(bucketName, randomFileName).toString())
                        .build();
            } catch (IOException e) {
                throw new ImageUploadFailException("이미지 업로드를 실패했습니다.");
            }
    }

    // S3에서 ImageFile 삭제
    @Override
    public void deleteImageFile(String imageFileName) {
        try {
            amazonS3.deleteObject(bucketName, imageFileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ImageDeleteFailException("이미지 삭제를 실패했습니다.");
        }
    }
    // S3에서 ImageFile 삭제
    @Override
    public void deleteImageFile(List<ImageFile> imageFiles) {
        try {
            imageFiles.stream().forEach(imageFile ->
                    amazonS3.deleteObject(bucketName, imageFile.getFileName()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ImageDeleteFailException("이미지 삭제를 실패했습니다.");
        }
    }

    private String randomImageName(String originImageName) {
        String random = UUID.randomUUID().toString();
        return random + originImageName;
    }
}
