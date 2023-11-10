package com.moya.myblogboot.exception;

public class DuplicateBoardLikeException extends RuntimeException {
    public DuplicateBoardLikeException(String message) {
        super(message);
    }
}
