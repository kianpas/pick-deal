package com.pickdeal.source.domain;

import com.pickdeal.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 출처 표시/숨김 설정. ({@code user_id}, {@code source}) 조합이 유일하다.
 *
 * <p>행이 없으면 "표시"가 기본값이다 — 설정을 바꾼 출처만 행으로 남긴다.
 * MVP는 단일 사용자라 {@code user_id}는 고정값(1)이지만, 멀티유저 전환을 대비해 키로 유지한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "source_visibility",
        indexes = {
                @Index(name = "idx_source_visibility_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_source_visibility_user_source", columnNames = {"user_id", "source_id"})
        }
)
public class SourceVisibility extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false, foreignKey = @ForeignKey(name = "fk_source_visibility_source"))
    private Source source;

    @Column(nullable = false)
    private boolean visible;

    public SourceVisibility(Long userId, Source source, boolean visible) {
        this.userId = userId;
        this.source = source;
        this.visible = visible;
    }

    /** 표시 여부를 변경한다(도메인 행위). */
    public void updateVisible(boolean visible) {
        this.visible = visible;
    }
}
