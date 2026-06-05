package com.kep.document.converter;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 POI 产出的 HTML 提取首 H1/H2 作为默认 title。
 * 简单正则实现（不引 Jsoup，避免额外依赖）。
 */
@Component
public class DefaultTitleExtractor {

    private static final Pattern H1 = Pattern.compile(
        "<h1\\b[^>]*>(.*?)</h1>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern H2 = Pattern.compile(
        "<h2\\b[^>]*>(.*?)</h2>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    public String from(String html) {
        if (html == null || html.isBlank()) return "untitled";
        String h1 = matchFirst(H1, html);
        if (h1 != null) return stripTags(h1);
        String h2 = matchFirst(H2, html);
        if (h2 != null) return stripTags(h2);
        return "untitled";
    }

    private String matchFirst(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private String stripTags(String s) {
        return TAGS.matcher(s).replaceAll("").trim();
    }
}
