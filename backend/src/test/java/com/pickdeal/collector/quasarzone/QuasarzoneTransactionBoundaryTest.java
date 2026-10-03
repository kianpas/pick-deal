package com.pickdeal.collector.quasarzone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pickdeal.deal.application.DealGroupingService;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.SourceRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:${random.uuid};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "pickdeal.collector.sources.quasarzone.bootstrap-max-pages=1",
        "pickdeal.collector.sources.quasarzone.bootstrap-max-items=2",
        "pickdeal.collector.sources.quasarzone.max-items=2",
        "pickdeal.collector.sources.quasarzone.max-detail-requests=1"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class QuasarzoneTransactionBoundaryTest {
    @Autowired private QuasarzoneCollectService collector;
    @Autowired private DealRepository deals;
    @Autowired private SourceRepository sources;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private QuasarzoneClient client;
    @MockitoSpyBean private DealGroupingService grouping;

    @Test
    void httpRunsOutsideTransactionsEvenWhenCallerHasOneAndSaveIsAtomic() {
        given(client.fetchListHtml(anyInt())).willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return fixture("saleinfo-list.html");
        });
        given(client.fetchDetailHtml(anyString())).willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return fixture("saleinfo-detail.html");
        });
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            return invocation.callRealMethod();
        }).when(grouping).groupIfMatched(any());

        int saved = new TransactionTemplate(transactionManager).execute(status -> collector.collect());
        assertThat(saved).isEqualTo(2);
        var source = sources.findByCode("quasarzone").orElseThrow();
        assertThat(deals.findBySourceIdAndExternalId(source.getId(), "1968632").orElseThrow().getProductUrl()).isNotNull();
        verify(client).fetchDetailHtml("https://quasarzone.com/bbs/qb_saleinfo/views/1968632");
    }

    @Test
    void recollectionCommitsCommentUpdatesWithoutAnotherDetailRequest() {
        String html = fixture("saleinfo-list.html");
        given(client.fetchListHtml(1)).willReturn(html);
        given(client.fetchDetailHtml(anyString())).willReturn(fixture("saleinfo-detail.html"));
        assertThat(collector.collect()).isEqualTo(2);
        clearInvocations(client);
        String updatedHtml = html.replace("<span class=\"ctn-count \">1</span>",
                "<span class=\"ctn-count \">21</span>");
        assertThat(updatedHtml).isNotEqualTo(html);
        given(client.fetchListHtml(1)).willReturn(updatedHtml);

        assertThat(collector.collect()).isZero();
        var source = sources.findByCode("quasarzone").orElseThrow();
        assertThat(deals.findBySourceIdAndExternalId(source.getId(), "1968628").orElseThrow().getCommentCount()).isEqualTo(21);
        verify(client, never()).fetchDetailHtml(anyString());
    }

    @Test
    void listFailureDoesNotRegisterSourceOrSaveDeals() {
        long before = deals.count();
        given(client.fetchListHtml(1)).willThrow(new IllegalStateException("source rejected"));
        assertThatThrownBy(collector::collect).isInstanceOf(IllegalStateException.class);
        assertThat(sources.findByCode("quasarzone")).isEmpty();
        assertThat(deals.count()).isEqualTo(before);
    }

    @Test
    void detailFailureStillSavesListInformation() {
        given(client.fetchListHtml(1)).willReturn(fixture("saleinfo-list.html"));
        given(client.fetchDetailHtml(anyString())).willThrow(new IllegalStateException("detail rejected"));
        assertThat(collector.collect()).isEqualTo(2);
        var source = sources.findByCode("quasarzone").orElseThrow();
        assertThat(deals.findBySourceIdAndExternalId(source.getId(), "1968632").orElseThrow().getProductUrl()).isNull();
    }

    @Test
    void saveFailureRollsBackSourceAndWholeBatch() {
        long before = deals.count();
        given(client.fetchListHtml(1)).willReturn(fixture("saleinfo-list.html"));
        given(client.fetchDetailHtml(anyString())).willReturn(fixture("saleinfo-detail.html"));
        var calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 2) throw new IllegalStateException("save failed");
            return invocation.callRealMethod();
        }).when(grouping).groupIfMatched(any());

        assertThatThrownBy(collector::collect).isInstanceOf(IllegalStateException.class).hasMessage("save failed");
        assertThat(calls.get()).isEqualTo(2);
        assertThat(sources.findByCode("quasarzone")).isEmpty();
        assertThat(deals.count()).isEqualTo(before);
    }

    private static String fixture(String name) {
        try (var input = QuasarzoneTransactionBoundaryTest.class.getResourceAsStream("/fixtures/quasarzone/" + name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }
}
