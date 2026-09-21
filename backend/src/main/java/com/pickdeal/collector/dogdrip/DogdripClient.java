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

/** 고정 목록 1회만 요청. redirect/cookie 재사용/즉시 재시도/상세 요청 없음. */
@Component
public class DogdripClient {
    public static final String LIST_URL = "https://www.dogdrip.net/hotdeal";
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final Clock clock;
    private final Fetch fetch;
    private Instant nextRequest = Instant.MIN;

    record Response(int status, String html, String retryAfter) {}
    @FunctionalInterface interface Fetch { Response get() throws IOException; }

    @Autowired
    public DogdripClient(DogdripCollectorProperties properties) {
        this(Clock.systemUTC(), () -> {
            var response = Jsoup.connect(LIST_URL).userAgent("PickDeal/1.0")
                    .timeout(Math.toIntExact(properties.timeout().toMillis()))
                    .followRedirects(false).ignoreHttpErrors(true).maxBodySize(MAX_BYTES + 1).execute();
            if (response.bodyAsBytes().length > MAX_BYTES) throw new IOException("Body limit exceeded");
            return new Response(response.statusCode(), response.body(), response.header("Retry-After"));
        });
    }

    DogdripClient(Clock clock, Fetch fetch) { this.clock = clock; this.fetch = fetch; }

    public synchronized String fetchListHtml() {
        if (clock.instant().isBefore(nextRequest)) throw new IllegalStateException("개드립 요청 대기 중: " + nextRequest);
        try {
            Response response = fetch.get();
            if (response.status() == 403 || response.status() == 429) {
                nextRequest = clock.instant().plus(Duration.ofHours(24));
                Instant specified = retryAt(response.retryAfter());
                if (specified != null && specified.isAfter(nextRequest)) nextRequest = specified;
            }
            if (response.status() != 200) throw new IllegalStateException("개드립 HTTP " + response.status());
            return response.html();
        } catch (IOException e) {
            throw new UncheckedIOException("개드립 목록 요청 실패", e);
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
