-- V12: payload_hash column on screen_metadata.
--
-- ScreenMetadataSeeder writes the SHA-256 of the canonicalised payload JSON
-- here on every upsert so the seeder can short-circuit unchanged files on
-- subsequent boots. update_serial remains the JPA @Version column on
-- BaseEntity — keeping the two responsibilities separate.

ALTER TABLE demoschema.screen_metadata
    ADD COLUMN payload_hash VARCHAR(64);

COMMENT ON COLUMN demoschema.screen_metadata.payload_hash IS
    'SHA-256 hex digest of the canonicalised payload JSON written by '
    'ScreenMetadataSeeder. Lets the seeder skip rows whose source resource '
    'file is unchanged. NULL on rows that predate the seeder.';
