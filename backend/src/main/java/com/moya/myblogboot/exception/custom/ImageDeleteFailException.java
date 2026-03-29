package com.moya.myblogboot.exception.custom;

import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;

public class ImageDeleteFailException extends BusinessException {
    public ImageDeleteFailException() {
        super(ErrorCode.IMAGE_DELETE_FAIL);
    }
}
