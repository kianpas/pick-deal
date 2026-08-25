package com.pickdeal.collector.ruliweb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
        "pickdeal.collector.sources.ruliweb.bootstrap-max-pages=1",
        "pickdeal.collector.sources.ruliweb.bootstrap-max-items=1",
        "pickdeal.collector.sources.ruliweb.max-detail-requests=1"
})
@Transactional
class RuliwebDetailEnrichmentTest {

    @Autowired
    private RuliwebCollectService collectService;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @MockitoBean
    private RuliwebClient client;

    @Test
    @DisplayName("신규 Deal은 제목 판매몰과 상세 출처 URL을 함께 저장한다")
    void enrichesNewDeal() {
        String detailUrl = "https://bbs.ruliweb.com/market/board/1020/read/9910002";
        given(client.fetchListHtml(1)).willReturn(singleItemHtml("9910002"));
        given(client.fetchDetailHtml(detailUrl))
                .willReturn(readResource("/fixtures/ruliweb/hotdeal-detail.html"));

        assertThat(collectService.collect()).isEqualTo(1);

        Source source = sourceRepository.findByCode("ruliweb").orElseThrow();
        Deal saved = dealRepository.findBySourceIdAndExternalId(source.getId(), "9910002").orElseThrow();
        assertThat(saved.getShopName()).isEqualTo("롯데온");
        assertThat(saved.getProductUrl()).isEqualTo("https://s.lotteon.com/xt85P9DdUQ");
    }

    private static String singleItemHtml(String externalId) {
        return """
                <table><tbody>
                  <tr class="table_body blocktarget">
                    <td class="id">%s</td>
                    <td class="divsn"><a href="?cate=12">음식</a></td>
                    <td class="subject">
                      <a class="subject_link deco" href="https://bbs.ruliweb.com/market/board/1020/read/%s?">
                        [롯데온] 상세 보강 상품 10,000원
                      </a>
                    </td>
                    <td class="time">10:20</td>
                  </tr>
                </tbody></table>
                """.formatted(externalId, externalId);
    }

    private static String readResource(String path) {
        try (InputStream in = RuliwebDetailEnrichmentTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
