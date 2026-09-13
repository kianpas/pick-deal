package com.pickdeal.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "pickdeal.collector.scheduling.enabled=false")
@ActiveProfiles("compose")
class ComposeMigrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        // PostgreSQL 검증 시에는 반드시 새로 만든 일회용 테스트 DB만 지정한다.
        String url = System.getenv("MIGRATION_TEST_DB_URL");
        boolean postgres = url != null;
        registry.add("spring.datasource.url", () -> postgres ? url
                : "jdbc:h2:mem:migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> postgres ? "org.postgresql.Driver" : "org.h2.Driver");
        registry.add("spring.datasource.username", () -> postgres ? "migration_test" : "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired ApplicationContext context;

    @Test
    void migratesEmptyDatabaseValidatesEntitiesAndPreservesDataOnNextMigration() {
        // 컨텍스트 기동 자체가 compose 프로필의 Hibernate validate 통과를 검증한다.
        assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(context.getBeansOfType(SeedDataInitializer.class)).isEmpty();
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("1");
        for (String table : new String[]{"source", "deal", "deal_group", "keyword", "source_visibility"}) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Long.class)).isZero();
        }
        jdbc.update("insert into source (name, base_url, code) values ('test', 'https://example.com', 'test')");
        Long sourceId = jdbc.queryForObject("select id from source where code='test'", Long.class);
        String insertDeal = "insert into deal (source_id, external_id, title, original_url, posted_at) values (?, ?, 'test', 'https://example.com/1', current_timestamp)";
        jdbc.update(insertDeal, sourceId, "one");
        jdbc.update(insertDeal, sourceId, "two"); // null group_id는 여러 행 허용
        assertThatThrownBy(() -> jdbc.update(insertDeal, sourceId, "one"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insertDeal, -1L, "missing-source"))
                .isInstanceOf(DataIntegrityViolationException.class);
        Long dealId = jdbc.queryForObject("select id from deal where external_id='one'", Long.class);
        jdbc.update("insert into deal_group (representative_deal_id, canonical_title) values (?, 'test')", dealId);
        Long groupId = jdbc.queryForObject("select id from deal_group", Long.class);
        jdbc.update("update deal set group_id=? where id=?", groupId, dealId);
        assertThatThrownBy(() -> jdbc.update("update deal set group_id=? where external_id='two'", groupId))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("select count(*) from deal", Long.class)).isEqualTo(2L);
        assertThat(flyway.info().applied()).hasSize(1);
    }
}
