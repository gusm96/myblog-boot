-- tag : Tag.java + BaseTimeEntity(create/update/delete_date는 NULL 허용)
CREATE TABLE IF NOT EXISTS tag (
    tag_id      BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(40)  NOT NULL,
    slug        VARCHAR(60)  NOT NULL,
    post_count  INT          NOT NULL,
    create_date DATETIME(6)  NULL,
    update_date DATETIME(6)  NULL,
    delete_date DATETIME(6)  NULL,
    PRIMARY KEY (tag_id),
    UNIQUE KEY uk_tag_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- post_tag : PostTag.java
CREATE TABLE IF NOT EXISTS post_tag (
    post_tag_id BIGINT      NOT NULL AUTO_INCREMENT,
    post_id     BIGINT      NOT NULL,
    tag_id      BIGINT      NOT NULL,
    sort_order  INT         NOT NULL,
    is_primary  BIT         NOT NULL,
    create_date DATETIME(6) NOT NULL,
    PRIMARY KEY (post_tag_id),
    CONSTRAINT uk_post_tag UNIQUE (post_id, tag_id),
    CONSTRAINT fk_post_tag_post FOREIGN KEY (post_id) REFERENCES post (post_id),
    CONSTRAINT fk_post_tag_tag  FOREIGN KEY (tag_id)  REFERENCES tag  (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- tag_alias : TagAlias.java
CREATE TABLE IF NOT EXISTS tag_alias (
    from_slug VARCHAR(60) NOT NULL,
    to_tag_id BIGINT      NOT NULL,
    merged_at DATETIME(6) NOT NULL,
    PRIMARY KEY (from_slug),
    CONSTRAINT fk_tag_alias_tag FOREIGN KEY (to_tag_id) REFERENCES tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
