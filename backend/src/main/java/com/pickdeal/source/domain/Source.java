package com.pickdeal.source.domain;

import com.pickdeal.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 딜을 수집해오는 출처 사이트(커뮤니티/딜 사이트). 예: 루리웹, 에펨코리아.
 *
 * <p>판매처(쿠팡/네이버 등 쇼핑몰)와는 다른 개념이다 — 여기서 출처는 "딜 정보를 긁어오는 곳"을 뜻한다.
 * 사용자별 표시/숨김 설정은 {@link SourceVisibility}로 분리해 관리한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "source",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_source_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_source_code", columnNames = "code")
        }
)
public class Source extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** 출처 사이트 기본 URL. */
    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    /** 코드 키(예: {@code ruliweb}) — 수집기/설정에서 출처를 식별할 때 쓴다. */
    @Column(length = 50)
    private String code;

    /** 수집 활성화 여부. 운영 차원의 on/off(사용자별 표시 설정과 무관). */
    @Column(nullable = false)
    private boolean active;

    public Source(String name, String baseUrl, String code, boolean active) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.code = code;
        this.active = active;
    }
}
