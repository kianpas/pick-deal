package com.pickdeal.deal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.pickdeal.deal.application.DealService;
import com.pickdeal.deal.domain.*;
import com.pickdeal.keyword.domain.KeywordRepository;
import com.pickdeal.source.domain.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "pickdeal.read-only=true")
@AutoConfigureMockMvc
@Transactional
class ShopFilterTest {
    @Autowired MockMvc mvc;
    @Autowired DealService service;
    @Autowired DealRepository deals;
    @Autowired DealGroupRepository groups;
    @Autowired SourceRepository sources;
    @Autowired SourceVisibilityRepository visibility;
    @Autowired KeywordRepository keywords;
    Source first;
    Source second;

    @BeforeEach void setup() {
        keywords.deleteAll();
        first = sources.save(new Source("필터 A", "https://a.example", "shop-test-a", true));
        second = sources.save(new Source("필터 B", "https://b.example", "shop-test-b", true));
    }

    @Test void optionsAreSortedDistinctNonblankAndExcludeHiddenSources() throws Exception {
        save(first, "1", "테스트몰B");
        save(first, "2", "테스트몰A");
        save(first, "3", "테스트몰B");
        save(first, "4", null);
        save(first, "5", " ");
        save(second, "6", "숨긴테스트몰");
        visibility.save(new SourceVisibility(1L, second, false));
        assertThat(service.findShops()).contains("테스트몰A", "테스트몰B")
                .doesNotContain("숨긴테스트몰", " ").doesNotContainNull().doesNotHaveDuplicates().isSorted();
        mvc.perform(get("/api/v1/deals/shops")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test void multipleShopsCombineWithFiltersBeforePagination() throws Exception {
        save(first, "1", "필터몰A");
        save(first, "2", "필터몰B");
        save(first, "3", "필터몰C");
        save(second, "4", "필터몰A");
        mvc.perform(get("/api/v1/deals").param("shopName", "필터몰A", "필터몰B")
                        .param("sourceId", first.getId().toString()).param("q", "상품")
                        .param("category", "기타").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.meta.totalElements").value(2)).andExpect(jsonPath("$.meta.hasNext").value(true));
        mvc.perform(get("/api/v1/deals").param("shopName", "필터몰A", "필터몰B")
                        .param("sourceId", first.getId().toString()).param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.meta.hasNext").value(false));
        mvc.perform(get("/api/v1/deals").param("shopName", "필터몰"))
                .andExpect(jsonPath("$.meta.totalElements").value(0));
        mvc.perform(get("/api/v1/deals").param("shopName", "필터몰A").param("q", "일치하지않음"))
                .andExpect(jsonPath("$.meta.totalElements").value(0));
    }

    @Test void commaIsPartOfNameAndNoSelectionIncludesNullShop() throws Exception {
        save(first, "1", "테스트,몰");
        save(first, "2", null);
        mvc.perform(get("/api/v1/deals").param("shopName", "테스트,몰"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].shopName").value("테스트,몰"))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
        mvc.perform(get("/api/v1/deals").param("sourceId", first.getId().toString()))
                .andExpect(jsonPath("$.meta.totalElements").value(2));
        visibility.save(new SourceVisibility(1L, first, false));
        mvc.perform(get("/api/v1/deals").param("shopName", "테스트,몰"))
                .andExpect(jsonPath("$.meta.totalElements").value(0));
    }

    @Test void representativeAndCountIncludeAllAliasMembers() {
        Deal a = save(first, "1", "G마켓");
        Deal b = save(second, "2", "g마켓");
        DealGroup group = groups.save(new DealGroup(a));
        a.joinGroup(group);
        b.joinGroup(group);
        deals.flush();
        var result = service.findDeals(0, 20, "latest", null, null, null, List.of("g마켓"));
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(a.getId());
            assertThat(item.shopName()).isEqualTo("G마켓");
            assertThat(item.sourceCount()).isEqualTo(2);
        });
        assertThat(service.findDeals(0, 20, "latest", null, null, null, List.of("지마켓")).items()).hasSize(1);
    }

    @Test void aliasesShareOptionsAndFilterWhilePreservingOriginalNames() throws Exception {
        save(first, "1", "카카오쇼핑");
        save(first, "2", "카카오톡딜");
        save(first, "3", "네이버쇼핑");
        save(first, "4", "네이버");
        save(first, "5", "네이버페이");
        save(first, "6", "카카오선물하기");
        save(first, "7", "카카오 톡딜");
        save(first, "8", "카카오");
        save(first, "9", "카카오 쇼핑");
        save(first, "10", "🥤네이버페이");
        assertThat(service.findShops()).contains("카카오쇼핑", "네이버", "네이버페이", "카카오선물하기")
                .doesNotContain("카카오톡딜", "카카오 톡딜", "카카오", "카카오 쇼핑", "🥤네이버페이", "네이버쇼핑");
        for (String alias : List.of("카카오쇼핑", "카카오톡딜", "카카오 톡딜", "카카오", "카카오 쇼핑")) {
            mvc.perform(get("/api/v1/deals").param("shopName", alias)
                            .param("sourceId", first.getId().toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.meta.totalElements").value(5));
        }
        for (String alias : List.of("네이버페이", "🥤네이버페이")) {
            mvc.perform(get("/api/v1/deals").param("shopName", alias)
                            .param("sourceId", first.getId().toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.meta.totalElements").value(2));
        }
        var result = service.findDeals(0, 20, "latest", List.of(first.getId()), null, null,
                List.of("카카오쇼핑", "카카오톡딜", "네이버"));
        assertThat(result.items()).extracting(item -> item.shopName())
                .containsExactlyInAnyOrder("카카오쇼핑", "카카오톡딜", "카카오 톡딜", "카카오", "카카오 쇼핑", "네이버쇼핑", "네이버");
        visibility.save(new SourceVisibility(1L, first, false));
        assertThat(service.findShops()).doesNotContain("카카오쇼핑");
    }

    private Deal save(Source source, String id, String shop) {
        var now = OffsetDateTime.now();
        return deals.save(new Deal(source, "테스트 상품 " + id, null, 1000L, null, null, "KRW",
                "기타", shop, null, null, source.getBaseUrl() + "/" + id, null, id,
                null, DealStatus.EXPIRED, now, now));
    }
}
