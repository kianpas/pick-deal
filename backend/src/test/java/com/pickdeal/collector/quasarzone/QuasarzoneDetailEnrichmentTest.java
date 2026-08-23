package com.pickdeal.collector.quasarzone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "pickdeal.collector.sources.quasarzone.bootstrap-max-pages=1",
        "pickdeal.collector.sources.quasarzone.bootstrap-max-items=1",
        "pickdeal.collector.sources.quasarzone.max-detail-requests=1"
})
@Transactional
class QuasarzoneDetailEnrichmentTest {

    @Autowired
    private QuasarzoneCollectService collectService;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @MockitoBean
    private QuasarzoneClient client;

    @Test
    @DisplayName("신규 Deal은 상세의 판매처와 상품 URL을 저장하고 재수집에서는 상세를 다시 요청하지 않는다")
    void enrichesOnlyNewDeal() {
        String detailUrl = "https://quasarzone.com/bbs/qb_saleinfo/views/9910001";
        given(client.fetchListHtml(1)).willReturn(singleItemHtml("9910001"));
        given(client.fetchDetailHtml(detailUrl))
                .willReturn(readResource("/fixtures/quasarzone/saleinfo-detail.html"));

        assertThat(collectService.collect()).isEqualTo(1);

        Source source = sourceRepository.findByCode("quasarzone").orElseThrow();
        Deal saved = dealRepository.findBySourceIdAndExternalId(source.getId(), "9910001").orElseThrow();
        assertThat(saved.getShopName()).isEqualTo("쿠팡");
        assertThat(saved.getProductUrl())
                .isEqualTo("https://www.coupang.com/vp/products/9320059618?vendorItemId=95248275824");

        clearInvocations(client);
        assertThat(collectService.collect()).isZero();
        verify(client, never()).fetchDetailHtml(detailUrl);
        assertThat(saved.getProductUrl()).isNotNull();
    }

    private static String singleItemHtml(String externalId) {
        return """
                <div class="market-info-list">
                  <div class="market-info-list-cont">
                    <p class="tit">
                      <span class="label">진행중</span>
                      <a href="/bbs/qb_saleinfo/views/%s" class="subject-link">
                        <span class="ellipsis-with-reply-cnt">[목록몰] 상세 보강 상품</span>
                      </a>
                    </p>
                    <div class="market-info-sub">
                      <p><span class="category">기타</span></p>
                      <p><span class="date">10분 전</span></p>
                    </div>
                  </div>
                </div>
                """.formatted(externalId);
    }

    private static String readResource(String path) {
        try (InputStream in = QuasarzoneDetailEnrichmentTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
