package com.rbatllet.blockchain.validation;

import com.rbatllet.blockchain.entity.Block;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive validation result for the entire blockchain
 * Provides granular information about different aspects of chain validation
 */
public class ChainValidationResult {
    private final boolean structuralIntegrity;
    private final boolean authorizationCompliance;
    private final long totalBlocks;
    private final long validBlocks;
    private final long revokedBlocks;
    private final long invalidBlocks;
    private final List<BlockValidationResult> blockResults;
    private final String summary;
    
    public ChainValidationResult(List<BlockValidationResult> blockResults) {
        this.blockResults = blockResults;
        this.totalBlocks = blockResults.size();
        
        // Count blocks by status
        Map<BlockStatus, Long> statusCounts = blockResults.stream()
            .collect(Collectors.groupingBy(
                BlockValidationResult::getStatus, 
                Collectors.counting()
            ));
        
        this.validBlocks = statusCounts.getOrDefault(BlockStatus.VALID, 0L).longValue();
        this.revokedBlocks = statusCounts.getOrDefault(BlockStatus.REVOKED, 0L).longValue();
        this.invalidBlocks = statusCounts.getOrDefault(BlockStatus.INVALID, 0L).longValue();
        
        // Structural integrity: all blocks are structurally/cryptographically valid
        this.structuralIntegrity = blockResults.stream()
            .allMatch(result -> result.isStructurallyValid() && result.isCryptographicallyValid());
        
        // Authorization compliance: all blocks have valid authorizations
        this.authorizationCompliance = blockResults.stream()
            .allMatch(BlockValidationResult::isAuthorizationValid);
        
        // Generate summary
        this.summary = generateSummary();
    }
    
    // Getters
    public boolean isStructurallyIntact() { return structuralIntegrity; }
    public boolean isFullyCompliant() { return authorizationCompliance; }
    public long getTotalBlocks() { return totalBlocks; }
    public long getValidBlocks() { return validBlocks; }
    public long getRevokedBlocks() { return revokedBlocks; }
    public long getInvalidBlocks() { return invalidBlocks; }
    public List<BlockValidationResult> getBlockResults() { return Collections.unmodifiableList(blockResults); }
    public String getSummary() { return summary; }
    
    /**
     * Legacy compatibility: returns true if chain is structurally intact.
     * This provides structural integrity validation for compatibility.
     */
    public boolean isValid() {
        return structuralIntegrity;
    }
    
    /**
     * Returns blocks that have been affected by key revocations
     */
    public List<Block> getOrphanedBlocks() {
        return blockResults.stream()
            .filter(result -> result.getStatus() == BlockStatus.REVOKED)
            .map(BlockValidationResult::getBlock)
            .collect(Collectors.toList());
    }
    
    /**
     * Returns blocks that are structurally/cryptographically invalid
     */
    public List<Block> getInvalidBlocksList() {
        return blockResults.stream()
            .filter(result -> result.getStatus() == BlockStatus.INVALID)
            .map(BlockValidationResult::getBlock)
            .collect(Collectors.toList());
    }
    
    /**
     * Returns blocks that are completely valid
     */
    public List<Block> getValidBlocksList() {
        return blockResults.stream()
            .filter(result -> result.getStatus() == BlockStatus.VALID)
            .map(BlockValidationResult::getBlock)
            .collect(Collectors.toList());
    }
    
    /**
     * Returns affected block numbers for revoked keys
     */
    public List<Long> getAffectedBlockNumbers() {
        return getOrphanedBlocks().stream()
            .map(Block::getBlockNumber)
            .sorted()
            .collect(Collectors.toList());
    }
    
    private String generateSummary() {
        StringBuilder sb = new StringBuilder();
        
        if (structuralIntegrity && authorizationCompliance) {
            sb.append("✅ Chain is fully valid");
        } else if (structuralIntegrity && !authorizationCompliance) {
            sb.append("⚠️ Chain is structurally intact but has authorization issues");
        } else {
            sb.append("❌ Chain has structural or cryptographic problems");
        }
        
        sb.append(" (").append(totalBlocks).append(" blocks: ");
        sb.append(validBlocks).append(" valid");
        
        if (revokedBlocks > 0) {
            sb.append(", ").append(revokedBlocks).append(" revoked");
        }
        
        if (invalidBlocks > 0) {
            sb.append(", ").append(invalidBlocks).append(" invalid");
        }
        
        sb.append(")");
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return summary;
    }
    
    /**
     * Detailed report for debugging and auditing
     */
    public String getDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 BLOCKCHAIN VALIDATION REPORT\n");
        sb.append("================================\n");
        sb.append("Structural Integrity: ").append(structuralIntegrity ? "✅ INTACT" : "❌ COMPROMISED").append("\n");
        sb.append("Authorization Compliance: ").append(authorizationCompliance ? "✅ COMPLIANT" : "⚠️ NON-COMPLIANT").append("\n");
        sb.append("Total Blocks: ").append(totalBlocks).append("\n");
        sb.append("Valid Blocks: ").append(validBlocks).append("\n");
        sb.append("Revoked Blocks: ").append(revokedBlocks).append("\n");
        sb.append("Invalid Blocks: ").append(invalidBlocks).append("\n\n");
        
        if (revokedBlocks > 0) {
            sb.append("🔍 REVOKED BLOCKS:\n");
            getOrphanedBlocks().forEach(block -> 
                sb.append("   - Block #").append(block.getBlockNumber())
                  .append(" (signed by revoked key)\n"));
            sb.append("\n");
        }
        
        if (invalidBlocks > 0) {
            sb.append("❌ INVALID BLOCKS:\n");
            blockResults.stream()
                .filter(result -> result.getStatus() == BlockStatus.INVALID)
                .forEach(result -> 
                    sb.append("   - Block #").append(result.getBlock().getBlockNumber())
                      .append(": ").append(result.getErrorMessage()).append("\n"));
            sb.append("\n");
        }
        
        sb.append("📝 SUMMARY: ").append(summary);
        
        return sb.toString();
    }
}
