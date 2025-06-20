package com.rbatllet.demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager;
import com.rbatllet.blockchain.recovery.RecoveryConfig;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;

/**
 * Example demonstrating how to use the recovery system
 * Shows different configuration options and usage patterns
 */
public class EnhancedRecoveryExample {
    
    public static void main(String[] args) {
        demonstrateBasicUsage();
        System.out.println("\n" + "=".repeat(60) + "\n");
        demonstrateAdvancedConfiguration();
        System.out.println("\n" + "=".repeat(60) + "\n");
        demonstrateProductionUsage();
    }
    
    /**
     * Basic usage with default configuration
     */
    public static void demonstrateBasicUsage() {
        System.out.println("🔧 EXAMPLE 1: Basic Recovery Usage");
        System.out.println("=================================");
        
        try {
            // Initialize blockchain
            Blockchain blockchain = new Blockchain();
            blockchain.clearAndReinitialize();
            
            // Create recovery manager
            ChainRecoveryManager recoveryManager = 
                new ChainRecoveryManager(blockchain);
            
            // Setup test scenario
            KeyPair userKey = CryptoUtil.generateKeyPair();
            String publicKey = CryptoUtil.publicKeyToString(userKey.getPublic());
            
            blockchain.addAuthorizedKey(publicKey, "Test User");
            blockchain.addBlock("Test transaction", userKey.getPrivate(), userKey.getPublic());
            
            System.out.println("✅ Initial setup complete");
            System.out.println("📊 Chain valid: " + blockchain.validateChain());
            
            // Simulate corruption
            blockchain.dangerouslyDeleteAuthorizedKey(publicKey, true, "Test corruption");
            System.out.println("💥 Corruption introduced");
            System.out.println("📊 Chain valid: " + blockchain.validateChain());
            
            // Perform diagnostic
            ChainRecoveryManager.ChainDiagnostic diagnostic = 
                recoveryManager.diagnoseCorruption();
            
            System.out.println("🔍 Diagnostic: " + diagnostic);
            
            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(publicKey, "Test User");
            
            System.out.println("🔧 Recovery result: " + result.getMethod());
            System.out.println("📊 Success: " + result.isSuccess());
            System.out.println("📝 Message: " + result.getMessage());
            
        } catch (Exception e) {
            System.err.println("❌ Basic example failed: " + e.getMessage());
        }
    }
    
