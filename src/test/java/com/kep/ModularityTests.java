package com.kep;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    static final ApplicationModules MODULES = ApplicationModules.of(KepApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        // 违反 allowedDependencies / 访问他模块非命名接口类型时，此处抛异常使构建失败
        MODULES.verify();
    }

    @Test
    void writesDocumentation() {
        MODULES.forEach(System.out::println);
    }
}
