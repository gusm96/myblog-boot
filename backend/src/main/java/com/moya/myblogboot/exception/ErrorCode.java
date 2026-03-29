package com.moya.myblogboot.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "유효하지 않은 입력입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C002", "요청 값의 타입이 올바르지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "C003", "요청 본문을 읽을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),

    // 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "리프레시 토큰이 만료되었습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "A006", "비밀번호가 일치하지 않습니다."),

    // 회원
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원이 존재하지 않습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "M002", "이미 존재하는 아이디입니다."),

    // 게시글
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "해당 게시글이 존재하지 않습니다."),
    BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "B002", "게시글 수정/삭제 권한이 없습니다."),
    DUPLICATE_BOARD_LIKE(HttpStatus.CONFLICT, "B003", "이미 좋아요한 게시글입니다."),
    BOARD_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "B004", "좋아요하지 않은 게시글입니다."),

    // 댓글
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "해당 댓글이 존재하지 않습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CM002", "댓글 수정/삭제 권한이 없습니다."),

    // 카테고리
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CT001", "해당 카테고리를 찾을 수 없습니다."),
    DUPLICATE_CATEGORY(HttpStatus.CONFLICT, "CT002", "이미 존재하는 카테고리입니다."),
    CATEGORY_HAS_BOARDS(HttpStatus.BAD_REQUEST, "CT003", "등록된 게시글이 존재해 삭제할 수 없습니다."),

    // 파일
    IMAGE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "이미지 업로드를 실패했습니다."),
    IMAGE_DELETE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "F002", "이미지 삭제를 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    public int getStatusValue() {
        return status.value();
    }
}
