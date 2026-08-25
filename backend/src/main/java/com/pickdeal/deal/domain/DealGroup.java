package com.pickdeal.deal.domain;

import com.pickdeal.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 여러 출처의 원본 Deal을 보존한 채 같은 딜로 연결하는 최소 그룹. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "deal_group")
public class DealGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "representative_deal_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_deal_group_representative_deal")
    )
    private Deal representativeDeal;

    @Column(name = "canonical_title", nullable = false, length = 300)
    private String canonicalTitle;

    public DealGroup(Deal representativeDeal) {
        this.representativeDeal = representativeDeal;
        this.canonicalTitle = representativeDeal.getTitle();
    }

    /** 정보가 더 풍부한 Deal을 대표로 삼고, 동률이면 최신 게시글을 선택한다. */
    public void considerRepresentative(Deal candidate) {
        if (isBetterRepresentative(candidate, representativeDeal)) {
            this.representativeDeal = candidate;
            this.canonicalTitle = candidate.getTitle();
        }
    }

    private boolean isBetterRepresentative(Deal candidate, Deal current) {
        int candidateScore = representativeScore(candidate);
        int currentScore = representativeScore(current);
        if (candidateScore != currentScore) {
            return candidateScore > currentScore;
        }
        return postedAt(candidate).isAfter(postedAt(current));
    }

    private int representativeScore(Deal deal) {
        int score = 0;
        if (hasText(deal.getProductUrl())) {
            score += 4;
        }
        if (deal.getPrice() != null) {
            score += 2;
        }
        if (hasText(deal.getThumbnailUrl())) {
            score += 1;
        }
        return score;
    }

    private OffsetDateTime postedAt(Deal deal) {
        return deal.getPostedAt() != null ? deal.getPostedAt() : OffsetDateTime.MIN;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
