package com.pickdeal.deal.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 최신순 목록의 1단계: 필터·그룹 접기·정렬·페이지 자르기를 DB에서 처리해 이번 페이지의 카드 키만 고른다.
 * 카드 키는 그룹이면 그룹 ID, 단독 Deal이면 음수 Deal ID다. 응답 조립은 호출 측이 해당 Deal만 읽어 한다.
 *
 * <p>분류·쇼핑몰 별칭 규칙은 SQL에 복제하지 않는다. 호출 측이 규칙으로 허용 값 목록을 구해 넘긴다.
 * 검색어·키워드의 포함 규칙(대소문자 무시 부분 일치)은 {@code lower(...) like}로 같게 적용한다.
 * H2 단위 테스트와 PostgreSQL에서 모두 동작하도록 표준 SQL만 쓴다.
 *
 * <p>JDBC를 직접 쓰지 않고 JPA 네이티브 쿼리로 실행한다. 같은 트랜잭션에서 아직 DB에 반영되지 않은 변경을
 * 쿼리 전에 반영(auto flush)해, JPQL로 읽던 기존 목록과 같은 시점의 데이터를 본다.
 */
@Repository
@RequiredArgsConstructor
public class DealListQueryRepository {

    private final EntityManager entityManager;

    /** 이름 붙은 파라미터를 모아 네이티브 쿼리에 한꺼번에 묶는다. */
    private static final class QueryParams {
        private final Map<String, Object> values = new LinkedHashMap<>();

        QueryParams addValue(String name, Object value) {
            values.put(name, value);
            return this;
        }

        Query bind(Query query) {
            values.forEach(query::setParameter);
            return query;
        }
    }

    /** 원본 분류 조합. category가 null이면 분류 없음과 일치한다. */
    public record SourceCategory(String sourceCode, String category) {
    }

    /**
     * 목록 조건. null 목록은 "조건 없음", 빈 목록은 쓰지 않는다(호출 측이 결과 없음으로 처리한다).
     * 검색어·키워드는 이미 소문자로 정규화된 값이다.
     */
    public record Criteria(
            long userId,
            List<Long> sourceIds,
            List<String> rawShopNames,
            List<SourceCategory> categories,
            List<String> queryTerms,
            List<String> excludeKeywords,
            List<String> interestKeywords,
            boolean hideEnded
    ) {
    }

    public long countCards(Criteria criteria) {
        QueryParams params = new QueryParams();
        String sql = "select count(*) from (" + cardsSql(criteria, params) + ") cards";
        return ((Number) params.bind(entityManager.createNativeQuery(sql)).getSingleResult()).longValue();
    }

    /** 최신 게시 시각 내림차순, 같으면 카드 키 내림차순으로 정렬한 이번 페이지의 카드 키. */
    public List<Long> findCardKeys(Criteria criteria, long offset, int limit) {
        QueryParams params = new QueryParams();
        String sql = cardsSql(criteria, params) + " order by latest desc, card_key desc limit :limit offset :offset";
        params.addValue("limit", limit).addValue("offset", offset);
        List<?> rows = params.bind(entityManager.createNativeQuery(sql)).getResultList();
        return rows.stream().map(row -> ((Number) ((Object[]) row)[0]).longValue()).toList();
    }

    private String cardsSql(Criteria c, QueryParams params) {
        params.addValue("userId", c.userId());
        StringBuilder where = new StringBuilder("""
                s.active = true
                  and not exists (
                        select 1 from source_visibility sv
                        where sv.user_id = :userId and sv.source_id = s.id and sv.visible = false)""");
        if (c.sourceIds() != null) {
            where.append(" and d.source_id in (:sourceIds)");
            params.addValue("sourceIds", c.sourceIds());
        }
        if (c.rawShopNames() != null) {
            where.append(" and d.shop_name in (:shopNames)");
            params.addValue("shopNames", c.rawShopNames());
        }

        // 그룹 단위 조건: 구성원 중 하나라도(또는 아무도) 만족하는지를 max(case ...)로 판정한다.
        List<String> having = new ArrayList<>();
        if (c.hideEnded()) {
            having.add("max(case when e.status = 'ACTIVE' then 1 else 0 end) = 1");
        }
        if (c.categories() != null) {
            List<String> matches = new ArrayList<>();
            for (int i = 0; i < c.categories().size(); i++) {
                SourceCategory category = c.categories().get(i);
                params.addValue("catSource" + i, category.sourceCode());
                if (category.category() == null) {
                    matches.add("(e.source_code = :catSource" + i + " and e.category is null)");
                } else {
                    params.addValue("catValue" + i, category.category());
                    matches.add("(e.source_code = :catSource" + i + " and e.category = :catValue" + i + ")");
                }
            }
            having.add(anyMember(String.join(" or ", matches)) + " = 1");
        }
        if (!c.queryTerms().isEmpty()) {
            // 모든 단어가 같은 게시글의 제목 또는 본문에 있어야 한다(docs/01 §3).
            List<String> terms = new ArrayList<>();
            for (int i = 0; i < c.queryTerms().size(); i++) {
                terms.add(contains("term" + i, c.queryTerms().get(i), params));
            }
            having.add(anyMember(String.join(" and ", terms)) + " = 1");
        }
        if (!c.excludeKeywords().isEmpty()) {
            having.add(anyMember(anyKeyword("exclude", c.excludeKeywords(), params)) + " = 0");
        }
        if (!c.interestKeywords().isEmpty()) {
            having.add(anyMember(anyKeyword("interest", c.interestKeywords(), params)) + " = 1");
        }

        return """
                select e.card_key, max(e.posted_at) as latest
                from (
                    select d.posted_at, d.status, d.title, d.description, d.category, s.code as source_code,
                           case when d.group_id is null then -d.id else d.group_id end as card_key
                    from deal d
                    join source s on s.id = d.source_id
                    where %s
                ) e
                group by e.card_key
                """.formatted(where) + (having.isEmpty() ? "" : " having " + String.join(" and ", having));
    }

    private static String anyMember(String condition) {
        return "max(case when (" + condition + ") then 1 else 0 end)";
    }

    private static String anyKeyword(String prefix, List<String> keywords, QueryParams params) {
        List<String> matches = new ArrayList<>();
        for (int i = 0; i < keywords.size(); i++) {
            matches.add(contains(prefix + i, keywords.get(i), params));
        }
        return String.join(" or ", matches);
    }

    /** 제목 또는 본문에 소문자 단어가 포함되는지. LIKE 특수문자는 '!'로 이스케이프한다. */
    private static String contains(String name, String lowerWord, QueryParams params) {
        params.addValue(name, "%" + escapeLike(lowerWord) + "%");
        return "(lower(e.title) like :" + name + " escape '!' or lower(e.description) like :" + name + " escape '!')";
    }

    static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
