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

    protected SourceVisibility() {
    }

    public SourceVisibility(Long userId, Source source, boolean visible) {
        this.userId = userId;
        this.source = source;
        this.visible = visible;
    }

    public void updateVisible(boolean visible) {
        this.visible = visible;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Source getSource() {
        return source;
    }

    public boolean isVisible() {
        return visible;
    }
}
