package com.pickdeal.collector.quasarzone;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuasarzonePostedAtResolverTest {

    // 목록의 게시 시각은 상대 표기뿐이라, 수집 시점 기준 근사 변환한다 (docs/notes 2026-07-09 결정)
    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 7, 9, 22, 0, 0, 0, ZoneOffset.ofHours(9));

    private final QuasarzonePostedAtResolver resolver = new QuasarzonePostedAtResolver();

    @Test
    @DisplayName("'N분 전' / 'N시간 전'을 기준 시각에서 뺀 근사 시각으로 변환한다")
    void resolvesRelativeMinutesAndHours() {
        assertThat(resolver.resolve("30분 전", NOW)).isEqualTo(NOW.minusMinutes(30));
        assertThat(resolver.resolve("5시간 전", NOW)).isEqualTo(NOW.minusHours(5));
    }

    @Test
    @DisplayName("'MM-DD' 표기는 기준 시각의 연도로 해석하고, 미래가 되면 작년으로 본다")
    void resolvesMonthDayText() {
        assertThat(resolver.resolve("07-08", NOW))
                .isEqualTo(OffsetDateTime.of(2026, 7, 8, 0, 0, 0, 0, ZoneOffset.ofHours(9)));
        // 연말에 수집한 "01-02" 같은 표기가 미래로 계산되면 작년 날짜다
        OffsetDateTime yearEnd = OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.ofHours(9));
        assertThat(resolver.resolve("12-31", yearEnd))
                .isEqualTo(OffsetDateTime.of(2025, 12, 31, 0, 0, 0, 0, ZoneOffset.ofHours(9)));
    }

    @Test
    @DisplayName("해석할 수 없는 표기는 null을 반환한다 (호출부가 수집 시각으로 대체)")
    void returnsNullForUnknownFormat() {
        assertThat(resolver.resolve("어제", NOW)).isNull();
        assertThat(resolver.resolve("", NOW)).isNull();
        assertThat(resolver.resolve(null, NOW)).isNull();
    }
}
