package com.moya.myblogboot.exception.custom;

import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;

public class InvalidateTokenException extends BusinessException {
    public InvalidateTokenException() {
        super(ErrorCode.INVALID_TOKEN);
    }
}
