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
        name = "deals",
        indexes = {
                @Index(name = "idx_deals_source_id", columnList = "source_id"),
                @Index(name = "idx_deals_created_at", columnList = "created_at"),
                @Index(name = "idx_deals_posted_at", columnList = "posted_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deals_source_original_url", columnNames = {"source_id", "original_url"})
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

    private Integer price;

    @Column(name = "shipping_fee")
    private Integer shippingFee;

    @Column(name = "original_url", nullable = false, length = 1000)
    private String originalUrl;

    @Column(name = "original_id", length = 200)
    private String originalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DealStatus status;

    @Column(name = "posted_at")
    private OffsetDateTime postedAt;

    @Column(name = "collected_at")
    private OffsetDateTime collectedAt;

    protected Deal() {
    }

    public Deal(
            Source source,
            String title,
            String description,
            Integer price,
            Integer shippingFee,
            String originalUrl,
            String originalId,
            DealStatus status,
            OffsetDateTime postedAt,
            OffsetDateTime collectedAt
    ) {
        this.source = source;
        this.title = title;
        this.description = description;
        this.price = price;
        this.shippingFee = shippingFee;
        this.originalUrl = originalUrl;
        this.originalId = originalId;
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

    public Integer getPrice() {
        return price;
    }

    public Integer getShippingFee() {
        return shippingFee;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getOriginalId() {
        return originalId;
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

