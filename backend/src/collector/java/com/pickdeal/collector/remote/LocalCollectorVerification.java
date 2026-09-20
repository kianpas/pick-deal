package com.pickdeal.collector.remote;

import com.pickdeal.collector.ppomppu.PpomppuListParser;
import com.pickdeal.collector.support.CollectedDeal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

/** 격리된 Docker 검증 전용. 고정 loopback 수신기와 마운트한 fixture만 사용한다. */
public final class LocalCollectorVerification {
    private static final String ORIGIN = "http://127.0.0.1:8080";
    private static final String ROOT = "/api/v1/internal/collected-deals";
    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("No arguments allowed");
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        boolean ready = false;
        for (int attempt = 0; attempt < 60; attempt++) {
            try {
                if (http.send(get("/api/v1/deals?size=1"), HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    ready = true;
                    break;
                }
            } catch (java.io.IOException ignored) { }
            Thread.sleep(1000);
        }
        check(ready, "Receiver did not become ready");
        String html = Files.readString(Path.of("/fixtures/ppomppu/hotdeal-list.html"));
        var items = new PpomppuListParser().parse(html).stream().map(i -> new CollectedDeal(i.externalId(), i.url(),
                i.storeName(), i.title(), i.price(), i.category(), i.commentCount(), i.thumbnailUrl(), null,
                LocalSources.ppomppuTime(i.postedAtText()), null)).toList();
        check(items.size() == 4, "Unexpected fixture");
        String token = System.getenv("COLLECTOR_INGRESS_TOKEN");
        var source = new RemoteCollectionRunner.Source() {
            public String code() { return "ppomppu"; }
            public List<CollectedDeal> list(int page) { check(page == 1, "Unexpected page"); return items; }
            public boolean supportsDetails() { return false; }
            public CollectedDeal detail(CollectedDeal deal) { throw new AssertionError("No external requests allowed"); }
        };
        try (var api = new RemoteApiClient(ORIGIN, token)) {
            var ids = items.stream().map(CollectedDeal::externalId).toList();
            check(!api.known("ppomppu", ids).hasCollectedDeals(), "Receiver DB must be empty");
            var runner = new RemoteCollectionRunner(api, 1, 0);
            runner.run(source);
            runner.run(source);
            check(api.known("ppomppu", ids).ids().size() == 4, "Stored IDs differ");
            var first = items.get(0);
            api.send("ppomppu", List.of(new CollectedDeal(first.externalId(), first.url(), first.storeName(), first.title(),
                    first.price(), first.category(), first.commentCount(), first.thumbnailUrl(), true, first.postedAt(), null)));
            api.send("ppomppu", List.of(first)); // 종료 미확인 재전송은 기존 EXPIRED를 유지해야 한다.
        }
        check(http.send(post(ROOT, null), HttpResponse.BodyHandlers.discarding()).statusCode() == 401, "Missing token not rejected");
        check(http.send(post(ROOT, "invalid-test-token"), HttpResponse.BodyHandlers.discarding()).statusCode() == 401, "Wrong token not rejected");
        check(http.send(post("/api/v1/keywords", token), HttpResponse.BodyHandlers.discarding()).statusCode() == 403, "Public writes unlocked");
        var response = http.send(get("/api/v1/deals?size=100"), HttpResponse.BodyHandlers.ofString());
        check(response.statusCode() == 200, "Public read failed");
        var data = JsonMapper.builder().build().readTree(response.body()).path("data");
        check(data.isArray() && data.size() == 4, "Duplicate/missing public deals");
        System.out.println("verification=PASS; parsed=4; replayed=4; missingToken=401; wrongToken=401; publicWrite=403; publicRead=200");
        System.out.println("collectorDatabase=false; externalSiteRequests=0; receiverDatabase=isolated-postgresql");
    }
    private static HttpRequest get(String path) {
        return HttpRequest.newBuilder(URI.create(ORIGIN + path)).timeout(Duration.ofSeconds(2)).GET().build();
    }
    private static HttpRequest post(String path, String token) {
        var request = HttpRequest.newBuilder(URI.create(ORIGIN + path)).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}"));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return request.build();
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
