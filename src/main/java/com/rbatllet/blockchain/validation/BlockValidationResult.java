package com.rbatllet.blockchain.validation;

import com.rbatllet.blockchain.entity.Block;

/**
 * Detailed validation result for a single block
 * Contains information about different aspects of block validation
 */
public class BlockValidationResult {
    private final Block block;
    private final BlockStatus status;
    private final boolean structurallyValid;
    private final boolean cryptographicallyValid;
    private final boolean authorizationValid;
    private final boolean offChainDataValid;
    private final String errorMessage;
    private final String warningMessage;
    
    private BlockValidationResult(Builder builder) {
        this.block = builder.block;
        this.status = builder.status;
        this.structurallyValid = builder.structurallyValid;
        this.cryptographicallyValid = builder.cryptographicallyValid;
        this.authorizationValid = builder.authorizationValid;
        this.offChainDataValid = builder.offChainDataValid;
        this.errorMessage = builder.errorMessage;
        this.warningMessage = builder.warningMessage;
    }
    
    // Getters
    public Block getBlock() { return block; }
    public BlockStatus getStatus() { return status; }
    public boolean isStructurallyValid() { return structurallyValid; }
    public boolean isCryptographicallyValid() { return cryptographicallyValid; }
    public boolean isAuthorizationValid() { return authorizationValid; }
    public boolean isOffChainDataValid() { return offChainDataValid; }
    public String getErrorMessage() { return errorMessage; }
    public String getWarningMessage() { return warningMessage; }
    
    /**
     * Returns true if the block is valid (either VALID or REVOKED status)
     * REVOKED blocks are considered "valid" from a structural/cryptographic perspective
     */
    public boolean isValid() {
        return status == BlockStatus.VALID || status == BlockStatus.REVOKED;
    }
    
    /**
     * Returns true only if the block is completely valid (VALID status)
     */
    public boolean isFullyValid() {
        return status == BlockStatus.VALID;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Block #").append(block.getBlockNumber()).append(": ").append(status);
        if (errorMessage != null) {
            sb.append(" - ").append(errorMessage);
        }
        if (warningMessage != null) {
            sb.append(" (").append(warningMessage).append(")");
        }
        return sb.toString();
    }
    
    // Builder pattern for easy construction
    public static class Builder {
        private Block block;
        private BlockStatus status = BlockStatus.INVALID;
        private boolean structurallyValid = false;
        private boolean cryptographicallyValid = false;
        private boolean authorizationValid = false;
        private boolean offChainDataValid = true; // Default to true for blocks without off-chain data
        private String errorMessage;
        private String warningMessage;
        
        public Builder(Block block) {
            this.block = block;
        }
        
        public Builder status(BlockStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder structurallyValid(boolean valid) {
            this.structurallyValid = valid;
            return this;
        }
        
        public Builder cryptographicallyValid(boolean valid) {
            this.cryptographicallyValid = valid;
            return this;
        }
        
        public Builder authorizationValid(boolean valid) {
            this.authorizationValid = valid;
            return this;
        }
        
        public Builder offChainDataValid(boolean valid) {
            this.offChainDataValid = valid;
            return this;
        }
        
        public Builder errorMessage(String message) {
            this.errorMessage = message;
            return this;
        }
        
        public Builder warningMessage(String message) {
            this.warningMessage = message;
            return this;
        }
        
        public BlockValidationResult build() {
            // Auto-determine status if not explicitly set
            if (status == BlockStatus.INVALID) {
                if (structurallyValid && cryptographicallyValid && authorizationValid && offChainDataValid) {
                    status = BlockStatus.VALID;
                } else if (structurallyValid && cryptographicallyValid && !authorizationValid && offChainDataValid) {
                    status = BlockStatus.REVOKED;
                } else {
                    status = BlockStatus.INVALID;
                }
            }
            
            return new BlockValidationResult(this);
        }
    }
    
    // Static factory methods for common cases
    public static BlockValidationResult success(String message) {
        // Create a dummy block for success case - this is for general validation results
        Block dummyBlock = new Block();
        return new Builder(dummyBlock)
            .status(BlockStatus.VALID)
            .structurallyValid(true)
            .cryptographicallyValid(true)
            .authorizationValid(true)
            .offChainDataValid(true)
            .warningMessage(message)
            .build();
    }
    
    public static BlockValidationResult failure(String errorMessage) {
        // Create a dummy block for failure case - this is for general validation results
        Block dummyBlock = new Block();
        return new Builder(dummyBlock)
            .status(BlockStatus.INVALID)
            .structurallyValid(false)
            .cryptographicallyValid(false)
            .authorizationValid(false)
            .offChainDataValid(false)
            .errorMessage(errorMessage)
            .build();
    }
}
