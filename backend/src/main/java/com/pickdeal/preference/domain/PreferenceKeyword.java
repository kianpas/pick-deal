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
        name = "keyword",
        indexes = {
                @Index(name = "idx_keyword_user_type", columnList = "user_id, type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_keyword_user_keyword_type", columnNames = {"user_id", "keyword", "type"})
        }
)
public class PreferenceKeyword extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "varchar(10)")
    private KeywordType type;

    @Column(nullable = false, length = 50)
    private String keyword;

    protected PreferenceKeyword() {
    }

    public PreferenceKeyword(Long userId, KeywordType type, String keyword) {
        this.userId = userId;
        this.type = type;
        this.keyword = keyword;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public KeywordType getType() {
        return type;
    }

    public String getKeyword() {
        return keyword;
    }
}
