-- Add file_id column to send history so we can retrieve/surface Telegram file ids
PRAGMA foreign_keys=off;
BEGIN TRANSACTION;
ALTER TABLE qaa0_bot_send_history ADD COLUMN file_id TEXT;
COMMIT;
PRAGMA foreign_keys=on;
