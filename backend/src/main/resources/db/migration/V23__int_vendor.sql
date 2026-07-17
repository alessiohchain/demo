-- Integration read-model: pom's vendor master, replicated via WAL CDC →
-- Channel-2 events (pom.vendor.changed, configured centrally on the platform's
-- CDCC screen) → this module's idempotent consumer. Deletes are TOMBSTONED
-- (is_deleted = TRUE, last_op = 'D') so history stays observable. Integration
-- tables carry the int_ prefix. Non-PK columns are nullable on purpose: a
-- delete event for a never-seen vendor inserts a PK-only tombstone (the WAL
-- delete image carries only the PK). The event payload carries every source
-- column; this mirror keeps the subset DEMO needs.

CREATE TABLE demo.int_vendor (
    vendor_code    VARCHAR(16)  PRIMARY KEY,
    vendor_name    VARCHAR(120),
    city           VARCHAR(60),
    country        VARCHAR(60),
    contact_email  VARCHAR(120),
    active         BOOLEAN,
    last_op        VARCHAR(1)   NOT NULL,              -- 'I' | 'U' | 'D'
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    last_event_at  TIMESTAMPTZ,

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
