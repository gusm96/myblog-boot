package com.moya.myblogboot.exception.custom;

public class ImageDeleteFailException extends RuntimeException {
    public ImageDeleteFailException(String message) {
        super(message);
    }
}
