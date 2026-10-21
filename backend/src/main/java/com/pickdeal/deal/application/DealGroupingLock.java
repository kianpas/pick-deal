package com.pickdeal.deal.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 그룹화 판정을 트랜잭션 단위로 직렬화한다. 후보 조회와 그룹 연결 사이에 다른 트랜잭션이 끼면
 * 서로의 미커밋 Deal을 보지 못해 묶이지 않거나, 같은 기존 Deal을 두고 그룹이 갈라진다.
 *
 * <p>PostgreSQL 트랜잭션 advisory lock을 사용해 커밋·롤백 때 자동으로 풀린다. 앱 인스턴스가
 * 여러 개여도 같은 DB를 쓰면 유효하다. 수집 저장량이 적어 전역 직렬화 비용은 무시할 수준이며,
 * 실제 대기 시간은 {@code pickdeal.grouping.lock.wait} 지표로 확인한다.
 * H2 단위 테스트는 단일 스레드라 잠금을 생략한다. 동시성은 PostgreSQL 테스트로 검증한다.
 */
@Component
public class DealGroupingLock {

    /** 이 앱의 advisory lock 키. 다른 용도의 잠금과 겹치지 않는 고정값. */
    private static final long LOCK_KEY = 0x5049434B4752504CL; // "PICKGRPL"

    /** 현재 트랜잭션이 이미 잠금을 보유했는지 표시하는 트랜잭션 리소스 키. */
    private static final Object HELD = new Object();

    private final EntityManager entityManager;
    private final boolean supported;
    private final Timer waitTimer;

    public DealGroupingLock(EntityManager entityManager, DataSource dataSource, MeterRegistry registry) {
        this.entityManager = entityManager;
        this.supported = isPostgres(dataSource);
        this.waitTimer = Timer.builder("pickdeal.grouping.lock.wait")
                .description("트랜잭션별 그룹화 잠금 획득 대기 시간")
                .register(registry);
    }

    /**
     * 현재 트랜잭션이 끝날 때까지 다른 트랜잭션의 그룹화를 기다리게 한다. 같은 트랜잭션에서
     * 다시 호출하면 DB를 거치지 않고 통과하므로, 대기 시간은 트랜잭션당 한 번만 기록된다.
     */
    public void acquire() {
        if (!supported) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("그룹화 잠금은 트랜잭션 안에서만 획득할 수 있습니다.");
        }
        if (TransactionSynchronizationManager.hasResource(HELD)) {
            return;
        }
        waitTimer.record(() -> entityManager
                .createNativeQuery("select 1 from (select pg_advisory_xact_lock(:key)) locked")
                .setParameter("key", LOCK_KEY)
                .getSingleResult());
        TransactionSynchronizationManager.bindResource(HELD, Boolean.TRUE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                TransactionSynchronizationManager.unbindResourceIfPossible(HELD);
            }
        });
    }

    private static boolean isPostgres(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return "PostgreSQL".equals(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            throw new IllegalStateException("DB 종류를 확인하지 못했습니다.", e);
        }
    }
}
