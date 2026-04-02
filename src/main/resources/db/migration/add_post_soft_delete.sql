-- Add soft-delete flag for posts
ALTER TABLE posts
ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;
