package com.moya.myblogboot.exception.custom;

import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;
import lombok.Getter;

@Getter
public class TooManyLoginAttemptsException extends BusinessException {

    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
