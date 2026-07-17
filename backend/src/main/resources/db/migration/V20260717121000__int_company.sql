-- Integration read-model: the platform's company master, replicated via WAL CDC
-- -> Channel-2 events (platform.company.changed, configured centrally on the
-- platform's CDCC screen) -> this module's idempotent consumer. This is the
-- platform -> demo leg of the module-to-module triangle. Deletes are TOMBSTONED
-- (is_deleted = TRUE, last_op = 'D') so history stays observable. Integration
-- tables carry the int_ prefix. Non-PK columns are nullable on purpose: a delete
-- event for a never-seen company inserts a PK-only tombstone (the WAL delete
-- image carries only the PK).

CREATE TABLE demo.int_company (
    cpy_cd         VARCHAR(8)   PRIMARY KEY,
    description    VARCHAR(255),
    active         BOOLEAN,
    last_op        VARCHAR(1)   NOT NULL,              -- 'I' | 'U' | 'D'
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    last_event_at  TIMESTAMPTZ,

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
