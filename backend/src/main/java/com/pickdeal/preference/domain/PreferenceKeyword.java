package com.pickdeal.preference.domain;

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

@Entity
@Table(
        name = "keywords",
        indexes = {
                @Index(name = "idx_keywords_type", columnList = "keyword_type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_keywords_type_value", columnNames = {"keyword_type", "keyword_value"})
        }
)
public class PreferenceKeyword extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "keyword_type", nullable = false, length = 30, columnDefinition = "varchar(30)")
    private KeywordType type;

    @Column(name = "keyword_value", nullable = false, length = 100)
    private String value;

    protected PreferenceKeyword() {
    }

    public PreferenceKeyword(KeywordType type, String value) {
        this.type = type;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public KeywordType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
