-- 관리자
ALTER TABLE admin
DROP PRIMARY KEY; -- 관리자 기본키

-- 관리자
DROP TABLE IF EXISTS admin RESTRICT;

-- 관리자
CREATE TABLE admin
(
    aidx       BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT, -- 관리자번호
    admin_id   VARCHAR(50)  NOT NULL,                            -- 관리자아이디
    admin_pw   VARCHAR(255) NOT NULL,                            -- 관리자비밀번호
    admin_name VARCHAR(50)  NOT NULL                             -- 관리자이름;
);

-- 방문자
ALTER TABLE visitor
DROP PRIMARY KEY; -- 방문자 기본키

-- 방문자
DROP TABLE IF EXISTS visitor RESTRICT;

-- 방문자
CREATE TABLE visitor
(
    vidx       BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT, -- 방문자번호
    nickname   VARCHAR(50)  NOT NULL,                            -- 닉네임
    visitor_pw VARCHAR(255) NOT NULL                             -- 비밀번호
);


-- 게시판
ALTER TABLE board_type
DROP PRIMARY KEY; -- 게시판 기본키

-- 게시판
DROP TABLE IF EXISTS board_type RESTRICT;

-- 게시판
CREATE TABLE board_type
(
    btidx     BIGINT      NOT NULL PRIMARY KEY, -- 게시판번호
    type_name VARCHAR(50) NOT NULL              -- 게시판이름
);

-- 게시글
ALTER TABLE board
DROP FOREIGN KEY btidx; -- 게시판 -> 게시글

-- 게시글
ALTER TABLE board
DROP FOREIGN KEY aidx; -- 관리자 -> 게시글

-- 게시글
ALTER TABLE board
DROP PRIMARY KEY; -- 게시글 기본키

-- 게시글
DROP TABLE IF EXISTS board RESTRICT;

-- 게시글
CREATE TABLE board
(
    bidx        BIGINT       NOT NULL AUTO_INCREMENT, -- 게시글번호
    btidx       BIGINT       NOT NULL,                -- 게시판번호
    aidx        BIGINT       NOT NULL,                -- 관리자번호
    title       VARCHAR(255) NOT NULL,                -- 제목
    hits        INT          NOT NULL,                -- 조회수
    upload_date TIMESTAMP    NOT NULL,                -- 작성일
    edit_date   TIMESTAMP NULL,                       -- 수정일
    delete_date TIMESTAMP NULL,                       -- 삭제일
    board_status BOOLEAN      NOT NULL DEFAULT TRUE,   -- 게시여부
    PRIMARY KEY (bidx),
    FOREIGN KEY (btidx) REFERENCES board_type(btidx),
    FOREIGN KEY (aidx) REFERENCES admin(aidx)
);


-- 게시글내용mysql
ALTER TABLE board_content
DROP FOREIGN KEY bidx; -- 게시글 -> 게시글내용

-- 게시글내용
ALTER TABLE board_content
DROP PRIMARY KEY; -- 게시글내용 기본키

-- 게시글내용
DROP TABLE IF EXISTS board_content RESTRICT;

-- 게시글내용
CREATE TABLE board_content
(
    bidx    BIGINT NOT NULL PRIMARY KEY, -- 게시글번호
    content TEXT   NOT NULL,              -- 내용
    FOREIGN KEY (bidx) REFERENCES board(bidx)
);


-- 첨부파일
ALTER TABLE file
DROP FOREIGN KEY bidx; -- 게시글 -> 첨부파일

-- 첨부파일
ALTER TABLE file
DROP PRIMARY KEY; -- 첨부파일 기본키

-- 첨부파일
DROP TABLE IF EXISTS file RESTRICT;

-- 첨부파일
CREATE TABLE file
(
    fidx      BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT, -- 파일번호
    bidx      BIGINT       NOT NULL,                            -- 게시글번호
    file_name VARCHAR(255) NOT NULL,                            -- 파일이름
    FOREIGN KEY (bidx) REFERENCES board (bidx)
);

-- 댓글
ALTER TABLE reply
DROP FOREIGN KEY bidx; -- 게시글 -> 댓글

-- 댓글
ALTER TABLE reply
DROP FOREIGN KEY vidx; -- 방문자 -> 댓글

-- 댓글
ALTER TABLE reply
DROP FOREIGN KEY adix; -- 관리자 -> 댓글

-- 댓글
ALTER TABLE reply
DROP PRIMARY KEY; -- 댓글 기본키

-- 댓글
DROP TABLE IF EXISTS reply RESTRICT;

-- 댓글
CREATE TABLE reply
(
    ridx        BIGINT    NOT NULL PRIMARY KEY AUTO_INCREMENT, -- 댓글번호
    bidx        BIGINT    NOT NULL, -- 게시글번호
    vidx        BIGINT NULL,        -- 방문자번호
    aidx        BIGINT NULL,        -- 관리자번호
    content     TEXT      NOT NULL, -- 내용
    write_date  TIMESTAMP NOT NULL, -- 작성일
    edit_status BOOLEAN   NOT NULL, -- 수정여부
    FOREIGN KEY (bidx) REFERENCES board (bidx),
    FOREIGN KEY (vidx) REFERENCES visitor (vidx),
    FOREIGN KEY (aidx) REFERENCES admin (aidx)
);

-- 대댓글
ALTER TABLE re_reply
DROP FOREIGN KEY ridx; -- 댓글 -> 대댓글

-- 대댓글
ALTER TABLE re_reply
DROP FOREIGN KEY vidx; -- 방문자 -> 대댓글

-- 대댓글
ALTER TABLE re_reply
DROP FOREIGN KEY aidx; -- 관리자 -> 대댓글

-- 대댓글
ALTER TABLE re_reply
DROP PRIMARY KEY; -- 대댓글 기본키

-- 대댓글
DROP TABLE IF EXISTS re_reply RESTRICT;

-- 대댓글
CREATE TABLE re_reply
(
    rridx       BIGINT    NOT NULL PRIMARY KEY AUTO_INCREMENT, -- 대댓글번호
    ridx        BIGINT    NOT NULL, -- 댓글번호
    vidx        BIGINT NULL,        -- 방문자번호
    aidx        BIGINT NULL,        -- 관리자번호
    content     TEXT      NOT NULL, -- 내용
    write_date  TIMESTAMP NOT NULL, -- 작성일
    edit_status BOOLEAN   NOT NULL, -- 수정여부
    FOREIGN KEY (ridx) REFERENCES reply (ridx),
    FOREIGN KEY (vidx) REFERENCES visitor (vidx),
    FOREIGN KEY (aidx) REFERENCES admin (aidx)
);


