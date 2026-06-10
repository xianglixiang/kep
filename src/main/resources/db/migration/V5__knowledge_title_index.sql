-- 支持 lower(title) LIKE 'foo%' 前模糊搜索
CREATE INDEX idx_knowledge_tenant_title ON knowledge (tenant_id, lower(title));
