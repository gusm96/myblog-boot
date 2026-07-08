package com.moya.myblogboot.repository;

import java.util.List;

public interface TagQuerydslRepository {
    List<Long> findTagIdsWithMismatchedPostCount();
    int countActivePostsByTagId(Long tagId);
}
