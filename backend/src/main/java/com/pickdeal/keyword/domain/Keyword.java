package com.pickdeal.keyword.domain;

import com.pickdeal.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 등록한 관심/제외 키워드. 딜 목록 필터링에 쓰인다(docs/01 §3.2).
 *
 * <p>{@link KeywordType#INTEREST}는 "포함해야 노출", {@link KeywordType#EXCLUDE}는 "포함되면 제외".
 * ({@code user_id}, {@code keyword}, {@code type}) 조합이 유일하다. MVP 단일 사용자라 {@code user_id}는 고정값.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "keyword",
        indexes = {
                @Index(name = "idx_keyword_user_type", columnList = "user_id, type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_keyword_user_keyword_type", columnNames = {"user_id", "keyword", "type"})
        }
)
public class Keyword extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 관심(INTEREST) / 제외(EXCLUDE) 구분. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "varchar(10)")
    private KeywordType type;

    @Column(nullable = false, length = 50)
    private String keyword;

    public Keyword(Long userId, KeywordType type, String keyword) {
        this.userId = userId;
        this.type = type;
        this.keyword = keyword;
    }
}
