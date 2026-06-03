package com.kep.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 集成测试基类：Testcontainers Postgres，默认剖面（启用 JPA/Flyway）。需 Docker。
 *
 * <p>容器放在一个独立的 holder 类中，由父 classloader 加载一次，多个继承此基类的
 * IT 共享同一个容器实例。直接在此类中放 static 字段会在 surefire 3.5.x 的
 * useSystemClassLoader 默认行为下为每个 test class 重新初始化，导致端口漂移。
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
public abstract class IntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = SharedPostgresContainer.INSTANCE;

    private static final class SharedPostgresContainer {
        static final PostgreSQLContainer<?> INSTANCE = create();

        private static PostgreSQLContainer<?> create() {
            PostgreSQLContainer<?> c = new PostgreSQLContainer<>("postgres:16-alpine");
            c.start();
            return c;
        }
    }
}
