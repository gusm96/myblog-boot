package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.file.ImageFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {

}
