package com.pickdeal.collector.quasarzone;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuasarzoneListParserTest {

    private static String fixtureHtml;

    private final QuasarzoneListParser parser = new QuasarzoneListParser();

    @BeforeAll
    static void loadFixture() {
        fixtureHtml = readResource("/fixtures/quasarzone/saleinfo-list.html");
    }

    @Test
    @DisplayName("핫딜 목록 HTML에서 공지를 제외한 딜 항목을 추출한다")
    void parsesDealItemsExcludingNotices() {
        List<QuasarzoneDealItem> items = parser.parse(fixtureHtml);

        // 픽스처: 전체 31행 중 공지 1건 제외 → 딜 30건
        assertThat(items).hasSize(30);

        QuasarzoneDealItem first = items.get(0);
        assertThat(first.externalId()).isEqualTo("1968632");
        assertThat(first.url()).isEqualTo("https://quasarzone.com/bbs/qb_saleinfo/views/1968632");
    }

    @Test
    @DisplayName("제목의 [쇼핑몰] 접두사를 storeName으로 분리하고 제목은 깨끗하게 남긴다")
    void splitsStorePrefixFromTitle() {
        List<QuasarzoneDealItem> items = parser.parse(fixtureHtml);

        QuasarzoneDealItem first = items.get(0);
        assertThat(first.storeName()).isEqualTo("기타");
        assertThat(first.title()).isEqualTo("Towaga: Among Shadows");

        QuasarzoneDealItem second = items.get(1);
        assertThat(second.storeName()).isEqualTo("쿠팡");
        assertThat(second.title()).isEqualTo("오리코 PD 100W SD/TF 리더 M.2 SSD 외장하드 케이스");
    }

    @Test
    @DisplayName("가격 문자열(콤마·'원' 접미 변형 포함)을 정수 금액으로 파싱한다")
    void parsesPriceAsAmount() {
        List<QuasarzoneDealItem> items = parser.parse(fixtureHtml);

        assertThat(items.get(0).price()).isEqualTo(0L);      // 무료 딜: ￦ 0 (KRW)
        assertThat(items.get(1).price()).isEqualTo(36000L);  // ￦ 36,000 (KRW)

        // 픽스처의 모든 딜은 가격을 가진다 — '10,850원 (KRW)' 같은 접미 변형도 파싱돼야 한다
        assertThat(items).allSatisfy(item ->
                assertThat(item.price()).isNotNull().isGreaterThanOrEqualTo(0L));
    }

    @Test
    @DisplayName("카테고리와 썸네일 URL을 추출한다")
    void parsesCategoryAndThumbnail() {
        List<QuasarzoneDealItem> items = parser.parse(fixtureHtml);

        QuasarzoneDealItem first = items.get(0);
        assertThat(first.category()).isEqualTo("게임/SW");
        assertThat(first.thumbnailUrl())
                .isEqualTo("https://img2.quasarzone.com/editor/2026/07/09/thumb_9d952a90599dd24f75efe95fca13b94f.jpg");
    }

    @Test
    @DisplayName("종료된 딜은 ended로 표시한다")
    void marksEndedDeals() {
        List<QuasarzoneDealItem> items = parser.parse(fixtureHtml);

        // 픽스처: 종료 라벨 2건, 나머지(진행중·인기)는 진행 중
        assertThat(items).filteredOn(QuasarzoneDealItem::ended).hasSize(2);
        assertThat(items.get(0).ended()).isFalse();
    }

    @Test
    @DisplayName("게시 시각 원문 텍스트를 추출한다 (상대/축약 표기 그대로)")
    void extractsPostedAtText() {
        List<QuasarzoneDealItem> items = parser.parse(fixtureHtml);

        assertThat(items.get(0).postedAtText()).isEqualTo("30분 전");
        // 픽스처에는 "N분 전", "N시간 전", "07-08"(월-일) 세 형태가 섞여 있다
        assertThat(items).allSatisfy(item -> assertThat(item.postedAtText()).isNotBlank());
    }

    @Test
    @DisplayName("블라인드 처리된 글은 건너뛴다 (제목 span 없이 잠금 문구만 있음)")
    void skipsBlindedPosts() {
        // 라이브에서 관찰한 블라인드 글 마크업 축약본
        String blinded = """
                <div class="market-info-list">
                  <div class="market-info-list-cont">
                    <p class="tit">
                      <span class="label done">종료</span>
                      <a href="/bbs/qb_saleinfo/views/1968615" class="subject-link">
                        <i class="fa fa-lock"></i> 블라인드 처리된 글입니다.
                      </a>
                    </p>
                  </div>
                </div>
                """;

        assertThat(parser.parse(blinded)).isEmpty();
    }

    private static String readResource(String path) {
        try (InputStream in = QuasarzoneListParserTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
