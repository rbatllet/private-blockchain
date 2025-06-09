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
        allChecksPass &= checkClass("org.junit.jupiter.api.Test", "JUnit 5 Test");
        allChecksPass &= checkClass("org.junit.jupiter.api.BeforeEach", "JUnit 5 BeforeEach");
        allChecksPass &= checkClass("org.junit.jupiter.api.DisplayName", "JUnit 5 DisplayName");
        allChecksPass &= checkClass("org.junit.jupiter.api.Order", "JUnit 5 Order");
        
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
            int maxLength = blockchain.getMaxBlockDataLength();
            System.out.println("   ✅ Block size validation - Max bytes: " + maxSize + ", Max length: " + maxLength);
            
            // Verify that the Additional Advanced Functions exist (without executing them)
            java.lang.reflect.Method[] methods = Blockchain.class.getDeclaredMethods();
            boolean hasExportChain = false;
            boolean hasImportChain = false;
            boolean hasRollbackBlocks = false;
            boolean hasSearchByContent = false;
            boolean hasGetBlockByHash = false;
            
            for (java.lang.reflect.Method method : methods) {
                switch (method.getName()) {
                    case "exportChain" -> hasExportChain = true;
                    case "importChain" -> hasImportChain = true;
                    case "rollbackBlocks" -> hasRollbackBlocks = true;
                    case "searchBlocksByContent" -> hasSearchByContent = true;
                    case "getBlockByHash" -> hasGetBlockByHash = true;
                }
            }
            
            allChecksPass &= checkMethod(hasExportChain, "exportChain");
            allChecksPass &= checkMethod(hasImportChain, "importChain");
            allChecksPass &= checkMethod(hasRollbackBlocks, "rollbackBlocks");
            allChecksPass &= checkMethod(hasSearchByContent, "searchBlocksByContent");
            allChecksPass &= checkMethod(hasGetBlockByHash, "getBlockByHash");
            
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
            System.out.println("  ./run_core_tests.sh");
            System.out.println("  ./run_all_tests.sh");
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
            System.out.println("   ❌ " + description + " - No trobat: " + className);
            return false;
        }
    }
    
    private static boolean checkMethod(boolean exists, String methodName) {
        if (exists) {
            System.out.println("   ✅ Mètode " + methodName + " disponible");
            return true;
        } else {
            System.out.println("   ❌ Mètode " + methodName + " no trobat");
            return false;
        }
    }
}