    /**
     * Advanced usage with custom configuration
     */
    public static void demonstrateAdvancedConfiguration() {
        System.out.println("⚙️ EXAMPLE 2: Advanced Configuration Usage");
        System.out.println("=========================================");
        
        try {
            // Create custom recovery configuration
            RecoveryConfig customConfig = RecoveryConfig.builder()
                .enableReauthorization(true)
                .enableRollback(true)
                .enablePartialExport(true)
                .enableAutoRecovery(false) // Manual recovery only
                .withAuditLogging(true, "custom_recovery_audit.log")
                .withVerboseLogging(true)
                .withRecoveryLimits(5, 45000) // 5 attempts, 45 second timeout
                .withBackupDirectory("custom_backups")
                .withRollbackLimits(50, 0.15) // Max 50 blocks, 15% safety margin
                .build();
            
            System.out.println("🔧 Custom config: " + customConfig);
            System.out.println("✅ Config valid: " + customConfig.isValid());
            
            // Initialize blockchain
            Blockchain blockchain = new Blockchain();
            blockchain.clearAndReinitialize();
            
            ChainRecoveryManager recoveryManager = 
                new ChainRecoveryManager(blockchain);
            
            // Create a more complex scenario
            KeyPair alice = CryptoUtil.generateKeyPair();
            KeyPair bob = CryptoUtil.generateKeyPair();
            KeyPair charlie = CryptoUtil.generateKeyPair();
            
            String aliceKey = CryptoUtil.publicKeyToString(alice.getPublic());
            String bobKey = CryptoUtil.publicKeyToString(bob.getPublic());
            String charlieKey = CryptoUtil.publicKeyToString(charlie.getPublic());
            
            // Setup complex blockchain
            blockchain.addAuthorizedKey(aliceKey, "Alice");
            blockchain.addAuthorizedKey(bobKey, "Bob");
            blockchain.addAuthorizedKey(charlieKey, "Charlie");
            
            blockchain.addBlock("Alice's contract", alice.getPrivate(), alice.getPublic());
            blockchain.addBlock("Bob's payment", bob.getPrivate(), bob.getPublic());
            blockchain.addBlock("Charlie's agreement", charlie.getPrivate(), charlie.getPublic());
            blockchain.addBlock("Alice's update", alice.getPrivate(), alice.getPublic());
            blockchain.addBlock("Bob's confirmation", bob.getPrivate(), bob.getPublic());
            
            System.out.println("✅ Complex blockchain setup complete");
            System.out.println("📊 Blocks: " + blockchain.getBlockCount());
            System.out.println("👥 Users: " + blockchain.getAuthorizedKeys().size());
            
            // Diagnostic on healthy chain
            ChainRecoveryManager.ChainDiagnostic healthyDiag = 
                recoveryManager.diagnoseCorruption();
            
            System.out.println("🏥 Healthy state: " + healthyDiag);
            
            // Create complex corruption (delete multiple keys)
            System.out.println("💥 Creating complex corruption...");
            blockchain.dangerouslyDeleteAuthorizedKey(charlieKey, true, "Complex test - Charlie");
            blockchain.dangerouslyDeleteAuthorizedKey(aliceKey, true, "Complex test - Alice");
            
            // Analyze complex corruption
            ChainRecoveryManager.ChainDiagnostic corruptDiag = 
                recoveryManager.diagnoseCorruption();
            
            System.out.println("💀 Corruption analysis:");
            System.out.println("   📊 " + corruptDiag);
            System.out.println("   🏥 Healthy: " + corruptDiag.isHealthy());
            System.out.println("   ❌ Corrupted blocks: " + corruptDiag.getCorruptedBlocks());
            
            // Attempt recovery of first key
            System.out.println("🔧 Attempting recovery of Charlie's key...");
            ChainRecoveryManager.RecoveryResult charlieResult = 
                recoveryManager.recoverCorruptedChain(charlieKey, "Charlie");
            
            System.out.println("📋 Charlie recovery: " + charlieResult);
            
            // Attempt recovery of second key
            if (charlieResult.isSuccess()) {
                System.out.println("🔧 Attempting recovery of Alice's key...");
                ChainRecoveryManager.RecoveryResult aliceResult = 
                    recoveryManager.recoverCorruptedChain(aliceKey, "Alice");
                
                System.out.println("📋 Alice recovery: " + aliceResult);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Advanced example failed: " + e.getMessage());
        }
    }
    
    /**
     * Production-ready usage example
     */
    public static void demonstrateProductionUsage() {
        System.out.println("🏭 EXAMPLE 3: Production-Ready Usage");
        System.out.println("===================================");
        
        try {
            // Use production configuration
            RecoveryConfig prodConfig = RecoveryConfig.productionConfig();
            System.out.println("🏭 Production config: " + prodConfig);
            
            // Initialize with production settings
            Blockchain blockchain = new Blockchain();
            blockchain.clearAndReinitialize();
            
            ChainRecoveryManager recoveryManager = 
                new ChainRecoveryManager(blockchain);
            
            // Simulate production scenario
            System.out.println("🔧 Setting up production-like scenario...");
            
            // Create multiple users and transactions
            for (int i = 1; i <= 5; i++) {
                KeyPair userKey = CryptoUtil.generateKeyPair();
                String publicKey = CryptoUtil.publicKeyToString(userKey.getPublic());
                String userName = "ProductionUser" + i;
                
                blockchain.addAuthorizedKey(publicKey, userName);
                blockchain.addBlock("Production transaction " + i, userKey.getPrivate(), userKey.getPublic());
                
                // Simulate a problematic key that will be "accidentally" deleted
                if (i == 3) {
                    System.out.println("🎯 Marking User3 key for 'accidental' deletion");
                    
                    // Perform comprehensive diagnostic before corruption
                    ChainRecoveryManager.ChainDiagnostic beforeCorruption = 
                        recoveryManager.diagnoseCorruption();
                    
                    System.out.println("📊 Pre-corruption state: " + beforeCorruption);
                    
                    // Simulate accidental deletion
                    System.out.println("💥 Simulating accidental key deletion...");
                    boolean deleted = blockchain.dangerouslyDeleteAuthorizedKey(publicKey, true, 
                        "Production incident: Accidental key deletion during maintenance");
                    
                    if (deleted) {
                        System.out.println("🚨 PRODUCTION INCIDENT: Key deletion detected!");
                        
                        // Immediate diagnostic
                        ChainRecoveryManager.ChainDiagnostic incident = 
                            recoveryManager.diagnoseCorruption();
                        
                        System.out.println("🔍 Incident assessment: " + incident);
                        
                        if (!incident.isHealthy()) {
                            System.out.println("🚨 IMMEDIATE ACTION REQUIRED!");
                            
                            // Execute production recovery
                            System.out.println("🔧 Executing production recovery protocol...");
                            ChainRecoveryManager.RecoveryResult prodRecovery = 
                                recoveryManager.recoverCorruptedChain(publicKey, userName);
                            
                            System.out.println("📋 Production recovery result:");
                            System.out.println("   ✅ Success: " + prodRecovery.isSuccess());
                            System.out.println("   🛠️ Method: " + prodRecovery.getMethod());
                            System.out.println("   💬 Details: " + prodRecovery.getMessage());
                            
                            if (prodRecovery.isSuccess()) {
                                System.out.println("✅ PRODUCTION RECOVERY SUCCESSFUL");
                                System.out.println("📊 Chain integrity restored");
                                
                                // Verify recovery
                                boolean finalValid = blockchain.validateChain();
                                System.out.println("🔍 Post-recovery validation: " + 
                                    (finalValid ? "✅ PASS" : "❌ FAIL"));
                                
                                if (!finalValid) {
                                    System.out.println("🚨 ESCALATION REQUIRED: Recovery incomplete");
                                }
                            } else {
                                System.out.println("❌ PRODUCTION RECOVERY FAILED");
                                System.out.println("🚨 ESCALATING TO LEVEL 2 SUPPORT");
                            }
                        } else {
                            System.out.println("ℹ️ Low severity incident - monitoring continue");
                        }
                    }
                }
            }
            
            // Final production assessment
            System.out.println();
            System.out.println("📊 PRODUCTION STATUS SUMMARY");
            System.out.println("============================");
            
            ChainRecoveryManager.ChainDiagnostic finalAssessment = 
                recoveryManager.diagnoseCorruption();
            
            System.out.println("🏥 System health: " + (finalAssessment.isHealthy() ? "HEALTHY" : "CORRUPTED"));
            System.out.println("🔢 Total blocks: " + finalAssessment.getTotalBlocks());
            System.out.println("✅ Valid blocks: " + finalAssessment.getValidBlocks());
            System.out.println("❌ Corrupted blocks: " + finalAssessment.getCorruptedBlocks());
            
            // Production readiness assessment
            boolean productionReady = finalAssessment.isHealthy();
            
            System.out.println();
            System.out.println("🎯 PRODUCTION READINESS: " + 
                (productionReady ? "✅ READY" : "❌ REQUIRES ATTENTION"));
            
            if (!productionReady) {
                System.out.println("⚠️ RECOMMENDATIONS:");
                System.out.println("   1. Review corrupted blocks and missing keys");
                System.out.println("   2. Consider manual intervention if automated recovery failed");
                System.out.println("   3. Implement additional monitoring for key management");
                System.out.println("   4. Review access controls and deletion procedures");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Production example failed: " + e.getMessage());
            System.err.println("🚨 This would trigger production alerts in real environment");
        }
    }
    
    /**
     * Utility method to demonstrate configuration options
     */
    public static void showConfigurationOptions() {
        System.out.println("⚙️ AVAILABLE CONFIGURATION OPTIONS");
        System.out.println("=================================");
        
        System.out.println("📋 Preset Configurations:");
        System.out.println("   🛡️ Conservative: " + RecoveryConfig.conservativeConfig());
        System.out.println("   ⚡ Aggressive: " + RecoveryConfig.aggressiveConfig());
        System.out.println("   🏭 Production: " + RecoveryConfig.productionConfig());
        
        System.out.println();
        System.out.println("🔧 Custom Configuration Example:");
        
        RecoveryConfig custom = RecoveryConfig.builder()
            .enableReauthorization(true)
            .enableRollback(false)
            .enablePartialExport(true)
            .enableAutoRecovery(true)
            .withAuditLogging(true, "my_audit.log")
            .withVerboseLogging(false)
            .withRecoveryLimits(2, 20000)
            .withBackupDirectory("my_backups")
            .withRollbackLimits(25, 0.2)
            .build();
        
        System.out.println("   📝 " + custom);
        System.out.println("   ✅ Valid: " + custom.isValid());
    }
}