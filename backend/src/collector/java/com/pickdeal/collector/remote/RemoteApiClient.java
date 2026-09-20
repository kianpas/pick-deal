package com.pickdeal.collector.remote;

import com.pickdeal.collector.dto.CollectionRequests;
import com.pickdeal.collector.support.CollectedDeal;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import okhttp3.*;
import tools.jackson.databind.json.JsonMapper;

/** 토큰은 수신 서버에만 전송한다. redirect/쿠키/프록시/즉시 재시도 없음. */
public final class RemoteApiClient implements AutoCloseable {
    public record Known(Set<String> ids, boolean hasCollectedDeals) {}
    public static final class Failure extends RuntimeException {
        private final boolean permanent;
        Failure(String message, boolean permanent) { super(message); this.permanent = permanent; }
        public boolean permanent() { return permanent; }
    }
    private static final String ROOT = "/api/v1/internal/collected-deals";
    private final String baseUrl;
    private final String token;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final OkHttpClient client = new OkHttpClient.Builder().proxy(Proxy.NO_PROXY)
            .cookieJar(CookieJar.NO_COOKIES).followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).callTimeout(Duration.ofSeconds(20)).build();

    public RemoteApiClient(String baseUrl, String token) {
        URI uri = URI.create(baseUrl);
        boolean loopback = Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost() == null ? "" : uri.getHost());
        if (!("https".equals(uri.getScheme()) || (loopback && "http".equals(uri.getScheme())))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getRawQuery() != null
                || uri.getFragment() != null || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))) {
            throw new IllegalArgumentException("Receiver must be an HTTPS origin (HTTP only for loopback tests)");
        }
        if (token == null || !token.matches("[A-Za-z0-9_-]{32,256}")) throw new IllegalArgumentException("Invalid collector token format");
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.token = token;
    }

    public Known known(String source, List<String> ids) {
        var data = post(ROOT + "/known-external-ids", new CollectionRequests.KnownIds(source, ids));
        if (!data.path("knownExternalIds").isArray() || !data.path("hasCollectedDeals").isBoolean()) {
            throw new Failure("Invalid known-ID response", true);
        }
        Set<String> known = new HashSet<>();
        data.path("knownExternalIds").forEach(node -> {
            if (!node.isString() || !ids.contains(node.asString())) throw new Failure("Unexpected known ID", true);
            known.add(node.asString());
        });
        return new Known(Set.copyOf(known), data.path("hasCollectedDeals").asBoolean());
    }

    public void send(String source, List<CollectedDeal> deals) {
        var items = deals.stream().map(d -> new CollectionRequests.Item(d.externalId(), d.url(), d.storeName(),
                d.title(), d.price(), d.category(), d.commentCount(), d.thumbnailUrl(), d.ended(), d.postedAt(), d.productUrl())).toList();
        var result = post(ROOT, new CollectionRequests.Batch(source, items));
        if (!result.path("received").isIntegralNumber() || result.path("received").asInt() != deals.size()) {
            throw new Failure("Unconfirmed batch acceptance", true);
        }
    }

    private tools.jackson.databind.JsonNode post(String path, Object value) {
        String json = mapper.writeValueAsString(value);
        if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1024 * 1024) throw new Failure("Batch exceeds 1MiB", true);
        var request = new Request.Builder().url(baseUrl + path).header("Authorization", "Bearer " + token)
                .post(RequestBody.create(json, MediaType.get("application/json; charset=utf-8"))).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) throw new Failure("Receiver HTTP " + response.code(),
                    response.code() < 500 && response.code() != 429);
            try (var body = response.peekBody(65537)) {
                if (body.contentLength() > 65536) throw new Failure("Receiver response too large", true);
                var root = mapper.readTree(body.string());
                if (root == null || !root.path("data").isObject() || root.hasNonNull("error")) throw new Failure("Invalid receiver response", true);
                return root.path("data");
            }
        } catch (IOException e) {
            throw new Failure("Receiver connection failed", false);
        } catch (tools.jackson.core.JacksonException e) {
            throw new Failure("Invalid receiver JSON", true);
        }
    }

    @Override public void close() {
        client.dispatcher().executorService().shutdownNow();
        client.connectionPool().evictAll();
    }
}
