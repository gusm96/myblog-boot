package com.moya.myblogboot.exception.custom;

import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;

public class DuplicateException extends BusinessException {
    public DuplicateException(ErrorCode errorCode) {
        super(errorCode);
    }
}
