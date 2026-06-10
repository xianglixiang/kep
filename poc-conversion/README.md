# KEP M2-PoC 文档转换保真度评估

Word (.docx) → HTML 的 3 个候选方案对比。

## 依赖

- JDK 21 + Maven
- Node.js 18+ (`mammoth` via npm, 安装在 `src/main/resources/mammoth`)
- LibreOffice (`sudo apt install libreoffice` 或 Mac: `brew install --cask libreoffice`)

## 启动

    cd poc-conversion
    npm install --prefix src/main/resources/mammoth   # 安装 mammoth
    ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=18090

打开 http://localhost:18090 上传 docx 即可看 3 方案并排结果。

## 跑测试

    ./mvnw test

## 读报告

[REPORT.md](./REPORT.md)

## 已知限制

- 方案 B 需宿主装 LibreOffice（`apt install libreoffice` 或 `brew install --cask libreoffice`），本评估环境无 soffice，方案 B 全部测试 skipped（`@EnabledIf` 守卫）
- 方案 C 需宿主装 Node.js 18+ 并在 `src/main/resources/mammoth/` 跑 `npm install`（`node_modules/` 已入 .gitignore）
- 任一依赖缺失时该方案 `@EnabledIf` 跳过，测试矩阵仍可通过（`./mvnw test` 8 测全绿 + 3 跳）
- 性能基准默认禁用（`@EnabledIfSystemProperty`），跑： `./mvnw test -Dtest=PerformanceBenchmarkTest -Dbenchmark=true`
- 真实生产路径（M2-A）将用 JODConverter 池化 LO + npm install 集成到 Docker 镜像，PoC 不做

## 复现 PoC 结论

1. `bash fixtures/generate.sh` 生成测试 docx（需 python-docx：`pip install --user python-docx`）
2. `cd poc-conversion && npm install --prefix src/main/resources/mammoth` 安装 mammoth
3. `./mvnw test` 跑所有快速测试（默认不跑 benchmark）
4. `./mvnw test -Dtest=PerformanceBenchmarkTest -Dbenchmark=true` 跑性能（捕获 BENCH 行）
5. `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=18090` 启动 demo
6. 浏览器开 http://localhost:18090 上传 docx 看 3 列并排

## 测试矩阵

| 测试类 | 用例数 | 备注 |
|--------|--------|------|
| PoiDocxToHtmlTest | 3 | 快速 |
| LibreOfficeConverterTest | 2 | `@EnabledIf` 跳 (本机无 soffice) |
| MammothConverterTest | 2 | 快速 (Node + mammoth 可用) |
| ConversionControllerIT | 1 | 快速 (HTTP 端到端) |
| PerformanceBenchmarkTest | 1 | 默认跳 (`-Dbenchmark=true` 启用) |
| **小计** | **8 + 3 跳** | `./mvnw test` |

## 报告

[REPORT.md](./REPORT.md) 给出主选 A-POI / Fallback C-Mammoth / 不推荐 B-LibreOffice 的决策建议。
