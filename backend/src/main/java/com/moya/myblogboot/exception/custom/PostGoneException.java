package com.moya.myblogboot.exception.custom;

import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;

public class PostGoneException extends BusinessException {
    public PostGoneException(ErrorCode errorCode) {
        super(errorCode);
    }
}
