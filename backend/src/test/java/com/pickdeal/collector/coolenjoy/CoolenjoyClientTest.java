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
            assertThat(url).isEqualTo("https://coolenjoy.net/bbs/rss.php?bo_table=jirum");
            count.incrementAndGet();
            return new CoolenjoyClient.Response(200, "rss", null);
        });
        assertThat(client.fetchRss()).isEqualTo("rss");
        assertThatThrownBy(client::fetchRss).isInstanceOf(IllegalStateException.class);
        assertThat(count.get()).isEqualTo(1);
        clock.now = clock.now.plusSeconds(1200);
        assertThat(client.fetchRss()).isEqualTo("rss");
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
            assertThatThrownBy(client::fetchRss).hasMessageContaining("HTTP " + status);
            clock.now = clock.now.plus(Duration.ofHours(25));
            assertThatThrownBy(client::fetchRss).hasMessageContaining("대기");
            assertThat(count.get()).isEqualTo(1);
            clock.now = clock.now.plus(Duration.ofHours(23));
            assertThatThrownBy(client::fetchRss).hasMessageContaining("HTTP " + status);
            assertThat(count.get()).isEqualTo(2);
        }
    }

    @Test void failsWithoutRetryOnRedirectOrNetworkFailure() {
        var count = new AtomicInteger();
        var redirect = new CoolenjoyClient(new MutableClock(), url -> {
            count.incrementAndGet();
            return new CoolenjoyClient.Response(302, "", null);
        });
        assertThatThrownBy(redirect::fetchRss).hasMessageContaining("HTTP 302");
        assertThat(count.get()).isEqualTo(1);
        var failed = new CoolenjoyClient(new MutableClock(), url -> { throw new IOException("timeout"); });
        assertThatThrownBy(failed::fetchRss).isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(failed::fetchRss).hasMessageContaining("대기");
    }
}
