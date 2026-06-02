package com.moya.myblogboot.exception.custom;

import lombok.Getter;

@Getter
public class PostMovedPermanentlyException extends RuntimeException {

    private final String currentSlug;

    public PostMovedPermanentlyException(String currentSlug) {
        super("Post moved permanently to slug: " + currentSlug);
        this.currentSlug = currentSlug;
    }
}
