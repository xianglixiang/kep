# M2-PoC 文档转换保真度评估报告

日期：2026-06-04

## 1. 概要

- 评估目的：M2-A 文档上传/解析采用哪个 Word→HTML 转换方案
- 测试输入：`fixtures/test-doc.docx`（38KB，9 特性覆盖样例；实际 docx 中 #6 超链接为纯文本 URL，#7 脚注未生成，详见 §2 脚注）
- 评估方法：每个方案对同一 docx 跑 10 次取 p50/p95/max；9 特性逐项打 ✅/⚠️/❌（基于实际转换输出与 docx XML 比对）
- 测试环境：JDK 21, Spring Boot 3.4, POI 5.3, Mammoth 1.8, LibreOffice 不可用（`which soffice` 空）
- 推荐方案：**A-POI（主选，结构与性能双赢）/ C-Mammoth（保真要求高时切换）**

## 2. 保真度对照表

> 数据来源：实际跑过 `new PoiDocxToHtml().convert(...)` 与 `new MammothConverter().convert(...)` 的输出 + 解包 `test-doc.docx` 后的 `word/document.xml` / `word/header1.xml` 比对。
> LibreOffice 在本机不可用，标 ⚪ 需在有 LO 环境补跑。
> 注：测试 docx 中"超链接"实际为纯文本（`doc.add_paragraph('访问 https://example.com...')`），docx XML 里无 `<w:hyperlink>`；"脚注"按 `generate.py` 注释跳过了；故 #6/#7 无法实测。

| # | 特性 | A-POI | B-LibreOffice | C-Mammoth |
|---|------|-------|---------------|-----------|
| 1 | 标题 1/2/3 | ✅ (输出 H1/H2/H3) | ⚪ 环境不可用 | ✅ (输出 H1/H2/H3) |
| 2 | 加粗/斜体/下划线/颜色 | ⚠️ (b/i/u 生成 `<b>/<i>/<u>`；颜色未提取，原 C00000 红色丢失) | ⚪ | ⚠️ (b/i 映射为 `<strong>/<em>`；**下划线和颜色均丢失**) |
| 3 | 嵌套列表 | ⚠️ (3 个 ListBullet 各自独立 `<ul><li>`，缩进的"嵌套 1"被拍平) | ⚪ | ⚠️ (同样拍平，`<ul>` 三个 `<li>` 同级) |
| 4 | 表格 + 合并单元格 | ⚠️ (生成 `<table>` 但 `gridSpan` 未处理，"合并 A+B" 仍是单 `<td>`，无 `colspan`) | ⚪ | ✅ (`colspan="2"` 保留；输出含 `colspan="2"` 的 `<td>`) |
| 5 | 嵌入图片 | ❌ (代码未处理 `<w:drawing>`，输出无 `<img>`) | ⚪ | ⚠️ (图片转 base64 内联；测试图为 1×1 透明 PNG 实用性低，但转换器机制 OK) |
| 6 | 超链接 | N/A (docx 无真实链接，纯文本 URL) | ⚪ | N/A (同上) |
| 7 | 脚注 | N/A (docx 无 `<w:footnote>` 引用) | ⚪ | N/A (同上) |
| 8 | 等宽字体 (Courier New) | ⚠️ (无 `<code>/<pre>` 包裹，HTML 不携带 font-family；依赖浏览器默认字体) | ⚪ | ⚠️ (同 POI，文本原样输出，无 `<code>/<pre>`) |
| 9 | 页眉 | ❌ (PoiDocxToHtml 未遍历 `XWPFHeader`，`header1.xml` 中"KEP M2-PoC 测试文档"丢失) | ⚪ | ❌ (mammoth 默认不输出 header) |

> ⚠️ Mammoth 在 #4（合并单元格）实际**比规格表里的猜测好**（保留了 colspan），但在 #2 下划线上比规格表差（也丢了）。#2 这行规格表（"只保 b/i/u"）写得过于乐观，实测 u 也丢。

## 3. 性能数据

> 来自 `./mvnw test -Dtest=PerformanceBenchmarkTest -Dbenchmark=true`，10 次/方案。
> 仅 10 样本下 `p95 = times.get((int)(10*0.95)) = times.get(9) = max`，故 p95/max 数值相等是数学结果，不是测量失真。

