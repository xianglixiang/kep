package poc.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIf("poc.converter.LibreOfficeConverterTest#sofficeAvailable")
class LibreOfficeConverterTest {

    static boolean sofficeAvailable() {
        try {
            Process p = new ProcessBuilder("soffice", "--version").start();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void name() {
        assertThat(new LibreOfficeConverter().name()).isEqualTo("B-LibreOffice");
    }

    @Test
    void convert_produces_html_with_table_and_image() throws Exception {
        try (InputStream in = new ClassPathResource("fixtures/test-doc.docx").getInputStream()) {
            String html = new LibreOfficeConverter().convert(in);
            assertThat(html.toLowerCase()).contains("<table");
            assertThat(html.toLowerCase()).contains("<img");
        }
    }
}
