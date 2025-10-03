package com.rbatllet.blockchain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for configuration storage
 * Database-agnostic replacement for SQL-based configuration tables
 */
@Entity
@Table(
    name = "configuration",
    uniqueConstraints = @UniqueConstraint(columnNames = {"config_key", "config_type"})
)
public class ConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "config_key", nullable = false, length = 255)
    private String configKey;

    @Column(name = "config_type", nullable = false, length = 50)
    private String configType;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Default constructor required by JPA
     */
    public ConfigurationEntity() {
        // JPA requires no-arg constructor
    }

    /**
     * Constructor with required fields
     */
    public ConfigurationEntity(String configKey, String configType, String configValue) {
        this.configKey = configKey;
        this.configType = configType;
        this.configValue = configValue;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Called before persist
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Called before update
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        // Validate database column limit (255 chars)
        if (configKey != null && configKey.length() > 255) {
            throw new IllegalArgumentException(
                "configKey exceeds maximum length of 255 characters (got: " +
                configKey.length() + "). Please use shorter configuration keys."
            );
        }
        this.configKey = configKey;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        // Validate database column limit (50 chars)
        if (configType != null && configType.length() > 50) {
            throw new IllegalArgumentException(
                "configType exceeds maximum length of 50 characters (got: " +
                configType.length() + "). Please use standard type names."
            );
        }
        this.configType = configType;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ConfigurationEntity{" +
                "id=" + id +
                ", configKey='" + configKey + '\'' +
                ", configType='" + configType + '\'' +
                ", configValue='" + configValue + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
