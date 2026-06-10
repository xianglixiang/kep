package com.kep.document.api;

import java.io.InputStream;

/** 文档 → HTML 转换端口。 */
public interface Converter {

    String name();

    String convert(InputStream docx) throws Exception;
}
