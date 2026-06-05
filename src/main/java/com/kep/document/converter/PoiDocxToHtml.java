package com.kep.document.converter;

import com.kep.document.api.Converter;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * POI 纯 Java docx → HTML。从 M2-PoC 移植。
 * M2-A 接受 POI 的保真度局限（颜色/页眉/超链接丢）；M2-B+ 可增量补。
 */
@Component
public class PoiDocxToHtml implements Converter {

    @Override
    public String name() { return "POI"; }

    @Override
    public String convert(InputStream docx) throws Exception {
        StringBuilder out = new StringBuilder();
        out.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>")
           .append("body{font-family:sans-serif;max-width:800px;margin:2em auto;padding:0 1em}")
           .append("table{border-collapse:collapse}td,th{border:1px solid #999;padding:4px 8px}")
           .append("img{max-width:100%}</style></head><body>");

        try (XWPFDocument doc = new XWPFDocument(docx)) {
            for (IBodyElement el : doc.getBodyElements()) {
                if (el instanceof XWPFParagraph p) {
                    appendParagraph(p, out);
                } else if (el instanceof XWPFTable t) {
                    appendTable(t, out);
                }
            }
        }
        out.append("</body></html>");
        return out.toString();
    }

    private void appendParagraph(XWPFParagraph p, StringBuilder out) {
        String style = p.getStyle();
        String text = p.getText();
        if (text == null) text = "";

        if (style != null) {
            switch (style) {
                case "Heading1" -> { out.append("<h1>").append(escape(text)).append("</h1>"); return; }
                case "Heading2" -> { out.append("<h2>").append(escape(text)).append("</h2>"); return; }
                case "Heading3" -> { out.append("<h3>").append(escape(text)).append("</h3>"); return; }
                case "ListBullet" -> { out.append("<ul><li>").append(escape(text)).append("</li></ul>"); return; }
                case "ListNumber" -> { out.append("<ol><li>").append(escape(text)).append("</li></ol>"); return; }
                default -> {}
            }
        }
        out.append("<p>");
        for (XWPFRun run : p.getRuns()) {
            String r = escape(run.getText(0));
            if (run.isBold()) r = "<b>" + r + "</b>";
            if (run.isItalic()) r = "<i>" + r + "</i>";
            if (run.getUnderline() != null) r = "<u>" + r + "</u>";
            out.append(r);
        }
        out.append("</p>");
    }

    private void appendTable(XWPFTable t, StringBuilder out) {
        out.append("<table>");
        for (XWPFTableRow row : t.getRows()) {
            out.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                out.append("<td>").append(escape(cell.getText())).append("</td>");
            }
            out.append("</tr>");
        }
        out.append("</table>");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
