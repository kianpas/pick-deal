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

    @Test
    @DisplayName("링크 행이 없으면 본문(org_contents)의 외부 링크를 상품 URL로 쓴다")
    void fallsBackToBodyLink() {
        CollectedProductInfo result = parser.parse(readFixture("saleinfo-detail-body-link.html"));

        assertThat(result.shopName()).isEqualTo("SSG");
        // 배너·광고는 본문 밖이라 선택되면 안 된다
        assertThat(result.productUrl()).isEqualTo("http://ssg.li/dh3w3CMOOx");
    }

    @Test
    @DisplayName("본문에 쓸 만한 외부 링크가 없으면 상품 URL을 만들지 않는다")
    void ignoresBodyWithoutExternalLink() {
        CollectedProductInfo result = parser.parse("""
                <a href="https://ads.example.com/banner">본문 밖 광고</a>
                <textarea id="org_contents"><p>이미지만 있는 본문</p><p><a href="https://quasarzone.com/bbs/qb_saleinfo/views/1">내부 링크</a></p></textarea>
                """);

        assertThat(result.productUrl()).isNull();
    }

    private static String readFixture() {
        return readFixture("saleinfo-detail.html");
    }

    private static String readFixture(String name) {
        try (InputStream in = QuasarzoneDetailParserTest.class
                .getResourceAsStream("/fixtures/quasarzone/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
