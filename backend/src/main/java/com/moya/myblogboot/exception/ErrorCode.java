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
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C005", "허용되지 않은 HTTP 메서드입니다."),

    // 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "리프레시 토큰이 만료되었습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "A006", "비밀번호가 일치하지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A007", "아이디 또는 비밀번호가 일치하지 않습니다."),
    REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "A008", "리프레시 토큰 재사용이 감지되었습니다."),
    TOO_MANY_LOGIN_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "A009", "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."),

    // 어드민
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원이 존재하지 않습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "M002", "이미 존재하는 아이디입니다."),

    // 게시글
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "해당 게시글이 존재하지 않습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "B002", "게시글 수정/삭제 권한이 없습니다."),
    DUPLICATE_POST_LIKE(HttpStatus.CONFLICT, "B003", "이미 좋아요한 게시글입니다."),
    POST_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "B004", "좋아요하지 않은 게시글입니다."),
    POST_GONE(HttpStatus.GONE, "B005", "삭제된 게시글입니다."),
    DUPLICATE_POST_SLUG(HttpStatus.CONFLICT, "B006", "이미 사용 중인 게시글 슬러그입니다."),

    // 댓글
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "해당 댓글이 존재하지 않습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CM002", "댓글 수정/삭제 권한이 없습니다."),

    // 태그
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "TG001", "해당 태그를 찾을 수 없습니다."),
    DUPLICATE_TAG(HttpStatus.CONFLICT, "TG002", "이미 존재하는 태그입니다."),
    INVALID_TAG_NAME(HttpStatus.BAD_REQUEST, "TG003", "유효하지 않은 태그 이름입니다."),
    INVALID_TAG_SLUG(HttpStatus.BAD_REQUEST, "TG004", "유효하지 않은 태그 슬러그입니다."),
    TAG_COUNT_BELOW_MIN(HttpStatus.BAD_REQUEST, "TG005", "태그를 1개 이상 입력해야 합니다."),
    TAG_COUNT_ABOVE_MAX(HttpStatus.BAD_REQUEST, "TG006", "태그는 최대 5개까지 입력할 수 있습니다."),
    TAG_MERGE_SAME_TARGET(HttpStatus.BAD_REQUEST, "TG007", "같은 태그로 병합할 수 없습니다."),
    TAG_HAS_POSTS(HttpStatus.CONFLICT, "TG008", "게시글에 사용 중인 태그는 삭제할 수 없습니다."),
    TAG_HAS_SOFT_DELETED_POSTS(HttpStatus.CONFLICT, "TG009", "삭제된 게시글에 연결된 태그는 삭제할 수 없습니다."),
    TAG_HAS_INBOUND_ALIASES(HttpStatus.CONFLICT, "TG010", "리다이렉트 별칭이 연결된 태그는 삭제할 수 없습니다."),
    TAG_MERGE_CYCLE(HttpStatus.CONFLICT, "TG011", "태그 병합 리다이렉트 순환이 발생할 수 있습니다."),

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
