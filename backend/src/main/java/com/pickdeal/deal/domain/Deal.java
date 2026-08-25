package com.pickdeal.deal.domain;

import com.pickdeal.common.domain.BaseTimeEntity;
import com.pickdeal.source.domain.Source;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 핫딜 한 건. 수집/등록된 딜의 핵심 정보를 담는 애그리거트 루트.
 *
 * <p>{@code source}(수집 출처)당 {@code externalId}는 유일하다 — 동일 딜의 중복 수집을 막는다.
 * {@code category}는 현재 자유 문자열(출처가 준 값 또는 수동 입력)이며, 통합 분류 체계는 수집기 단계로 보류한다(docs/03 §2.1).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "deal",
        indexes = {
                @Index(name = "idx_deal_source_id", columnList = "source_id"),
                @Index(name = "idx_deal_posted_at", columnList = "posted_at"),
                @Index(name = "idx_deal_discount_rate", columnList = "discount_rate"),
                @Index(name = "idx_deal_status", columnList = "status"),
                @Index(name = "idx_deal_title_norm_hash", columnList = "title_norm_hash"),
                @Index(name = "idx_deal_group_id", columnList = "group_id")
        },
        uniqueConstraints = {
                // 같은 출처에서 같은 외부 ID의 딜은 한 번만 — 중복 수집 방지(docs/03 §2.3).
                @UniqueConstraint(name = "uk_deal_source_external_id", columnNames = {"source_id", "external_id"}),
                // 한 그룹에는 출처별 대표 게시글 하나만 연결한다. group_id null 행은 서로 충돌하지 않는다.
                @UniqueConstraint(name = "uk_deal_group_source", columnNames = {"group_id", "source_id"})
        }
)
public class Deal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 딜을 수집한 출처 사이트(커뮤니티/딜 사이트). 판매처(쇼핑몰)가 아니다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false, foreignKey = @ForeignKey(name = "fk_deals_source"))
    private Source source;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    /** 판매가. 가격 정보가 없을 수 있어 nullable. */
    private Long price;

    @Column(name = "original_price")
    private Long originalPrice;

    @Column(name = "discount_rate")
    private Integer discountRate;

    @Column(nullable = false, length = 8)
    private String currency;

    /** 자유 문자열 카테고리. 통합 분류는 미도입(docs/03 §2.1). */
    @Column(length = 50)
    private String category;

    /** 출처가 표시한 판매몰 이름 원문. 별도 Shop 모델·표준화는 아직 적용하지 않는다. */
    @Column(name = "shop_name", length = 100)
    private String shopName;

    /** 마지막 수집 시 원문 게시글 목록에서 확인한 댓글 수. 출처가 제공하지 않으면 nullable. */
    @Column(name = "comment_count")
    private Integer commentCount;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /** 원문(출처 게시글) 링크. */
    @Column(name = "original_url", nullable = false, length = 1000)
    private String originalUrl;

    /** 판매몰 상품·기획전 링크. 상세에서 확인하지 못하면 nullable. */
    @Column(name = "product_url", length = 2000)
    private String productUrl;

    /** 출처 내 고유 식별자. {@code source}와 조합해 유일하다. */
    @Column(name = "external_id", nullable = false, length = 200)
    private String externalId;

    /** 제목 정규화 SHA-256 — 출처 간 동일 딜 후보 탐색용. */
    @Column(name = "title_norm_hash", length = 64)
    private String titleNormHash;

    /** 같은 상품·딜로 판정된 서로 다른 출처의 원본 Deal 그룹. 판정 전에는 nullable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", foreignKey = @ForeignKey(name = "fk_deal_group"))
    private DealGroup dealGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DealStatus status;

    /** 출처에 게시된 시각. */
    @Column(name = "posted_at", nullable = false)
    private OffsetDateTime postedAt;

    /** PickDeal이 수집/등록한 시각. */
    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;

    public Deal(
            Source source,
            String title,
            String description,
            Long price,
            Long originalPrice,
            Integer discountRate,
            String currency,
            String category,
            String shopName,
            Integer commentCount,
            String thumbnailUrl,
            String originalUrl,
            String productUrl,
            String externalId,
            String titleNormHash,
            DealStatus status,
            OffsetDateTime postedAt,
            OffsetDateTime collectedAt
    ) {
        this.source = source;
        this.title = title;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountRate = discountRate;
        this.currency = currency;
        this.category = category;
        this.shopName = shopName;
        this.commentCount = commentCount;
        this.thumbnailUrl = thumbnailUrl;
        this.originalUrl = originalUrl;
        this.productUrl = productUrl;
        this.externalId = externalId;
        this.titleNormHash = titleNormHash;
        this.status = status;
        this.postedAt = postedAt;
        this.collectedAt = collectedAt;
    }

    /** 재수집 시 변동 가능한 값과 새로 확인된 판매몰 정보를 갱신한다. */
    public void updateFromRecollection(
            Long price,
            String category,
            String shopName,
            String productUrl,
            Integer commentCount,
            DealStatus status
    ) {
        this.price = price;
        this.category = category;
        if (shopName != null && !shopName.isBlank()) {
            this.shopName = shopName;
        }
        if (productUrl != null && !productUrl.isBlank()) {
            this.productUrl = productUrl;
        }
        this.commentCount = commentCount;
        this.status = status;
    }

    public void updateTitleNormHash(String titleNormHash) {
        this.titleNormHash = titleNormHash;
    }

    public void joinGroup(DealGroup dealGroup) {
        if (this.dealGroup != null && this.dealGroup != dealGroup) {
            throw new IllegalStateException("Deal is already assigned to another group");
        }
        this.dealGroup = dealGroup;
    }
}
