package com.pickdeal.collector.support;

/**
 * 출처 하나의 수집을 담당한다. 새 출처 = 새 구현체이며, 스케줄러는 구현체를 모두 주입받아
 * 순회하므로 출처가 늘어도 스케줄러는 바뀌지 않는다(docs/05 A.3).
 */
public interface SourceCollector {

    /** 출처 코드({@code source.code})와 일치한다. 로그 식별용. */
    String sourceCode();

    /**
     * 목록을 수집해 신규 딜은 저장하고 기존 딜은 갱신한다.
     *
     * @return 신규 저장된 딜 수
     */
    int collect();
}
