# M2-PoC 文档转换保真度评估 Design

- 日期：2026-06-04
- 类型：研究/评估 PoC（不进入 M2 主体；用于决策 M2-A 文档上传/解析采用哪个转换方案）
- 状态：已评审通过

## 0. 背景与目标

M2 整体范围（文档+版本）过大，本 PoC 是 M2 链路的**先决**子项目。目标：选一个适合企业知识管理场景的"Office 文档 → 统一富文本（HTML）"转换方案。M2 后续工作（M2-A 上传/解析、M2-B 版本+编辑锁）都依赖本 PoC 的结论。

## 1. 范围

**仅评估 1 种主格式：Word (.docx)**（最复杂的常见格式）。Excel/PPTX/MD/PDF/DOC 在 PoC 结论出来后用同样的 3 方案另行快速评估，本 PoC 不展开。

**3 个候选方案：**

| 方案 | 机制 | 输出 |
|------|------|------|
| A. Apache POI 纯 Java | XWPF 读 docx XML，自写 HTML 序列化器（仅支持 9 类常见元素，复杂布局报 WARN） | HTML |
| B. LibreOffice headless | `soffice --headless --convert-to html` 通过子进程调用 | HTML |
| C. Mammoth（第三方） | npm 库，`.docx → clean HTML`，专做语义保留 | HTML |

## 2. 测试文档设计

手工制作 1 份"特性覆盖" docx，~30KB，9 类元素：

| # | 元素 | 验证目的 |
|---|------|---------|
| 1 | 标题 1/2/3 级 | 段落结构 |
| 2 | 加粗/斜体/下划线/字体颜色 | 行内样式 |
| 3 | 有序+无序列表（嵌套 2 层） | 嵌套结构 |
| 4 | 表格（3×4，合并单元格） | 表格保真 |
| 5 | 嵌入图片（PNG） | 媒体引用 |
| 6 | 超链接 | 链接保留 |
| 7 | 脚注 | 边缘特性 |
| 8 | 等宽字体段落（模拟代码块） | 字体 |
| 9 | 页眉 | 文档级元数据 |

> 文件存于 `poc-conversion/fixtures/test-doc.docx`，3 个方案对同一份文件转换，保证对比公平。

## 3. Demo 架构

独立子目录 `poc-conversion/`，自带 pom（不依赖 KEP 主仓 pom）：

```
poc-conversion/
├── pom.xml                          Spring Boot 3.4 + POI 5.x + JODConverter
├── src/main/java/poc/ConversionController.java
├── src/main/java/poc/converter/PoiDocxToHtml.java        (方案 A)
├── src/main/java/poc/converter/LibreOfficeConverter.java (方案 B)
├── src/main/java/poc/converter/MammothConverter.java     (方案 C)
├── src/main/resources/mammoth/convert.mjs                (调 mammoth ESM)
├── src/main/resources/static/index.html                  (3 列对比页)
├── fixtures/test-doc.docx                                (特性覆盖)
├── REPORT.md                                             (评估结论)
└── README.md                                             (怎么跑)
```

**前端**：单 HTML 页面（vanilla JS + Bootstrap CDN），上传 docx → 3 段 HTML 在 `<iframe srcdoc>` 中并排展示（沙箱化避免样式互相污染）。不需登录、不需持久化、不需 DB。

**后端**：Spring Boot 3 个 REST 端点 `POST /convert/{a|b|c}` (multipart)，返回 HTML 字符串。3 个 Converter 在 Controller 里串行调用（PoC 顺序即可，不需并行）。

**端口**：固定 `18090`（避免与 KEP 本地默认冲突）。

## 4. 评估指标

4 维度（按权重）：

| 指标 | 权重 | 评分方式 |
|------|------|---------|
| **保真度** | 40% | 每类元素对照 9 个特性点打 ✅完全 / ⚠️部分 / ❌丢失 |
| **能力范围** | 20% | 哪些元素"能转但有损"、哪些"完全不能"（明确边界） |
| **性能/资源** | 20% | 同一 docx 跑 10 次取 p50/p95 耗时 + 内存峰值（粗略 jcmd 估） |
| **依赖体积/运维** | 20% | JAR 增量大小 / 是否需外部进程 / 部署文档复杂度 |

## 5. 报告结构（`REPORT.md`）

1. 概要 + 推荐结论（A/B/C 哪个胜出、是否需要混用）
2. 9 个特性 × 3 方案的保真度对照表
3. 性能数据（p50/p95 耗时表）
4. 依赖/部署对比表
5. 决策建议：M2-A 选哪个，fallback 策略若有
6. 待 M2 启动时同步评估的格式（Excel/PPTX/PDF/DOC）— 用同方法快速跑

## 6. 完成判据

- 三方案对同一 docx 都跑通
- 浏览器打开 `http://localhost:18090` 可上传文件 + 看 3 段并排
- REPORT.md 给明确推荐 + 决策依据
- README 写清依赖（Node + npm install mammoth、LibreOffice 系统包）与启动步骤

## 7. 不在 PoC 范围

- 真实富文本编辑器的渲染（PoC 仅 iframe 展示 HTML，不接 TipTap/Quill）
- 多文件批量转换、异步队列（M2-A 才有）
- M2-A 的存储、版本、编辑锁（PoC 之外）
- 其他格式（Excel/PPTX/PDF/DOC）的详细评估（PoC 结论后另开子 PoC 复用方法）

## 8. 不在 KEP 主仓的依赖

`poc-conversion/` 是独立 Maven 子项目，自带 pom，不被 KEP 主 pom 包含（避免拉入 mammoth npm + LibreOffice 系统依赖污染 KEP 主构建）。其代码也不进入 KEP 业务代码。

## 9. PoC 之后

- 选定方案 → 进入 M2-A 正式 spec（"上传/解析/统一富文本存储"）
- M2-A 不再做选型评估（PoC 结论复用）
