# M2-B2 diff + 回滚 + force 抢占 Design

- 日期：2026-06-05
- 类型：子系统 spec（M2-B1 基础上增量）
- 状态：已评审通过

## 0. 范围

M2-B1 已落地编辑锁 + 版本链。本 spec 增量补 **3 个能力**：
1. **diff** — JSON 行级（v_n vs v_m）
2. **回滚** — 把 v_n 的内容复制为 v_n+1（change_type='ROLLBACK'），符合 M0 spec "不删历史，复制为新版本" 语义
3. **force 抢锁** — `?force=true` 参数允许在锁未过期时抢锁

## 1. 已确认参数

| 维度 | 决策 |
|------|------|
| 范围 | diff + 回滚 + force 抢占（完整） |
| diff 形式 | JSON 行级，无新增依赖 |
| 回滚语义 | 回滚 = 复制 v_n 为 v_n+1，change_type='ROLLBACK'，parent_version_id=当前 |

## 2. 数据模型

**无新表**。`knowledge_version.change_type` 已有列，扩展取值：
- 已有：`CREATE`（M2-A）, `UPDATE`（M2-B1）
- 新增：`ROLLBACK`

## 3. API

### 3.1 GET /api/knowledge/{id}/diff?from=1&to=2

```
Auth: check(userId, knowledge.catalogNodeId, READ)
Params: from (int, version_no), to (int, version_no)
Returns: 200 JSON
  {
    "from": { "versionNo": 1, "createdAt": "..." },
    "to":   { "versionNo": 2, "createdAt": "..." },
    "segments": [
      { "type": "equal",  "lines": ["<p>不变的行</p>"] },
      { "type": "remove", "lines": ["<p>被删的行</p>"] },
      { "type": "add",    "lines": ["<p>新增的行</p>"] }
    ]
  }
400 (Bad Request) if from == to or either version doesn't exist
```

### 3.2 POST /api/knowledge/{id}/rollback/{n}

```
Auth:
  - check(userId, knowledge.catalogNodeId, WRITE)
  - 必须有 lock 且 lock.holder == current_user
Path: {n} 是要被回滚到的版本号
Logic:
  1. check WRITE
  2. 查 lock; 不持有 → 403
  3. 找 v_n（要回滚到的版本）
  4. 找当前 max version_no，new_no = max + 1
  5. INSERT knowledge_version（version_no=new_no, content_richtext=v_n.content_richtext,
     original_file_key=v_n.original_file_key（拷贝原文件到新 key）, file_format=v_n.file_format,
     change_type='ROLLBACK', parent_version_id=current.id, editor_id=userId）
  6. UPDATE knowledge.current_version_id = new.id
  7. 锁保持
Returns: 200 KnowledgeView
400 if n 不存在; 403 if lock 不持有; 404 if knowledge 不存在
```

### 3.3 POST /api/knowledge/{id}/lock?force=true

```
Auth: check(userId, knowledge.catalogNodeId, WRITE)
Logic:
  - 与 acquireLock 一样但无视 expires_at
  - 若已有锁（无论过期/持锁人）→ 强制 UPDATE holder=current_user, acquired_at=now(), expires_at=now()+30min
  - 若无锁 → 正常 INSERT
Returns: 200 + EditLockView
400 if force=true 但不传 / 缺值：实际由 Spring 自动按 bool 解析，无需校验
```

## 4. 集成点

### 4.1 DocumentService 扩展

```java
public DiffView diff(long userId, long knowledgeId, int fromNo, int toNo);
public KnowledgeView rollback(long userId, long knowledgeId, int targetVersionNo);
public EditLockView acquireLockForce(long userId, long knowledgeId);
```

`acquireLock(long, long)` 内部委派给 `acquireLockForce(long, long, boolean force)`。

### 4.2 DocumentController 扩展

