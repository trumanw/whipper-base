# --- !Ups
SET NAMES utf8;
CREATE TABLE `question` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `content` text COMMENT 'stem content of multiple choice',
  `choices` text COMMENT 'a json array object which is constituted by choice json object',
  `answer` varchar(100) DEFAULT NULL COMMENT 'question answer which constituted by choices index list',
  `answer_num` tinyint(1) unsigned DEFAULT NULL COMMENT 'number of answer(s)',
  `score` double DEFAULT NULL,
  `attrs` text COMMENT 'a json array object which contains extensive info',
  `update_time` int(10) NOT NULL COMMENT 'last update timestamp',
  `init_time` int(10) NOT NULL COMMENT 'init timestamp',
  `tombstone` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `composite` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `content` text COMMENT 'stem content of group choice',
  `struct` text COMMENT 'descrp of structure',
  `update_time` int(10) NOT NULL COMMENT 'last update timestamp',
  `init_time` int(10) NOT NULL COMMENT 'init timestamp',
  `tombstone` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `paper` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL COMMENT 'name of paper',
  `attrs` text COMMENT 'attrs of paper',
  `struct` text COMMENT 'struct of paper',
  `review_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0 - editing, 1 -  preview, 2 - wariting for review, 3 - ready for exam',
  `update_time` int(10) NOT NULL COMMENT 'last update timestamp',
  `init_time` int(10) NOT NULL COMMENT 'init timestamp',
  `tombstone` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `exam` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `attrs` text,
  `name` varchar(255) DEFAULT '' COMMENT 'name of the exam',
  `struct` text COMMENT 'struct of the exam',
  `uppertime` int(10) unsigned DEFAULT '0',
  `lowertime` int(10) unsigned DEFAULT '0',
  `duration` int(10) unsigned DEFAULT '0',
  `init_time` int(10) unsigned DEFAULT '0',
  `update_time` int(10) unsigned DEFAULT '0',
  `tombstone` tinyint(1) unsigned DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `catalog` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Tag PRIMARY KEY',
  `name` text COMMENT 'name of the tag',
  `mpath` varchar(255) NOT NULL DEFAULT '0' COMMENT 'materialized path',
  `struct` text COMMENT 'Set of Question''s id',
  `update_time` int(10) NOT NULL DEFAULT '0',
  `init_time` int(10) NOT NULL DEFAULT '0',
  `tombstone` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  FULLTEXT KEY `MPathIndex` (`mpath`)
) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

# --- !Downs
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS composite;
DROP TABLE IF EXISTS paper;
DROP TABLE IF EXISTS exam;
DROP TABLE IF EXISTS catalog2question;
DROP TABLE IF EXISTS catalog2paper;
DROP TABLE IF EXISTS catalog2exam;