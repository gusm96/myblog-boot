ALTER TABLE post DROP FOREIGN KEY fk_post_category;
ALTER TABLE post DROP COLUMN category_id;
DROP TABLE category;
