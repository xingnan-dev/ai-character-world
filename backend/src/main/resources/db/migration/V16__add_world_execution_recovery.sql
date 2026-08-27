ALTER TABLE t_world_round
    ADD COLUMN execution_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE t_world_round
    ADD COLUMN lease_until DATETIME DEFAULT NULL;

CREATE INDEX idx_world_round_recovery
    ON t_world_round (status, lease_until);

ALTER TABLE t_world_event
    ADD CONSTRAINT uk_world_event_participant UNIQUE (round_id, participant_id);
