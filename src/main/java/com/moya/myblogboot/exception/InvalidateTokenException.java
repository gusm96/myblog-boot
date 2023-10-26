package com.moya.myblogboot.exception;

public class InvalidateTokenException extends RuntimeException{
    public InvalidateTokenException (String message){
        super(message);
    }
}
