package com.pickdeal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class PickDealApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void dealsApplyKeywordAndVisibilityFilters() throws Exception {
        mockMvc.perform(get("/api/v1/deals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("무선 마우스 특가"))
                .andExpect(jsonPath("$.data[1].title").value("기계식 키보드 주말 할인"))
                .andExpect(jsonPath("$.meta.totalElements").value(2))
                .andExpect(jsonPath("$.meta.hasNext").value(false));
    }

    @Test
    void dealsFilterByCategory() throws Exception {
        // 관심/표시 필터를 통과하는 딜은 모두 "전자제품" 카테고리다.
        mockMvc.perform(get("/api/v1/deals").param("category", "전자제품"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 일치하는 카테고리가 없으면 빈 목록.
        mockMvc.perform(get("/api/v1/deals").param("category", "식품"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.meta.totalElements").value(0));
    }

    @Test
    void sourcesReturnUserVisibility() throws Exception {
        mockMvc.perform(get("/api/v1/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].visible").value(true))
                .andExpect(jsonPath("$.data[2].visible").value(false));
    }
}
