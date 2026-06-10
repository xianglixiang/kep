package com.kep.document.converter;

import com.kep.document.api.Converter;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PoiDocxToHtmlTest {

    Converter converter = new PoiDocxToHtml();

    @Test
    void name() {
        assertThat(converter.name()).isEqualTo("POI");
    }

    @Test
    void convert_produces_html_with_headings_and_text() throws Exception {
        try (InputStream in = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream()) {
            String html = converter.convert(in);
            assertThat(html).contains("<h1").contains("测试文档");
            assertThat(html).contains("<h2").contains("保真度");
            assertThat(html).contains("<h3");
        }
    }

    @Test
    void convert_produces_table() throws Exception {
        try (InputStream in = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream()) {
            String html = converter.convert(in);
            assertThat(html).contains("<table").contains("<td").contains("表头");
        }
    }
}
