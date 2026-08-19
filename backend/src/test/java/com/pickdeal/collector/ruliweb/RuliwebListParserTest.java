package com.pickdeal.collector.ruliweb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuliwebListParserTest {

    private static String fixtureHtml;

    private final RuliwebListParser parser = new RuliwebListParser();

    @BeforeAll
    static void loadFixture() {
        fixtureHtml = readResource("/fixtures/ruliweb/hotdeal-list.html");
    }

    @Test
    @DisplayName("핫딜 목록에서 공지와 BEST(중복 노출) 행을 제외한 딜만 추출한다")
    void parsesDealRowsExcludingNoticeAndBest() {
        List<RuliwebDealItem> items = parser.parse(fixtureHtml);

        // 픽스처: 일반 딜 28건 (공지 2건, BEST 4건은 아래 일반 목록과 중복이라 제외)
        assertThat(items).hasSize(28);
        assertThat(items).allSatisfy(item -> {
            assertThat(item.externalId()).matches("\\d+");
            assertThat(item.url()).startsWith("https://bbs.ruliweb.com/market/board/1020/read/");
        });
    }

    @Test
    @DisplayName("제목의 [판매처] 접두사를 분리하고, 댓글 수·아이콘은 제목에서 제외한다")
    void splitsStorePrefixFromTitle() {
        List<RuliwebDealItem> items = parser.parse(fixtureHtml);

        RuliwebDealItem first = items.get(0);
        assertThat(first.externalId()).isEqualTo("106481");
        assertThat(first.storeName()).isEqualTo("롯데온");
        assertThat(first.title()).isEqualTo("스위치2+스플래툰 레이더스+ 아미보 / 730900원");
        // 댓글 수 "(5)"는 별도 요소라 제목에 섞이면 안 된다
        assertThat(items).allSatisfy(item -> assertThat(item.title()).isNotBlank().doesNotContain("(5)"));
    }

    @Test
    @DisplayName("카테고리와 게시 시각 원문을 추출한다")
    void parsesCategoryAndPostedAtText() {
        List<RuliwebDealItem> items = parser.parse(fixtureHtml);

        assertThat(items.get(0).category()).isEqualTo("게임H/W");
        assertThat(items.get(0).postedAtText()).isEqualTo("19:19");
        // 픽스처에는 "HH:mm"(오늘)과 "YYYY.MM.DD"(지난 날) 두 형태가 섞여 있다
        assertThat(items).allSatisfy(item -> assertThat(item.postedAtText()).isNotBlank());
    }

    @Test
    @DisplayName("[종료] 뱃지가 붙은 딜을 ended로 표시한다")
    void marksEndedDeals() {
        List<RuliwebDealItem> items = parser.parse(fixtureHtml);

        assertThat(items.get(0).ended()).isTrue();
        assertThat(items).filteredOn(RuliwebDealItem::ended).isNotEmpty();
        assertThat(items).filteredOn(item -> !item.ended()).isNotEmpty();
    }

    @Test
    @DisplayName("제목 끝의 가격 관례(\"... / 730900원\")에서 금액을 추출하고, 없으면 null이다")
    void parsesPriceFromTitleConvention() {
        List<RuliwebDealItem> items = parser.parse(fixtureHtml);

        assertThat(items.get(0).price()).isEqualTo(730900L);
        // 관례를 따르지 않는 제목도 많다 — 그 경우 죽지 않고 null이어야 한다
        assertThat(items).anySatisfy(item -> assertThat(item.price()).isNull());
    }

    @Test
    @DisplayName("가격은 제목 '끝'의 명확한 관례만 인정한다 — 애매하면 null로 둔다")
    void parsesOnlyUnambiguousTrailingPrice() {
        // 실제 목록에서 관찰한 표기들. 파서는 순수 함수라 제목만으로 검증할 수 있다.
        assertThat(priceOf("스위치2+스플래툰 레이더스+ 아미보 / 730900원")).isEqualTo(730900L);
        assertThat(priceOf("스위치2 +포켓몬za 727,725원")).isEqualTo(727725L);
        assertThat(priceOf("스위치 2 / 648000")).isEqualTo(648000L);

        // 괄호 안 표기·잘린 제목은 가격 위치가 불분명해 인정하지 않는다
        assertThat(priceOf("웰치스 6종 355ml 24캔 (12,940원~/무료)")).isNull();
        assertThat(priceOf("아몬드브리즈24팩+바리스타 950ml 2팩 증정 (15,370...")).isNull();
        assertThat(priceOf("정글 게임 랩 인디게임 18종 (무료/무료)")).isNull();
        // 날짜 조각("8/26")이 가격으로 오인되면 안 된다
        assertThat(priceOf("홈플러스&롯데마트 이번주 전단 8/26")).isNull();
    }

    /** 제목 1건짜리 최소 목록 HTML을 만들어 파서에 태운다. */
    private Long priceOf(String title) {
        // tr/td는 table 안에 있어야 jsoup이 구조를 유지한다
        String html = """
                <table><tbody>
                <tr class="table_body blocktarget">
                  <td class="divsn"><a href="?cate=1">테스트</a></td>
                  <td class="subject">
                    <a class="subject_link deco" href="https://bbs.ruliweb.com/market/board/1020/read/1?">%s</a>
                  </td>
                  <td class="time">19:19</td>
                </tr>
                </tbody></table>
                """.formatted(title);
        return parser.parse(html).get(0).price();
    }

    private static String readResource(String path) {
        try (InputStream in = RuliwebListParserTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
