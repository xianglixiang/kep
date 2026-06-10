# KEP · 企业知识管理系统

模块化单体（Spring Modulith）。当前里程碑：M0 工程骨架。

## 模块边界
- shared(OPEN)：租户上下文、统一返回、异常、对象存储端口
- permission：鉴权地基（M0 桩）
- version / document / catalog / review / subscription：限界上下文（依赖单向，见各 package-info.java）

## 端口-适配器与 local-mock
DB 与中间件均置于端口接口之后：生产用 JPA/MinIO 适配器（`@Profile("!local-mock")`），
本地/单测用内存适配器（`@Profile("local-mock")`）。

## 运行与测试（AI IDE 友好：默认零 Docker）
- 快速测试（内存适配器，无需 Docker）：`./mvnw test`
- 完整测试（含 Testcontainers 集成，需 Docker）：`./mvnw test -Pit`
- 零中间件本地启动：`./mvnw spring-boot:run -Dspring-boot.run.profiles=local-mock`

## 关键验证
- 模块边界：`ModularityTests`（非法跨模块依赖在此失败）
- 租户隔离：`CatalogServiceTest`（内存语义，快速）/ `TenantIsolationIT`（Hibernate 框架级，集成）

## M0 验收
1. 非法跨模块依赖在 `ApplicationModules.verify()` 测试期失败
2. 带 tenant_id 的 catalog_node 空 CRUD 端到端跑通，跨租户不可见（快速层内存验证 + 集成层 Hibernate 验证）

## M1 权限地基（已完成）

- 落地三层鉴权: 租户隔离 / 目录 ACL 就近优先 + DENY 覆盖 / 读-写分离
- 数据模型: app_user / org_unit (含 path) / user_org / acl / review_request + catalog_node.path 物化
- 端口-适配器: UserOrgPort / CatalogPathPort / AclPort（生产 JPA + local-mock 内存）
- SecurityContext + SecurityFilter（X-User-Id 头，M2+ 替换为真实 Spring Security）
- 业务接入: catalog.create/listChildren 真实过 check, review.submit/decide 骨架
- 12 用例表驱动算法单测 + 3 端口单测 + 真实 JPA IT + HTTP 端到端 IT

## M2-A 文档上传/解析（已完成）

- 落地: V3 迁移 + knowledge/knowledge_version 双表
- 选型: Apache POI 纯 Java（M2-PoC 结论主选，p50 85ms，结构要素全过）
- 存储: HTML 落 `knowledge_version.content_richtext` + 原 docx 落 MinIO（key=tenant-x/knowledge/{uuid}/v1/original.docx）
- 鉴权: M1 check(user, node, WRITE|READ) 守住
- API: POST /api/knowledge, GET /api/knowledge/{id}, /api/knowledge/{id}/versions/{n}, /api/knowledge/{id}/file
- 测试: 单测 8 (converter+extractor) + IT 4 = 12 新用例
- 已知局限（按 M2-PoC 评估）: 颜色 / 超链接 / 图片 / 页眉 4 项保真度丢，M2-B+ 增量补
- M2-PoC 仍保留在 `poc-conversion/` 用于将来 fallback (Mammoth) 试验

## 测试

- 快速: `./mvnw test`
- 完整: `./mvnw test -Pit`
- 累计 (M0 + M1 + M2-A + M2-PoC): 快速 ~40 / 完整 ~80

## M2-B1 编辑锁 + 版本链（已完成）

- 落地: V4 迁移 + edit_lock 表（UNIQUE knowledge_id + 30 min TTL）
- API: POST /api/knowledge/{id}/lock, DELETE /api/knowledge/{id}/lock, POST /api/knowledge/{id}/versions, PUT /api/knowledge/{id}/content (501 桩)
- 锁策略: 可过期 30 min，过期后任何 WRITE 者可抢；上传 v2 后锁保持（手动释放）
- 版本号: MAX+1，INSERT 时计算（锁是排他的，低并发下无冲突）
- 鉴权: M1 check(WRITE) + 锁持有者双重门
- PUT HTML: 501 桩（NOT_IMPLEMENTED 错误码），留给 M2-C 富文本编辑器
- 测试: 单元 4 (EditLock TTL/refresh/holder) + IT 7 (锁/版本/401/403/409/501) = 11 新用例
- 不在 M2-B1 范围: diff/回滚/force 抢占 留 M2-B2

## 测试

- 快速: `./mvnw test` (~40 用例,零 Docker)
- 完整: `./mvnw test -Pit` (~60 用例,需 Docker + Testcontainers)
- 累计: M0 + M1 + M2-A + M2-B1 + M2-PoC

## M2-B2 diff + 回滚 + force 抢占（已完成）

- 落地: 3 个新端点,零新表
- diff: GET /api/knowledge/{id}/diff?from=1&to=2 → JSON 行级 segments (zero deps, 纯 Java LCS)
- 回滚: POST /api/knowledge/{id}/rollback/{n} → 创建 v_n+1 (change_type='ROLLBACK'),复用 v_n 的 content + original_file_key
- force 抢锁: POST /api/knowledge/{id}/lock?force=true → 无视 TTL 抢锁
- 鉴权: M1 check(WRITE) + 锁持有者 (rollback 路径) / READ (diff 路径)
- 测试: 单元 6 (LineDiff) + IT 7 (diff/rollback/force) = 13 新用例
- 不在 M2-B2 范围: 字符级 diff 高亮 (M2-B2+ / 前端层), 审计日志 (M3 统一加)

## 测试

- 快速: `./mvnw test` (~48 用例,零 Docker)
- 完整: `./mvnw test -Pit` (~74 用例,需 Docker + Testcontainers)
- 累计: M0 + M1 + M2-A + M2-B1 + M2-B2 + M2-PoC

## M3 知识地图（已完成）

- 落地: V5 迁移 (title 索引) + KnowledgeRepository 6 个 query method (含 title 前模糊) + KnowledgeQueryApi (document 模块 api 接口, 供 catalog 跨模块用) + KnowledgeQueryService + CatalogController 3 端点
- API: GET /api/catalog/nodes/tree (嵌套 JSON, 不可读节点隐藏), GET /api/catalog/nodes/knowledge (4 维过滤 + 分页), GET /api/catalog/nodes/breadcrumb
- 详情复用 M2-A: GET /api/knowledge/{id}
- 鉴权: M1 PermissionChecker.hasRead(user, node, READ) 控可见性;面包屑不查 READ
- 跨租户: @TenantId SQL 层自动 404
- 测试: 单元 4 (KnowledgeQueryService) + IT 6 (HTTP 端到端) = 10 新用例
- 不在 M3 范围: ES 全文搜索 / 标签 / 知识图谱 / 审计日志 / 大树懒加载

## 测试

- 快速: `./mvnw test` (~50 用例,零 Docker)
- 完整: `./mvnw test -Pit` (~85 用例,需 Docker)
- 累计: M0 + M1 + M2-A + M2-B1 + M2-B2 + M3 + M2-PoC
