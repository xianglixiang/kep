package poc.converter;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.file.*;

/**
 * 方案 C：Mammoth（npm）通过 Node 子进程调用。
 * Mammoth 专做 .docx → clean HTML，丢弃样式装饰保语义结构。
 */
@Component
public class MammothConverter implements Converter {

    @Override
    public String name() { return "C-Mammoth"; }

    @Override
    public String convert(InputStream docx) throws Exception {
        Path tmpIn = Files.createTempFile("poc-mammoth-", ".docx");
        try {
            Files.copy(docx, tmpIn, StandardCopyOption.REPLACE_EXISTING);
            ProcessBuilder pb = new ProcessBuilder(
                "node", "convert.mjs", tmpIn.toString());
            pb.directory(new File("src/main/resources/mammoth"));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes());
            int code = p.waitFor();
            if (code != 0) {
                throw new RuntimeException("mammoth 失败 code=" + code + " out=" + out);
            }
            return out;
        } finally {
            Files.deleteIfExists(tmpIn);
        }
    }
}
