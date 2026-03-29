package com.moya.myblogboot.exception.custom;

import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;

public class ExpiredRefreshTokenException extends BusinessException {
    public ExpiredRefreshTokenException() {
        super(ErrorCode.EXPIRED_REFRESH_TOKEN);
    }
}
