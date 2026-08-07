ALTER TABLE t_chat_message
    ADD COLUMN status TINYINT NOT NULL DEFAULT 2;

ALTER TABLE t_chat_message
    ADD COLUMN error_code VARCHAR(50) DEFAULT NULL;

ALTER TABLE t_chat_message
    ADD COLUMN error_message VARCHAR(500) DEFAULT NULL;

ALTER TABLE t_chat_message
    ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE t_chat_message
    ADD COLUMN completion_time DATETIME DEFAULT NULL;

-- All rows created before lifecycle tracking are completed historical messages.
-- No historical message relationship or completion time is inferred.
UPDATE t_chat_message
SET status = 2;
