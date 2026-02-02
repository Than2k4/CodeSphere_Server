-- Fix message_type column to support SYSTEM value
-- Run this script manually in your MySQL database

-- Option 1: If message_type is VARCHAR, just ensure it's long enough
ALTER TABLE messages MODIFY COLUMN message_type VARCHAR(20) NOT NULL;

-- Option 2: If message_type is ENUM, you need to alter it to VARCHAR
-- First, check current structure: DESCRIBE messages;
-- If it's ENUM, run this:
-- ALTER TABLE messages MODIFY COLUMN message_type VARCHAR(20) NOT NULL;

-- Verify the change
-- DESCRIBE messages;

