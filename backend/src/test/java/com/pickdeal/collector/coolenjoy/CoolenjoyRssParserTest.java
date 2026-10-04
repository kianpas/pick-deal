package com.pickdeal.collector.coolenjoy;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CoolenjoyRssParserTest {
    private final CoolenjoyRssParser parser = new CoolenjoyRssParser();

    static String fixture() throws Exception {
        try (var input = CoolenjoyRssParserTest.class.getResourceAsStream("/fixtures/coolenjoy/jirum-rss.xml")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String feed(String items) {
        return "<rss version=\"2.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><channel>"
                + "<link>https://coolenjoy.net/bbs/jirum</link>" + items + "</channel></rss>";
    }

    static String item(String id, String title, String body) {
        return "<item><title>" + title + "</title><link>https://coolenjoy.net/bbs/jirum/" + id
                + "</link><description><![CDATA[" + body + "]]></description></item>";
    }

    @Test void actualFeedExtractsOnlyObservedFields() throws Exception {
        var deals = parser.parse(fixture());
        assertThat(deals).hasSize(25);
        var jacket = deals.stream().filter(d -> d.externalId().equals("3563135")).findFirst().orElseThrow();
        assertThat(jacket.price()).isEqualByComparingTo("36660");
        assertThat(jacket.postedAt()).isNotNull();
        assertThat(jacket.thumbnailUrl()).startsWith("https://");
        assertThat(jacket.ended()).isNull();
        assertThat(jacket.commentCount()).isNull();
        assertThat(deals.get(0).storeName()).isNull();
        var memory = deals.stream().filter(d -> d.externalId().equals("3562526")).findFirst().orElseThrow();
        assertThat(memory.productUrl()).isEqualTo("https://item.gmarket.co.kr/Item?goodscode=4620694901");
        assertThat(memory.postedAt().toString()).isEqualTo("2026-10-02T17:30:15+09:00");
    }

    @Test void ambiguousPricesAndLinksRemainUnknown() {
        for (String title : new String[]{"적립 상품 (50,310원/무배)", "상품 (18,900원/실체감가17,100원)",
                "상품 880원(4개월)", "상품 10,000원 20,000원", "상품 $20", "상품 1,00원"}) {
            assertThat(parser.parse(feed(item("1", title, ""))).get(0).price()).as(title).isNull();
        }
        String links = "<hr><p><a href='https://shop.example/a'>https://shop.example/a</a></p>"
                + "<hr><p><a href='https://shop.example/b'>https://shop.example/b</a></p>";
        assertThat(parser.parse(feed(item("1", "상품", links))).get(0).productUrl()).isNull();
    }

    @Test void deduplicatesAndSkipsExplicitPromotions() {
        var deals = parser.parse(feed(item("1", "상품 (10,000원/무료배송)", "<img src='/data/a.jpg'>")
                + item("1", "중복", "") + item("2", "[광고] 광고", "")));
        assertThat(deals).hasSize(1);
        assertThat(deals.get(0).price()).isEqualByComparingTo("10000");
        assertThat(deals.get(0).thumbnailUrl()).isEqualTo("https://coolenjoy.net/data/a.jpg");
        assertThat(parser.parse(feed(""))).isEmpty();
    }

    @Test void rejectsBlockPagesBrokenXmlWrongChannelsAndExternalEntities() {
        for (String invalid : new String[]{"<html>blocked</html>", "<rss>",
                feed("").replace("/bbs/jirum", "/bbs/free"),
                feed(item("1", "상품", "")).replace("/jirum/1", "/free/1"),
                "<!DOCTYPE rss [<!ENTITY secret SYSTEM 'file:///must-not-read'>]>" + feed("&secret;")}) {
            assertThatThrownBy(() -> parser.parse(invalid)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void capsUniqueItemsAtFifty() {
        var items = new StringBuilder();
        for (int i = 1; i <= 60; i++) items.append(item(Integer.toString(i), "상품", ""));
        assertThat(parser.parse(feed(items.toString()))).hasSize(50);
    }
}
