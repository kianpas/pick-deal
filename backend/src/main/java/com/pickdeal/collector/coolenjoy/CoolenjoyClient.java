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

/** 고정 RSS 주소에만 요청한다. 상세 요청·redirect·쿠키 재사용·즉시 재시도 없음. */
@Component
public class CoolenjoyClient {
    public static final String RSS_URL = "https://coolenjoy.net/bbs/rss.php?bo_table=jirum";
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final Clock clock;
    private final Fetch fetch;
    private Instant nextRequest = Instant.MIN;

    record Response(int status, String body, String retryAfter) {}
    @FunctionalInterface interface Fetch { Response get(String url) throws IOException; }

    @Autowired
    public CoolenjoyClient(CoolenjoyCollectorProperties properties) {
        this(Clock.systemUTC(), url -> {
            var response = Jsoup.connect(url).userAgent("PickDeal/1.0")
                    .timeout(Math.toIntExact(properties.timeout().toMillis()))
                    .followRedirects(false).ignoreHttpErrors(true).ignoreContentType(true)
                    .maxBodySize(MAX_BYTES + 1).execute();
            if (response.bodyAsBytes().length > MAX_BYTES) throw new IOException("RSS body limit exceeded");
            return new Response(response.statusCode(), response.body(), response.header("Retry-After"));
        });
    }

    CoolenjoyClient(Clock clock, Fetch fetch) {
        this.clock = clock;
        this.fetch = fetch;
    }

    public synchronized String fetchRss() {
        if (clock.instant().isBefore(nextRequest)) throw new IllegalStateException("쿨엔조이 요청 대기 중: " + nextRequest);
        try {
            var response = fetch.get(RSS_URL);
            if (response.status() == 403 || response.status() == 429) {
                nextRequest = clock.instant().plus(Duration.ofHours(24));
                var specified = retryAt(response.retryAfter());
                if (specified != null && specified.isAfter(nextRequest)) nextRequest = specified;
            }
            if (response.status() != 200) throw new IllegalStateException("쿨엔조이 RSS HTTP " + response.status());
            return response.body();
        } catch (IOException e) {
            throw new UncheckedIOException("쿨엔조이 RSS 요청 실패", e);
        } finally {
            var minimum = clock.instant().plus(Duration.ofMinutes(20));
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
