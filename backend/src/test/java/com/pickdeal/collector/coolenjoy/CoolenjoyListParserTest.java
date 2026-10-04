package com.pickdeal.collector.coolenjoy;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class CoolenjoyListParserTest {
    private final CoolenjoyListParser parser = new CoolenjoyListParser();
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-04T18:00:00+09:00");
    static String fixture() throws Exception {
        try (var input = CoolenjoyListParserTest.class.getResourceAsStream("/fixtures/coolenjoy/jirum-list.html")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    static String feed(String rows) {
        return "<input name='bo_table' value='jirum'><ul class='na-table'>" + rows + "</ul>";
    }
    static String item(String id, String title, String price) {
        return "<li class='d-md-table-row'><a class='na-subject' href='/bbs/jirum/" + id
                + "?page=3'>" + title + "</a><div><span class='sr-only'>가격</span>" + price
                + "</div><div><span class='sr-only'>등록일</span>10.03</div></li>";
    }
    @Test void actualListExtractsPriceShopCategoryAndComments() throws Exception {
        var deals = parser.parse(fixture(), NOW);
        assertThat(deals).hasSize(25);
        assertThat(deals).noneMatch(d -> d.externalId().equals("3553133"));
        var jacket = deals.stream().filter(d -> d.externalId().equals("3563135")).findFirst().orElseThrow();
        assertThat(jacket.price()).isEqualByComparingTo("36660");
        assertThat(jacket.storeName()).isEqualTo("옥션");
        assertThat(jacket.category()).isEqualTo("의류잡화");
        assertThat(jacket.commentCount()).isEqualTo(2);
        assertThat(jacket.thumbnailUrl()).isNull();
        assertThat(jacket.ended()).isNull();
        assertThat(jacket.postedAt()).isNotNull();
        // 제목의 적립 문구로 목록의 명시 가격을 버리지 않는다.
        assertThat(deals.stream().filter(d -> d.externalId().equals("3563103")).findFirst().orElseThrow().price())
                .isEqualByComparingTo("50310");
    }
    @Test void usesOnlyPriceCellNotTitleAndNormalizesPageQuery() {
        var result = parser.parse(feed(item("1", "[몰] 적립 상품 999원", "<font>26,900원</font>")), NOW).get(0);
        assertThat(result.price()).isEqualByComparingTo("26900");
        assertThat(result.url()).isEqualTo("https://coolenjoy.net/bbs/jirum/1");
        assertThat(result.title()).isEqualTo("적립 상품 999원");
        assertThat(result.postedAt()).isEqualTo("2026-10-03T00:00:00+09:00");
        for (String value : new String[]{"", "다양", "1,00원", "10,000원 / 20,000원", "$10", "문의"}) {
            assertThat(parser.parse(feed(item("1", "상품 999원", value)), NOW).get(0).price()).isNull();
        }
    }
    @Test void rejectsOtherBoardsBlockedAndBrokenLists() {
        for (String html : new String[]{"<html>blocked</html>", feed(item("1", "상품", "1원")).replace("value='jirum'", "value='free'"),
                feed(item("1", "상품", "1원")).replace("/bbs/jirum/1", "https://evil.example/bbs/jirum/1"), feed("")}) {
            assertThatThrownBy(() -> parser.parse(html, NOW)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(parser.parse(feed("게시물이 없습니다"), NOW)).isEmpty();
    }
    @Test void deduplicatesAndCapsAtFifty() {
        String row = item("1", "상품", "1원");
        assertThat(parser.parse(feed(row + row), NOW)).hasSize(1);
        var rows = new StringBuilder();
        for (int i = 1; i <= 60; i++) rows.append(item(Integer.toString(i), "상품", "1원"));
        assertThat(parser.parse(feed(rows.toString()), NOW)).hasSize(50);
    }
}
