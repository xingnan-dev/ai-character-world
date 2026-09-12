ALTER TABLE t_character_relationship
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER deleted;
