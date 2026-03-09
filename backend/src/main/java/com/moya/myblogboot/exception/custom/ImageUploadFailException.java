package com.moya.myblogboot.exception.custom;

public class ImageUploadFailException extends RuntimeException{
    public ImageUploadFailException (String message) {
        super(message);
    }
}
