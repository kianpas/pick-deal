package com.pickdeal.collector.coolenjoy;

import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CoolenjoyClientTest {
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-04T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    @Test void usesFixedFeedAndEnforcesTwentyMinuteInterval() {
        var clock = new MutableClock();
        var count = new AtomicInteger();
        var client = new CoolenjoyClient(clock, url -> {
            assertThat(url).isEqualTo("https://coolenjoy.net/bbs/jirum");
            count.incrementAndGet();
            return new CoolenjoyClient.Response(200, "rss", null);
        });
        assertThat(client.fetchListHtml()).isEqualTo("rss");
        assertThatThrownBy(client::fetchListHtml).isInstanceOf(IllegalStateException.class);
        assertThat(count.get()).isEqualTo(1);
        clock.now = clock.now.plusSeconds(1200);
        assertThat(client.fetchListHtml()).isEqualTo("rss");
        assertThat(count.get()).isEqualTo(2);
    }

    @Test void blocksForTwentyFourHoursOrLongerRetryAfter() {
        for (int status : new int[]{403, 429}) {
            var clock = new MutableClock();
            var count = new AtomicInteger();
            var client = new CoolenjoyClient(clock, url -> {
                count.incrementAndGet();
                return new CoolenjoyClient.Response(status, "blocked", "172800");
            });
            assertThatThrownBy(client::fetchListHtml).hasMessageContaining("HTTP " + status);
            clock.now = clock.now.plus(Duration.ofHours(25));
            assertThatThrownBy(client::fetchListHtml).hasMessageContaining("대기");
            assertThat(count.get()).isEqualTo(1);
            clock.now = clock.now.plus(Duration.ofHours(23));
            assertThatThrownBy(client::fetchListHtml).hasMessageContaining("HTTP " + status);
            assertThat(count.get()).isEqualTo(2);
        }
    }

    @Test void failsWithoutRetryOnRedirectOrNetworkFailure() {
        var count = new AtomicInteger();
        var redirect = new CoolenjoyClient(new MutableClock(), url -> {
            count.incrementAndGet();
            return new CoolenjoyClient.Response(302, "", null);
        });
        assertThatThrownBy(redirect::fetchListHtml).hasMessageContaining("HTTP 302");
        assertThat(count.get()).isEqualTo(1);
        var failed = new CoolenjoyClient(new MutableClock(), url -> { throw new IOException("timeout"); });
        assertThatThrownBy(failed::fetchListHtml).isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(failed::fetchListHtml).hasMessageContaining("대기");
    }

    @Test void spacesListAndDetailsAndStopsImmediatelyAfterBlocking() {
        var clock = new MutableClock();
        var calls = new java.util.ArrayList<Instant>();
        var client = new CoolenjoyClient(clock, url -> {
            calls.add(clock.instant());
            return new CoolenjoyClient.Response(calls.size() == 3 ? 429 : 200, "html", null);
        }, millis -> clock.now = clock.now.plusMillis(millis));
        client.fetchListHtml();
        client.fetchDetailHtml("https://coolenjoy.net/bbs/jirum/1");
        assertThat(Duration.between(calls.get(0), calls.get(1))).isEqualTo(Duration.ofSeconds(10));
        assertThatThrownBy(() -> client.fetchDetailHtml("https://coolenjoy.net/bbs/jirum/2")).hasMessageContaining("HTTP 429");
        assertThat(Duration.between(calls.get(1), calls.get(2))).isEqualTo(Duration.ofSeconds(10));
        assertThatThrownBy(() -> client.fetchDetailHtml("https://coolenjoy.net/bbs/jirum/3")).hasMessageContaining("대기");
        assertThat(calls).hasSize(3);
        for (String url : new String[]{"http://coolenjoy.net/bbs/jirum/1", "https://evil.example/bbs/jirum/1", "https://coolenjoy.net/bbs/jirum/1?x=1"})
            assertThatThrownBy(() -> client.fetchDetailHtml(url)).isInstanceOf(IllegalArgumentException.class);
        assertThat(calls).hasSize(3);
    }
}
