package com.kep.document.dto;

import com.kep.document.KnowledgeVersion;

import java.time.OffsetDateTime;
import java.util.List;

public record DiffView(
    DiffVersion from,
    DiffVersion to,
    List<DiffSegment> segments
) {
    public record DiffVersion(int versionNo, OffsetDateTime createdAt) {
        public static DiffVersion from(KnowledgeVersion v) {
            return new DiffVersion(v.getVersionNo(), v.getCreatedAt());
        }
    }

    public record DiffSegment(String type, List<String> lines) {
        public static DiffSegment equal(List<String> lines)  { return new DiffSegment("equal", lines); }
        public static DiffSegment add(List<String> lines)    { return new DiffSegment("add", lines); }
        public static DiffSegment remove(List<String> lines) { return new DiffSegment("remove", lines); }
    }
}
