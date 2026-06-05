CREATE TABLE edit_lock (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     VARCHAR(64)  NOT NULL,
    knowledge_id  BIGINT       NOT NULL,
    holder_id     BIGINT       NOT NULL,
    acquired_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_edit_lock_knowledge UNIQUE (knowledge_id)
);
CREATE INDEX idx_edit_lock_tenant_expires ON edit_lock (tenant_id, expires_at);
