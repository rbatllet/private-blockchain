package com.rbatllet.blockchain.recovery;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Chain Recovery Manager for handling blockchain corruption after key deletion
 * Provides multiple recovery strategies when forced key deletion breaks chain validation
 */
public class ChainRecoveryManager {
    
    private final Blockchain blockchain;
    
    public ChainRecoveryManager(Blockchain blockchain) {
        this.blockchain = blockchain;
    }
    
    /**
     * Main recovery method - tries multiple strategies in order of preference
     * @param deletedPublicKey The key that was deleted and caused corruption
     * @param ownerName Original owner name for re-authorization
     * @return RecoveryResult with success status and details
     */
    public RecoveryResult recoverCorruptedChain(String deletedPublicKey, String ownerName) {
        System.out.println("🚨 CHAIN RECOVERY INITIATED");
        System.out.println("🔑 Deleted key: " + deletedPublicKey.substring(0, Math.min(32, deletedPublicKey.length())) + "...");
        System.out.println("👤 Owner: " + ownerName);
        System.out.println();
        
        // Strategy 1: Re-authorization (least disruptive)
        System.out.println("🔄 STRATEGY 1: Re-authorization recovery");
        RecoveryResult reAuthResult = attemptReauthorization(deletedPublicKey, ownerName);
        if (reAuthResult.isSuccess()) {
            return reAuthResult;
        }
        System.out.println("❌ Re-authorization failed: " + reAuthResult.getMessage());
        System.out.println();
        
        // Strategy 2: Rollback corrupted blocks
        System.out.println("🔄 STRATEGY 2: Rollback recovery");
        RecoveryResult rollbackResult = attemptRollbackRecovery(deletedPublicKey);
        if (rollbackResult.isSuccess()) {
            return rollbackResult;
        }
        System.out.println("❌ Rollback failed: " + rollbackResult.getMessage());
        System.out.println();
        
        // Strategy 3: Export valid portion
        System.out.println("🔄 STRATEGY 3: Partial export recovery");
        RecoveryResult exportResult = attemptPartialExport(deletedPublicKey);
        if (exportResult.isSuccess()) {
            return exportResult;
        }
        System.out.println("❌ Partial export failed: " + exportResult.getMessage());
        System.out.println();
        
        // All strategies failed
        return new RecoveryResult(false, "FAILED", 
            "All recovery strategies failed. Manual intervention required.");
    }
    
    /**
     * Strategy 1: Re-authorize the deleted key to restore chain validity
     */
    private RecoveryResult attemptReauthorization(String deletedPublicKey, String ownerName) {
        try {
            System.out.println("🔑 Attempting to re-authorize deleted key...");
            
            // Re-add the key with recovery marker
            String recoveryOwnerName = ownerName + " (RECOVERED-" + 
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ")";
            
            boolean reauthorized = blockchain.addAuthorizedKey(deletedPublicKey, recoveryOwnerName);
            
            if (!reauthorized) {
                return new RecoveryResult(false, "RE_AUTHORIZATION", 
                    "Failed to re-authorize key - key might already exist");
            }
            
            // Verify chain is now valid
            if (blockchain.validateChain()) {
                System.out.println("✅ Chain recovered by re-authorization!");
                return new RecoveryResult(true, "RE_AUTHORIZATION", 
                    "Chain successfully recovered by re-authorizing key as: " + recoveryOwnerName);
            } else {
                // Re-authorization didn't help, remove the key again
                blockchain.revokeAuthorizedKey(deletedPublicKey);
                return new RecoveryResult(false, "RE_AUTHORIZATION", 
                    "Re-authorization completed but chain still invalid");
            }
            
        } catch (Exception e) {
            return new RecoveryResult(false, "RE_AUTHORIZATION", 
                "Re-authorization failed with exception: " + e.getMessage());
        }
    }
    
    /**
     * Strategy 2: Rollback to remove corrupted blocks
     */
    private RecoveryResult attemptRollbackRecovery(String deletedPublicKey) {
        try {
            System.out.println("🔄 Identifying corrupted blocks...");
            
            // Find all blocks signed by the deleted key
            List<Block> allBlocks = blockchain.getAllBlocks();
            List<Integer> corruptedBlockNumbers = new ArrayList<>();
            
            for (Block block : allBlocks) {
                if (deletedPublicKey.equals(block.getSignerPublicKey())) {
                    corruptedBlockNumbers.add(block.getBlockNumber());
                }
            }
            
            if (corruptedBlockNumbers.isEmpty()) {
                return new RecoveryResult(false, "ROLLBACK", 
                    "No corrupted blocks found, issue might be elsewhere");
            }
            
            // Find the earliest corrupted block
            int earliestCorruptedBlock = corruptedBlockNumbers.stream().min(Integer::compare).orElse(0);
            int rollbackTarget = Math.max(0, earliestCorruptedBlock - 1);
            
            System.out.println("📊 Found " + corruptedBlockNumbers.size() + " corrupted blocks");
            System.out.println("🎯 Rolling back to block #" + rollbackTarget);
            
            // Perform rollback
            boolean rollbackSuccess = blockchain.rollbackToBlock(rollbackTarget);
            
            if (rollbackSuccess && blockchain.validateChain()) {
                return new RecoveryResult(true, "ROLLBACK", 
                    "Chain recovered by rolling back to block #" + rollbackTarget + 
                    ". Removed " + corruptedBlockNumbers.size() + " corrupted blocks.");
            } else {
                return new RecoveryResult(false, "ROLLBACK", 
                    "Rollback completed but chain still invalid");
            }
            
        } catch (Exception e) {
            return new RecoveryResult(false, "ROLLBACK", 
                "Rollback failed with exception: " + e.getMessage());
        }
    }
    