```java
@GetMapping("/{id}/diff")
public ApiResponse<DiffView> diff(@PathVariable Long id,
                                  @RequestParam int from, @RequestParam int to);

@PostMapping("/{id}/rollback/{n}")
public ApiResponse<KnowledgeView> rollback(@PathVariable Long id, @PathVariable int n);

@PostMapping("/{id}/lock")  // 已有,加 @RequestParam(required=false) Boolean force
public ApiResponse<EditLockView> acquireLock(@PathVariable Long id,
                                            @RequestParam(value="force", required=false) Boolean force);
```

### 4.3 DiffView DTO

```java
public record DiffView(
    DiffVersion from,
    DiffVersion to,
    List<DiffSegment> segments
) {
    public record DiffVersion(int versionNo, OffsetDateTime createdAt) {}
    public record DiffSegment(String type, List<String> lines) {}
}
```

### 4.4 行级 diff 算法

**纯 Java 实现，零依赖**：
1. 两边 content_richtext 去除 HTML 标签（用 M2-B1 计划的同款 `TAGS.replaceAll`）— 简单但够用（M3+ 可换 jsoup）
2. 移除首尾空白行后按 `\n` 拆分
3. 标准 LCS（Longest Common Subsequence）算法求行级 diff
4. 输出 `{type: equal|add|remove, lines: []}` 段列表

时间复杂度 O(n*m) — 对单文档 KB 级 OK；M2-B2+ 可优化到 Myers diff。

## 5. 关键设计取舍

- **行级 diff**（不是字符级）— 简单，零依赖，演示够用；M2-B2 不做服务端高亮，前端做。
- **回滚 = 复制为新版本**（不是改指针）— 符合 M0 spec；保留全部历史；用户能在 v_n 之后看到 ROLLBACK 标记。
- **回滚时拷贝原文件到新 OSS key** — 避免 history 中的 ROLLBACK 引用已删的原文件。但实际上 v_n.original_file_key 不会被删，所以**直接复用 v_n 的 key，不拷贝**。新 v_n+1 行只新增一个 `original_file_key` 引用（指向 v_n 已有的 OSS 对象）— 节省存储，保留可追溯。
- **force 抢锁的鉴权**：仅 WRITE 用户可 force 抢（不能从 READ 升级）；锁被谁持有的信息在 EditLockView.holderId 已暴露，前端可显示 "A 正在编辑，force 抢将由你接管"。
- **force 抢锁应记录事件**（M3+ 审计需求）。M2-B2 仅换 holder + 刷新 TTL，**不记录 audit log**（M3 统一加）。

## 6. 测试策略

### 层 1：单元（快速，零 DB）
- `LineDiffTest` — 4 用例：纯 equal / 纯 add / 纯 remove / 混合；空字符串；空 diff

### 层 2：集成（Testcontainers Postgres + MinIO）
- 复用 `EditLockAndVersionIT` 的环境（同一 MinIO 容器）
- 新增 `EditLockAndVersionControllerIT.diff_...` 不必要 — diff 走 GET，与现有 IT 模式一致

`DiffAndRollbackIT`:
- `diff_equal_versions_returns_only_equal_segments`
- `diff_added_content_shows_add_segment`
- `diff_removed_content_shows_remove_segment`
- `diff_mixed_changes_returns_mixed_segments`
- `rollback_creates_new_version_with_target_content`
- `rollback_records_change_type_as_ROLLBACK`
- `rollback_without_lock_returns_403`
- `acquire_lock_with_force_takes_over_from_active_holder`
- `acquire_lock_with_force_on_expired_works_too`（顺带覆盖）

## 7. 不在 M2-B2 范围

- 字符级 diff / 高亮（M2-B2+ / 前端层）
- 审计日志（M3 统一加）
- 并发冲突检测（两人同时 force 抢同一锁 — 第二个得 409 还是直接覆盖？M2-B2 选覆盖；M3 细化为乐观锁）
- 锁拥有者通知（前端层 + M3 知识地图）
