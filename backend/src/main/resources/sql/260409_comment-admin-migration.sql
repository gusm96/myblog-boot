-- ============================================================
-- 260409 Comment & Admin 마이그레이션
-- ============================================================
-- 1. admin 테이블 생성
CREATE TABLE IF NOT EXISTS admin (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    username VARCHAR(20)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 기존 member(role=ADMIN) 데이터를 admin 테이블로 이관
INSERT INTO admin (username, password)
SELECT username, password
FROM member
WHERE role = 'ADMIN';

-- 3. board 테이블: admin_id 컬럼 추가 & FK 설정
ALTER TABLE board ADD COLUMN admin_id BIGINT NOT NULL AFTER category_id;

UPDATE board b
JOIN member m ON b.member_id = m.id
JOIN admin a  ON m.username  = a.username
SET b.admin_id = a.id;

ALTER TABLE board ADD CONSTRAINT FK_board_admin FOREIGN KEY (admin_id) REFERENCES admin(id);

-- 4. board 테이블: 기존 member_id FK 삭제
ALTER TABLE board DROP FOREIGN KEY FK_board_member;  -- 실제 FK명으로 교체 필요
ALTER TABLE board DROP COLUMN member_id;

-- 5. comment 테이블: 새 컬럼 추가
ALTER TABLE comment
    ADD COLUMN nickname      VARCHAR(10)  NOT NULL DEFAULT '' AFTER comment,
    ADD COLUMN discriminator VARCHAR(4)   NOT NULL DEFAULT '0000' AFTER nickname,
    ADD COLUMN password      VARCHAR(255) NOT NULL DEFAULT '' AFTER discriminator,
    ADD COLUMN is_admin      TINYINT(1)   NOT NULL DEFAULT 0 AFTER password;

-- 6. 기존 댓글 데이터: 어드민이 쓴 것은 is_admin=1, 나머지는 is_admin=0으로 처리
UPDATE comment c
JOIN member m ON c.member_id = m.id
SET c.nickname = m.nickname,
    c.discriminator = LPAD(FLOOR(1000 + RAND() * 9000), 4, '0'),
    c.is_admin = IF(m.role = 'ADMIN', 1, 0)
WHERE c.member_id IS NOT NULL;

-- 7. comment 테이블: 기존 member_id FK 삭제
ALTER TABLE comment DROP FOREIGN KEY FK_comment_member;  -- 실제 FK명으로 교체 필요
ALTER TABLE comment DROP COLUMN member_id;

-- 8. member 테이블 삭제 (데이터 백업 후 진행)
-- DROP TABLE member;
