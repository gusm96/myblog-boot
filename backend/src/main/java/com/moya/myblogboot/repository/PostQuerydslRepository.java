package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.SearchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PostQuerydslRepository {

    List<Post> findByDeleteDate(LocalDateTime deleteDate);
    Page<Post> findBySearchType(Pageable pageable, SearchType searchType, String contents);
}
