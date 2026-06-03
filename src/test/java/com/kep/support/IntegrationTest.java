package com.kep.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 集成测试基类：Testcontainers Postgres（共享单例），默认剖面（启用 JPA/Flyway）。需 Docker。
 *
 * <p>容器放在独立的顶层类 {@link SharedPostgres} 中，由父 classloader 加载一次。
 * 多个继承此基类的 IT 共享同一个容器实例，避免 surefire 3.5.x 的 per-class
 * classloader 行为导致各 IT 重新创建容器（端口漂移、连接冲突）。
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
public abstract class IntegrationTest {

    @ServiceConnection
    static final org.testcontainers.containers.PostgreSQLContainer<?> POSTGRES = SharedPostgres.CONTAINER;
}
