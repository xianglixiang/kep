package poc.converter;

import java.io.InputStream;

/** 文档 → HTML 转换端口。3 个方案各自实现。 */
public interface Converter {

    /** 方案名（A/B/C），用于前端标签与报告归类。 */
    String name();

    /** 输入 docx，输出 HTML 字符串。 */
    String convert(InputStream docx) throws Exception;
}
