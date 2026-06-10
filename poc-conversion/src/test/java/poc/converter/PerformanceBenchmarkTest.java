package poc.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 性能基准：每个方案跑 10 次取 p50/p95/max。
 * 默认禁用（@EnabledIfSystemProperty），跑：
 *   {@code ./mvnw test -Dtest=PerformanceBenchmarkTest -Dbenchmark=true}
 *
 * 失败的方案（如 LibreOffice 不可用）记录 "unavailable" 而非崩溃。
 */
@EnabledIfSystemProperty(named = "benchmark", matches = "true")
class PerformanceBenchmarkTest {

    record BenchResult(String name, long p50, long p95, long maxMs, String status) {}

    @Test
    void bench_all_three() throws Exception {
        List<Converter> converters = List.of(
            new PoiDocxToHtml(),
            new LibreOfficeConverter(),
            new MammothConverter()
        );

        List<BenchResult> results = new ArrayList<>();
        for (Converter c : converters) {
            List<Long> times = new ArrayList<>();
            int ok = 0, fail = 0;
            for (int r = 0; r < 10; r++) {
                try (InputStream in = new ClassPathResource("fixtures/test-doc.docx").getInputStream()) {
                    long t0 = System.nanoTime();
                    c.convert(in);
                    times.add((System.nanoTime() - t0) / 1_000_000);
                    ok++;
                } catch (Exception e) {
                    fail++;
                }
            }
            if (ok == 0) {
                results.add(new BenchResult(c.name(), -1, -1, -1, "unavailable (" + fail + " 失败)"));
                System.out.printf("[BENCH] %s: unavailable (%d/10 失败)%n", c.name(), fail);
                continue;
            }
            times.sort(Long::compare);
            long p50 = times.get(times.size() / 2);
            long p95 = times.get((int)(times.size() * 0.95));
            long max = times.get(times.size() - 1);
            results.add(new BenchResult(c.name(), p50, p95, max, "ok"));
            System.out.printf("[BENCH] %s: p50=%dms p95=%dms max=%dms (ok=%d fail=%d)%n",
                c.name(), p50, p95, max, ok, fail);
        }

        // 输出 JSON 格式结果供 REPORT.md 引用
        System.out.println("[BENCH_JSON]" + toJson(results));
    }

    private String toJson(List<BenchResult> rs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rs.size(); i++) {
            if (i > 0) sb.append(",");
            BenchResult r = rs.get(i);
            sb.append(String.format(
                "{\"name\":\"%s\",\"p50\":%d,\"p95\":%d,\"max\":%d,\"status\":\"%s\"}",
                r.name(), r.p50, r.p95, r.maxMs, r.status()));
        }
        return sb.append("]").toString();
    }
}
