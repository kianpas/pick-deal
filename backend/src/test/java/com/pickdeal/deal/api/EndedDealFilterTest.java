package com.pickdeal.deal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.pickdeal.deal.domain.*;
import com.pickdeal.keyword.domain.KeywordRepository;
import com.pickdeal.source.domain.*;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {"pickdeal.read-only=true", "pickdeal.seed.enabled=false", "logging.level.org.hibernate.SQL=OFF"})
@AutoConfigureMockMvc
@Transactional
class EndedDealFilterTest {
    @Autowired MockMvc mvc;
    @Autowired DealRepository deals;
    @Autowired DealGroupRepository groups;
    @Autowired SourceRepository sources;
    @Autowired SourceVisibilityRepository visibility;
    @Autowired KeywordRepository keywords;
    @Autowired EntityManager em;
    Source source;

    @BeforeEach void setup() {
        keywords.deleteAll();
        source = sources.save(new Source("종료필터", "https://ended.example", "ended-test", true));
    }

    @Test void filtersBeforePaginationAndDefaultIncludesEnded() throws Exception {
        save(source, "1", DealStatus.ACTIVE);
        save(source, "2", DealStatus.EXPIRED);
        save(source, "3", DealStatus.SOLD_OUT);
        save(source, "4", DealStatus.ACTIVE);
        for (String value : new String[]{"false", "true"}) {
            mvc.perform(get("/api/v1/deals").param("sourceId", source.getId().toString())
                    .param("hideEnded", value).param("size", "1").param("q", "상품").param("shopName", "테스트몰"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.meta.totalElements").value(value.equals("true") ? 2 : 4))
                    .andExpect(jsonPath("$.meta.hasNext").value(true));
        }
        mvc.perform(get("/api/v1/deals").param("sourceId", source.getId().toString()))
                .andExpect(jsonPath("$.meta.totalElements").value(4));
        mvc.perform(get("/api/v1/deals").param("sourceId", source.getId().toString())
                .param("hideEnded", "true").param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.meta.hasNext").value(false));
    }

    @Test void mixedGroupRemainsButUsesOnlyEligibleSources() throws Exception {
        Source other = sources.save(new Source("다른출처", "https://other.example", "ended-other", true));
        Deal ended = save(source, "1", DealStatus.EXPIRED);
        Deal active = save(other, "2", DealStatus.ACTIVE);
        DealGroup group = groups.save(new DealGroup(ended));
        ended.joinGroup(group);
        active.joinGroup(group);
        mvc.perform(get("/api/v1/deals").param("hideEnded", "true"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].sourceCount").value(2));
        mvc.perform(get("/api/v1/deals").param("hideEnded", "true").param("sourceId", source.getId().toString()))
                .andExpect(jsonPath("$.meta.totalElements").value(0));
        visibility.save(new SourceVisibility(1L, other, false));
        mvc.perform(get("/api/v1/deals").param("hideEnded", "true"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test void allEndedGroupIsHiddenAndInvalidFlagIsRejected() throws Exception {
        Deal a = save(source, "1", DealStatus.EXPIRED);
        Source other = sources.save(new Source("다른출처", "https://other.example", "ended-other", true));
        Deal b = save(other, "2", DealStatus.SOLD_OUT);
        DealGroup group = groups.save(new DealGroup(a));
        a.joinGroup(group);
        b.joinGroup(group);
        mvc.perform(get("/api/v1/deals").param("hideEnded", "true"))
                .andExpect(jsonPath("$.meta.totalElements").value(0))
                .andExpect(jsonPath("$.meta.hasNext").value(false));
        mvc.perform(get("/api/v1/deals").param("hideEnded", "invalid"))
                .andExpect(status().isBadRequest());
    }

    /** Opt-in local H2 + MockMvc comparison, not an OCI/network performance guarantee. */
    @Test
    @EnabledIfEnvironmentVariable(named = "PICKDEAL_FILTER_BENCHMARK", matches = "true")
    void compareFilterLatency() throws Exception {
        for (int i = 0; i < 2000; i++) {
            save(source, "perf-" + i, i % 2 == 0 ? DealStatus.ACTIVE : DealStatus.EXPIRED);
        }
        em.flush();
        var off = new ArrayList<Double>();
        var on = new ArrayList<Double>();
        for (int round = 0; round < 40; round++) {
            // Alternate the first condition to reduce ordering bias; discard 10 warmup rounds.
            for (int condition = 0; condition < 2; condition++) {
                boolean hide = (round + condition) % 2 == 0;
                em.clear();
                long start = System.nanoTime();
                var response = mvc.perform(get("/api/v1/deals").param("hideEnded", Boolean.toString(hide))
                        .param("sourceId", source.getId().toString()).param("size", "20")).andReturn();
                double millis = (System.nanoTime() - start) / 1_000_000.0;
                assertThat(response.getResponse().getStatus()).isEqualTo(200);
                if (round >= 10) (hide ? on : off).add(millis);
            }
        }
        Collections.sort(off);
        Collections.sort(on);
        System.out.printf("FILTER_BENCHMARK rows=2000 samples=30 H2/MockMvc offMedian=%.2fms offP95=%.2fms onMedian=%.2fms onP95=%.2fms%n",
                off.get(15), off.get(28), on.get(15), on.get(28));
    }

    private Deal save(Source target, String id, DealStatus status) {
        var now = OffsetDateTime.now();
        return deals.save(new Deal(target, "상품 " + id, null, 1000L, null, null, "KRW", "기타", "테스트몰",
                null, null, target.getBaseUrl() + "/" + id, null, id, null, status, now, now));
    }
}
