package com.kep.catalog;

import com.kep.shared.security.SecurityFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local-mock")
@AutoConfigureMockMvc
class CatalogControllerLocalMockTest {

    @Autowired MockMvc mvc;

    @Test
    void create_and_list_node_scoped_by_tenant_header() throws Exception {
        mvc.perform(post("/api/catalog/nodes")
                .header(SecurityFilter.TENANT_HEADER, "tenant-x")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"产品\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.success").value(true))
           .andExpect(jsonPath("$.data.name").value("产品"));

        mvc.perform(get("/api/catalog/nodes").header(SecurityFilter.TENANT_HEADER, "tenant-x"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.length()").value(1))
           .andExpect(jsonPath("$.data[0].name").value("产品"));

        mvc.perform(get("/api/catalog/nodes").header(SecurityFilter.TENANT_HEADER, "tenant-y"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.length()").value(0));
    }
}
