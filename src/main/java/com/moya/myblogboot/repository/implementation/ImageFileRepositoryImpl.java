package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.repository.ImageFileRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class ImageFileRepositoryImpl implements ImageFileRepository {
    private EntityManager em;
    public ImageFileRepositoryImpl(EntityManager em) {
        this.em = em;
    }
    @Override
    public ImageFile save(ImageFile file) {
        em.persist(file);
        return file;
    }
}
