package com.pickdeal.collector.remote;

import static org.assertj.core.api.Assertions.*;
import com.pickdeal.collector.ppomppu.PpomppuListParser;
import com.pickdeal.collector.support.CollectedDeal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.SourceRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

/** 외부 사이트 대신 fixture를 쓰되 수집기→HTTP→서버→테스트 DB는 실제 연결한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:collector_remote;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "pickdeal.read-only=true", "pickdeal.collector.ingress.enabled=true",
        "pickdeal.collector.ingress.token=test-only-collector-token-0123456789"})
@DirtiesContext
class RemoteCollectionHttpTest {
    @LocalServerPort int port;
    @Autowired DealRepository deals;
    @Autowired SourceRepository sources;

    @Test void sendsParsedFixtureAndReplaysUsingRealHttpWithoutCollectorDatabase() throws Exception {
        String html;
        try (var input = getClass().getResourceAsStream("/fixtures/ppomppu/hotdeal-list.html")) {
            html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var parsed = new PpomppuListParser().parse(html).stream().map(i -> new CollectedDeal(i.externalId(), i.url(),
                i.storeName(), i.title(), i.price(), i.category(), i.commentCount(), i.thumbnailUrl(), null,
                LocalSources.ppomppuTime(i.postedAtText()), null)).toList();
        var source = new RemoteCollectionRunner.Source() {
            public String code() { return "ppomppu"; }
            public List<CollectedDeal> list(int page) { return parsed; }
            public boolean supportsDetails() { return false; }
            public CollectedDeal detail(CollectedDeal deal) { throw new AssertionError("No ppomppu detail requests"); }
        };
        try (var api = new RemoteApiClient("http://127.0.0.1:" + port, "test-only-collector-token-0123456789")) {
            var runner = new RemoteCollectionRunner(api, 1, 3);
            runner.run(source);
            runner.run(source);
            assertThat(api.known("ppomppu", parsed.stream().map(CollectedDeal::externalId).toList()).ids()).hasSize(4);
            long sourceId = sources.findByCode("ppomppu").orElseThrow().getId();
            assertThat(deals.findKnownExternalIds(sourceId, parsed.stream().map(CollectedDeal::externalId).toList())).hasSize(4);
            var saved = deals.findBySourceIdAndExternalId(sourceId, "735731").orElseThrow();
        assertThat(saved.getPrice()).isEqualByComparingTo("24800");
            assertThat(saved.getPostedAt()).isEqualTo(LocalSources.ppomppuTime("26.09.20 14:07:27"));
            assertThat(saved.getProductUrl()).isNull();
        }
    }

    @Test
    void sendsUsdThroughRunnerAndHttpWithExactCents() throws Exception {
        var item = new CollectedDeal("1989607", "https://quasarzone.com/bbs/qb_saleinfo/views/1989607",
                "알리", "미니PC", new java.math.BigDecimal("382.76"), "PC/하드웨어", 0,
                null, false, java.time.OffsetDateTime.now(), null, "USD");
        var source = new RemoteCollectionRunner.Source() {
            public String code() { return "quasarzone"; }
            public List<CollectedDeal> list(int page) { return List.of(item); }
            public CollectedDeal detail(CollectedDeal deal) { return deal.withProductInfo("알리", null); }
        };
        try (var api = new RemoteApiClient("http://127.0.0.1:" + port, "test-only-collector-token-0123456789")) {
            var runner = new RemoteCollectionRunner(api, 1, 1);
            runner.run(source);
            runner.run(source);
            long sourceId = sources.findByCode("quasarzone").orElseThrow().getId();
            var saved = deals.findBySourceIdAndExternalId(sourceId, "1989607").orElseThrow();
            assertThat(saved.getPrice()).isEqualByComparingTo("382.76");
            assertThat(saved.getCurrency()).isEqualTo("USD");
            var response = java.net.http.HttpClient.newHttpClient().send(
                    java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                            "http://127.0.0.1:" + port + "/api/v1/deals/" + saved.getId())).GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            var data = tools.jackson.databind.json.JsonMapper.builder().build().readTree(response.body()).get("data");
            assertThat(data.get("price").decimalValue()).isEqualByComparingTo("382.76");
            assertThat(data.get("currency").asString()).isEqualTo("USD");
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "PICKDEAL_LIVE_COLLECTOR_TEST", matches = "true")
    void singleLivePpomppuListToIsolatedTestReceiver() {
        try (var api = new RemoteApiClient("http://127.0.0.1:" + port, "test-only-collector-token-0123456789");
                var clients = new LocalSources()) {
            // 명시적 실행에 한해 목록 1회, 상세 0회. 테스트 전용 H2에만 저장한다.
            new RemoteCollectionRunner(api, 1, 0).run(clients.source("ppomppu"));
            var source = sources.findByCode("ppomppu").orElseThrow();
            assertThat(deals.existsBySourceId(source.getId())).isTrue();
        }
    }
}
