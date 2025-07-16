package com.rbatllet.blockchain.demos;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.logging.AdvancedLoggingService;
import com.rbatllet.blockchain.logging.LoggingManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * Demonstration of advanced logging capabilities
 * Shows comprehensive operation tracking, performance monitoring, and security logging
 */
public class AdvancedLoggingDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedLoggingDemo.class);
    private static Blockchain blockchain;
    
    public static void main(String[] args) {
        System.out.println("🔍 ADVANCED LOGGING SYSTEM DEMONSTRATION");
        System.out.println("=" .repeat(50));
        
        try {
            // Initialize logging system
            initializeLoggingSystem();
            
            // Initialize blockchain
            blockchain = new Blockchain();
            
            // Demonstrate different logging capabilities
            demonstrateBlockchainOperationLogging();
            demonstrateSearchOperationLogging();
            demonstrateCryptoOperationLogging();
            demonstrateSecurityEventLogging();
            demonstratePerformanceMonitoring();
            demonstrateFailureHandling();
            
            // Generate comprehensive report
            generateComprehensiveReport();
            
        } catch (Exception e) {
            logger.error("❌ Demo failed", e);
            System.err.println("❌ Demo failed: " + e.getMessage());
        } finally {
            // Cleanup
            cleanup();
        }
    }
    
    private static void initializeLoggingSystem() {
        System.out.println("\n🚀 Initializing Advanced Logging System");
        
        // Initialize and start logging manager
        LoggingManager.initialize();
        LoggingManager.start();
        
        System.out.println("✅ Logging system initialized and started");
    }
    
    private static void demonstrateBlockchainOperationLogging() {
        System.out.println("\n📦 Demonstrating Blockchain Operation Logging");
        
        try {
            // Log block creation operations
            for (int i = 1; i <= 3; i++) {
                final int blockNum = i;
                LoggingManager.logBlockchainOperation(
                    "BLOCK_CREATION",
                    "create_test_block",
                    (long) blockNum,
                    1024L,
                    () -> {
                        String data = "Test block data " + blockNum;
                        KeyPair keyPair = CryptoUtil.generateKeyPair();
                        blockchain.addBlock(data, keyPair.getPrivate(), keyPair.getPublic());
                        return "Block " + blockNum + " created successfully";
                    }
                );
            }
            
            // Log blockchain validation
            LoggingManager.logBlockchainOperation(
                "BLOCKCHAIN_VALIDATION",
                "validate_full_chain",
                null,
                blockchain.getAllBlocks().size() * 1024L,
                () -> {
                    // Simulate blockchain validation
                    int blockCount = blockchain.getAllBlocks().size();
                    boolean isValid = blockCount > 0;
                    return "Chain validation: " + (isValid ? "VALID" : "INVALID") + " (" + blockCount + " blocks)";
                }
            );
            
            System.out.println("✅ Blockchain operations logged successfully");
            
        } catch (Exception e) {
            logger.error("❌ Blockchain operation logging failed", e);
            System.err.println("❌ Blockchain operation logging failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateSearchOperationLogging() {
        System.out.println("\n🔍 Demonstrating Search Operation Logging");
        
        try {
            // Demonstrate different search operations (simplified)
            LoggingManager.logSearchOperation(
                "KEYWORD_SEARCH",
                "document",
                () -> {
                    // Simulate search operation
                    List<String> results = Arrays.asList("Document 1", "Document 2", "Document 3");
                    return results;
                }
            );
            
            LoggingManager.logSearchOperation(
                "SEMANTIC_SEARCH",
                "blockchain technology",
                () -> {
                    // Simulate semantic search
                    List<String> results = Arrays.asList("Blockchain Guide", "Technology Overview");
                    return results;
                }
            );
            
            LoggingManager.logSearchOperation(
                "ADVANCED_SEARCH",
                "logging system",
                () -> {
                    // Simulate advanced search
                    List<String> results = Arrays.asList("Logging Documentation", "System Logs");
                    return results;
                }
            );
            
            System.out.println("✅ Search operations logged successfully");
            
        } catch (Exception e) {
            logger.error("❌ Search operation logging failed", e);
            System.err.println("❌ Search operation logging failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateCryptoOperationLogging() {
        System.out.println("\n🔐 Demonstrating Crypto Operation Logging");
        
        try {
            String testData = "Sensitive data for encryption testing";
            
            // Demonstrate encryption logging
            String encryptedData = LoggingManager.logCryptoOperation(
                "ENCRYPT",
                "AES-256-GCM",
                testData.length(),
                () -> CryptoUtil.encryptWithGCM(testData, "password123")
            );
            
            // Demonstrate decryption logging
            String decryptedData = LoggingManager.logCryptoOperation(
                "DECRYPT",
                "AES-256-GCM",
                encryptedData.length(),
                () -> CryptoUtil.decryptWithGCM(encryptedData, "password123")
            );
            
            // Demonstrate key operations
            LoggingManager.logKeyOperation(
                "GENERATE",
                "EC_SECP256R1",
                "demo_key_1",
                () -> {
                    KeyPair keyPair = CryptoUtil.generateKeyPair();
                    return CryptoUtil.publicKeyToString(keyPair.getPublic());
                }
            );
            
            System.out.println("✅ Crypto operations logged successfully");
            System.out.println("ℹ️  Original data: " + testData);
            System.out.println("ℹ️  Decrypted data: " + decryptedData);
            
        } catch (Exception e) {
            logger.error("❌ Crypto operation logging failed", e);
            System.err.println("❌ Crypto operation logging failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateSecurityEventLogging() {
        System.out.println("\n🛡️ Demonstrating Security Event Logging");
        
        try {
            // Simulate various security events
            AdvancedLoggingService.logSecurityEvent(
                "AUTHENTICATION_SUCCESS",
                "User successfully authenticated with valid credentials",
                AdvancedLoggingService.SecuritySeverity.LOW,
                "demo_user_1"
            );
            
            AdvancedLoggingService.logSecurityEvent(
                "FAILED_LOGIN_ATTEMPT",
                "Multiple failed login attempts detected",
                AdvancedLoggingService.SecuritySeverity.MEDIUM,
                "demo_user_2"
            );
            
            AdvancedLoggingService.logSecurityEvent(
                "UNAUTHORIZED_ACCESS",
                "Attempt to access restricted blockchain data",
                AdvancedLoggingService.SecuritySeverity.HIGH,
                "unknown_user"
            );
            
            AdvancedLoggingService.logSecurityEvent(
                "KEY_ROTATION",
                "Scheduled key rotation completed successfully",
                AdvancedLoggingService.SecuritySeverity.LOW,
                "system"
            );
            
            System.out.println("✅ Security events logged successfully");
            
        } catch (Exception e) {
            logger.error("❌ Security event logging failed", e);
            System.err.println("❌ Security event logging failed: " + e.getMessage());
        }
    }
    
    private static void demonstratePerformanceMonitoring() {
        System.out.println("\n📊 Demonstrating Performance Monitoring");
        
        try {
            // Create performance test data
            Map<String, Object> performanceDetails = new HashMap<>();
            performanceDetails.put("cacheHitRate", 85.5);
            performanceDetails.put("averageResponseTime", 125.3);
            performanceDetails.put("concurrentUsers", 50);
            performanceDetails.put("throughput", 1000);
            
            // Log performance metrics
            AdvancedLoggingService.logPerformanceMetrics(
                "SYSTEM_PERFORMANCE",
                "overall_system_health",
                2500, // 2.5 seconds
                1024 * 1024 * 5, // 5MB processed
                performanceDetails
            );
            
            // Demonstrate memory monitoring
            AdvancedLoggingService.logMemoryEvent(
                "MEMORY_USAGE_CHECK",
                256, // 256MB before
                128, // 128MB after
                "Routine memory optimization completed"
            );
            
            // Demonstrate database performance
            AdvancedLoggingService.logDatabaseOperation("SELECT", "blocks", 450, 1000);
            AdvancedLoggingService.logDatabaseOperation("INSERT", "transactions", 75, 1);
            
            System.out.println("✅ Performance monitoring demonstrated successfully");
            
        } catch (Exception e) {
            logger.error("❌ Performance monitoring failed", e);
            System.err.println("❌ Performance monitoring failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateFailureHandling() {
        System.out.println("\n❌ Demonstrating Failure Handling");
        
        try {
            // Simulate a failed operation
            try {
                LoggingManager.logBlockchainOperation(
                    "BLOCK_VALIDATION",
                    "validate_corrupted_block",
                    999L,
                    2048L,
                    () -> {
                        throw new RuntimeException("Simulated block validation failure");
                    }
                );
            } catch (RuntimeException e) {
                System.out.println("ℹ️  Expected failure caught: " + e.getMessage());
            }
            
            // Simulate failed crypto operation
            try {
                LoggingManager.logCryptoOperation(
                    "DECRYPT",
                    "AES-256-GCM",
                    512L,
                    () -> {
                        throw new RuntimeException("Simulated decryption failure");
                    }
                );
            } catch (RuntimeException e) {
                System.out.println("ℹ️  Expected failure caught: " + e.getMessage());
            }
            
            System.out.println("✅ Failure handling demonstrated successfully");
            
        } catch (Exception e) {
            logger.error("❌ Failure handling demonstration failed", e);
            System.err.println("❌ Failure handling demonstration failed: " + e.getMessage());
        }
    }
    
    private static void generateComprehensiveReport() {
        System.out.println("\n📋 Generating Comprehensive Logging Report");
        
        try {
            // Get advanced logging report
            String advancedReport = AdvancedLoggingService.getPerformanceReport();
            
            // Get system report
            String systemReport = LoggingManager.getSystemReport();
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 ADVANCED LOGGING REPORT");
            System.out.println("=".repeat(60));
            System.out.println(advancedReport);
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🔍 COMPREHENSIVE SYSTEM REPORT");
            System.out.println("=".repeat(60));
            System.out.println(systemReport);
            
            System.out.println("\n✅ Comprehensive reports generated successfully");
            
        } catch (Exception e) {
            logger.error("❌ Report generation failed", e);
            System.err.println("❌ Report generation failed: " + e.getMessage());
        }
    }
    
    private static void cleanup() {
        System.out.println("\n🧹 Cleaning up resources");
        
        try {
            // Generate final report
            System.out.println("\n📋 Final Performance Summary:");
            System.out.println(AdvancedLoggingService.getPerformanceReport());
            
            // Shutdown logging system
            LoggingManager.shutdown();
            
            // Reset metrics
            AdvancedLoggingService.resetMetrics();
            
            System.out.println("✅ Cleanup completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Cleanup failed", e);
            System.err.println("❌ Cleanup failed: " + e.getMessage());
        }
    }
}