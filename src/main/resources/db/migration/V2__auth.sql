-- 用户
CREATE TABLE app_user (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     VARCHAR(64)  NOT NULL,
    username      VARCHAR(200) NOT NULL,
    display_name  VARCHAR(200),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, username)
);
CREATE INDEX idx_app_user_tenant ON app_user (tenant_id);

-- 组织（M1 新表，含物化路径）
CREATE TABLE org_unit (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   VARCHAR(64)  NOT NULL,
    parent_id   BIGINT,
    name        VARCHAR(200) NOT NULL,
    path        VARCHAR(1024) NOT NULL DEFAULT '/',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_org_unit_tenant_path ON org_unit (tenant_id, path);

-- 用户∈组织
CREATE TABLE user_org (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   VARCHAR(64) NOT NULL,
    user_id     BIGINT       NOT NULL,
    org_unit_id BIGINT       NOT NULL,
    UNIQUE (user_id, org_unit_id)
);
CREATE INDEX idx_user_org_user ON user_org (tenant_id, user_id);

-- ACL
CREATE TABLE acl (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     VARCHAR(64)  NOT NULL,
    resource_type VARCHAR(40)  NOT NULL,
    resource_id   BIGINT       NOT NULL,
    subject_type  VARCHAR(20)  NOT NULL,
    subject_id    BIGINT       NOT NULL,
    permission    VARCHAR(20)  NOT NULL,
    effect        VARCHAR(10)  NOT NULL DEFAULT 'GRANT',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_acl_query ON acl (tenant_id, resource_id, subject_type, subject_id);

-- catalog_node.path 已在 V1 中存在；补建 (tenant_id, path) 索引
CREATE INDEX idx_catalog_node_tenant_path ON catalog_node (tenant_id, path);

-- 回填 org_unit.path：递归用 CTE
WITH RECURSIVE org_tree AS (
    SELECT id, parent_id, '/' || id || '/' AS path
    FROM org_unit
    WHERE parent_id IS NULL
    UNION ALL
    SELECT c.id, c.parent_id, t.path || c.id || '/'
    FROM org_unit c
    JOIN org_tree t ON c.parent_id = t.id
)
UPDATE org_unit o SET path = t.path
FROM org_tree t WHERE o.id = t.id;

-- 回填 catalog_node.path：递归用 CTE
WITH RECURSIVE node_tree AS (
    SELECT id, parent_id, '/' || id || '/' AS path
    FROM catalog_node
    WHERE parent_id IS NULL
    UNION ALL
    SELECT c.id, c.parent_id, t.path || c.id || '/'
    FROM catalog_node c
    JOIN node_tree t ON c.parent_id = t.id
)
UPDATE catalog_node c SET path = t.path
FROM node_tree t WHERE c.id = t.id;

-- 审核
CREATE TABLE review_request (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id             VARCHAR(64)  NOT NULL,
    knowledge_id          BIGINT       NOT NULL,
    version_id            BIGINT,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    submitter_id          BIGINT       NOT NULL,
    reviewer_subject_type VARCHAR(20)  NOT NULL,
    reviewer_subject_id   BIGINT       NOT NULL,
    decided_at            TIMESTAMPTZ,
    opinion               TEXT,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_review_tenant_status ON review_request (tenant_id, status);
