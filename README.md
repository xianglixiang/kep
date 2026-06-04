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

## 测试

- 快速套件: `./mvnw test`（默认 24 测试，零 Docker）
- 集成套件: `./mvnw test -Pit`（36 测试，需 Docker + Testcontainers）
- M0 + M1 合计: 24 快速 / 36 完整
