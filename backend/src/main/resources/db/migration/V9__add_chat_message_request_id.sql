ALTER TABLE t_chat_message
    ADD COLUMN request_id VARCHAR(64) DEFAULT NULL;

ALTER TABLE t_chat_message
    ADD UNIQUE KEY uk_chat_message_session_request_role (session_id, request_id, role);

-- Historical rows intentionally keep a NULL request_id. No historical pairing is inferred.
