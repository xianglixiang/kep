package com.kep;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local-mock")
class ContextLoadsLocalMockTest {

    @Test
    void contextLoads() {
        // local-mock 下应用零中间件启动即通过（无需 Docker）
    }
}
