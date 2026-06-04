package com.kep.shared.web;

import com.kep.catalog.CatalogService;
import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import com.kep.shared.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.BoomController.class, SecurityConfig.class})
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mvc;

    // 满足 @WebMvcTest 切片里 CatalogController 的依赖（其依赖 CatalogService），
    // 切到不加载 JPA/内存适配器。本测试不调用 catalog 接口，mock 行为无关。
    @MockBean CatalogService catalogService;

    @RestController
    static class BoomController {
        @GetMapping("/boom")
        String boom() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
        }
    }

    @Test
    void business_exception_is_wrapped_into_api_response() throws Exception {
        mvc.perform(get("/boom"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.success").value(false))
           .andExpect(jsonPath("$.code").value("NOT_FOUND"))
           .andExpect(jsonPath("$.message").value("资源不存在"));
    }
}
