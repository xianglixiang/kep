package poc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import poc.converter.Converter;
import poc.converter.LibreOfficeConverter;
import poc.converter.MammothConverter;
import poc.converter.PoiDocxToHtml;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/convert")
public class ConversionController {

    private final Map<String, Converter> converters = new LinkedHashMap<>();

    @Autowired
    public ConversionController(PoiDocxToHtml a, LibreOfficeConverter b, MammothConverter c) {
        converters.put("a", a);
        converters.put("b", b);
        converters.put("c", c);
    }

    @PostMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> convert(@PathVariable String id, @RequestParam MultipartFile file) throws Exception {
        Converter c = converters.get(id);
        if (c == null) throw new IllegalArgumentException("未知 converter: " + id);
        return Map.of("name", c.name(), "html", c.convert(file.getInputStream()));
    }

    @GetMapping("/all")
    public Map<String, String> allNames() {
        Map<String, String> m = new LinkedHashMap<>();
        converters.forEach((k, v) -> m.put(k, v.name()));
        return m;
    }
}
