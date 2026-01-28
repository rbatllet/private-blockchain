package com.rbatllet.blockchain.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quick validator to verify that the test environment is correctly configured
 */
public class TestEnvironmentValidator {
    private static final Logger logger = LoggerFactory.getLogger(TestEnvironmentValidator.class);
    
    public static void main(String[] args) {
        logger.info("=== TEST ENVIRONMENT VALIDATOR ===\n");
        
        boolean allChecksPass = true;
        
        // Verify necessary classes
        logger.info("📦 Verifying class dependencies...");
        allChecksPass &= checkClass("com.rbatllet.blockchain.core.Blockchain", "Blockchain core class");
        allChecksPass &= checkClass("com.rbatllet.blockchain.util.CryptoUtil", "CryptoUtil class");
        allChecksPass &= checkClass("com.rbatllet.blockchain.entity.Block", "Block entity");
        allChecksPass &= checkClass("com.rbatllet.blockchain.entity.AuthorizedKey", "AuthorizedKey entity");
        allChecksPass &= checkClass("com.rbatllet.blockchain.dto.ChainExportData", "ChainExportData DTO");
        
        // Verify JUnit dependencies
        logger.info("\n🧪 Verifying test dependencies...");
        allChecksPass &= checkClass("org.junit.jupiter.api.Test", "JUnit 6 Test");
        allChecksPass &= checkClass("org.junit.jupiter.api.BeforeEach", "JUnit 6 BeforeEach");
        allChecksPass &= checkClass("org.junit.jupiter.api.DisplayName", "JUnit 6 DisplayName");
        allChecksPass &= checkClass("org.junit.jupiter.api.Order", "JUnit 6 Order");
        
        // Verify Jackson dependencies
        logger.info("\n📝 Verifying JSON dependencies...");
        allChecksPass &= checkClass("com.fasterxml.jackson.databind.ObjectMapper", "Jackson ObjectMapper");
        allChecksPass &= checkClass("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", "Jackson JSR310 Module");
        
        // Verify Additional Advanced Functions
        logger.info("\n🔧 Verifying availability of Additional Advanced Functions...");
        try {
            Blockchain blockchain = new Blockchain();
            
            // Verify size validation methods
            int maxSize = blockchain.getMaxBlockSizeBytes();
            int offChainThreshold = blockchain.getOffChainThresholdBytes();
            logger.info("   ✅ Block size validation - Max bytes: {}, Off-chain threshold: {}", maxSize, offChainThreshold);
            
            // Verify that the Additional Advanced Functions exist (without executing them)
            java.lang.reflect.Method[] methods = Blockchain.class.getDeclaredMethods();
            boolean hasExportChain = false;
            boolean hasImportChain = false;
            boolean hasRollbackBlocks = false;
            boolean hasSearchByContent = false;
            boolean hasGetBlockByHash = false;
            boolean hasRevokeAuthorizedKey = false;
            boolean hasCanDeleteAuthorizedKey = false;
            boolean hasDeleteAuthorizedKey = false;
            boolean hasDangerouslyDeleteAuthorizedKey = false;
            boolean hasGetBlocksByDateRange = false;
            boolean hasStreamBlocksByTimeRange = false;
            boolean hasGetAuthorizedKeyByOwner = false;
            boolean hasGetAllAuthorizedKeys = false;
            boolean hasClearAndReinitialize = false;
            
            for (java.lang.reflect.Method method : methods) {
                switch (method.getName()) {
                    case "exportChain" -> hasExportChain = true;
                    case "importChain" -> hasImportChain = true;
                    case "rollbackBlocks" -> hasRollbackBlocks = true;
                    case "searchBlocksByContent" -> hasSearchByContent = true;
                    case "getBlockByHash" -> hasGetBlockByHash = true;
                    case "revokeAuthorizedKey" -> hasRevokeAuthorizedKey = true;
                    case "canDeleteAuthorizedKey" -> hasCanDeleteAuthorizedKey = true;
                    case "deleteAuthorizedKey" -> hasDeleteAuthorizedKey = true;
                    case "dangerouslyDeleteAuthorizedKey" -> hasDangerouslyDeleteAuthorizedKey = true;
                    case "getBlocksByDateRange" -> hasGetBlocksByDateRange = true;
                    case "streamBlocksByTimeRange" -> hasStreamBlocksByTimeRange = true;
                    case "getAuthorizedKeyByOwner" -> hasGetAuthorizedKeyByOwner = true;
                    case "getAllAuthorizedKeys" -> hasGetAllAuthorizedKeys = true;
                    case "clearAndReinitialize" -> hasClearAndReinitialize = true;
                }
            }
            
            // Verify basic advanced functions
            logger.info("\n🔍 Verifying basic advanced functions...");
            allChecksPass &= checkMethod(hasExportChain, "exportChain");
            allChecksPass &= checkMethod(hasImportChain, "importChain");
            allChecksPass &= checkMethod(hasRollbackBlocks, "rollbackBlocks");
            allChecksPass &= checkMethod(hasSearchByContent, "searchBlocksByContent");
            allChecksPass &= checkMethod(hasGetBlockByHash, "getBlockByHash");
            
            // Verify authorized key management functions
            logger.info("\n🔑 Verifying authorized key management functions...");
            allChecksPass &= checkMethod(hasRevokeAuthorizedKey, "revokeAuthorizedKey");
            allChecksPass &= checkMethod(hasCanDeleteAuthorizedKey, "canDeleteAuthorizedKey");
            allChecksPass &= checkMethod(hasDeleteAuthorizedKey, "deleteAuthorizedKey");
            allChecksPass &= checkMethod(hasDangerouslyDeleteAuthorizedKey, "dangerouslyDeleteAuthorizedKey");
            
            // Verify temporal query functions
            logger.info("\n⏰ Verifying temporal query functions...");
            allChecksPass &= checkMethod(hasGetBlocksByDateRange, "getBlocksByDateRange");
            allChecksPass &= checkMethod(hasStreamBlocksByTimeRange, "streamBlocksByTimeRange");
            
            // Verify additional functions
            logger.info("\n🔍 Verifying additional functions...");
            allChecksPass &= checkMethod(hasGetAuthorizedKeyByOwner, "getAuthorizedKeyByOwner");
            allChecksPass &= checkMethod(hasGetAllAuthorizedKeys, "getAllAuthorizedKeys");
            allChecksPass &= checkMethod(hasClearAndReinitialize, "clearAndReinitialize");
            
        } catch (Exception e) {
            logger.error("   ❌ Error initializing Blockchain: {}", e.getMessage());
            allChecksPass = false;
        }
        
        // Final summary
        logger.info("\n=== VALIDATION SUMMARY ===");
        if (allChecksPass) {
            logger.info("🎉 TEST ENVIRONMENT CORRECTLY CONFIGURED!");
            logger.info("✅ You can run tests with confidence.");
            logger.info("\nCommands to run tests:");
            logger.info("  mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest");
            logger.info("  scripts/run_advanced_tests.zsh");
            logger.info("  scripts/run_all_tests.zsh");
        } else {
            logger.error("❌ PROBLEMS DETECTED IN TEST ENVIRONMENT");
            logger.error("📝 Review the errors above before running tests.");
            logger.info("\nSuggestions:");
            logger.info("  - Run 'mvn clean compile' to recompile");
            logger.info("  - Verify that all dependencies are in pom.xml");
            logger.info("  - Make sure that Additional Advanced Functions are implemented");
        }
        
        System.exit(allChecksPass ? 0 : 1);
    }
    
    private static boolean checkClass(String className, String description) {
        try {
            Class.forName(className);
            logger.info("   ✅ {}", description);
            return true;
        } catch (ClassNotFoundException e) {
            logger.error("   ❌ {} - Not found: {}", description, className);
            return false;
        }
    }
    
    private static boolean checkMethod(boolean exists, String methodName) {
        if (exists) {
            logger.info("   ✅ Method {} available", methodName);
            return true;
        } else {
            logger.error("   ❌ Method {} not found", methodName);
            return false;
        }
    }
}
