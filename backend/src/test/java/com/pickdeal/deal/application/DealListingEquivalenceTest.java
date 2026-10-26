package com.pickdeal.deal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealCategory;
import com.pickdeal.deal.domain.DealGroup;
import com.pickdeal.deal.domain.DealGroupRepository;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.deal.dto.DealListResponse;
import com.pickdeal.keyword.domain.Keyword;
import com.pickdeal.keyword.domain.KeywordRepository;
import com.pickdeal.keyword.domain.KeywordType;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import com.pickdeal.source.domain.SourceVisibility;
import com.pickdeal.source.domain.SourceVisibilityRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 최신순 목록의 DB 처리가 기존 메모리 처리와 같은 결과를 내는지 운영과 같은 PostgreSQL에서 비교한다.
 * 별칭·출처별 분류·검색어 대소문자·LIKE 특수문자·게시 시각 동률·그룹·숨김 출처·키워드를 섞은 데이터에
 * 필터 조합을 무작위(고정 시드)로 바꿔 가며 두 처리의 응답 전체를 비교한다. Docker가 없으면 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "pickdeal.seed.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DealListingEquivalenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-bookworm");

    private static final String[] SHOPS = {"카카오톡딜", "카카오쇼핑", "카카오 쇼핑", "G마켓", "지마켓", "g마켓",
            "네이버쇼핑", "네이버", "쿠팡", "11번가", "알리익스프레스", " ", null};
    private static final String[] COMMON_CATEGORIES = {"PC/하드웨어", "식품", "생활/식품", "의류/잡화", "게임/SW",
            "상품권/쿠폰", "알수없는분류", null};
    private static final String[] COOLENJOY_CATEGORIES = {"PC관련", "가전", "쿠폰", "인터넷", "식품", null};
    private static final String[] WORDS = {"SSD", "ssd", "Ssd", "삼성", "노트북", "게이밍", "라면", "100%할인", "a_b",
            "중고", "1TB", "키보드", "!특가!"};

    @Autowired private DealService dealService;
    @Autowired private DealRepository deals;
    @Autowired private DealGroupRepository groups;
    @Autowired private SourceRepository sources;
    @Autowired private SourceVisibilityRepository visibility;
    @Autowired private KeywordRepository keywords;
    @Autowired private PlatformTransactionManager transactionManager;

    private final List<Long> sourceIds = new ArrayList<>();

    @BeforeAll
    void seed() {
        Random random = new Random(20261026);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(txStatus -> {
            Source quasarzone = sources.save(new Source("퀘이사존", "https://quasarzone.com", "quasarzone", true));
            Source dogdrip = sources.save(new Source("개드립", "https://www.dogdrip.net", "dogdrip", true));
            Source coolenjoy = sources.save(new Source("쿨엔조이", "https://coolenjoy.net", "coolenjoy", true));
            Source hidden = sources.save(new Source("숨김출처", "https://hidden.example", "hidden", true));
            Source inactive = sources.save(new Source("비활성출처", "https://inactive.example", "inactive", false));
            visibility.save(new SourceVisibility(1L, hidden, false));
            List<Source> all = List.of(quasarzone, dogdrip, coolenjoy, hidden, inactive);
            all.forEach(source -> sourceIds.add(source.getId()));

            // 게시 시각 후보를 적게 두어 동률을 일부러 많이 만든다.
            OffsetDateTime base = OffsetDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Deal> created = new ArrayList<>();
            for (int i = 0; i < 1500; i++) {
                Source source = all.get(random.nextInt(all.size()));
                String category = source == coolenjoy
                        ? COOLENJOY_CATEGORIES[random.nextInt(COOLENJOY_CATEGORIES.length)]
                        : COMMON_CATEGORIES[random.nextInt(COMMON_CATEGORIES.length)];
                String title = "[" + i + "] " + words(random, 1 + random.nextInt(3));
                String description = random.nextInt(3) == 0 ? "본문 " + words(random, 2 + random.nextInt(4)) : null;
                DealStatus status = DealStatus.values()[random.nextInt(DealStatus.values().length)];
                OffsetDateTime posted = base.plusMinutes(random.nextInt(300) * 10L);
                created.add(deals.save(new Deal(source, title, description, random.nextInt(10) == 0 ? null : 1000 + i,
                        null, random.nextInt(5) == 0 ? random.nextInt(90) : null, "KRW", category,
                        SHOPS[random.nextInt(SHOPS.length)], null, random.nextBoolean() ? "https://img.example/" + i : null,
                        source.getBaseUrl() + "/" + i, random.nextInt(10) == 0 ? "https://shop.example/" + i : null,
                        "eq-" + i, null, status, posted, posted.plusMinutes(1))));
            }

            // 출처가 서로 다른 2~3건을 그룹으로 묶는다. 대표가 숨김 출처인 그룹도 생긴다.
            List<Deal> pool = new ArrayList<>(created);
            java.util.Collections.shuffle(pool, random);
            int index = 0;
            for (int g = 0; g < 180 && index + 3 <= pool.size(); g++) {
                List<Deal> members = new ArrayList<>();
                for (Deal candidate : pool.subList(index, Math.min(index + 6, pool.size()))) {
                    if (members.size() < 2 + random.nextInt(2)
                            && members.stream().noneMatch(m -> m.getSource() == candidate.getSource())) {
                        members.add(candidate);
                    }
                }
                index += 6;
                if (members.size() < 2) continue;
                DealGroup group = groups.save(new DealGroup(members.get(0)));
                members.forEach(member -> member.joinGroup(group));
            }
        });
    }

    @Test
    @DisplayName("최신순 DB 처리는 필터·키워드·페이지 조합 전체에서 기존 메모리 처리와 같은 응답을 낸다")
    void databaseListingMatchesInMemoryListing() {
        Random random = new Random(42);
        String[][] keywordScenarios = {{}, {"ssd", "라면"}, {"!중고"}, {"게이밍", "!100%"}, {"A_B"}};
        int compared = 0;
        int nonEmpty = 0;
        for (String[] scenario : keywordScenarios) {
            setKeywords(scenario);
            for (int i = 0; i < 80; i++) {
                List<Long> sourceFilter = pick(random, new List[]{null, null, null, List.of(),
                        List.of(sourceIds.get(0)), List.of(sourceIds.get(1), sourceIds.get(2)),
                        List.of(sourceIds.get(3)), List.of(sourceIds.get(2), sourceIds.get(3), sourceIds.get(4))});
                String category = random.nextInt(2) == 0 ? null
                        : DealCategory.values()[random.nextInt(DealCategory.values().length)].name();
                String query = pick(random, new String[]{null, null, null, null, "", "ssd", "SSD 1TB", "삼성 ssd", "100%", "a_b", "!특가",
                        "없는단어", "  노트북   게이밍 "});
                List<String> shops = pick(random, new List[]{null, null, null, null, List.of(), List.of("카카오쇼핑"), List.of("g마켓"),
                        List.of("네이버", "쿠팡"), List.of("없는몰"), List.of(" "), List.of("카카오톡딜", "11번가")});
                boolean hideEnded = random.nextBoolean();
                int size = pick(random, new Integer[]{1, 7, 20, 100});
                int page = pick(random, new Integer[]{0, 0, 0, 1, 2, 40});

                DealListResponse expected = read(() -> dealService.findDealsInMemory(
                        page, size, "latest", sourceFilter, category, query, shops, hideEnded));
                DealListResponse actual = read(() -> dealService.findDeals(
                        page, size, "latest", sourceFilter, category, query, shops, hideEnded));
                assertThat(actual)
                        .as("keywords=%s sources=%s category=%s q=%s shops=%s hideEnded=%s page=%d size=%d",
                                Arrays.toString(scenario), sourceFilter, category, query, shops, hideEnded, page, size)
                        .isEqualTo(expected);
                compared++;
                if (!expected.items().isEmpty()) nonEmpty++;
            }
        }
        // 대부분 빈 결과끼리 비교하는 일이 없도록 결과가 있는 조합도 충분한지 확인한다.
        assertThat(compared).isEqualTo(400);
        assertThat(nonEmpty).isGreaterThan(150);
    }

    private void setKeywords(String[] scenario) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            keywords.deleteAll();
            for (String value : scenario) {
                boolean exclude = value.startsWith("!");
                keywords.save(new Keyword(1L, exclude ? KeywordType.EXCLUDE : KeywordType.INTEREST,
                        exclude ? value.substring(1) : value));
            }
        });
    }

    private DealListResponse read(java.util.function.Supplier<DealListResponse> call) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setReadOnly(true);
        return tx.execute(status -> call.get());
    }

    private static String words(Random random, int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(' ');
            text.append(WORDS[random.nextInt(WORDS.length)]);
        }
        return text.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T> T pick(Random random, Object[] options) {
        return (T) options[random.nextInt(options.length)];
    }
}
