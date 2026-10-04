package com.pickdeal.collector.coolenjoy;

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

/** 목록·상세 요청 간 10초 간격. redirect·쿠키 재사용·즉시 재시도 없음. */
@Component
public class CoolenjoyClient {
    public static final String LIST_URL = "https://coolenjoy.net/bbs/jirum";
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final Clock clock;
    private final Fetch fetch;
    private final Sleeper sleeper;
    private Instant nextRequest = Instant.MIN;
    private Instant nextList = Instant.MIN;
    private Instant blockedUntil = Instant.MIN;

    record Response(int status, String body, String retryAfter) {}
    @FunctionalInterface interface Fetch { Response get(String url) throws IOException; }
    @FunctionalInterface interface Sleeper { void sleep(long millis) throws InterruptedException; }

    @Autowired
    public CoolenjoyClient(CoolenjoyCollectorProperties properties) {
        this(Clock.systemUTC(), url -> {
            var response = Jsoup.connect(url).userAgent("PickDeal/1.0")
                    .timeout(Math.toIntExact(properties.timeout().toMillis()))
                    .followRedirects(false).ignoreHttpErrors(true).ignoreContentType(true)
                    .maxBodySize(MAX_BYTES + 1).execute();
            if (response.bodyAsBytes().length > MAX_BYTES) throw new IOException("HTML 목록 body limit exceeded");
            return new Response(response.statusCode(), response.body(), response.header("Retry-After"));
        });
    }

    CoolenjoyClient(Clock clock, Fetch fetch) {
        this(clock, fetch, Thread::sleep);
    }

    CoolenjoyClient(Clock clock, Fetch fetch, Sleeper sleeper) {
        this.clock = clock;
        this.fetch = fetch;
        this.sleeper = sleeper;
    }

    public synchronized String fetchListHtml() {
        if (clock.instant().isBefore(nextList)) throw new IllegalStateException("쿨엔조이 요청 대기 중: " + nextList);
        try { return request(LIST_URL); }
        finally { nextList = clock.instant().plus(Duration.ofMinutes(20)); }
    }

    public synchronized String fetchDetailHtml(String url) {
        if (url == null || !url.matches("https://coolenjoy\\.net/bbs/jirum/[1-9][0-9]{0,19}"))
            throw new IllegalArgumentException("쿨엔조이 상세 URL이 아님");
        return request(url);
    }

    private String request(String url) {
        if (clock.instant().isBefore(blockedUntil)) throw new IllegalStateException("쿨엔조이 차단 대기 중: " + blockedUntil);
        while (clock.instant().isBefore(nextRequest)) {
            try { sleeper.sleep(Math.max(1, Duration.between(clock.instant(), nextRequest).toMillis())); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("쿨엔조이 요청 대기 중단", e);
            }
        }
        try {
            var response = fetch.get(url);
            if (response.status() == 403 || response.status() == 429) {
                nextRequest = clock.instant().plus(Duration.ofHours(24));
                var specified = retryAt(response.retryAfter());
                if (specified != null && specified.isAfter(nextRequest)) nextRequest = specified;
                blockedUntil = nextRequest;
            }
            if (response.status() != 200) throw new IllegalStateException("쿨엔조이 HTML 목록 HTTP " + response.status());
            return response.body();
        } catch (IOException e) {
            throw new UncheckedIOException("쿨엔조이 HTML 목록 요청 실패", e);
        } finally {
            var minimum = clock.instant().plusSeconds(10);
            if (nextRequest.isBefore(minimum)) nextRequest = minimum;
        }
    }

    private Instant retryAt(String value) {
        if (value == null) return null;
        try {
            return value.trim().matches("[0-9]+") ? clock.instant().plusSeconds(Long.parseLong(value.trim()))
                    : ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (RuntimeException ignored) { return null; }
    }
}
