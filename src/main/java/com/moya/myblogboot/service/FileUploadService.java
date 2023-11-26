package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    String saveImageFIle(MultipartFile file);
}
