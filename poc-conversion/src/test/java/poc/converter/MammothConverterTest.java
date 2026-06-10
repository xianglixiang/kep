package poc.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIf("poc.converter.MammothConverterTest#nodeMammothAvailable")
class MammothConverterTest {

    static boolean nodeMammothAvailable() {
        try {
            boolean nodeOk = new ProcessBuilder("node", "--version").start().waitFor() == 0;
            if (!nodeOk) return false;
            File dir = new File("src/main/resources/mammoth");
            if (!dir.exists()) return false;
            return new ProcessBuilder("node", "-e", "require.resolve('mammoth')")
                .directory(dir).start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void name() {
        assertThat(new MammothConverter().name()).isEqualTo("C-Mammoth");
    }

    @Test
    void convert_produces_html_with_table() throws Exception {
        try (InputStream in = new ClassPathResource("fixtures/test-doc.docx").getInputStream()) {
            String html = new MammothConverter().convert(in);
            assertThat(html.toLowerCase()).contains("<table");
        }
    }
}
