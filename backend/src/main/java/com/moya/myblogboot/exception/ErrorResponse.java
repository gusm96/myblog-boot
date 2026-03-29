package com.moya.myblogboot.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class ErrorResponse {
    private final String code;
    private final String message;
    private final int status;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<FieldError> errors;

    @Builder
    private ErrorResponse(String code, String message, int status,
                          List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.errors = errors != null ? errors : List.of();
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatusValue())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode,
                                   List<FieldError> errors) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatusValue())
                .errors(errors)
                .build();
    }

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String value;
        private final String reason;
    }
}
