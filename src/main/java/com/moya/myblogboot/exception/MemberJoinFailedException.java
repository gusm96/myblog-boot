package com.moya.myblogboot.exception;

public class MemberJoinFailedException extends RuntimeException {
    public MemberJoinFailedException(String message) {
        super(message);
    }
}
