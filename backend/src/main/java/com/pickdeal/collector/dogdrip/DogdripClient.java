package com.pickdeal.collector.dogdrip;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 목록과 상세에 같은 요청 간격·차단 대기를 적용한다. redirect/cookie 재사용/즉시 재시도 없음. */
@Component
public class DogdripClient {
    public static final String LIST_URL = "https://www.dogdrip.net/hotdeal";
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final Clock clock;
    private final Fetch fetch;
    private final Sleeper sleeper;
    private Instant nextRequest = Instant.MIN;
    private Instant blockedUntil = Instant.MIN;

    record Response(int status, String html, String retryAfter) {}
    @FunctionalInterface interface Fetch { Response get(String url) throws IOException; }
    @FunctionalInterface interface Sleeper { void sleep(long millis) throws InterruptedException; }

    @Autowired
    public DogdripClient(DogdripCollectorProperties properties) {
        this(Clock.systemUTC(), url -> {
            var response = Jsoup.connect(url).userAgent("PickDeal/1.0")
                    .timeout(Math.toIntExact(properties.timeout().toMillis()))
                    .followRedirects(false).ignoreHttpErrors(true).maxBodySize(MAX_BYTES + 1).execute();
            if (response.bodyAsBytes().length > MAX_BYTES) throw new IOException("Body limit exceeded");
            return new Response(response.statusCode(), response.body(), response.header("Retry-After"));
        });
    }

    DogdripClient(Clock clock, Fetch fetch) { this(clock, fetch, Thread::sleep); }

    DogdripClient(Clock clock, Fetch fetch, Sleeper sleeper) {
        this.clock = clock;
        this.fetch = fetch;
        this.sleeper = sleeper;
    }

    public synchronized String fetchListHtml() {
        if (clock.instant().isBefore(nextRequest)) throw new IllegalStateException("개드립 요청 대기 중: " + nextRequest);
        return request(LIST_URL);
    }

    public synchronized String fetchDetailHtml(String url) {
        if (url == null || !url.matches("https://www\\.dogdrip\\.net/[1-9][0-9]{0,19}")) {
            throw new IllegalArgumentException("개드립 상세 URL이 아님");
        }
        if (clock.instant().isBefore(blockedUntil)) throw new IllegalStateException("개드립 차단 대기 중: " + blockedUntil);
        while (clock.instant().isBefore(nextRequest)) {
            try {
                sleeper.sleep(Math.max(1, Duration.between(clock.instant(), nextRequest).toMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("개드립 요청 대기 중단", e);
            }
        }
        return request(url);
    }

    private String request(String url) {
        try {
            Response response = fetch.get(url);
            if (response.status() == 403 || response.status() == 429) {
                nextRequest = clock.instant().plus(Duration.ofHours(24));
                Instant specified = retryAt(response.retryAfter());
                if (specified != null && specified.isAfter(nextRequest)) nextRequest = specified;
                blockedUntil = nextRequest;
            }
            if (response.status() != 200) throw new IllegalStateException("개드립 HTTP " + response.status());
            return response.html();
        } catch (IOException e) {
            throw new UncheckedIOException("개드립 요청 실패", e);
        } finally {
            Instant minimum = clock.instant().plusSeconds(10);
            if (nextRequest.isBefore(minimum)) nextRequest = minimum;
        }
    }

    private Instant retryAt(String value) {
        if (value == null) return null;
        try {
            return value.trim().matches("[0-9]+") ? clock.instant().plusSeconds(Long.parseLong(value.trim()))
                    : ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (RuntimeException ignored) { return null; }
    }
}
