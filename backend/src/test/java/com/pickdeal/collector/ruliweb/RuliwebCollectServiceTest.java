package com.pickdeal.collector.ruliweb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional // 각 테스트 후 롤백 — 테스트 간 수집 결과가 섞이지 않게 한다
class RuliwebCollectServiceTest {

    @Autowired
    private RuliwebCollectService collectService;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @MockitoBean
    private RuliwebClient client;

    @Test
    @DisplayName("목록을 수집하면 출처를 등록하고 딜을 저장한다")
    void collectSavesDealsWithSource() {
        given(client.fetchListHtml(1)).willReturn(readResource("/fixtures/ruliweb/hotdeal-list.html"));

        int saved = collectService.collect();

        assertThat(saved).isEqualTo(28);

        Source source = sourceRepository.findByCode("ruliweb").orElseThrow();
        assertThat(source.getName()).isEqualTo("루리웹");

        Deal deal = dealRepository.findBySourceIdAndExternalId(source.getId(), "106481").orElseThrow();
        // 제목은 출처 게시판 원문 그대로 저장한다(판매처 접두사 포함)
        assertThat(deal.getTitle()).isEqualTo("[롯데온] 스위치2+스플래툰 레이더스+ 아미보 / 730900원");
        assertThat(deal.getPrice()).isEqualTo(730900L);
        assertThat(deal.getCurrency()).isEqualTo("KRW");
        assertThat(deal.getCategory()).isEqualTo("게임H/W");
        assertThat(deal.getCommentCount()).isEqualTo(5);
        assertThat(deal.getOriginalUrl()).isEqualTo("https://bbs.ruliweb.com/market/board/1020/read/106481");
        assertThat(deal.getPostedAt()).isNotNull();
        assertThat(deal.getCollectedAt()).isNotNull();
        // [종료] 뱃지가 붙은 딜이라 EXPIRED로 저장된다
        assertThat(deal.getStatus()).isEqualTo(DealStatus.EXPIRED);
    }

    @Test
    @DisplayName("같은 목록을 다시 수집해도 딜이 중복 저장되지 않는다")
    void recollectDoesNotDuplicate() {
        given(client.fetchListHtml(1)).willReturn(readResource("/fixtures/ruliweb/hotdeal-list.html"));

        int firstRun = collectService.collect();
        int secondRun = collectService.collect();

        assertThat(secondRun).isZero();

        Source source = sourceRepository.findByCode("ruliweb").orElseThrow();
        long total = dealRepository.findAll().stream()
                .filter(deal -> deal.getSource().getId().equals(source.getId()))
                .count();
        assertThat(total).isEqualTo(firstRun);
    }

    @Test
    @DisplayName("퀘이사존 딜과 같은 external_id여도 출처가 다르면 별개로 저장된다")
    void sameExternalIdAcrossSourcesIsSeparate() {
        given(client.fetchListHtml(1)).willReturn(readResource("/fixtures/ruliweb/hotdeal-list.html"));
        collectService.collect();

        Source ruliweb = sourceRepository.findByCode("ruliweb").orElseThrow();
        List<Deal> deals = dealRepository.findAll().stream()
                .filter(deal -> deal.getSource().getId().equals(ruliweb.getId()))
                .toList();

        assertThat(deals).isNotEmpty();
        assertThat(deals).allSatisfy(deal ->
                assertThat(deal.getSource().getCode()).isEqualTo("ruliweb"));
    }

    @Test
    @DisplayName("확실한 카테고리 별칭만 PickDeal 대표 문자열로 저장한다")
    void normalizesKnownCategoryAlias() {
        given(client.fetchListHtml(1)).willReturn(readResource("/fixtures/ruliweb/hotdeal-list.html"));

        collectService.collect();

        Source source = sourceRepository.findByCode("ruliweb").orElseThrow();
        List<String> categories = dealRepository.findAll().stream()
                .filter(deal -> deal.getSource().getId().equals(source.getId()))
                .map(Deal::getCategory)
                .distinct()
                .toList();
        assertThat(categories).contains("게임/SW", "게임H/W", "상품권");
        assertThat(categories).doesNotContain("게임S/W");
    }

    private static String readResource(String path) {
        try (InputStream in = RuliwebCollectServiceTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
