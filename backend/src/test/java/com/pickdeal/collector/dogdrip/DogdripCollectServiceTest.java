package com.pickdeal.collector.dogdrip;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.SourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "pickdeal.collector.sources.dogdrip.enabled=true")
@Transactional
class DogdripCollectServiceTest {
    @MockitoBean DogdripClient client;
    @Autowired DogdripCollectService service;
    @Autowired DealRepository deals;
    @Autowired SourceRepository sources;

    @Test void savesAndDeduplicates() throws Exception {
        when(client.fetchListHtml()).thenReturn(DogdripCollectorTest.fixture());
        when(client.fetchDetailHtml(anyString())).thenReturn(DogdripDetailParserTest.fixture());
        assertThat(service.collect()).isEqualTo(3);
        assertThat(service.collect()).isZero();
        var source = sources.findByCode("dogdrip").orElseThrow();
        var deal = deals.findBySourceIdAndExternalId(source.getId(), "725757041").orElseThrow();
        assertThat(deal.getStatus().name()).isEqualTo("EXPIRED");
        assertThat(deal.getProductUrl()).startsWith("https://sharkninja.co.kr/");
        assertThat(deal.getPrice()).isEqualTo(20800L);
        verify(client, times(2)).fetchListHtml();
        verify(client, times(3)).fetchDetailHtml(anyString());
    }

    @Test void failedDetailsStillSaveAndAreNotRetriedOnRecollection() throws Exception {
        when(client.fetchListHtml()).thenReturn(DogdripCollectorTest.fixture());
        when(client.fetchDetailHtml(anyString())).thenThrow(new IllegalStateException("HTTP 403"));
        assertThat(service.collect()).isEqualTo(3);
        assertThat(service.collect()).isZero();
        verify(client, times(3)).fetchDetailHtml(anyString());
        var source = sources.findByCode("dogdrip").orElseThrow();
        assertThat(deals.findBySourceIdAndExternalId(source.getId(), "725854470").orElseThrow().getProductUrl()).isNull();
    }

    @Test void rejectionDoesNotRegisterOrSave() {
        when(client.fetchListHtml()).thenThrow(new IllegalStateException("HTTP 403"));
        assertThatThrownBy(service::collect).isInstanceOf(IllegalStateException.class);
        assertThat(sources.findByCode("dogdrip")).isEmpty();
    }
}
