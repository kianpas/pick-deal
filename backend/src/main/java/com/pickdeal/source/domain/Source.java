package com.pickdeal.source.domain;

import com.pickdeal.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(length = 50)
    private String code;

    @Column(nullable = false)
    private boolean active;

    protected Source() {
    }

    public Source(String name, String baseUrl, String code, boolean active) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.code = code;
        this.active = active;
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

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }
}
