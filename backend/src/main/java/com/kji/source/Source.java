package com.kji.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "adapter_kind", nullable = false, length = 32)
    private AdapterKind adapterKind;

    @Column(name = "runtime_available", nullable = false)
    private boolean runtimeAvailable;

    @Column(name = "trust_tier", nullable = false)
    private short trustTier;

    @Column(name = "stable_external_id", nullable = false)
    private boolean stableExternalId;

    @Column(name = "provides_full_description", nullable = false)
    private boolean providesFullDescription;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "base_url")
    private String baseUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String config = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Source() {
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AdapterKind getAdapterKind() {
        return adapterKind;
    }

    public boolean isRuntimeAvailable() {
        return runtimeAvailable;
    }

    public short getTrustTier() {
        return trustTier;
    }

    public boolean isStableExternalId() {
        return stableExternalId;
    }

    public boolean isProvidesFullDescription() {
        return providesFullDescription;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getConfig() {
        return config;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
