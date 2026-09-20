package com.pickdeal.collector.remote;

import static org.assertj.core.api.Assertions.*;
import com.pickdeal.collector.support.CollectedDeal;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoteCollectionRunnerTest {
    private static final String TOKEN = "test-only-collector-token-0123456789";
    private HttpServer server;
    private RemoteApiClient api;
    private final List<String> requests = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private int knownStatus = 200;
    private int sendStatus = 200;
    private String known = "{\"data\":{\"knownExternalIds\":[\"1\"],\"hasCollectedDeals\":true}}";
    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            synchronized (requests) {
                requests.add(path);
                bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            }
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer " + TOKEN);
            boolean query = path.endsWith("known-external-ids");
            int received = query ? 0 : tools.jackson.databind.json.JsonMapper.builder().build()
                    .readTree(bodies.get(bodies.size() - 1)).path("deals").size();
            byte[] body = (query ? known : "{\"data\":{\"received\":" + received + ",\"created\":1,\"updated\":1}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Location", "/should-not-follow");
            exchange.sendResponseHeaders(query ? knownStatus : sendStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        api = new RemoteApiClient("http://127.0.0.1:" + server.getAddress().getPort(), TOKEN);
    }
    @AfterEach void stop() { api.close(); server.stop(0); }
    private CollectedDeal item(String id) {
        return new CollectedDeal(id, "https://quasarzone.com/bbs/qb_saleinfo/views/" + id, "몰", "상품", 100L,
                "기타", 0, null, false, java.time.OffsetDateTime.parse("2026-09-20T12:00:00+09:00"), null);
    }
    private RemoteCollectionRunner.Source source(AtomicInteger lists, AtomicInteger details, boolean detailFails) {
        return new RemoteCollectionRunner.Source() {
            public String code() { return "quasarzone"; }
            public List<CollectedDeal> list(int page) { lists.incrementAndGet(); return List.of(item("1"), item("2")); }
            public CollectedDeal detail(CollectedDeal item) {
                assertThat(item.externalId()).isEqualTo("2");
                details.incrementAndGet();
                if (detailFails) throw new IllegalStateException("detail failed");
                return item.withProductInfo("몰", "https://shop.example/product/2");
            }
        };
    }
    @Test void enrichesOnlyNewIdsAndSerializesOffsetTime() {
        var lists = new AtomicInteger(); var details = new AtomicInteger();
        new RemoteCollectionRunner(api, 3, 1).run(source(lists, details, false));
        assertThat(lists.get()).isEqualTo(1);
        assertThat(details.get()).isEqualTo(1);
        assertThat(bodies.get(1)).contains("https://shop.example/product/2", "2026-09-20T12:00:00+09:00");
    }
    @Test void detailFailureStillSendsList() {
        new RemoteCollectionRunner(api, 1, 1).run(source(new AtomicInteger(), new AtomicInteger(), true));
        assertThat(requests).hasSize(2);
        assertThat(bodies.get(1)).doesNotContain("https://shop.example");
    }
    @Test void retriesPendingBatchBeforeAnyNewFetchOnNextRun() {
        var lists = new AtomicInteger(); var details = new AtomicInteger();
        var source = source(lists, details, false);
        var runner = new RemoteCollectionRunner(api, 1, 1);
        sendStatus = 503;
        assertThatThrownBy(() -> runner.run(source)).isInstanceOf(RemoteApiClient.Failure.class);
        sendStatus = 200;
        runner.run(source);
        assertThat(lists.get()).isEqualTo(1);
        assertThat(details.get()).isEqualTo(1);
        assertThat(bodies.get(2)).isEqualTo(bodies.get(1));
    }
    @Test void unauthorizedKnownQueryStopsBeforeDetailsOrSave() {
        knownStatus = 401;
        var details = new AtomicInteger();
        assertThatThrownBy(() -> new RemoteCollectionRunner(api, 1, 3).run(source(new AtomicInteger(), details, false)))
                .isInstanceOf(RemoteApiClient.Failure.class)
                .satisfies(e -> assertThat(((RemoteApiClient.Failure)e).permanent()).isTrue());
        assertThat(details.get()).isZero();
        assertThat(requests).hasSize(1);
    }
    @Test void neverFollowsRedirectWithBearerToken() {
        knownStatus = 302;
        assertThatThrownBy(() -> api.known("quasarzone", List.of("1"))).isInstanceOf(RemoteApiClient.Failure.class);
        assertThat(requests).hasSize(1);
    }
    @Test void bootstrapPagesAreBoundedAndDeduplicated() {
        known = "{\"data\":{\"knownExternalIds\":[],\"hasCollectedDeals\":false}}";
        var lists = new AtomicInteger();
        new RemoteCollectionRunner(api, 3, 0).run(source(lists, new AtomicInteger(), false));
        assertThat(lists.get()).isEqualTo(3);
        assertThat(requests).hasSize(3);
    }
    @Test void failsClosedOnUnverifiedKnownResponse() {
        known = "{\"data\":{\"knownExternalIds\":[\"999\"],\"hasCollectedDeals\":false}}";
        assertThatThrownBy(() -> api.known("quasarzone", List.of("1"))).isInstanceOf(RemoteApiClient.Failure.class);
    }
    @Test void emptyReceiverResponseIsPermanentProtocolFailure() {
        known = "";
        assertThatThrownBy(() -> api.known("quasarzone", List.of("1")))
                .isInstanceOf(RemoteApiClient.Failure.class)
                .satisfies(e -> assertThat(((RemoteApiClient.Failure)e).permanent()).isTrue());
    }
    @Test void refusesPlainHttpOutsideLoopback() {
        assertThatThrownBy(() -> new RemoteApiClient("http://example.com", TOKEN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RemoteApiClient("https://user@example.com/path", TOKEN)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void parsesPpomppuAbsoluteTimeWithoutGuessingMissingDate() {
        assertThat(LocalSources.ppomppuTime("26.09.20 14:07:27").toString()).isEqualTo("2026-09-20T14:07:27+09:00");
        assertThat(LocalSources.ppomppuTime("14:07:27")).isNull();
        assertThat(LocalSources.ppomppuTime("26.02.31 14:07:27")).isNull();
    }

    @Test void capsIncrementalItemsAndDetailRequests() {
        known = "{\"data\":{\"knownExternalIds\":[],\"hasCollectedDeals\":true}}";
        var details = new AtomicInteger();
        var source = new RemoteCollectionRunner.Source() {
            public String code() { return "quasarzone"; }
            public List<CollectedDeal> list(int page) {
                assertThat(page).isEqualTo(1);
                return java.util.stream.IntStream.rangeClosed(1, 200).mapToObj(i -> item("" + i)).toList();
            }
            public CollectedDeal detail(CollectedDeal deal) {
                details.incrementAndGet();
                return deal.withProductInfo("x".repeat(400), "javascript:alert(1)");
            }
        };
        new RemoteCollectionRunner(api, 3, 3).run(source);
        assertThat(details.get()).isEqualTo(3);
        var json = tools.jackson.databind.json.JsonMapper.builder().build().readTree(bodies.get(1));
        assertThat(json.path("deals").size()).isEqualTo(50);
        assertThat(bodies.get(1)).doesNotContain("javascript:", "x".repeat(400));
    }
}
