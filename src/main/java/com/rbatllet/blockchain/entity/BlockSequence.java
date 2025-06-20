package com.rbatllet.blockchain.entity;

import jakarta.persistence.*;

/**
 * Sequence entity for generating unique block numbers atomically
 * This prevents race conditions in high-concurrency scenarios
 */
@Entity
@Table(name = "block_sequence")
public class BlockSequence {
    
    @Id
    @Column(name = "sequence_name")
    private String sequenceName = "block_number";
    
    @Column(name = "next_value")
    private Long nextValue = 1L;
    
    public BlockSequence() {
    }
    
    public BlockSequence(String sequenceName, Long nextValue) {
        this.sequenceName = sequenceName;
        this.nextValue = nextValue;
    }
    
    public String getSequenceName() {
        return sequenceName;
    }
    
    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }
    
    public Long getNextValue() {
        return nextValue;
    }
    
    public void setNextValue(Long nextValue) {
        this.nextValue = nextValue;
    }
}
