package poc.converter;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.*;

/**
 * 方案 B：LibreOffice headless 转 HTML。
 * 调 {@code soffice --headless --convert-to html --outdir <tmp> <input>};
 * 读取生成的同名 .html 返回。
 * 每次转换起新子进程（PoC 顺序即可；生产用 JODConverter 池化）。
 */
@Component
public class LibreOfficeConverter implements Converter {

    @Override
    public String name() { return "B-LibreOffice"; }

    @Override
    public String convert(InputStream docx) throws Exception {
        Path tmpIn = Files.createTempFile("poc-in-", ".docx");
        Path tmpDir = Files.createTempDirectory("poc-out-");
        try {
            Files.copy(docx, tmpIn, StandardCopyOption.REPLACE_EXISTING);
            ProcessBuilder pb = new ProcessBuilder(
                "soffice", "--headless", "--convert-to", "html",
                "--outdir", tmpDir.toString(), tmpIn.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes());
            int code = p.waitFor();
            if (code != 0) {
                throw new RuntimeException("soffice 失败 code=" + code + " out=" + out);
            }
            String baseName = tmpIn.getFileName().toString();
            // 替换扩展名 (保留文件名主体)
            String baseNoExt = baseName.substring(0, baseName.lastIndexOf('.'));
            Path htmlFile = tmpDir.resolve(baseNoExt + ".html");
            if (!Files.exists(htmlFile)) {
                throw new RuntimeException("soffice 未生成 HTML 文件，输出=" + out);
            }
            return Files.readString(htmlFile);
        } finally {
            Files.deleteIfExists(tmpIn);
            if (Files.exists(tmpDir)) {
                try (var s = Files.list(tmpDir)) {
                    s.forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignore) {} });
                }
                Files.deleteIfExists(tmpDir);
            }
        }
    }
}
