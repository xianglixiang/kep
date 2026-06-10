package com.kep.document.converter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTitleExtractorTest {

    private final DefaultTitleExtractor extractor = new DefaultTitleExtractor();

    @Test
    void extract_first_h1() {
        String html = "<html><body><h1>一级标题</h1><p>...</p></body></html>";
        assertThat(extractor.from(html)).isEqualTo("一级标题");
    }

    @Test
    void falls_back_to_h2_when_no_h1() {
        String html = "<html><body><h2>次级标题</h2></body></html>";
        assertThat(extractor.from(html)).isEqualTo("次级标题");
    }

    @Test
    void returns_untitled_when_no_heading() {
        String html = "<html><body><p>just a paragraph</p></body></html>";
        assertThat(extractor.from(html)).isEqualTo("untitled");
    }

    @Test
    void handles_html_with_attributes() {
        String html = "<html><body><h1 class=\"foo\" id=\"x\">带属性的标题</h1></body></html>";
        assertThat(extractor.from(html)).isEqualTo("带属性的标题");
    }

    @Test
    void strips_inner_html_from_h1() {
        // 若 H1 含 <span>，只取文本
        String html = "<html><body><h1>前缀<span>内嵌</span>后缀</h1></body></html>";
        assertThat(extractor.from(html)).isEqualTo("前缀内嵌后缀");
    }
}
