package com.kep.document.util;

import com.kep.document.dto.DiffView.DiffSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 行级 diff:去 HTML 标签后按 \n 拆分,标准 LCS 算法生成 add/remove/equal 段。
 * 零外部依赖,纯 Java。O(n*m) 时间/空间,对单文档 KB 级 OK。
 */
public final class LineDiff {

    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");

    private LineDiff() {}

    public static List<DiffSegment> diff(String fromHtml, String toHtml) {
        List<String> a = splitLines(stripHtml(fromHtml));
        List<String> b = splitLines(stripHtml(toHtml));
        return computeSegments(a, b);
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        return HTML_TAGS.matcher(html).replaceAll("").trim();
    }

    private static List<String> splitLines(String s) {
        if (s.isEmpty()) return List.of();
        String trimmed = s.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
        if (trimmed.isEmpty()) return List.of();
        return List.of(trimmed.split("\\n", -1));
    }

    private static List<DiffSegment> computeSegments(List<String> a, List<String> b) {
        int n = a.size(), m = b.size();
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (a.get(i).equals(b.get(j))) {
                    lcs[i + 1][j + 1] = lcs[i][j] + 1;
                } else {
                    lcs[i + 1][j + 1] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }
        List<DiffSegment> segments = new ArrayList<>();
        List<String> pending = new ArrayList<>();
        String pendingType = null;
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && a.get(i - 1).equals(b.get(j - 1))) {
                flushOldType(segments, pending, pendingType);
                pendingType = "equal";
                pending.add(0, a.get(i - 1));
                i--; j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                flushOldType(segments, pending, pendingType);
                pendingType = "add";
                pending.add(0, b.get(j - 1));
                j--;
            } else {
                flushOldType(segments, pending, pendingType);
                pendingType = "remove";
                pending.add(0, a.get(i - 1));
                i--;
            }
        }
        // 把最后累积的 flush 进去
        if (pendingType != null && !pending.isEmpty()) {
            segments.add(toSegment(pendingType, pending));
        }
        Collections.reverse(segments);
        return segments;
    }

    private static void flushOldType(List<DiffSegment> out, List<String> pending, String oldType) {
        if (oldType == null || pending.isEmpty()) return;
        out.add(toSegment(oldType, pending));
        pending.clear();
    }

    private static DiffSegment toSegment(String type, List<String> lines) {
        return switch (type) {
            case "add" -> DiffSegment.add(lines);
            case "remove" -> DiffSegment.remove(lines);
            default -> DiffSegment.equal(lines);
        };
    }
}
