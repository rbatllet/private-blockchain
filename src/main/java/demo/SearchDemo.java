package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.search.SearchLevel;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

/**
 * Demo of the enhanced search functionality
 */
public class SearchDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== 🔍 ENHANCED SEARCH FUNCTIONALITY DEMO ===");
        System.out.println();
        
        // Clean up first
        cleanup();
        
        Blockchain blockchain = new Blockchain();
        
        // Setup keys
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(publicKey), "SearchDemoUser");
        
        System.out.println("📝 Creating test blocks with different content types...");
        System.out.println();
        
        // Test 1: Block with manual keywords - Medical
        String[] medicalKeywords = {"PATIENT-001", "CARDIOLOGY", "ECG-2024", "MEDICAL"};
        Block medicalBlock = blockchain.addBlockWithKeywords(
            "Patient John Doe underwent ECG examination on 2024-01-15. Results show normal cardiac rhythm.", 
            medicalKeywords, 
            "MEDICAL", 
            privateKey, 
            publicKey
        );
        validateBlockCreation(medicalBlock, "Medical block");
        
        // Test 2: Block with manual keywords - Finance
        String[] financeKeywords = {"PROJECT-ALPHA", "BUDGET-2024", "EUR", "FINANCE"};
        Block financeBlock = blockchain.addBlockWithKeywords(
            "Project Alpha budget allocation: 50000 EUR for Q1 2024. Investment approved.", 
            financeKeywords, 
            "FINANCE", 
            privateKey, 
            publicKey
        );
        validateBlockCreation(financeBlock, "Finance block");
        
        // Test 3: Block without manual keywords (auto only)
        Block autoBlock = blockchain.addBlockWithKeywords(
            "API integration with SQL database completed. JSON format used for data exchange. Contact: admin@company.com", 
            null, 
            "TECHNICAL", 
            privateKey, 
            publicKey
        );
        validateBlockCreation(autoBlock, "Technical block");
        
        // Test 4: Large block (off-chain) with keywords
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("Large dataset for PROJECT-BETA-2024 containing financial analysis data. ");
            largeContent.append("Budget review meeting scheduled. Contact finance@company.com for details. ");
            largeContent.append("EUR currency analysis included. ");
        }
        String[] betaKeywords = {"PROJECT-BETA", "DATASET", "FINANCE", "EUR"};
        Block offChainBlock = blockchain.addBlockWithKeywords(
            largeContent.toString(), 
            betaKeywords, 
            "FINANCE", 
            privateKey, 
            publicKey
        );
        validateBlockCreation(offChainBlock, "Off-chain block");
        
        System.out.println();
        System.out.println("✅ Created " + (blockchain.getBlockCount() - 1) + " test blocks");
        System.out.println();
        
        // Testing search functionality
        runSearchTests(blockchain);
        
        cleanup();
    }
    
    private static void runSearchTests(Blockchain blockchain) {
        System.out.println("=== 🔍 SEARCH TESTS ===");
        System.out.println();
        
        // Test 1: Fast search with manual keywords
        System.out.println("1️⃣ Fast search for 'MEDICAL':");
        List<Block> medicalResults = blockchain.searchBlocksFast("MEDICAL");
        printResults(medicalResults);
        System.out.println();
        
        // Test 2: Fast search with automatic keywords
        System.out.println("2️⃣ Fast search for 'API':");
        List<Block> apiResults = blockchain.searchBlocksFast("API");
        printResults(apiResults);
        System.out.println();
        
        // Test 3: Search by category
        System.out.println("3️⃣ Search by category 'FINANCE':");
        List<Block> financeResults = blockchain.searchByCategory("FINANCE");
        printResults(financeResults);
        System.out.println();
        
        // Test 4: Search with data included
        System.out.println("4️⃣ Search including data for 'John':");
        List<Block> johnResults = blockchain.searchBlocks("John", SearchLevel.INCLUDE_DATA);
        printResults(johnResults);
        System.out.println();
        
        // Test 5: Exhaustive search (including off-chain)
        System.out.println("5️⃣ Exhaustive search for 'PROJECT-BETA':");
        List<Block> betaResults = blockchain.searchBlocksComplete("PROJECT-BETA");
        printResults(betaResults);
        System.out.println();
        
        // Test 6: Search validation - short term
        System.out.println("6️⃣ Search validation test with short term 'hi':");
        List<Block> shortResults = blockchain.searchBlocksFast("hi");
        printResults(shortResults);
        System.out.println();
        
        // Test 7: Search validation - valid short term (exception)
        System.out.println("7️⃣ Search validation test with valid short term 'API':");
        List<Block> apiShortResults = blockchain.searchBlocksFast("API");
        printResults(apiShortResults);
        System.out.println();
        
        // Test 8: Search with numbers
        System.out.println("8️⃣ Search for year '2024':");
        List<Block> yearResults = blockchain.searchBlocksFast("2024");
        printResults(yearResults);
        System.out.println();
        
        // Test 9: Search off-chain content
        System.out.println("9️⃣ Exhaustive search for 'financial analysis' (should find off-chain content):");
        List<Block> offChainResults = blockchain.searchBlocksComplete("financial analysis");
        printResults(offChainResults);
        System.out.println();
        
        System.out.println("🎯 SEARCH DEMO COMPLETE!");
    }
    
    private static void validateBlockCreation(Block block, String blockType) {
        if (block == null) {
            throw new RuntimeException("❌ Failed to create " + blockType + " - returned null");
        }
        
        System.out.println("✅ " + blockType + " created successfully - Block #" + block.getBlockNumber());
        
        // Validate keywords are set
        if (block.getManualKeywords() != null || block.getAutoKeywords() != null) {
            System.out.println("   🏷️ Keywords assigned: " + 
                (block.getManualKeywords() != null ? "Manual✓ " : "") +
                (block.getAutoKeywords() != null ? "Auto✓" : ""));
        }
        
        // Validate category
        if (block.getContentCategory() != null) {
            System.out.println("   📂 Category: " + block.getContentCategory());
        }
        
        // Check if off-chain
        if (block.hasOffChainData()) {
            System.out.println("   💾 Off-chain data stored");
        }
        
        System.out.println();
    }
    
    private static void printResults(List<Block> results) {
        if (results.isEmpty()) {
            System.out.println("   No results found");
            return;
        }
        
        for (Block block : results) {
            System.out.println("   📄 Block #" + block.getBlockNumber() + 
                             (block.getContentCategory() != null ? " [" + block.getContentCategory() + "]" : "") +
                             (block.hasOffChainData() ? " (off-chain)" : ""));
            if (block.getManualKeywords() != null && !block.getManualKeywords().trim().isEmpty()) {
                System.out.println("      🏷️ Manual: " + block.getManualKeywords());
            }
            if (block.getAutoKeywords() != null && !block.getAutoKeywords().trim().isEmpty()) {
                String autoPreview = block.getAutoKeywords().length() > 50 ? 
                    block.getAutoKeywords().substring(0, 50) + "..." : 
                    block.getAutoKeywords();
                System.out.println("      🤖 Auto: " + autoPreview);
            }
        }
    }
    
    private static void cleanup() {
        try {
            // Clean up database files
            deleteFileIfExists("blockchain.db");
            deleteFileIfExists("blockchain.db-shm");
            deleteFileIfExists("blockchain.db-wal");
            
            // Clean up off-chain directory
            java.io.File offChainDir = new java.io.File("off-chain-data");
            if (offChainDir.exists()) {
                deleteDirectory(offChainDir);
            }
            
            // Clean up backup directory
            java.io.File backupDir = new java.io.File("off-chain-backup");
            if (backupDir.exists()) {
                deleteDirectory(backupDir);
            }
        } catch (Exception e) {
            System.err.println("Could not clean up all files: " + e.getMessage());
        }
    }
    
    private static void deleteFileIfExists(String fileName) {
        java.io.File file = new java.io.File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }
    
    private static void deleteDirectory(java.io.File directory) {
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}