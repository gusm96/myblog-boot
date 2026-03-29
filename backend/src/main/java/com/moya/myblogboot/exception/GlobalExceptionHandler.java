package com.moya.myblogboot.exception;

import com.moya.myblogboot.exception.custom.ExpiredRefreshTokenException;
import com.moya.myblogboot.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 예외 — 모든 커스텀 예외를 한 번에 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business exception: {} - {}", errorCode.getCode(), e.getMessage(), e);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    // RefreshToken 만료 — 쿠키 삭제 후 에러 응답
    @ExceptionHandler(ExpiredRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredRefreshTokenException(
            HttpServletRequest request, HttpServletResponse response,
            ExpiredRefreshTokenException e) {
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token_key");
        if (refreshTokenCookie != null) {
            CookieUtil.deleteCookie(response, refreshTokenCookie);
        }
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Refresh token expired: {}", errorCode.getCode());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    // 인가 실패 (403) — Spring Security AccessDeniedException
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ErrorCode.ACCESS_DENIED));
    }

    // Validation 예외 — @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors =
                e.getBindingResult().getFieldErrors().stream()
                        .map(error -> ErrorResponse.FieldError.builder()
                                .field(error.getField())
                                .value(error.getRejectedValue() != null
                                        ? error.getRejectedValue().toString() : "")
                                .reason(error.getDefaultMessage())
                                .build())
                        .toList();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors));
    }

    // 타입 불일치 — @RequestParam, @PathVariable 타입 변환 실패
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: param={}, value={}, requiredType={}",
                e.getName(), e.getValue(), e.getRequiredType());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_TYPE_VALUE));
    }

    // JSON 파싱 실패 — 잘못된 요청 본문
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("Message not readable: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST_BODY));
    }

    // 예상치 못한 예외 — 최후의 방어선
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected exception: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
