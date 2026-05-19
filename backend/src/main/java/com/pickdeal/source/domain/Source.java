package com.pickdeal.source.domain;

import com.pickdeal.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "sources",
        indexes = {
                @Index(name = "idx_sources_visible", columnList = "visible")
        }
)
public class Source extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean visible;

    protected Source() {
    }

    public Source(String name, String baseUrl, String description, boolean visible) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.description = description;
        this.visible = visible;
    }

    public void updateVisibility(boolean visible) {
        this.visible = visible;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getDescription() {
        return description;
    }

    public boolean isVisible() {
        return visible;
    }
}

