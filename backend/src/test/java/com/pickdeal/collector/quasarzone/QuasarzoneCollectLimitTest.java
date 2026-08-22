package com.pickdeal.collector.quasarzone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "pickdeal.collector.sources.quasarzone.max-pages=3",
        "pickdeal.collector.sources.quasarzone.max-items=2"
})
@Transactional
class QuasarzoneCollectLimitTest {

    @Autowired
    private QuasarzoneCollectService collectService;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @MockitoBean
    private QuasarzoneClient client;

    @Test
    @DisplayName("maxItems에 도달하면 남은 페이지를 요청하지 않는다")
    void stopsRequestingPagesAtMaxItems() {
        given(client.fetchListHtml(1)).willReturn(singleItemHtml("9900001"));
        given(client.fetchListHtml(2)).willReturn(
                singleItemHtml("9900001") + singleItemHtml("9900002") + singleItemHtml("9900003"));

        int saved = collectService.collect();

        assertThat(saved).isEqualTo(2);
        Source source = sourceRepository.findByCode("quasarzone").orElseThrow();
        assertThat(dealRepository.findAll()).filteredOn(
                deal -> deal.getSource().getId().equals(source.getId())).hasSize(2);
        verify(client).fetchListHtml(1);
        verify(client).fetchListHtml(2);
        verify(client, never()).fetchListHtml(3);
    }

    private static String singleItemHtml(String externalId) {
        return """
                <div class="market-info-list">
                  <div class="market-info-list-cont">
                    <p class="tit">
                      <span class="label">진행중</span>
                      <a href="/bbs/qb_saleinfo/views/%s" class="subject-link">
                        <span class="ellipsis-with-reply-cnt">[테스트몰] 수집 한도 상품</span>
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
}
