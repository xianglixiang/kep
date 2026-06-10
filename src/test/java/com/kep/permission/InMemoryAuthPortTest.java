package com.kep.permission;

import com.kep.permission.api.Permission;
import com.kep.permission.port.InMemoryAuthPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.kep.permission.AuthFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAuthPortTest {

    InMemoryAuthPort port = new InMemoryAuthPort();

    @BeforeEach
    void reset() { port.clear(); }

    @Test
    void ancestorOrgIds_includes_self_and_parent() {
        seedOrgTree(port);
        seedUser(port, 100L, 4L);   // user 100 在子组 1(4)
        assertThat(port.ancestorOrgIds(100L)).containsExactlyInAnyOrder(1L, 4L);
    }

    @Test
    void ancestorIds_parses_path() {
        port.addCatalogNode(1L, "/1/");
        port.addCatalogNode(4L, "/1/4/");
        port.addCatalogNode(9L, "/1/4/9/");
        assertThat(port.ancestorIds(9L)).containsExactly(1L, 4L, 9L);
    }

    @Test
    void findHits_filters_by_resource_subject_permission() {
        seedOrgTree(port);
        seedAcl(port, TENANT_A, 1L, org(1L), Permission.READ, "GRANT");
        seedAcl(port, TENANT_A, 4L, org(1L), Permission.WRITE, "GRANT");
        seedAcl(port, TENANT_A, 4L, user(100L), Permission.READ, "DENY");

        var hits = port.findHits(TENANT_A, java.util.List.of(1L, 4L),
            java.util.Set.of(org(1L), user(100L)), Permission.READ);
        assertThat(hits).hasSize(2);
    }
}
