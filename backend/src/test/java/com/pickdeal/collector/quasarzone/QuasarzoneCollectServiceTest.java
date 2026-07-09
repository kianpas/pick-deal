package com.pickdeal.collector.quasarzone;

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
class QuasarzoneCollectServiceTest {

    @Autowired
    private QuasarzoneCollectService collectService;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @MockitoBean
    private QuasarzoneClient client;

    @Test
    @DisplayName("목록을 수집하면 출처를 등록하고 딜을 저장한다")
    void collectSavesDealsWithSource() {
        given(client.fetchListHtml()).willReturn(readResource("/fixtures/quasarzone/saleinfo-list.html"));

        int saved = collectService.collect();

        assertThat(saved).isEqualTo(30);

        Source source = sourceRepository.findByCode("quasarzone").orElseThrow();
        List<Deal> deals = dealRepository.findAll().stream()
                .filter(deal -> deal.getSource().getId().equals(source.getId()))
                .toList();
        assertThat(deals).hasSize(30);

        Deal first = deals.stream()
                .filter(deal -> deal.getExternalId().equals("1968628"))
                .findFirst().orElseThrow();
        assertThat(first.getTitle()).isEqualTo("[쿠팡] 오리코 PD 100W SD/TF 리더 M.2 SSD 외장하드 케이스");
        assertThat(first.getPrice()).isEqualTo(36000L);
        assertThat(first.getCurrency()).isEqualTo("KRW");
        assertThat(first.getStatus()).isEqualTo(DealStatus.ACTIVE);
        assertThat(first.getOriginalUrl()).isEqualTo("https://quasarzone.com/bbs/qb_saleinfo/views/1968628");
        assertThat(first.getPostedAt()).isNotNull();
        assertThat(first.getCollectedAt()).isNotNull();

        // 종료 라벨 딜은 EXPIRED로 저장된다
        assertThat(deals).filteredOn(deal -> deal.getStatus() == DealStatus.EXPIRED).hasSize(2);
    }

    @Test
    @DisplayName("같은 목록을 다시 수집해도 딜이 중복 저장되지 않는다")
    void recollectDoesNotDuplicate() {
        given(client.fetchListHtml()).willReturn(readResource("/fixtures/quasarzone/saleinfo-list.html"));

        int firstRun = collectService.collect();
        int secondRun = collectService.collect();

        assertThat(secondRun).isZero();

        Source source = sourceRepository.findByCode("quasarzone").orElseThrow();
        long total = dealRepository.findAll().stream()
                .filter(deal -> deal.getSource().getId().equals(source.getId()))
                .count();
        assertThat(total).isEqualTo(firstRun);
    }

    @Test
    @DisplayName("재수집에서 가격이 바뀌거나 종료된 딜은 기존 행이 갱신된다")
    void recollectUpdatesChangedDeal() {
        given(client.fetchListHtml())
                .willReturn(singleItemHtml("9999901", "진행중", "￦ 10,000 (KRW)"))
                .willReturn(singleItemHtml("9999901", "종료", "￦ 8,000 (KRW)"));

        collectService.collect();
        int secondRun = collectService.collect();

        assertThat(secondRun).isZero();

        Source source = sourceRepository.findByCode("quasarzone").orElseThrow();
        Deal updated = dealRepository.findBySourceIdAndExternalId(source.getId(), "9999901").orElseThrow();
        assertThat(updated.getPrice()).isEqualTo(8000L);
        assertThat(updated.getStatus()).isEqualTo(DealStatus.EXPIRED);
    }

    /** 실제 목록 마크업을 축약한 딜 1건짜리 목록 HTML. */
    private static String singleItemHtml(String externalId, String label, String priceText) {
        return """
                <div class="market-info-list">
                  <div class="market-info-list-cont">
                    <p class="tit">
                      <span class="label">%s</span>
                      <a href="/bbs/qb_saleinfo/views/%s" class="subject-link">
                        <span class="ellipsis-with-reply-cnt">[테스트몰] 갱신 확인용 상품</span>
                      </a>
                    </p>
                    <div class="market-info-sub">
                      <p>
                        <span class="category">기타</span>
                        <span>가격 <span class="text-orange">%s</span></span>
                      </p>
                      <p><span class="date">10분 전</span></p>
                    </div>
                  </div>
                </div>
                """.formatted(label, externalId, priceText);
    }

    private static String readResource(String path) {
        try (InputStream in = QuasarzoneCollectServiceTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