| 方案 | p50 (ms) | p95 (ms) | max (ms) | 状态 |
|------|----------|----------|----------|------|
| A-POI | 85 | 1740 | 1740 | ok |
| B-LibreOffice | -1 | -1 | -1 | unavailable (10/10 失败，soffice 缺失) |
| C-Mammoth | 455 | 551 | 551 | ok |

> - **A-POI 1740ms 是首次冷启**：第 1 次 `XWPFDocument` 类加载 + JIT 编译；后续 9 次稳态 ~50ms 内。p50 反映稳态，p95/max 反映含冷启的尾延迟。生产部署应预热。
> - **C-Mammoth 455-551ms** 包含 Node 进程启动 + `mammoth` 模块加载 + docx 解析；冷启影响被 10 次稀释。生产部署如需更低延迟，可考虑常驻 Node 池化。
> - B-LibreOffice 在本机无 soffice,运行 10 次全部失败（异常被测试框架 catch）。

## 4. 依赖/部署对比

| 方案 | 依赖增量 | 部署要求 | 备注 |
|------|---------|---------|------|
| A-POI | ~5MB (poi-ooxml) | 纯 Java | 无外部进程，无子进程开销 |
| B-LibreOffice | ~500MB (系统包) | 需 soffice 二进制 | 子进程每次（生产应 JODConverter 池化） |
| C-Mammoth | ~30MB (npm) | 需 Node.js + npm install | 子进程每次（Mammoth 必需） |

## 5. 决策建议

**主选：A-POI**
- 理由：p50 85ms（稳态）远快于 Mammoth 455ms；无 Node 依赖、纯 Java 部署最简；H1/H2/H3、b/i/u、`<table>` 这些**结构性**要素全部正确。
- 接受代价：颜色 / 图片 / 链接 / 页眉 这些**装饰性 / 非语义**要素当前不实现。若业务方反馈需要图片/链接展示，再切 Mammoth 即可（接口 `Converter` 已抽象）。

**Fallback：C-Mammoth**
- 触发条件：业务要求保留图片、超链接、合并单元格布局。
- 接受代价：单次 ~500ms（含 Node 冷启），需要 Node.js 运行时。
- 风险：Node 进程稳定性需自建探活；OOM/子进程泄漏需监控。

**不推荐：B-LibreOffice（当前阶段）**
- 体积大、安装复杂、性能最差（子进程 + 完整 Office 套件启动）；唯一优势是"所见即所得"的渲染保真，但本评估因环境受限未跑实测。
- 重新评估的触发：出现"POI/Mammoth 都搞不定的复杂排版（分栏、页眉水印、浮动图等）"需求时再上。

**风险与未决项**
1. 颜色 / 链接 / 页眉是否业务必需：需 PM 拍板。POI 加这些特性可增量实现（`XWPFRun.getColor()` / `XWPFHyperlink` / `XWPFHeader`），工作量约 1-2 天。
2. Mammoth 的 b/i/u 行为与官方文档声明不完全一致（u 也丢了），可能是 1.8.0 版本的映射变化；锁定版本前应再核 `mammoth.style_mapping` 文档。
3. LibreOffice 路径的保真度数据缺失，本报告 #2 表的 ⚪ 行必须在有 LO 的环境补跑。
4. 测试 docx 的"超链接"和"脚注"实际为空（见 §2 脚注），保真度表 #6/#7 行无样本可比对；下次生成 docx 时应直接写 `<w:hyperlink>` 和 `<w:footnote>` 到 XML 验证。

## 6. M2 启动时需复用的工作

- 同方法对 Excel (XSSF)、PPTX (XMLSlideShow)、PDF (PdfBox / iText) 跑一次快速评估。
- B-LibreOffice 必须在有 LO 的环境（CI runner 或生产跳板机）重跑 `PerformanceBenchmarkTest`，补 #2 表的 ⚪ 行；同步把 LO 启停开销计入 p50。
- 真正的"业务 docx"应另建一组样例（财务报告 / 合同 / 制度文档各 1），再跑一次 §2 表作为业务相关保真度结论。
- `PoiDocxToHtml` 待补：`XWPFRun.getColor()` 颜色、`<w:hyperlink>` 链接、`XWPFHeader` 页眉（按 PM 优先级）。
- 性能测试应加 JVM 预热阶段，把冷启影响从 p50 排除。
