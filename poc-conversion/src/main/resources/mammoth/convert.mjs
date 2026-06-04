// src/main/resources/mammoth/convert.mjs
// 读 docx 路径作为 argv[2],输出 HTML 到 stdout
import mammoth from "mammoth";
import { readFileSync } from "fs";

const inputPath = process.argv[2];
const buf = readFileSync(inputPath);
const result = await mammoth.convertToHtml({ buffer: buf });
process.stdout.write(result.value);
