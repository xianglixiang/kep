-- 租户
CREATE TABLE tenant (
    id         VARCHAR(64) PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 知识目录树（M0 演示实体；后续里程碑扩展知识/版本等）
CREATE TABLE catalog_node (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   VARCHAR(64)   NOT NULL,
    parent_id   BIGINT,
    name        VARCHAR(255)  NOT NULL,
    path        VARCHAR(1024) NOT NULL DEFAULT '/',
    sort        INT           NOT NULL DEFAULT 0,
    node_type   VARCHAR(20)   NOT NULL DEFAULT 'FOLDER',  -- FOLDER | KNOWLEDGE
    private     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- tenant_id 必为联合索引首列：多租户逻辑隔离的查询基线
CREATE INDEX idx_catalog_node_tenant_parent ON catalog_node (tenant_id, parent_id);
