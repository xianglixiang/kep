package com.kep.document.util;

import com.kep.document.dto.DiffView.DiffSegment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LineDiffTest {

    @Test
    void equal_strings_produce_only_equal_segment() {
        var segs = LineDiff.diff("<p>hello</p>", "<p>hello</p>");
        assertThat(segs).hasSize(1);
        assertThat(segs.get(0).type()).isEqualTo("equal");
    }

    @Test
    void added_content_produces_add_segment() {
        var segs = LineDiff.diff("<p>hello</p>", "<p>hello</p><p>world</p>");
        var adds = segs.stream().filter(s -> "add".equals(s.type())).toList();
        assertThat(adds).hasSize(1);
        assertThat(adds.get(0).lines()).anyMatch(l -> l.contains("world"));
    }

    @Test
    void removed_content_produces_remove_segment() {
        var segs = LineDiff.diff("<p>hello</p><p>world</p>", "<p>hello</p>");
        var removes = segs.stream().filter(s -> "remove".equals(s.type())).toList();
        assertThat(removes).hasSize(1);
        assertThat(removes.get(0).lines()).anyMatch(l -> l.contains("world"));
    }

    @Test
    void mixed_changes_produce_mixed_segments() {
        var segs = LineDiff.diff(
            "<p>keep</p><p>old</p>",
            "<p>keep</p><p>new</p>");
        assertThat(segs.stream().map(DiffSegment::type).collect(Collectors.toList()))
            .contains("equal", "remove", "add");
    }

    @Test
    void empty_strings_produce_no_segments() {
        var segs = LineDiff.diff("", "");
        assertThat(segs).isEmpty();
    }

    @Test
    void html_tags_stripped_before_diff() {
        var segs = LineDiff.diff("<p>hello</p>", "<b>hello</b>");
        assertThat(segs).hasSize(1);
        assertThat(segs.get(0).type()).isEqualTo("equal");
    }
}
