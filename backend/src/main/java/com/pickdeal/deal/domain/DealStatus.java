package com.pickdeal.deal.domain;

/**
 * 딜의 노출 상태. 닫힌 집합(값 추가 = 처리 로직 추가 = 배포)이라 코드 enum으로 관리한다.
 * DB에는 {@code @Enumerated(STRING)}으로 문자열 저장.
 */
public enum DealStatus {
    /** 노출 중. 목록 조회 대상. */
    ACTIVE,
    /** 품절. */
    SOLD_OUT,
    /** 만료/종료된 딜. */
    EXPIRED
}
