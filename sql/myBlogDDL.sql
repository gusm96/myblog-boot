CREATE DATABASE IF NOT EXISTS `myblog`;
USE `myblog`;

CREATE TABLE IF NOT EXISTS `board` (
  `board_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `board_status` varchar(255) DEFAULT NULL,
  `content` longtext NOT NULL,
  `delete_date` datetime(6) DEFAULT NULL,
  `edit_date` datetime(6) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `upload_date` datetime(6) DEFAULT NULL,
  `category_id` bigint(20) DEFAULT NULL,
  `member_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`board_id`),
  KEY `FK60jn05fpinj6cjhqcmtkacu2g` (`category_id`),
  KEY `FKsds8ox89wwf6aihinar49rmfy` (`member_id`),
  CONSTRAINT `FK60jn05fpinj6cjhqcmtkacu2g` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`),
  CONSTRAINT `FKsds8ox89wwf6aihinar49rmfy` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `category` (
  `category_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `comment` (
  `comment_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `comment` varchar(255) NOT NULL,
  `modification_status` varchar(255) DEFAULT NULL,
  `write_date` datetime(6) DEFAULT NULL,
  `board_id` bigint(20) DEFAULT NULL,
  `member_id` bigint(20) DEFAULT NULL,
  `parent_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`comment_id`),
  KEY `FKlij9oor1nav89jeat35s6kbp1` (`board_id`),
  KEY `FKmrrrpi513ssu63i2783jyiv9m` (`member_id`),
  KEY `FKde3rfu96lep00br5ov0mdieyt` (`parent_id`),
  CONSTRAINT `FKde3rfu96lep00br5ov0mdieyt` FOREIGN KEY (`parent_id`) REFERENCES `comment` (`comment_id`),
  CONSTRAINT `FKlij9oor1nav89jeat35s6kbp1` FOREIGN KEY (`board_id`) REFERENCES `board` (`board_id`),
  CONSTRAINT `FKmrrrpi513ssu63i2783jyiv9m` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `image_file` (
  `file_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `board_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`file_id`),
  KEY `FK1ucg6ge7tmjh1lg24buounvk9` (`board_id`),
  CONSTRAINT `FK1ucg6ge7tmjh1lg24buounvk9` FOREIGN KEY (`board_id`) REFERENCES `board` (`board_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `member` (
  `member_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nickname` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

myblogmyblogmyblog