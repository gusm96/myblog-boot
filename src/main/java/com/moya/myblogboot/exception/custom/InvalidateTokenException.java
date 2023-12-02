package com.moya.myblogboot.exception.custom;

public class InvalidateTokenException extends RuntimeException{
    public InvalidateTokenException (String message){
        super(message);
    }
}
