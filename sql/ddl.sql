-- 관리자
ALTER TABLE admin
DROP PRIMARY KEY; -- 관리자 기본키

-- 관리자
DROP TABLE IF EXISTS admin RESTRICT;

-- 관리자
CREATE TABLE admin (
                       aidx       BIGINT       NOT NULL PRIMARY KEY, -- 관리자번호
                       admin_id   VARCHAR(50)  NOT NULL, -- 관리자아이디
                       admin_pw   VARCHAR(255) NOT NULL, -- 관리자비밀번호
                       admin_name VARCHAR(50)  NOT NULL  -- 관리자이름
);

-- 방문자
ALTER TABLE visitor
DROP PRIMARY KEY; -- 방문자 기본키

-- 방문자
DROP TABLE IF EXISTS visitor RESTRICT;

-- 방문자
CREATE TABLE visitor (
                         vidx       BIGINT       NOT NULL PRIMARY KEY, -- 방문자번호
                         nickname   VARCHAR(50)  NOT NULL, -- 닉네임
                         visitor_pw VARCHAR(255) NOT NULL  -- 비밀번호
);

-- 게시판
ALTER TABLE post_type
DROP PRIMARY KEY; -- 게시판 기본키

-- 게시판
DROP TABLE IF EXISTS post_type RESTRICT;

-- 게시판
CREATE TABLE post_type (
                           ptidx     BIGINT      NOT NULL PRIMARY KEY, -- 게시판번호
                           type_name VARCHAR(50) NOT NULL  -- 게시판이름
);

-- 게시글
ALTER TABLE post
DROP FOREIGN KEY ptidx; -- 게시판 -> 게시글

-- 게시글
ALTER TABLE post
DROP FOREIGN KEY aidx; -- 관리자 -> 게시글

-- 게시글
ALTER TABLE post
DROP PRIMARY KEY; -- 게시글 기본키

-- 게시글
DROP TABLE IF EXISTS post RESTRICT;

-- 게시글
CREATE TABLE post (
                      pidx        BIGINT       NOT NULL, -- 게시글번호
                      ptidx       BIGINT       NOT NULL, -- 게시판번호
                      aidx        BIGINT       NOT NULL, -- 관리자번호
                      title       VARCHAR(255) NOT NULL, -- 제목
                      hits        INT          NOT NULL, -- 조회수
                      upload_date TIMESTAMP    NOT NULL, -- 작성일
                      edit_date   TIMESTAMP    NULL,     -- 수정일
                      delete_date TIMESTAMP    NULL,     -- 삭제일
                      post_status BOOLEAN      NOT NULL DEFAULT TRUE, -- 게시여부
                      PRIMARY KEY (pidx),
                      FOREIGN KEY (ptidx) REFERENCES post_type(ptidx),
                      FOREIGN KEY (aidx) REFERENCES admin(aidx)
);

-- 게시글내용
ALTER TABLE post_content
DROP FOREIGN KEY FK_post_TO_post_content; -- 게시글 -> 게시글내용

-- 게시글내용
ALTER TABLE post_content
DROP PRIMARY KEY; -- 게시글내용 기본키

-- 게시글내용
DROP TABLE IF EXISTS post_content RESTRICT;

-- 게시글내용
CREATE TABLE post_content (
                              pidx    BIGINT NOT NULL, -- 게시글번호
                              content TEXT   NOT NULL  -- 내용
);

-- 게시글내용
ALTER TABLE post_content
    ADD CONSTRAINT PK_post_content -- 게시글내용 기본키
        PRIMARY KEY (
                     pidx -- 게시글번호
            );

-- 게시글내용
ALTER TABLE post_content
    ADD CONSTRAINT FK_post_TO_post_content -- 게시글 -> 게시글내용
        FOREIGN KEY (
                     pidx -- 게시글번호
            )
            REFERENCES post ( -- 게시글
                             pidx -- 게시글번호
                );

-- 첨부파일
ALTER TABLE file
DROP FOREIGN KEY FK_post_TO_file; -- 게시글 -> 첨부파일

-- 첨부파일
ALTER TABLE file
DROP PRIMARY KEY; -- 첨부파일 기본키

-- 첨부파일
DROP TABLE IF EXISTS file RESTRICT;

-- 첨부파일
CREATE TABLE file (
                      fidx      BIGINT       NOT NULL, -- 파일번호
                      pidx      BIGINT       NOT NULL, -- 게시글번호
                      file_name VARCHAR(255) NOT NULL  -- 파일이름
);

-- 첨부파일
ALTER TABLE file
    ADD CONSTRAINT PK_file -- 첨부파일 기본키
        PRIMARY KEY (
                     fidx -- 파일번호
            );

