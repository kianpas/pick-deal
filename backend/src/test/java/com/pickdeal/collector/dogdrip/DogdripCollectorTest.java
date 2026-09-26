package com.pickdeal.collector.dogdrip;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import org.junit.jupiter.api.Test;

class DogdripCollectorTest {
    static String fixture() throws Exception {
        try (var input = DogdripCollectorTest.class.getResourceAsStream("/fixtures/dogdrip/hotdeal-list.html")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private final OffsetDateTime now = OffsetDateTime.parse("2026-09-21T20:00:00+09:00");

    @Test void actualList() throws Exception {
        var deals = new DogdripListParser().parse(fixture(), now);
        assertThat(deals).hasSize(3);
        var first = deals.get(0);
        assertThat(first.externalId()).isEqualTo("725854470");
        assertThat(first.storeName()).isEqualTo("자사몰");
        assertThat(first.price()).isEqualTo(399840L);
        assertThat(first.category()).isEqualTo("생활용품");
        assertThat(first.postedAt()).isEqualTo(now.minusMinutes(6));
        assertThat(first.thumbnailUrl()).startsWith("https://www.dogdrip.net/files/thumbnails/");
        assertThat(first.commentCount()).isNull();
        assertThat(first.ended()).isNull();
        assertThat(first.productUrl()).isNull();
        assertThat(deals.get(1).price()).isNull();
        assertThat(deals.get(2).ended()).isTrue();
        assertThat(deals.get(2).commentCount()).isEqualTo(1);
        assertThat(deals.get(2).thumbnailUrl()).isNull();
    }

    @Test void invalidPagesAndLinks() throws Exception {
        var parser = new DogdripListParser();
        assertThatThrownBy(() -> parser.parse("<title>Just a moment</title>", now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(fixture().replace("class=\"ed list\"", "class=\"other\""), now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(fixture().replace("https://www.dogdrip.net/", "https://evil.test/"), now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(fixture().replace("flex-middle webzine", "flex-middle webzine notice"), now)).isInstanceOf(IllegalArgumentException.class);
        assertThat(parser.parse(fixture() + fixture(), now)).hasSize(3);
        assertThat(parser.parse(fixture().replace("399,840원", "399.84 USD"), now).get(0).price()).isNull();
        assertThat(parser.parse(fixture().replace("399,840원", "클릭 78원"), now).get(0).price()).isNull();
    }

    @Test void minimumIntervalWithoutSleeping() throws Exception {
        Clock clock = mock(Clock.class);
        Instant start = now.toInstant();
        when(clock.instant()).thenReturn(start);
        DogdripClient.Fetch fetch = mock(DogdripClient.Fetch.class);
        when(fetch.get(anyString())).thenReturn(new DogdripClient.Response(200, "ok", null));
        var client = new DogdripClient(clock, fetch);
        assertThat(client.fetchListHtml()).isEqualTo("ok");
        assertThatThrownBy(client::fetchListHtml).isInstanceOf(IllegalStateException.class);
        verify(fetch, times(1)).get(anyString());
        when(clock.instant()).thenReturn(start.plusSeconds(10));
        client.fetchListHtml();
        verify(fetch, times(2)).get(anyString());
    }

    @Test void blockCooldownAndRetryAfter() throws Exception {
        for (int status : new int[]{403, 429}) {
            Clock clock = mock(Clock.class);
            Instant start = now.toInstant();
            when(clock.instant()).thenReturn(start);
            DogdripClient.Fetch fetch = mock(DogdripClient.Fetch.class);
            when(fetch.get(anyString())).thenReturn(new DogdripClient.Response(status, "", "172800"));
            var client = new DogdripClient(clock, fetch);
            assertThatThrownBy(client::fetchListHtml).isInstanceOf(IllegalStateException.class);
            when(clock.instant()).thenReturn(start.plusSeconds(86400));
            assertThatThrownBy(client::fetchListHtml).isInstanceOf(IllegalStateException.class);
            verify(fetch, times(1)).get(anyString());
        }
    }

    @Test void noImmediateRetryOnRedirect() throws Exception {
        DogdripClient.Fetch fetch = mock(DogdripClient.Fetch.class);
        when(fetch.get(anyString())).thenReturn(new DogdripClient.Response(302, "", null));
        assertThatThrownBy(new DogdripClient(Clock.systemUTC(), fetch)::fetchListHtml).isInstanceOf(IllegalStateException.class);
        verify(fetch, times(1)).get(anyString());
    }

    @Test void detailSharesIntervalAndBlockCooldownWithList() throws Exception {
        Clock clock = mock(Clock.class);
        var time = new java.util.concurrent.atomic.AtomicReference<>(now.toInstant());
        when(clock.instant()).thenAnswer(call -> time.get());
        DogdripClient.Fetch fetch = mock(DogdripClient.Fetch.class);
        when(fetch.get(anyString())).thenReturn(new DogdripClient.Response(200, "list", null),
                new DogdripClient.Response(429, "", "172800"));
        DogdripClient.Sleeper sleeper = mock(DogdripClient.Sleeper.class);
        doAnswer(call -> { time.updateAndGet(t -> t.plusMillis(call.getArgument(0, Long.class))); return null; })
                .when(sleeper).sleep(anyLong());
        var client = new DogdripClient(clock, fetch, sleeper);
        client.fetchListHtml();
        assertThatThrownBy(() -> client.fetchDetailHtml("https://www.dogdrip.net/725854470"))
                .isInstanceOf(IllegalStateException.class);
        verify(sleeper).sleep(10000L);
        assertThatThrownBy(() -> client.fetchDetailHtml("https://www.dogdrip.net/725757041"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(client::fetchListHtml).isInstanceOf(IllegalStateException.class);
        verify(fetch, times(2)).get(anyString());
        verifyNoMoreInteractions(sleeper);
    }

    @Test void invalidDetailUrlsNeverSendRequests() {
        DogdripClient.Fetch fetch = mock(DogdripClient.Fetch.class);
        var client = new DogdripClient(Clock.systemUTC(), fetch);
        for (String url : new String[]{"https://evil.test/123", "https://www.dogdrip.net/link.php",
                "https://www.dogdrip.net/123?act=IS", "https://www.dogdrip.net/123#link", "http://www.dogdrip.net/123"}) {
            assertThatThrownBy(() -> client.fetchDetailHtml(url)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(fetch);
    }
}
