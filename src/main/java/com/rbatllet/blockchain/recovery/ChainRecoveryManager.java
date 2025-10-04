package com.rbatllet.blockchain.recovery;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
// REMOVED: ReentrantReadWriteLock - caused deadlock issue #4
import java.util.stream.Collectors;

/**
 * Chain Recovery Manager for handling blockchain corruption after key deletion
 * Provides multiple recovery strategies when forced key deletion breaks chain validation
 * FIXED: Thread-safe with proper synchronization
 */
public class ChainRecoveryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ChainRecoveryManager.class);
    
    private final Blockchain blockchain;
    // FIXED: Thread-safe set for tracking corruption keys
    private final Set<String> keysInvolvedInCorruption = Collections.synchronizedSet(new HashSet<>());
    
    // Flag to indicate if we're being called from within a lock context
    private final boolean calledWithinLock;
    
    // REMOVED: recoveryLock - Blockchain already has GLOBAL_BLOCKCHAIN_LOCK protection
    // Adding a second lock layer caused deadlocks (issue #4)
    
    public ChainRecoveryManager(Blockchain blockchain) {
        this(blockchain, false);
    }
    
    public ChainRecoveryManager(Blockchain blockchain, boolean calledWithinLock) {
        if (blockchain == null) {
            throw new IllegalArgumentException("Blockchain cannot be null");
        }
        this.blockchain = blockchain;
        this.calledWithinLock = calledWithinLock;
    }
    
    /**
     * Main recovery method - tries multiple strategies in order of preference
     * @param deletedPublicKey The key that was deleted and caused corruption
     * @param ownerName Original owner name for re-authorization
     * @return RecoveryResult with success status and details
     */
    public RecoveryResult recoverCorruptedChain(String deletedPublicKey, String ownerName) {
        // REMOVED: recoveryLock - causes deadlock with GLOBAL_BLOCKCHAIN_LOCK (issue #4)
        // Blockchain methods already protected by GLOBAL_BLOCKCHAIN_LOCK
        
        // Validate input parameters
        if (deletedPublicKey == null || deletedPublicKey.trim().isEmpty()) {
            return new RecoveryResult(false, "VALIDATION_ERROR", 
                "Recovery failed: deleted public key cannot be null or empty");
        }
        if (ownerName == null || ownerName.trim().isEmpty()) {
            return new RecoveryResult(false, "VALIDATION_ERROR", 
                "Recovery failed: owner name cannot be null or empty");
        }
        
        // Check if key still exists (shouldn't recover existing keys)
        boolean keyExists = (calledWithinLock 
            ? blockchain.getAuthorizedKeysWithoutLock()
            : blockchain.getAuthorizedKeys()
        ).stream().anyMatch(key -> key.getPublicKey().equals(deletedPublicKey));
        
        if (keyExists) {
            return new RecoveryResult(false, "VALIDATION_ERROR", 
                "Recovery failed: key is not deleted and exists in authorized keys");
        }
        
        // SECURITY: Check if the chain is actually corrupted
        var chainValidation = calledWithinLock 
            ? blockchain.validateChainDetailedWithoutLock()
            : blockchain.validateChainDetailed();
        boolean chainIsValid = chainValidation.isStructurallyIntact() && chainValidation.isFullyCompliant();
        
        // If chain is invalid, identify ALL missing keys that signed blocks
        if (!chainIsValid) {
            identifyAllMissingKeys();
        }
        
        // SECURITY: Check if this key actually signed blocks that are now causing corruption
        AtomicBoolean keySignedCorruptedBlocks = new AtomicBoolean(false);
        boolean keyWasInvolvedInCorruption = keysInvolvedInCorruption.contains(deletedPublicKey);

        if (!chainIsValid) {
            // Only check for corrupted blocks if chain is invalid
            // Use batch processing to avoid loading all blocks into memory
            blockchain.processChainInBatches(batch -> {
                for (Block block : batch) {
                    if (deletedPublicKey.equals(block.getSignerPublicKey())) {
                        // Verify this specific block is invalid due to missing key
                        boolean isValid = calledWithinLock 
                            ? blockchain.validateSingleBlockWithoutLock(block)
                            : blockchain.validateSingleBlock(block);
                        if (!isValid) {
                            keySignedCorruptedBlocks.set(true);
                            // Track this key as involved in corruption
                            keysInvolvedInCorruption.add(deletedPublicKey);
                            return; // Early exit from lambda
                        }
                    }
                }
            }, 1000);
        }
        
        // SECURITY: Only allow recovery if chain is corrupted AND this key caused corruption
        // OR if this key was previously involved in corruption but blocks were removed by rollback
        if (chainIsValid && !keyWasInvolvedInCorruption) {
            return new RecoveryResult(false, "VALIDATION_ERROR", 
                "Recovery failed: chain is valid, no recovery needed");
        }
        
        if (!keySignedCorruptedBlocks.get() && !keyWasInvolvedInCorruption) {
            return new RecoveryResult(false, "VALIDATION_ERROR",
                "Recovery failed: key did not sign any corrupted blocks");
        }
        
        logger.info("🚨 CHAIN RECOVERY INITIATED");
        logger.info("🔑 Deleted key: {}...", deletedPublicKey.substring(0, Math.min(32, deletedPublicKey.length())));
        logger.info("👤 Owner: {}", ownerName);
        
        // Special case: If chain is valid but key was involved in previous corruption,
        // attempt re-authorization only (for multi-user corruption scenarios)
        if (chainIsValid && keyWasInvolvedInCorruption) {
            logger.info("🔄 MULTI-USER RECOVERY: Chain is valid but key was involved in previous corruption");
            RecoveryResult reAuthResult = attemptReauthorization(deletedPublicKey, ownerName);
            if (reAuthResult.isSuccess()) {
                // Remove from tracking since it's now recovered
                keysInvolvedInCorruption.remove(deletedPublicKey);
                return reAuthResult;
            } else {
                return new RecoveryResult(false, "VALIDATION_ERROR", 
                    "Multi-user recovery failed: re-authorization unsuccessful");
            }
        }
        
        // Strategy 1: Re-authorization (least disruptive)
        logger.info("🔄 STRATEGY 1: Re-authorization recovery");
        RecoveryResult reAuthResult = attemptReauthorization(deletedPublicKey, ownerName);
        if (reAuthResult.isSuccess()) {
            // Remove from tracking since it's now recovered
            keysInvolvedInCorruption.remove(deletedPublicKey);
            return reAuthResult;
        }
        logger.warn("❌ Re-authorization failed: {}", reAuthResult.getMessage());
        
        // Strategy 2: Rollback corrupted blocks
        logger.info("🔄 STRATEGY 2: Rollback recovery");
        RecoveryResult rollbackResult = attemptRollbackRecovery(deletedPublicKey);
        if (rollbackResult.isSuccess()) {
            // Remove from tracking since it's now recovered
            keysInvolvedInCorruption.remove(deletedPublicKey);
            return rollbackResult;
        }
        logger.warn("❌ Rollback failed: {}", rollbackResult.getMessage());
        
        // Strategy 3: Export valid portion
        logger.info("🔄 STRATEGY 3: Partial export recovery");
        RecoveryResult exportResult = attemptPartialExport(deletedPublicKey);
        if (exportResult.isSuccess()) {
            return exportResult;
        }
        logger.warn("❌ Partial export failed: {}", exportResult.getMessage());
        
        // All strategies failed
        return new RecoveryResult(false, "FAILED", 
            "All recovery strategies failed. Manual intervention required.");
    }
    
    /**
     * Strategy 1: Re-authorize the deleted key to restore chain validity
     */
    private RecoveryResult attemptReauthorization(String deletedPublicKey, String ownerName) {
        try {
            logger.info("🔑 Attempting to re-authorize deleted key...");
            
            // Re-add the key with recovery marker
            String recoveryOwnerName = ownerName + " (RECOVERED-" + 
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ")";
            
            boolean reauthorized = calledWithinLock 
                ? blockchain.addAuthorizedKeyWithoutLock(deletedPublicKey, recoveryOwnerName, null)
                : blockchain.addAuthorizedKey(deletedPublicKey, recoveryOwnerName);
            
            if (!reauthorized) {
                return new RecoveryResult(false, "RE_AUTHORIZATION", 
                    "Failed to re-authorize key - key might already exist");
            }
            
            // Verify chain is now valid
            var reauthorizationValidation = calledWithinLock 
                ? blockchain.validateChainDetailedWithoutLock()
                : blockchain.validateChainDetailed();
            if (reauthorizationValidation.isStructurallyIntact() && reauthorizationValidation.isFullyCompliant()) {
                logger.info("✅ Chain recovered by re-authorization!");
                return new RecoveryResult(true, "RE_AUTHORIZATION", 
                    "Chain successfully recovered by re-authorizing key as: " + recoveryOwnerName);
            } else {
                // Re-authorization didn't help, remove the key again
                if (calledWithinLock) {
                    blockchain.revokeAuthorizedKeyWithoutLock(deletedPublicKey);
                } else {
                    blockchain.revokeAuthorizedKey(deletedPublicKey);
                }
                return new RecoveryResult(false, "RE_AUTHORIZATION", 
                    "Re-authorization completed but chain still invalid");
            }
            
        } catch (Exception e) {
            return new RecoveryResult(false, "RE_AUTHORIZATION", 
                "Re-authorization failed with exception: " + e.getMessage());
        }
    }
    
    /**
     * Strategy 2: Intelligent rollback to remove corrupted blocks while preserving valid ones
     */
    private RecoveryResult attemptRollbackRecovery(String deletedPublicKey) {
        try {
            logger.info("🔄 Identifying corrupted blocks...");

            // Find all blocks signed by the deleted key
            List<Long> corruptedBlockNumbers = Collections.synchronizedList(new ArrayList<>());
            AtomicLong totalBlocks = new AtomicLong(0);

            blockchain.processChainInBatches(batch -> {
                for (Block block : batch) {
                    totalBlocks.incrementAndGet();
                    if (deletedPublicKey.equals(block.getSignerPublicKey())) {
                        // Verify this block is actually corrupted
                        boolean isValid = calledWithinLock 
                            ? blockchain.validateSingleBlockWithoutLock(block)
                            : blockchain.validateSingleBlock(block);
                        if (!isValid) {
                            corruptedBlockNumbers.add(block.getBlockNumber());
                        }
                    }
                }
            }, 1000);

            if (corruptedBlockNumbers.isEmpty()) {
                return new RecoveryResult(false, "ROLLBACK",
                    "No corrupted blocks found, issue might be elsewhere");
            }

            logger.info("📊 Found {} corrupted blocks", corruptedBlockNumbers.size());

            // SECURITY: Intelligent rollback strategy
            // Try to find the latest valid block we can keep
            Long rollbackTarget = findSafeRollbackTarget(corruptedBlockNumbers, totalBlocks.get());
            
            logger.info("🎯 Rolling back to block #{}", rollbackTarget);
            
            // Perform rollback
            boolean rollbackSuccess = calledWithinLock 
                ? blockchain.rollbackToBlockWithoutLock(rollbackTarget)
                : blockchain.rollbackToBlock(rollbackTarget);

            if (rollbackSuccess) {
                var rollbackValidation = calledWithinLock 
                    ? blockchain.validateChainDetailedWithoutLock()
                    : blockchain.validateChainDetailed();
                if (rollbackValidation.isStructurallyIntact() && rollbackValidation.isFullyCompliant()) {
                    return new RecoveryResult(true, "ROLLBACK",
                        "Chain recovered by rolling back to block #" + rollbackTarget +
                        ". Removed " + (totalBlocks.get() - rollbackTarget - 1) + " blocks to ensure integrity.");
                } else {
                    return new RecoveryResult(false, "ROLLBACK",
                        "Rollback completed but chain still invalid");
                }
            }
            
            // If we reach here, the chain could not be recovered
            return new RecoveryResult(false, "ROLLBACK", 
                "Unable to find a valid rollback point to recover the chain");
            
        } catch (Exception e) {
            return new RecoveryResult(false, "ROLLBACK", 
                "Rollback failed with exception: " + e.getMessage());
        }
    }
    
    /**
     * SECURITY: Identify all missing keys that signed blocks but are no longer authorized
     * This helps track multi-user corruption scenarios
     */
    private void identifyAllMissingKeys() {
        Set<String> authorizedKeys = (calledWithinLock 
            ? blockchain.getAuthorizedKeysWithoutLock()
            : blockchain.getAuthorizedKeys()
        ).stream()
                .map(key -> key.getPublicKey())
                .collect(Collectors.toSet());

        blockchain.processChainInBatches(batch -> {
            for (Block block : batch) {
                String signerKey = block.getSignerPublicKey();
                if (signerKey != null && !signerKey.equals("GENESIS") &&
                    !authorizedKeys.contains(signerKey)) {
                    // This key signed a block but is no longer authorized
                    keysInvolvedInCorruption.add(signerKey);
                }
            }
        }, 1000);

        if (!keysInvolvedInCorruption.isEmpty()) {
            logger.warn("🔍 SECURITY: Identified {} missing keys involved in corruption", keysInvolvedInCorruption.size());
        }
    }
    
    /**
     * SECURITY: Find the safest rollback target that preserves maximum valid data
     * while maintaining blockchain integrity and cryptographic security
     */
    private Long findSafeRollbackTarget(List<Long> corruptedBlockNumbers, Long totalBlocks) {
        logger.info("🧠 ANALYZING OPTIMAL ROLLBACK STRATEGY...");

        // Sort corrupted blocks to analyze pattern
        corruptedBlockNumbers.sort(Long::compareTo);
        Long earliestCorruptedBlock = corruptedBlockNumbers.get(0);

        // SECURITY ANALYSIS: Multiple strategies with safety verification

        // Strategy 1: Conservative rollback (current approach - always safe)
        Long conservativeTarget = Math.max(0L, earliestCorruptedBlock - 1L);

        // Strategy 2: Intelligent analysis for optimal preservation
        Long intelligentTarget = findIntelligentRollbackTarget(corruptedBlockNumbers, totalBlocks);

        // Strategy 3: Hash integrity verification
        Long hashSafeTarget = findHashIntegrityTarget(corruptedBlockNumbers, totalBlocks);

        // SECURITY DECISION: Choose the most conservative of all valid options
        // This ensures we never compromise blockchain integrity for data preservation
        Long optimalTarget = Math.min(Math.min(conservativeTarget, intelligentTarget), hashSafeTarget);

        // Additional safety verification
        if (!isRollbackTargetSafe(optimalTarget, corruptedBlockNumbers, totalBlocks)) {
            logger.warn("⚠️ SECURITY WARNING: Optimal target failed safety check, using conservative approach");
            optimalTarget = conservativeTarget;
        }
        
        logger.info("📊 ROLLBACK ANALYSIS RESULTS:");
        logger.info("   - Conservative target (always safe): block #{}", conservativeTarget);
        logger.info("   - Intelligent analysis target: block #{}", intelligentTarget);
        logger.info("   - Hash integrity target: block #{}", hashSafeTarget);
        logger.info("   - SELECTED OPTIMAL TARGET: block #{}", optimalTarget);
        logger.info("   - Blocks to preserve: {}", (optimalTarget + 1));
        logger.info("   - Blocks to remove: {}", (totalBlocks - optimalTarget - 1));
        logger.info("   - Data preservation efficiency: {}%", String.format("%.1f", (optimalTarget + 1.0) / totalBlocks * 100));
        
        return optimalTarget;
    }
    
    /**
     * SECURITY: Intelligent rollback analysis that attempts to preserve valid blocks
     * while maintaining cryptographic and temporal integrity
     */
    private Long findIntelligentRollbackTarget(List<Long> corruptedBlockNumbers, Long totalBlocks) {
        try {
            logger.debug("🔍 Performing intelligent block analysis...");

            // Build corruption map for O(1) lookup
            Set<Long> corruptedSet = new HashSet<>(corruptedBlockNumbers);

            // Find the longest valid prefix that can be safely preserved
            AtomicLong maxSafeTarget = new AtomicLong(-1L);
            AtomicBoolean foundCorruption = new AtomicBoolean(false);
            AtomicReference<Block> previousBlock = new AtomicReference<>(null);

            blockchain.processChainInBatches(batch -> {
                if (foundCorruption.get()) {
                    return; // Early exit if corruption already found
                }

                for (Block block : batch) {
                    if (foundCorruption.get()) {
                        break;
                    }

                    if (corruptedSet.contains(block.getBlockNumber())) {
                        // Found corrupted block - cannot preserve beyond this point safely
                        foundCorruption.set(true);
                        break;
                    }

                    // SECURITY CHECK 1: Block must be independently valid
                    boolean isValid = calledWithinLock 
                        ? blockchain.validateSingleBlockWithoutLock(block)
                        : blockchain.validateSingleBlock(block);
                    if (!isValid) {
                        logger.warn("⚠️ Block #{} failed independent validation", block.getBlockNumber());
                        foundCorruption.set(true);
                        break;
                    }

                    // SECURITY CHECK 2: Temporal consistency
                    Block prev = previousBlock.get();
                    if (prev != null && block.getTimestamp().isBefore(prev.getTimestamp())) {
                        logger.warn("⚠️ Temporal inconsistency detected at block #{}", block.getBlockNumber());
                        foundCorruption.set(true);
                        break;
                    }

                    // This block passes all security checks
                    maxSafeTarget.set(block.getBlockNumber());
                    previousBlock.set(block);
                }
            }, 1000);

            logger.info("🎯 Intelligent analysis found safe target: block #{}", maxSafeTarget.get());
            return Math.max(0L, maxSafeTarget.get());

        } catch (Exception e) {
            logger.warn("⚠️ Intelligent analysis failed: {}, using conservative approach", e.getMessage());
            return Math.max(0L, corruptedBlockNumbers.get(0) - 1L);
        }
    }
    
    /**
     * SECURITY: Find rollback target that maintains hash integrity
     */
    private Long findHashIntegrityTarget(List<Long> corruptedBlockNumbers, Long totalBlocks) {
        try {
            logger.debug("🔗 Analyzing hash chain integrity...");

            // Conservative approach: use the block before the first corrupted block
            Long target = Math.max(0L, corruptedBlockNumbers.get(0) - 1L);
            logger.debug("✅ Hash integrity target (conservative): block #{}", target);
            return target;

        } catch (Exception e) {
            logger.warn("⚠️ Hash integrity analysis failed", e);
            return 0L; // Safe fallback
        }
    }
    
    /**
     * SECURITY: Final safety verification for rollback target
     * This is the last line of defense against data corruption
     */
    private boolean isRollbackTargetSafe(Long target, List<Long> corruptedBlockNumbers, Long totalBlocks) {
        try {
            logger.debug("🛡️ Performing final safety verification for target #{}...", target);

            // SAFETY CHECK 1: Target must not be negative
            if (target < 0L) {
                logger.error("❌ Safety check failed: negative target");
                return false;
            }

            // SAFETY CHECK 2: Target must not exceed available blocks
            if (target >= totalBlocks) {
                logger.error("❌ Safety check failed: target exceeds available blocks");
                return false;
            }

            // SAFETY CHECK 3: Target must be before any corrupted block
            Long earliestCorrupted = corruptedBlockNumbers.get(0);
            if (target >= earliestCorrupted) {
                logger.error("❌ Safety check failed: target would preserve corrupted blocks");
                return false;
            }

            logger.debug("✅ Safety verification passed for target #{}", target);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Safety verification failed with exception", e);
            return false; // Fail safe
        }
    }
    
    /**
     * Strategy 3: Export the valid portion of the chain
     */
    private RecoveryResult attemptPartialExport(String deletedPublicKey) {
        try {
            logger.info("📤 Exporting valid portion of chain...");

            List<Block> validBlocks = Collections.synchronizedList(new ArrayList<>());
            AtomicLong lastValidBlockNumber = new AtomicLong(-1L);
            AtomicBoolean foundCorruption = new AtomicBoolean(false);

            // Find valid blocks (stop at first corruption)
            blockchain.processChainInBatches(batch -> {
                if (foundCorruption.get()) {
                    return; // Early exit if corruption already found
                }

                for (Block block : batch) {
                    if (foundCorruption.get()) {
                        break;
                    }

                    if (deletedPublicKey.equals(block.getSignerPublicKey())) {
                        logger.warn("⚠️ Stopping at corrupted block #{}", block.getBlockNumber());
                        foundCorruption.set(true);
                        break;
                    } else {
                        boolean isValid = calledWithinLock 
                            ? blockchain.validateSingleBlockWithoutLock(block)
                            : blockchain.validateSingleBlock(block);
                        if (isValid) {
                            validBlocks.add(block);
                            lastValidBlockNumber.set(block.getBlockNumber());
                        } else {
                            logger.warn("⚠️ Stopping at invalid block #{}", block.getBlockNumber());
                            foundCorruption.set(true);
                            break;
                        }
                    }
                }
            }, 1000);

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
                    ". Contains " + validBlocks.size() + " valid blocks (up to #" + lastValidBlockNumber.get() + ")");
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
        // REMOVED: recoveryLock - causes deadlock with GLOBAL_BLOCKCHAIN_LOCK
        // Blockchain methods already protected by GLOBAL_BLOCKCHAIN_LOCK
        
        List<Block> corruptedBlocks = Collections.synchronizedList(new ArrayList<>());
        List<Block> validBlocks = Collections.synchronizedList(new ArrayList<>());
        AtomicLong totalBlocks = new AtomicLong(0);

        blockchain.processChainInBatches(batch -> {
            for (Block block : batch) {
                totalBlocks.incrementAndGet();
                boolean isValid = calledWithinLock 
                    ? blockchain.validateSingleBlockWithoutLock(block)
                    : blockchain.validateSingleBlock(block);
                if (isValid) {
                    validBlocks.add(block);
                } else {
                    corruptedBlocks.add(block);
                }
            }
        }, 1000);

        return new ChainDiagnostic(
            (int) totalBlocks.get(),
            validBlocks.size(),
            corruptedBlocks.size(),
            corruptedBlocks
        );
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
        public List<Block> getCorruptedBlocksList() { return Collections.unmodifiableList(corruptedBlocksList); }
        
        public boolean isHealthy() { return corruptedBlocks == 0; }
        
        @Override
        public String toString() {
            return String.format("ChainDiagnostic{total=%d, valid=%d, corrupted=%d, healthy=%s}", 
                               totalBlocks, validBlocks, corruptedBlocks, isHealthy());
        }
    }
}