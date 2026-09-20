package com.pickdeal.collector.ppomppu;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.io.ByteArrayInputStream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PpomppuListParserTest {
    private final PpomppuListParser parser = new PpomppuListParser();

    private String fixture() throws Exception {
        try (var input = getClass().getResourceAsStream("/fixtures/ppomppu/hotdeal-list.html")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsesRealRowsAndExcludesNoticesAdvertisementsAndOtherBoards() throws Exception {
        var items = parser.parse(fixture());
        assertThat(items).extracting(PpomppuDealItem::externalId).containsExactly("735626", "735731", "735727", "735716");
        var first = items.get(0);
        assertThat(first.title()).isEqualTo("제주 햇감귤 4kg 로얄과 (8,980원/무료배송)");
        assertThat(first.storeName()).isEqualTo("G마켓");
        assertThat(first.price()).isEqualTo(8980L);
        assertThat(first.url()).isEqualTo("https://www.ppomppu.co.kr/zboard/view.php?id=ppomppu&no=735626");
        assertThat(first.category()).isEqualTo("식품/건강");
        assertThat(first.commentCount()).isEqualTo(16);
        assertThat(first.thumbnailUrl()).startsWith("https://cdn3.ppomppu.co.kr/");
        assertThat(first.postedAtText()).isEqualTo("26.09.19 19:43:05");
        assertThat(items.get(1).price()).isEqualTo(24800L);
        assertThat(items.get(1).commentCount()).isNull();
        assertThat(items.get(2).price()).isNull();
        assertThat(items.get(3).price()).isNull();
        assertThat(items.get(3).title()).contains("[큐냅 나스 스토리지 하드미포함]");
    }

    @Test
    void detectsEucKrWithoutCorruptingKorean() throws Exception {
        String html = fixture().replace("charset=\"utf-8\"", "charset=\"euc-kr\"");
        var bytes = html.getBytes(Charset.forName("EUC-KR"));
        var doc = Jsoup.parse(new ByteArrayInputStream(bytes), null, PpomppuListParser.BASE_URL);
        assertThat(parser.parse(doc.outerHtml()).get(0).title())
                .isEqualTo("제주 햇감귤 4kg 로얄과 (8,980원/무료배송)");
    }

    @Test
    void deduplicatesPopularAndNormalRows() throws Exception {
        var doc = Jsoup.parse(fixture());
        doc.selectFirst("table").appendChild(doc.selectFirst("tr.hotpop_bg_color").clone());
        assertThat(parser.parse(doc.outerHtml())).hasSize(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"상품 (29.99달러/무료)", "상품 ($29/무료)", "상품 (카드 9,900원/무료)",
            "상품 (9,900원~/무료)", "상품 (9,900/무료)", "상품 (무료/무료)", "상품 9/20",
            "상품 (12,34원/무료)", "상품 (99999999999999999999999원/무료)"})
    void doesNotGuessAmbiguousPrices(String title) throws Exception {
        assertThat(parser.parse(withTitle(title)).get(0).price()).isNull();
    }

    @Test
    void permitsExplicitZeroPriceAndPreservesTitle() throws Exception {
        assertThat(parser.parse(withTitle("상품 (0원/무료)")).get(0).price()).isZero();
    }

    private String withTitle(String title) throws Exception {
        var doc = Jsoup.parse(fixture());
        doc.selectFirst("tr.baseList a.baseList-title").text(title);
        return doc.outerHtml();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example/zboard/view.php?id=ppomppu&no=735626",
            "view.php?id=pmarket&no=735626", "view.php?id=ppomppu&no=735626&no=2",
            "view.php?id=ppomppu&no=999", "javascript:alert(1)", "view.php?id=ppomppu&no=bad"})
    void rejectsWrongLinks(String href) throws Exception {
        var doc = Jsoup.parse(fixture());
        doc.selectFirst("tr.baseList a.baseList-title").attr("href", href);
        assertThat(parser.parse(doc.outerHtml())).extracting(PpomppuDealItem::externalId).doesNotContain("735626");
    }

    @Test
    void handlesMissingOptionalFieldsAndOverflowWithoutDroppingOtherRows() throws Exception {
        var doc = Jsoup.parse(fixture());
        var row = doc.selectFirst("tr.baseList");
        row.select(".subject_preface, .baseList-small, time").remove();
        row.selectFirst("img").attr("src", "javascript:alert(1)");
        row.selectFirst(".baseList-c").text("99999999999999999");
        var item = parser.parse(doc.outerHtml()).get(0);
        assertThat(item.storeName()).isNull();
        assertThat(item.category()).isNull();
        assertThat(item.commentCount()).isNull();
        assertThat(item.postedAtText()).isNull();
        assertThat(item.thumbnailUrl()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "<html>Forbidden</html>", "<html>Just a moment...</html>",
            "<table><tr class='baseList'><td>advertisement only</td></tr></table>"})
    void doesNotTreatUnexpectedHtmlAsSuccessfulEmptyCollection(String html) {
        assertThatThrownBy(() -> parser.parse(html)).isInstanceOf(IllegalArgumentException.class);
    }
}
