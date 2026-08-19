package com.pickdeal.collector.ruliweb;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuliwebPostedAtResolverTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 8, 19, 21, 30, 0, 0, ZoneOffset.ofHours(9));

    private final RuliwebPostedAtResolver resolver = new RuliwebPostedAtResolver();

    @Test
    @DisplayName("'HH:mm'은 오늘 그 시각으로 해석한다")
    void resolvesTodayTime() {
        assertThat(resolver.resolve("19:19", NOW))
                .isEqualTo(OffsetDateTime.of(2026, 8, 19, 19, 19, 0, 0, ZoneOffset.ofHours(9)));
    }

    @Test
    @DisplayName("기준 시각보다 미래인 'HH:mm'은 어제 글로 본다 (자정 경계)")
    void resolvesFutureTimeAsYesterday() {
        OffsetDateTime earlyMorning = OffsetDateTime.of(2026, 8, 19, 0, 30, 0, 0, ZoneOffset.ofHours(9));
        assertThat(resolver.resolve("23:50", earlyMorning))
                .isEqualTo(OffsetDateTime.of(2026, 8, 18, 23, 50, 0, 0, ZoneOffset.ofHours(9)));
    }

    @Test
    @DisplayName("'YYYY.MM.DD'는 그 날 자정으로 해석한다")
    void resolvesAbsoluteDate() {
        assertThat(resolver.resolve("2026.08.14", NOW))
                .isEqualTo(OffsetDateTime.of(2026, 8, 14, 0, 0, 0, 0, ZoneOffset.ofHours(9)));
    }

    @Test
    @DisplayName("해석할 수 없는 표기는 null을 반환한다 (호출부가 수집 시각으로 대체)")
    void returnsNullForUnknownFormat() {
        assertThat(resolver.resolve("어제", NOW)).isNull();
        assertThat(resolver.resolve("", NOW)).isNull();
        assertThat(resolver.resolve(null, NOW)).isNull();
    }
}
