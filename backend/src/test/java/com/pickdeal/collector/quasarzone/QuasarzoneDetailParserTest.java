package com.pickdeal.collector.quasarzone;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.collector.support.CollectedProductInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuasarzoneDetailParserTest {

    private final QuasarzoneDetailParser parser = new QuasarzoneDetailParser();

    @Test
    @DisplayName("전용 상세 정보 표에서 판매처와 표시 상품 URL을 추출한다")
    void parsesProductInfoTable() {
        CollectedProductInfo result = parser.parse(readFixture());

        assertThat(result.shopName()).isEqualTo("쿠팡");
        assertThat(result.productUrl())
                .isEqualTo("https://www.coupang.com/vp/products/9320059618?vendorItemId=95248275824");
    }

    @Test
    @DisplayName("링크 표가 없거나 HTTP URL이 아니면 상품 URL을 만들지 않는다")
    void rejectsUntrustedLinks() {
        CollectedProductInfo result = parser.parse("""
                <table><tr><th>링크</th><td>javascript:openAd()</td></tr></table>
                <a href="https://ads.example.com">광고</a>
                """);

        assertThat(result.shopName()).isNull();
        assertThat(result.productUrl()).isNull();
    }

    @Test
    @DisplayName("전용 표에 있더라도 퀘이사존 내부 URL은 상품 URL로 저장하지 않는다")
    void rejectsSourceSiteUrl() {
        CollectedProductInfo result = parser.parse("""
                <table><tr><th>링크</th><td>https://quasarzone.com/bbs/qb_saleinfo/views/123</td></tr></table>
                """);

        assertThat(result.productUrl()).isNull();
    }

    private static String readFixture() {
        try (InputStream in = QuasarzoneDetailParserTest.class
                .getResourceAsStream("/fixtures/quasarzone/saleinfo-detail.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
