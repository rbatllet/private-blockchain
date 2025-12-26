package com.rbatllet.blockchain.core;

/**
 * Quick validator to verify that the test environment is correctly configured
 */
public class TestEnvironmentValidator {
    
    public static void main(String[] args) {
        System.out.println("=== TEST ENVIRONMENT VALIDATOR ===\n");
        
        boolean allChecksPass = true;
        
        // Verify necessary classes
        System.out.println("📦 Verifying class dependencies...");
        allChecksPass &= checkClass("com.rbatllet.blockchain.core.Blockchain", "Blockchain core class");
        allChecksPass &= checkClass("com.rbatllet.blockchain.util.CryptoUtil", "CryptoUtil class");
        allChecksPass &= checkClass("com.rbatllet.blockchain.entity.Block", "Block entity");
        allChecksPass &= checkClass("com.rbatllet.blockchain.entity.AuthorizedKey", "AuthorizedKey entity");
        allChecksPass &= checkClass("com.rbatllet.blockchain.dto.ChainExportData", "ChainExportData DTO");
        
        // Verify JUnit dependencies
        System.out.println("\n🧪 Verifying test dependencies...");
        allChecksPass &= checkClass("org.junit.jupiter.api.Test", "JUnit 6 Test");
        allChecksPass &= checkClass("org.junit.jupiter.api.BeforeEach", "JUnit 6 BeforeEach");
        allChecksPass &= checkClass("org.junit.jupiter.api.DisplayName", "JUnit 6 DisplayName");
        allChecksPass &= checkClass("org.junit.jupiter.api.Order", "JUnit 6 Order");
        
        // Verify Jackson dependencies
        System.out.println("\n📝 Verifying JSON dependencies...");
        allChecksPass &= checkClass("com.fasterxml.jackson.databind.ObjectMapper", "Jackson ObjectMapper");
        allChecksPass &= checkClass("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", "Jackson JSR310 Module");
        
        // Verify Additional Advanced Functions
        System.out.println("\n🔧 Verifying availability of Additional Advanced Functions...");
        try {
            Blockchain blockchain = new Blockchain();
            
            // Verify size validation methods
            int maxSize = blockchain.getMaxBlockSizeBytes();
            int offChainThreshold = blockchain.getOffChainThresholdBytes();
            System.out.println("   ✅ Block size validation - Max bytes: " + maxSize + ", Off-chain threshold: " + offChainThreshold);
            
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
            boolean hasGetBlocksByTimeRange = false;
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
                    case "getBlocksByTimeRange" -> hasGetBlocksByTimeRange = true;
                    case "getAuthorizedKeyByOwner" -> hasGetAuthorizedKeyByOwner = true;
                    case "getAllAuthorizedKeys" -> hasGetAllAuthorizedKeys = true;
                    case "clearAndReinitialize" -> hasClearAndReinitialize = true;
                }
            }
            
            // Verify basic advanced functions
            System.out.println("\n🔍 Verifying basic advanced functions...");
            allChecksPass &= checkMethod(hasExportChain, "exportChain");
            allChecksPass &= checkMethod(hasImportChain, "importChain");
            allChecksPass &= checkMethod(hasRollbackBlocks, "rollbackBlocks");
            allChecksPass &= checkMethod(hasSearchByContent, "searchBlocksByContent");
            allChecksPass &= checkMethod(hasGetBlockByHash, "getBlockByHash");
            
            // Verify authorized key management functions
            System.out.println("\n🔑 Verifying authorized key management functions...");
            allChecksPass &= checkMethod(hasRevokeAuthorizedKey, "revokeAuthorizedKey");
            allChecksPass &= checkMethod(hasCanDeleteAuthorizedKey, "canDeleteAuthorizedKey");
            allChecksPass &= checkMethod(hasDeleteAuthorizedKey, "deleteAuthorizedKey");
            allChecksPass &= checkMethod(hasDangerouslyDeleteAuthorizedKey, "dangerouslyDeleteAuthorizedKey");
            
            // Verify temporal query functions
            System.out.println("\n⏰ Verifying temporal query functions...");
            allChecksPass &= checkMethod(hasGetBlocksByDateRange, "getBlocksByDateRange");
            allChecksPass &= checkMethod(hasGetBlocksByTimeRange, "getBlocksByTimeRange");
            
            // Verify additional functions
            System.out.println("\n🔍 Verifying additional functions...");
            allChecksPass &= checkMethod(hasGetAuthorizedKeyByOwner, "getAuthorizedKeyByOwner");
            allChecksPass &= checkMethod(hasGetAllAuthorizedKeys, "getAllAuthorizedKeys");
            allChecksPass &= checkMethod(hasClearAndReinitialize, "clearAndReinitialize");
            
        } catch (Exception e) {
            System.out.println("   ❌ Error initializing Blockchain: " + e.getMessage());
            allChecksPass = false;
        }
        
        // Final summary
        System.out.println("\n=== VALIDATION SUMMARY ===");
        if (allChecksPass) {
            System.out.println("🎉 TEST ENVIRONMENT CORRECTLY CONFIGURED!");
            System.out.println("✅ You can run tests with confidence.");
            System.out.println("\nCommands to run tests:");
            System.out.println("  mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest");
            System.out.println("  scripts/run_advanced_tests.zsh");
            System.out.println("  scripts/run_all_tests.zsh");
        } else {
            System.out.println("❌ PROBLEMS DETECTED IN TEST ENVIRONMENT");
            System.out.println("📝 Review the errors above before running tests.");
            System.out.println("\nSuggestions:");
            System.out.println("  - Run 'mvn clean compile' to recompile");
            System.out.println("  - Verify that all dependencies are in pom.xml");
            System.out.println("  - Make sure that Additional Advanced Functions are implemented");
        }
        
        System.exit(allChecksPass ? 0 : 1);
    }
    
    private static boolean checkClass(String className, String description) {
        try {
            Class.forName(className);
            System.out.println("   ✅ " + description);
            return true;
        } catch (ClassNotFoundException e) {
            System.out.println("   ❌ " + description + " - Not found: " + className);
            return false;
        }
    }
    
    private static boolean checkMethod(boolean exists, String methodName) {
        if (exists) {
            System.out.println("   ✅ Method " + methodName + " available");
            return true;
        } else {
            System.out.println("   ❌ Method " + methodName + " not found");
            return false;
        }
    }
}