-- 첨부파일
ALTER TABLE file
    ADD CONSTRAINT FK_post_TO_file -- 게시글 -> 첨부파일
        FOREIGN KEY (
                     pidx -- 게시글번호
            )
            REFERENCES post ( -- 게시글
                             pidx -- 게시글번호
                );

-- 댓글
ALTER TABLE reply
DROP FOREIGN KEY FK_post_TO_reply; -- 게시글 -> 댓글

-- 댓글
ALTER TABLE reply
DROP FOREIGN KEY FK_visitor_TO_reply; -- 방문자 -> 댓글

-- 댓글
ALTER TABLE reply
DROP FOREIGN KEY FK_admin_TO_reply; -- 관리자 -> 댓글

-- 댓글
ALTER TABLE reply
DROP PRIMARY KEY; -- 댓글 기본키

-- 댓글
DROP TABLE IF EXISTS reply RESTRICT;

-- 댓글
CREATE TABLE reply (
                       ridx        BIGINT    NOT NULL, -- 댓글번호
                       pidx        BIGINT    NOT NULL, -- 게시글번호
                       vidx        BIGINT    NULL,     -- 방문자번호
                       aidx        BIGINT    NULL,     -- 관리자번호
                       content     TEXT      NOT NULL, -- 내용
                       write_date  TIMESTAMP NOT NULL, -- 작성일
                       edit_status BOOLEAN   NOT NULL  -- 수정여부
);

-- 댓글
ALTER TABLE reply
    ADD CONSTRAINT PK_reply -- 댓글 기본키
        PRIMARY KEY (
                     ridx -- 댓글번호
            );

-- 댓글
ALTER TABLE reply
    ADD CONSTRAINT FK_post_TO_reply -- 게시글 -> 댓글
        FOREIGN KEY (
                     pidx -- 게시글번호
            )
            REFERENCES post ( -- 게시글
                             pidx -- 게시글번호
                );

-- 댓글
ALTER TABLE reply
    ADD CONSTRAINT FK_visitor_TO_reply -- 방문자 -> 댓글
        FOREIGN KEY (
                     vidx -- 방문자번호
            )
            REFERENCES visitor ( -- 방문자
                                vidx -- 방문자번호
                );

-- 댓글
ALTER TABLE reply
    ADD CONSTRAINT FK_admin_TO_reply -- 관리자 -> 댓글
        FOREIGN KEY (
                     aidx -- 관리자번호
            )
            REFERENCES admin ( -- 관리자
                              aidx -- 관리자번호
                );

-- 대댓글
ALTER TABLE re_reply
DROP FOREIGN KEY FK_reply_TO_re_reply; -- 댓글 -> 대댓글

-- 대댓글
ALTER TABLE re_reply
DROP FOREIGN KEY FK_visitor_TO_re_reply; -- 방문자 -> 대댓글

-- 대댓글
ALTER TABLE re_reply
DROP FOREIGN KEY FK_admin_TO_re_reply; -- 관리자 -> 대댓글

-- 대댓글
ALTER TABLE re_reply
DROP PRIMARY KEY; -- 대댓글 기본키

-- 대댓글
DROP TABLE IF EXISTS re_reply RESTRICT;

-- 대댓글
CREATE TABLE re_reply (
                          rridx       BIGINT    NOT NULL, -- 대댓글번호
                          ridx        BIGINT    NOT NULL, -- 댓글번호
                          vidx        BIGINT    NULL,     -- 방문자번호
                          aidx        BIGINT    NULL,     -- 관리자번호
                          content     TEXT      NOT NULL, -- 내용
                          write_date  TIMESTAMP NOT NULL, -- 작성일
                          edit_status BOOLEAN   NOT NULL  -- 수정여부
);

-- 대댓글
ALTER TABLE re_reply
    ADD CONSTRAINT PK_re_reply -- 대댓글 기본키
        PRIMARY KEY (
                     rridx -- 대댓글번호
            );

-- 대댓글
ALTER TABLE re_reply
    ADD CONSTRAINT FK_reply_TO_re_reply -- 댓글 -> 대댓글
        FOREIGN KEY (
                     ridx -- 댓글번호
            )
            REFERENCES reply ( -- 댓글
                              ridx -- 댓글번호
                );

-- 대댓글
ALTER TABLE re_reply
    ADD CONSTRAINT FK_visitor_TO_re_reply -- 방문자 -> 대댓글
        FOREIGN KEY (
                     vidx -- 방문자번호
            )
            REFERENCES visitor ( -- 방문자
                                vidx -- 방문자번호
                );

-- 대댓글
ALTER TABLE re_reply
    ADD CONSTRAINT FK_admin_TO_re_reply -- 관리자 -> 대댓글
        FOREIGN KEY (
                     aidx -- 관리자번호
            )
            REFERENCES admin ( -- 관리자
                              aidx -- 관리자번호
                );

