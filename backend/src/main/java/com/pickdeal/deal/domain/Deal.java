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

@Entity
@Table(
        name = "deal",
        indexes = {
                @Index(name = "idx_deal_source_id", columnList = "source_id"),
                @Index(name = "idx_deal_posted_at", columnList = "posted_at"),
                @Index(name = "idx_deal_discount_rate", columnList = "discount_rate"),
                @Index(name = "idx_deal_status", columnList = "status"),
                @Index(name = "idx_deal_title_norm_hash", columnList = "title_norm_hash")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deal_source_external_id", columnNames = {"source_id", "external_id"})
        }
)
public class Deal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false, foreignKey = @ForeignKey(name = "fk_deals_source"))
    private Source source;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    private Long price;

    @Column(name = "original_price")
    private Long originalPrice;

    @Column(name = "discount_rate")
    private Integer discountRate;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(length = 50)
    private String category;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "original_url", nullable = false, length = 1000)
    private String originalUrl;

    @Column(name = "external_id", nullable = false, length = 200)
    private String externalId;

    @Column(name = "title_norm_hash", length = 64)
    private String titleNormHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DealStatus status;

    @Column(name = "posted_at", nullable = false)
    private OffsetDateTime postedAt;

    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;

    protected Deal() {
    }

    public Deal(
            Source source,
            String title,
            String description,
            Long price,
            Long originalPrice,
            Integer discountRate,
            String currency,
            String category,
            String thumbnailUrl,
            String originalUrl,
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
        this.thumbnailUrl = thumbnailUrl;
        this.originalUrl = originalUrl;
        this.externalId = externalId;
        this.titleNormHash = titleNormHash;
        this.status = status;
        this.postedAt = postedAt;
        this.collectedAt = collectedAt;
    }

    public Long getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Long getPrice() {
        return price;
    }

    public Long getOriginalPrice() {
        return originalPrice;
    }

    public Integer getDiscountRate() {
        return discountRate;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCategory() {
        return category;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitleNormHash() {
        return titleNormHash;
    }

    public DealStatus getStatus() {
        return status;
    }

    public OffsetDateTime getPostedAt() {
        return postedAt;
    }

    public OffsetDateTime getCollectedAt() {
        return collectedAt;
    }
}
