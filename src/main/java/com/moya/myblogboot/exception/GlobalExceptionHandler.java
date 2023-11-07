package com.moya.myblogboot.exception;

import com.moya.myblogboot.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 아이디 중복
    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<?> handleDuplicateUsernameException(DuplicateUsernameException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    // 존재하지 않는 아이디
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFoundException(UsernameNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    // 잘못된 비밀 번호
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    // 토큰 만료
    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<?> handleExpiredTokenException(ExpiredTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    // 존재하지 않는 토큰
    @ExceptionHandler(InvalidateTokenException.class)
    public ResponseEntity<?> handleInvalidateTokenException(InvalidateTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNoSuchElementException (NoSuchElementException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(BoardNotFoundException.class)
    public ResponseEntity<?> handleBoardNotFoundException (BoardNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(ExpiredRefreshTokenException.class)
    public ResponseEntity<?> handleExpiredRefreshTokenException (HttpServletRequest request, HttpServletResponse response, ExpiredRefreshTokenException e){
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token_key");
        CookieUtil.deleteCookie(response, refreshTokenCookie);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }
}
