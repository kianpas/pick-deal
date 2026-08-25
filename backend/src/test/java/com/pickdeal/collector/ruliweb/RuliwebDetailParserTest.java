package com.pickdeal.collector.ruliweb;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.collector.support.CollectedProductInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuliwebDetailParserTest {

    private final RuliwebDetailParser parser = new RuliwebDetailParser();

    @Test
    @DisplayName("전용 출처 영역의 표시 외부 URL을 광고보다 우선한다")
    void parsesDisplayedSourceUrl() {
        CollectedProductInfo result = parser.parse(readFixture());

        assertThat(result.shopName()).isNull();
        assertThat(result.productUrl()).isEqualTo("https://s.lotteon.com/xt85P9DdUQ");
    }

    @Test
    @DisplayName("표시 URL이 없으면 Ruliweb 경유 링크의 ol 목적지를 사용한다")
    void decodesRuliwebRedirectDestination() {
        CollectedProductInfo result = parser.parse("""
                <div class="source_url">
                  <a href="//web.ruliweb.com/link.php?ol=https%3A%2F%2Fstore.example.com%2Fproducts%2F123&amp;bbs=1020">
                    구매처
                  </a>
                </div>
                """);

        assertThat(result.productUrl()).isEqualTo("https://store.example.com/products/123");
    }

    @Test
    @DisplayName("전용 출처 영역이 없으면 본문과 광고 링크를 상품 URL로 추측하지 않는다")
    void ignoresBodyAndAdLinks() {
        CollectedProductInfo result = parser.parse("""
                <article><a href="https://store.example.com/products/123">본문 링크</a></article>
                <aside><a href="https://ads.example.com">광고</a></aside>
                """);

        assertThat(result.productUrl()).isNull();
    }

    private static String readFixture() {
        try (InputStream in = RuliwebDetailParserTest.class
                .getResourceAsStream("/fixtures/ruliweb/hotdeal-detail.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
