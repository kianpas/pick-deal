package com.pickdeal.collector.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.pickdeal.config.CollectorIngressFilter;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.SourceRepository;
import com.pickdeal.source.domain.Source;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"pickdeal.read-only=true", "pickdeal.collector.ingress.enabled=true",
        "pickdeal.collector.ingress.token=test-only-collector-token-0123456789"})
@AutoConfigureMockMvc
@Transactional
class InternalCollectionControllerTest {
    private static final String ROOT = CollectorIngressFilter.ROOT;
    private static final String AUTH = "Bearer test-only-collector-token-0123456789";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired DealRepository deals;
    @Autowired SourceRepository sources;

    private Map<String, Object> item(String id) {
        return new java.util.LinkedHashMap<>(Map.of("externalId", id,
                "originalUrl", "https://www.ppomppu.co.kr/zboard/view.php?id=ppomppu&no=" + id,
                "title", "테스트 상품 (9,900원/무료)", "shopName", "테스트몰", "price", 9900,
                "ended", false, "productUrl", "https://shop.example/product/1"));
    }
    private String batch(Map<String, Object> item) {
        return mapper.writeValueAsString(Map.of("sourceCode", "ppomppu", "deals", List.of(item)));
    }

    @Test void receivesAndReplaysWithoutDuplicatesAndPreservesProductInfo() throws Exception {
        var item = item("991001");
        mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(batch(item)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.created").value(1));
        item.remove("productUrl");
        item.remove("shopName");
        mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(batch(item)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.created").value(0))
                .andExpect(jsonPath("$.data.updated").value(1));
        var source = sources.findByCode("ppomppu").orElseThrow();
        var saved = deals.findBySourceIdAndExternalId(source.getId(), "991001").orElseThrow();
        assertThat(saved.getProductUrl()).isEqualTo("https://shop.example/product/1");
        assertThat(saved.getShopName()).isEqualTo("테스트몰");
        mvc.perform(post(ROOT + "/known-external-ids").header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceCode":"ppomppu","externalIds":["991001","991002","991001"]}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.knownExternalIds.length()").value(1))
                .andExpect(jsonPath("$.data.knownExternalIds[0]").value("991001"))
                .andExpect(jsonPath("$.data.hasCollectedDeals").value(true));
    }

    @Test void unknownSourceIsNotRegisteredByKnownQuery() throws Exception {
        mvc.perform(post(ROOT + "/known-external-ids").header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceCode":"ppomppu","externalIds":["991001"]}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.knownExternalIds").isEmpty())
                .andExpect(jsonPath("$.data.hasCollectedDeals").value(false));
        assertThat(sources.findByCode("ppomppu")).isEmpty();
    }

    @Test void rejectsBadCredentialsBeforeParsingJson() throws Exception {
        for (String path : List.of(ROOT, ROOT + "/known-external-ids")) {
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("broken-json"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post(path).header("Authorization", "Bearer wrong-token")
                            .contentType(MediaType.APPLICATION_JSON).content(batch(item("991001"))))
                    .andExpect(status().isUnauthorized());
        }
        assertThat(sources.findByCode("ppomppu")).isEmpty();
    }

    @Test void rejectsInvalidFieldsAndLeavesNoPartialWrites() throws Exception {
        for (var invalid : List.of(Map.entry("price", -1), Map.entry("ended", "missing"),
                Map.entry("title", "x".repeat(300)), Map.entry("productUrl", "javascript:alert(1)"),
                Map.entry("originalUrl", "https://evil.example/product"), Map.entry("commentCount", -1))) {
            var invalidItem = item("991002");
            invalidItem.put(invalid.getKey(), invalid.getValue());
            String json = mapper.writeValueAsString(Map.of("sourceCode", "ppomppu", "deals", List.of(item("991001"), invalidItem)));
            mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        assertThat(sources.findByCode("ppomppu")).isEmpty();
    }

    @Test void validatesBatchBoundsAndDuplicatesAndSourceAllowlist() throws Exception {
        for (Object body : List.of(
                Map.of("sourceCode", "arbitrary", "deals", List.of(item("991001"))),
                Map.of("sourceCode", "ppomppu", "deals", List.of()),
                Map.of("sourceCode", "ppomppu", "deals", java.util.Collections.nCopies(151, item("991001"))),
                Map.of("sourceCode", "ppomppu", "deals", List.of(item("991001"), item("991001"))))) {
            mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        }
    }

    @Test void publicWritesRemainBlockedWithValidCollectorToken() throws Exception {
        mvc.perform(post("/api/v1/keywords").header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("READ_ONLY"));
        mvc.perform(get(ROOT).header("Authorization", AUTH)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(ROOT + "/").header("Authorization", AUTH)).andExpect(status().isNotFound());
    }

    @Test void reusesCrossSourceGroupingAndScopesKnownIdsToSource() throws Exception {
        var other = item("991003");
        other.put("originalUrl", "https://quasarzone.com/bbs/qb_saleinfo/views/991003");
        mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("sourceCode", "quasarzone", "deals", List.of(other)))))
                .andExpect(status().isOk());
        mvc.perform(post(ROOT + "/known-external-ids").header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceCode":"ppomppu","externalIds":["991003"]}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.knownExternalIds").isEmpty());
        var current = item("991003");
        current.put("title", "다른 제목 (9,900원/무료)");
        mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(batch(current)))
                .andExpect(status().isOk());
        var pp = deals.findBySourceIdAndExternalId(sources.findByCode("ppomppu").orElseThrow().getId(), "991003").orElseThrow();
        var qz = deals.findBySourceIdAndExternalId(sources.findByCode("quasarzone").orElseThrow().getId(), "991003").orElseThrow();
        assertThat(pp.getDealGroup()).isNotNull();
        assertThat(pp.getDealGroup().getId()).isEqualTo(qz.getDealGroup().getId());
        assertThat(pp.getOriginalUrl()).isNotEqualTo(pp.getProductUrl());
    }

    @Test void rejectsInactiveSource() throws Exception {
        sources.saveAndFlush(new Source("뽐뿌", "https://www.ppomppu.co.kr", "ppomppu", false));
        mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(batch(item("991001"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post(ROOT + "/known-external-ids").header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceCode":"ppomppu","externalIds":["991001"]}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test void acceptsUnknownEndedButRejectsNullItemsAndInvalidKnownIdLists() throws Exception {
        var item = item("991001");
        item.remove("ended");
        mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(batch(item)))
                .andExpect(status().isOk());
        mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceCode\":\"ppomppu\",\"deals\":[null]}"))
                .andExpect(status().isBadRequest());
        for (Object ids : List.of(List.of(), List.of(" "), java.util.Collections.nCopies(151, "991001"))) {
            mvc.perform(post(ROOT + "/known-external-ids").header("Authorization", AUTH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("sourceCode", "ppomppu", "externalIds", ids))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test void unknownEndedDoesNotReactivateExpiredDeal() throws Exception {
        var item = item("991004");
        item.put("ended", true);
        mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(batch(item)))
                .andExpect(status().isOk());
        item.remove("ended");
        mvc.perform(post(ROOT).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(batch(item)))
                .andExpect(status().isOk());
        var saved = deals.findBySourceIdAndExternalId(sources.findByCode("ppomppu").orElseThrow().getId(), "991004").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(com.pickdeal.deal.domain.DealStatus.EXPIRED);
    }
}
