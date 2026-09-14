ALTER TABLE t_world_round ADD COLUMN world_snapshot JSON DEFAULT NULL AFTER user_input;
ALTER TABLE t_world_round ADD COLUMN world_snapshot_version INT DEFAULT NULL AFTER world_snapshot;
