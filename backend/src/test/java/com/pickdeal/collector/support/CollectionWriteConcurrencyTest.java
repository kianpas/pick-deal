package com.pickdeal.collector.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.Source;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 서로 다른 출처의 같은 상품이 동시에 저장될 때의 그룹화 정합성을 운영과 같은 PostgreSQL에서 검증한다.
 * H2는 격리·잠금 동작이 달라 이 검증을 대신하지 못한다. Docker가 없으면 건너뛴다.
 *
 * <p>각 저장 트랜잭션은 저장 후 커밋 전에 상대를 최대 {@link #OVERLAP_WAIT_SECONDS}초 기다린다.
 * 저장이 직렬화되지 않으면 두 트랜잭션이 서로의 미커밋 Deal을 보지 못한 채 겹치고,
 * 직렬화되면 뒤 트랜잭션이 앞의 커밋을 기다리므로 대기는 시간 초과 후 그대로 커밋한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "pickdeal.seed.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class CollectionWriteConcurrencyTest {

    private static final long OVERLAP_WAIT_SECONDS = 2;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-bookworm");

    @Autowired
    private DealUpsertSupport upsertSupport;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MeterRegistry registry;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("다른 출처의 같은 상품이 동시에 저장돼도 하나의 그룹으로 묶는다")
    void concurrentCrossSourceDealsAreGrouped() throws Exception {
        Source first = upsertSupport.findOrRegisterSource("race-new-a", "신규 경합 A", "https://race-new-a.example.com");
        Source second = upsertSupport.findOrRegisterSource("race-new-b", "신규 경합 B", "https://race-new-b.example.com");

        Timer lockWait = registry.get("pickdeal.grouping.lock.wait").timer();
        long waitsBefore = lockWait.count();

        List<Throwable> failures = upsertConcurrently(
                first, collected("a-1", "동시 저장 SSD 1TB"),
                second, collected("b-1", "동시 저장 SSD 1TB"));

        assertThat(failures).isEmpty();
        Long firstGroup = groupId(first, "a-1");
        assertThat(firstGroup).isNotNull().isEqualTo(groupId(second, "b-1"));
        // 트랜잭션당 한 번 기록되고, 뒤 트랜잭션은 앞 트랜잭션의 커밋 대기 시간만큼 기다린다.
        assertThat(lockWait.count() - waitsBefore).isEqualTo(2);
        assertThat(lockWait.max(TimeUnit.SECONDS)).isGreaterThan(1.0);
    }

    @Test
    @DisplayName("두 출처가 동시에 같은 기존 Deal과 일치해도 두 배치가 모두 저장되고 한 그룹에 모인다")
    void concurrentMatchesForExistingDealShareOneGroup() throws Exception {
        Source existing = upsertSupport.findOrRegisterSource("race-old-c", "기존 경합 C", "https://race-old-c.example.com");
        Source first = upsertSupport.findOrRegisterSource("race-old-a", "기존 경합 A", "https://race-old-a.example.com");
        Source second = upsertSupport.findOrRegisterSource("race-old-b", "기존 경합 B", "https://race-old-b.example.com");
        String title = "기존 일치 키보드";
        transaction().executeWithoutResult(status ->
                upsertSupport.upsertAll(existing, List.of(collected("c-1", title)), OffsetDateTime.now()));

        List<Throwable> failures = upsertConcurrently(
                first, collected("a-1", title),
                second, collected("b-1", title));

        assertThat(failures).isEmpty();
        Long group = groupId(existing, "c-1");
        assertThat(group).isNotNull();
        assertThat(groupId(first, "a-1")).isEqualTo(group);
        assertThat(groupId(second, "b-1")).isEqualTo(group);
    }

    /** 두 저장을 각자의 트랜잭션에서 동시에 실행하고 실패 원인을 모은다. */
    private List<Throwable> upsertConcurrently(Source firstSource, CollectedDeal firstDeal,
            Source secondSource, CollectedDeal secondDeal) throws Exception {
        CountDownLatch saved = new CountDownLatch(2);
        List<Future<?>> futures = List.of(
                executor.submit(() -> upsertAndWait(firstSource, firstDeal, saved)),
                executor.submit(() -> upsertAndWait(secondSource, secondDeal, saved)));
        List<Throwable> failures = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException e) {
                failures.add(e.getCause());
            }
        }
        return failures;
    }

    private void upsertAndWait(Source source, CollectedDeal deal, CountDownLatch saved) {
        transaction().executeWithoutResult(status -> {
            upsertSupport.upsertAll(source, List.of(deal), OffsetDateTime.now());
            saved.countDown();
            try {
                saved.await(OVERLAP_WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        });
    }

    /** 커밋된 Deal의 그룹 ID. 그룹이 없으면 null. */
    private Long groupId(Source source, String externalId) {
        return transaction().execute(status -> {
            var deal = dealRepository.findBySourceIdAndExternalId(source.getId(), externalId).orElseThrow();
            return deal.getDealGroup() == null ? null : deal.getDealGroup().getId();
        });
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private static CollectedDeal collected(String externalId, String title) {
        return new CollectedDeal(externalId, "https://source.example.com/deals/" + externalId,
                "쿠팡", title, 99_000L, "PC/하드웨어", null, null, false, OffsetDateTime.now());
    }
}
