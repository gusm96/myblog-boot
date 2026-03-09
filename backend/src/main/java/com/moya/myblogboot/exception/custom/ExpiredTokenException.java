package com.moya.myblogboot.exception.custom;

public class ExpiredTokenException extends RuntimeException {
    public ExpiredTokenException(String message) {
        super(message);
    }
}
