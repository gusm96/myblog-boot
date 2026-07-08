package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.dto.tag.TagResDto;

import java.util.List;

public interface TagService {
    List<Tag> resolveOrCreate(List<String> names);
    TagResDto create(String name);
    List<TagResDto> retrieveAll();
    List<TagResDto> retrievePublic();
    List<TagResDto> retrievePopular(int limit);
    TagLookupResult lookupBySlug(String slug);
    void merge(Long srcTagId, Long dstTagId);
    void rename(Long tagId, String newName);
    void delete(Long tagId);
}
