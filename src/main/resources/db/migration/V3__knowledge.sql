CREATE TABLE knowledge (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id          VARCHAR(64)  NOT NULL,
    catalog_node_id    BIGINT       NOT NULL,
    title              VARCHAR(500) NOT NULL,
    doc_type           VARCHAR(20)  NOT NULL DEFAULT 'WORD',
    status             VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    current_version_id BIGINT,
    created_by         BIGINT       NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_knowledge_tenant_node ON knowledge (tenant_id, catalog_node_id);
CREATE INDEX idx_knowledge_tenant_status ON knowledge (tenant_id, status);

CREATE TABLE knowledge_version (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id         VARCHAR(64)  NOT NULL,
    knowledge_id      BIGINT       NOT NULL,
    version_no        INT          NOT NULL,
    content_richtext  TEXT         NOT NULL,
    original_file_key VARCHAR(500),
    file_format       VARCHAR(20),
    change_type       VARCHAR(20)  NOT NULL,
    parent_version_id BIGINT,
    editor_id         BIGINT       NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    comment           TEXT,
    UNIQUE (knowledge_id, version_no)
);
CREATE INDEX idx_kv_tenant_knowledge ON knowledge_version (tenant_id, knowledge_id);

-- knowledge.current_version_id 互指 FK (DEFERRABLE 解决插入顺序)
ALTER TABLE knowledge ADD CONSTRAINT fk_knowledge_current_version
    FOREIGN KEY (current_version_id) REFERENCES knowledge_version(id) DEFERRABLE INITIALLY DEFERRED;
