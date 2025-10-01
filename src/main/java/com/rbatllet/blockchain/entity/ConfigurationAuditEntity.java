package com.rbatllet.blockchain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for configuration audit trail
 * Database-agnostic replacement for SQL-based audit tables
 */
@Entity
@Table(name = "configuration_audit")
public class ConfigurationAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "config_key", nullable = false, length = 255)
    private String configKey;

    @Column(name = "config_type", nullable = false, length = 50)
    private String configType;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "change_reason", length = 255)
    private String changeReason;

    /**
     * Default constructor required by JPA
     */
    public ConfigurationAuditEntity() {
        // JPA requires no-arg constructor
    }

    /**
     * Constructor with required fields
     */
    public ConfigurationAuditEntity(String configKey, String configType, String oldValue,
                                   String newValue, String operation, String changeReason) {
        this.configKey = configKey;
        this.configType = configType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.operation = operation;
        this.changeReason = changeReason;
        this.changedAt = LocalDateTime.now();
    }

    /**
     * Called before persist
     */
    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
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
        this.configKey = configKey;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    @Override
    public String toString() {
        return "ConfigurationAuditEntity{" +
                "id=" + id +
                ", configKey='" + configKey + '\'' +
                ", configType='" + configType + '\'' +
                ", operation='" + operation + '\'' +
                ", changedAt=" + changedAt +
                ", changeReason='" + changeReason + '\'' +
                '}';
    }
}
