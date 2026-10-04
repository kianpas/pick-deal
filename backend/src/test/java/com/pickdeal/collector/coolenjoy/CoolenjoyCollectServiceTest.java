package com.pickdeal.collector.coolenjoy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.source.domain.SourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest(properties = {
        "pickdeal.collector.sources.coolenjoy.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:${random.uuid};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CoolenjoyCollectServiceTest {
    @Autowired CoolenjoyCollectService collector;
    @Autowired DealRepository deals;
    @Autowired SourceRepository sources;
    @Autowired com.pickdeal.collector.support.DealUpsertSupport upsert;
    @MockitoBean CoolenjoyClient client;

    @Test void storesActualFeedWithoutDuplicatesAndRequestsOutsideTransaction() throws Exception {
        String xml = CoolenjoyListParserTest.fixture();
        given(client.fetchListHtml()).willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return xml;
        });
        assertThat(collector.collect()).isEqualTo(25);
        assertThat(collector.collect()).isZero();
        var source = sources.findByCode("coolenjoy").orElseThrow();
        assertThat(deals.findAll().stream().filter(d -> d.getSource().getId().equals(source.getId()))).hasSize(25);
        var deal = deals.findBySourceIdAndExternalId(source.getId(), "3563135").orElseThrow();
        assertThat(deal.getPrice()).isEqualByComparingTo("36660");
        assertThat(deal.getOriginalUrl()).isEqualTo("https://coolenjoy.net/bbs/jirum/3563135");
        deal.updateFromRecollection(null, null, null, null, null, DealStatus.EXPIRED);
        deals.saveAndFlush(deal);
        given(client.fetchListHtml()).willReturn(CoolenjoyListParserTest.feed(
                CoolenjoyListParserTest.item("3563135", "가격 미확인", "")));
        assertThat(collector.collect()).isZero();
        var recollected = deals.findById(deal.getId()).orElseThrow();
        assertThat(recollected.getStatus()).isEqualTo(DealStatus.EXPIRED);
        assertThat(recollected.getPrice()).isEqualByComparingTo("36660");
    }

    @Test void invalidResponseCreatesNoSourceOrDeals() {
        given(client.fetchListHtml()).willReturn("<html>blocked</html>");
        assertThatThrownBy(collector::collect).isInstanceOf(IllegalArgumentException.class);
        assertThat(sources.findByCode("coolenjoy")).isEmpty();
    }

    @Test void fillsRssPriceWithoutDuplicatingOrRemovingExistingMedia() throws Exception {
        upsert.upsertAll("coolenjoy", "쿨엔조이", "https://coolenjoy.net", java.util.List.of(
                new com.pickdeal.collector.support.CollectedDeal("3563135", "https://coolenjoy.net/bbs/jirum/3563135",
                        null, "RSS 원본 제목", null, null, null, "https://coolenjoy.net/data/original.jpg",
                        null, java.time.OffsetDateTime.now(), "https://shop.example/product")), java.time.OffsetDateTime.now());
        given(client.fetchListHtml()).willReturn(CoolenjoyListParserTest.fixture());
        assertThat(collector.collect()).isEqualTo(24);
        var source = sources.findByCode("coolenjoy").orElseThrow();
        var deal = deals.findBySourceIdAndExternalId(source.getId(), "3563135").orElseThrow();
        assertThat(deal.getPrice()).isEqualByComparingTo("36660");
        assertThat(deal.getThumbnailUrl()).isEqualTo("https://coolenjoy.net/data/original.jpg");
        assertThat(deal.getProductUrl()).isEqualTo("https://shop.example/product");
        assertThat(deal.getTitle()).isEqualTo("RSS 원본 제목");
    }
}
