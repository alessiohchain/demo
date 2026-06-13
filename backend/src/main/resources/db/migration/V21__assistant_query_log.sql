-- ============================================================
-- V21 — assistant query log. Records every smart-navigation prompt, the
-- chosen workflow, the outcome and result count, whether a repair round ran,
-- and (later) whether the user acted on the result. Source for measuring
-- accuracy / repair rate and mining new synonyms + few-shot examples — the
-- assistant's learning loop. Writing is OFF by default
-- (csnx.engine.ai.query-log.enabled unset/false); the table ships ready so the
-- flag can be flipped without a migration once a consumer exists.
-- ============================================================

CREATE TABLE demoschema.assistant_query_log (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prompt        VARCHAR(1000) NOT NULL,
    workflow      VARCHAR(64),
    outcome       VARCHAR(16)  NOT NULL,
    result_count  INTEGER      NOT NULL DEFAULT 0,
    repaired      BOOLEAN      NOT NULL DEFAULT FALSE,
    acted         BOOLEAN,
    company_code  VARCHAR(8),
    username      VARCHAR(64),

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    update_serial BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_assistant_query_log_outcome ON demoschema.assistant_query_log (outcome);
CREATE INDEX idx_assistant_query_log_created ON demoschema.assistant_query_log (created_at);
