CREATE TABLE post_slug_history (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    post_id    BIGINT       NOT NULL,
    old_slug   VARCHAR(255) NOT NULL,
    changed_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_slug_history_old_slug (old_slug),
    KEY ix_slug_history_post_id (post_id),
    CONSTRAINT fk_slug_history_post
        FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- rollback
-- DROP TABLE post_slug_history;