    /**
     * Strategy 3: Export the valid portion of the chain
     */
    private RecoveryResult attemptPartialExport(String deletedPublicKey) {
        try {
            System.out.println("📤 Exporting valid portion of chain...");
            
            List<Block> allBlocks = blockchain.getAllBlocks();
            List<Block> validBlocks = new ArrayList<>();
            int lastValidBlockNumber = -1;
            
            // Find valid blocks (stop at first corruption)
            for (Block block : allBlocks) {
                if (deletedPublicKey.equals(block.getSignerPublicKey())) {
                    System.out.println("⚠️ Stopping at corrupted block #" + block.getBlockNumber());
                    break;
                } else if (blockchain.validateSingleBlock(block)) {
                    validBlocks.add(block);
                    lastValidBlockNumber = block.getBlockNumber();
                } else {
                    System.out.println("⚠️ Stopping at invalid block #" + block.getBlockNumber());
                    break;
                }
            }
            
            if (validBlocks.isEmpty()) {
                return new RecoveryResult(false, "PARTIAL_EXPORT", 
                    "No valid blocks found to export");
            }
            
            // Generate backup filename
            String timestamp = java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFile = "corrupted_chain_recovery_" + timestamp + ".json";
            
            // Export valid portion
            boolean exported = blockchain.exportChain(backupFile);
            
            if (exported) {
                return new RecoveryResult(true, "PARTIAL_EXPORT", 
                    "Valid chain portion exported to: " + backupFile + 
                    ". Contains " + validBlocks.size() + " valid blocks (up to #" + lastValidBlockNumber + ")");
            } else {
                return new RecoveryResult(false, "PARTIAL_EXPORT", 
                    "Failed to export valid chain portion");
            }
            
        } catch (Exception e) {
            return new RecoveryResult(false, "PARTIAL_EXPORT", 
                "Partial export failed with exception: " + e.getMessage());
        }
    }
    
    /**
     * Diagnostic method to analyze chain corruption
     */
    public ChainDiagnostic diagnoseCorruption() {
        try {
            List<Block> allBlocks = blockchain.getAllBlocks();
            List<Block> corruptedBlocks = new ArrayList<>();
            List<Block> validBlocks = new ArrayList<>();
            
            for (Block block : allBlocks) {
                if (blockchain.validateSingleBlock(block)) {
                    validBlocks.add(block);
                } else {
                    corruptedBlocks.add(block);
                }
            }
            
            return new ChainDiagnostic(
                allBlocks.size(),
                validBlocks.size(), 
                corruptedBlocks.size(),
                corruptedBlocks
            );
            
        } catch (Exception e) {
            return new ChainDiagnostic(0, 0, -1, new ArrayList<>());
        }
    }
    
    /**
     * Result of a recovery operation
     */
    public static class RecoveryResult {
        private final boolean success;
        private final String method;
        private final String message;
        private final long timestamp;
        
        public RecoveryResult(boolean success, String method, String message) {
            this.success = success;
            this.method = method;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isSuccess() { return success; }
        public String getMethod() { return method; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("RecoveryResult{success=%s, method='%s', message='%s'}", 
                               success, method, message);
        }
    }
    
    /**
     * Diagnostic information about chain corruption
     */
    public static class ChainDiagnostic {
        private final int totalBlocks;
        private final int validBlocks;
        private final int corruptedBlocks;
        private final List<Block> corruptedBlocksList;
        
        public ChainDiagnostic(int totalBlocks, int validBlocks, int corruptedBlocks, 
                              List<Block> corruptedBlocksList) {
            this.totalBlocks = totalBlocks;
            this.validBlocks = validBlocks;
            this.corruptedBlocks = corruptedBlocks;
            this.corruptedBlocksList = corruptedBlocksList;
        }
        
        public int getTotalBlocks() { return totalBlocks; }
        public int getValidBlocks() { return validBlocks; }
        public int getCorruptedBlocks() { return corruptedBlocks; }
        public List<Block> getCorruptedBlocksList() { return corruptedBlocksList; }
        
        public boolean isHealthy() { return corruptedBlocks == 0; }
        
        @Override
        public String toString() {
            return String.format("ChainDiagnostic{total=%d, valid=%d, corrupted=%d, healthy=%s}", 
                               totalBlocks, validBlocks, corruptedBlocks, isHealthy());
        }
    }
}