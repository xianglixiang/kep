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
