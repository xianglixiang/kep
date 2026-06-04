package com.kep.support;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 进程级单例 Testcontainers Postgres。放在独立顶层类中（不嵌套在 IntegrationTest），
 * 以便 surefire 的 per-class classloader 行为不会让多个 IT 各自重新初始化。
 * 任何继承 {@link IntegrationTest} 的测试类都通过此引用同一个容器。
 */
public final class SharedPostgres {

    public static final PostgreSQLContainer<?> CONTAINER = create();

    private SharedPostgres() {}

    private static PostgreSQLContainer<?> create() {
        PostgreSQLContainer<?> c = new PostgreSQLContainer<>("postgres:16-alpine");
        c.start();
        Runtime.getRuntime().addShutdownHook(new Thread(c::stop));
        return c;
    }
}
