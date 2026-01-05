package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.search.SearchFrameworkEngine;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.SearchResult;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.search.OffChainMatch;
import com.rbatllet.blockchain.service.OffChainStorageService;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Practical Examples for TRUE Exhaustive Search
 * 
 * This class demonstrates real-world usage scenarios for the Search Framework Engine
 * with TRUE exhaustive search capabilities across on-chain and off-chain content.
 * 
 * @version 1.0.0
 * @since 2025-07-10
 */
public class ExhaustiveSearchExamples {
    
    private static final String DEMO_PASSWORD = "ExhaustiveDemo2024!";
    private static File tempDir;
    
    public static void main(String[] args) {
        System.out.println("🔍 TRUE EXHAUSTIVE SEARCH - Practical Examples");
        System.out.println("=" + "=".repeat(55));
        System.out.println();
        
        try {
            setupTempDirectory();
            
            // Example 1: Basic On-Chain Content Search
            System.out.println("📝 EXAMPLE 1: Basic On-Chain Content Search");
            System.out.println("-".repeat(50));
            basicOnChainSearch();
            System.out.println();
            
            // Example 2: Off-Chain File Search
            System.out.println("📁 EXAMPLE 2: Off-Chain File Search");
            System.out.println("-".repeat(50));
            offChainFileSearch();
            System.out.println();
            
            // Example 3: Mixed Content Search
            System.out.println("🔄 EXAMPLE 3: Mixed Content Search");
            System.out.println("-".repeat(50));
            mixedContentSearch();
            System.out.println();
            
            // Example 4: Security and Access Control
            System.out.println("🔐 EXAMPLE 4: Security and Access Control");
            System.out.println("-".repeat(50));
            securityValidationExample();
            System.out.println();
            
            // Example 5: Performance and Caching
            System.out.println("⚡ EXAMPLE 5: Performance and Caching");
            System.out.println("-".repeat(50));
            performanceExample();
            System.out.println();
            
            // Example 6: Thread Safety
            System.out.println("🧵 EXAMPLE 6: Thread Safety");
            System.out.println("-".repeat(50));
            threadSafetyExample();
            System.out.println();
            
            cleanupTempDirectory();
            System.out.println("✅ All examples completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Example failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Force exit to stop background threads
        System.exit(0);
    }
    
    /**
     * Example 1: Basic On-Chain Content Search
     * Demonstrates searching within block.getData() content (encrypted and plain text)
     */
    public static void basicOnChainSearch() throws Exception {
        System.out.println("Creating blockchain with on-chain content...");

        // Setup
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Authorize key
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        blockchain.createBootstrapAdmin(
            publicKeyString,
            "DemoUser"
        );
        
        // Create blocks with searchable content
        blockchain.addBlockWithKeywords(
            "Public medical announcement: New treatment protocol for diabetes available",
            new String[]{"medical", "public", "treatment", "diabetes"},
            "MEDICAL",
            privateKey, publicKey
        );
        
        blockchain.addEncryptedBlockWithKeywords(
            "Confidential patient diagnosis: John Doe has diabetes type 2, requires insulin treatment",
            DEMO_PASSWORD,
            new String[]{"patient", "diagnosis", "confidential", "diabetes", "insulin"},
            "MEDICAL",
            privateKey, publicKey
        );
        
        // Index blockchain
        searchEngine.indexBlockchain(blockchain, DEMO_PASSWORD, privateKey);
        
        // Search on-chain content
        System.out.println("Searching for 'diabetes' in on-chain content...");
        SearchResult result = searchEngine.searchExhaustiveOffChain(
            "diabetes", DEMO_PASSWORD, privateKey, 10);
        
        // Display results
        if (result.isSuccessful()) {
            System.out.println("✅ Found " + result.getResultCount() + " results");
            System.out.println("⏱️ Search time: " + String.format("%.2f", result.getTotalTimeMs()) + "ms");
            System.out.println("🎯 Strategy: " + result.getStrategyUsed());
            
            for (EnhancedSearchResult searchResult : result.getResults()) {
                System.out.println("  🔗 Block: " + searchResult.getBlockHash().substring(0, 8) + "...");
                System.out.println("  📊 Source: " + searchResult.getSource());
                System.out.println("  📝 Content: " + searchResult.getSummary());
                System.out.println("  ⭐ Relevance: " + searchResult.getRelevanceScore());
                System.out.println();
            }
        } else {
            System.out.println("❌ Search failed: " + result.getErrorMessage());
        }
        
        searchEngine.shutdown();
    }
    
    /**
     * Example 2: Off-Chain File Search
     * Demonstrates searching within external files referenced by blocks
     */
    public static void offChainFileSearch() throws Exception {
        System.out.println("Creating blockchain with off-chain files...");

        // Setup
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();
        OffChainStorageService offChainService = new OffChainStorageService();
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);

        // Authorize key
        blockchain.createBootstrapAdmin(
            publicKeyString,
            "DemoUser"
        );
        
        // Create off-chain medical file
        File medicalFile = new File(tempDir, "patient_record.txt");
        try (FileWriter writer = new FileWriter(medicalFile)) {
            writer.write("CONFIDENTIAL MEDICAL RECORD\\n" +
                        "Patient: Jane Smith\\n" +
                        "Diagnosis: Hypertension and cardiac arrhythmia\\n" +
                        "Treatment: Beta-blockers and ACE inhibitors\\n" +
                        "Notes: Patient shows excellent response to medication. " +
                        "Follow-up required in 3 months for blood pressure monitoring.\\n" +
                        "Doctor: Dr. Sarah Johnson\\n" +
                        "Date: 2024-07-10");
        }
        
        // Store off-chain data
        String fileContent = readFileContent(medicalFile);
        OffChainData offChainData = offChainService.storeData(
            fileContent.getBytes(),
            DEMO_PASSWORD,
            privateKey,
            publicKeyString,
            "text/plain"
        );

        // Create block with off-chain reference using the correct method
        // Keywords include content from off-chain file for searchability
        blockchain.addBlockWithOffChainData(
            "Medical record with off-chain detailed patient information",
            offChainData,
            new String[]{"medical", "patient", "record", "offchain", "hypertension", "cardiac", "diagnosis"},
            DEMO_PASSWORD,
            privateKey,
            publicKey
        );
        
        // Index blockchain with off-chain content
        searchEngine.indexBlockchain(blockchain, DEMO_PASSWORD, privateKey);
        
        // Search for content in off-chain file
        System.out.println("Searching for 'hypertension' in off-chain files...");
        SearchResult result = searchEngine.searchExhaustiveOffChain(
            "hypertension", DEMO_PASSWORD, privateKey, 10);
        
        // Display results
        if (result.isSuccessful()) {
            System.out.println("✅ Found " + result.getResultCount() + " results");
            
            for (EnhancedSearchResult searchResult : result.getResults()) {
                System.out.println("  🔗 Block: " + searchResult.getBlockHash().substring(0, 8) + "...");
                
                if (searchResult.hasOffChainMatch()) {
                    OffChainMatch match = searchResult.getOffChainMatch();
                    System.out.println("  📁 Off-chain file: " + match.getFileName());
                    System.out.println("  📄 Content type: " + match.getContentType());
                    System.out.println("  🎯 Matches found: " + match.getMatchCount());
                    System.out.println("  👀 Preview: " + match.getPreviewSnippet().substring(0, 
                        Math.min(100, match.getPreviewSnippet().length())) + "...");
                } else {
                    System.out.println("  📝 On-chain content: " + searchResult.getSummary());
                }
                System.out.println();
            }
        } else {
            System.out.println("❌ Search failed: " + result.getErrorMessage());
        }
        
        searchEngine.shutdown();
    }
    
    /**
     * Example 3: Mixed Content Search
     * Demonstrates searching across multiple content types simultaneously
     */
    public static void mixedContentSearch() throws Exception {
        System.out.println("Creating mixed content blockchain...");

        // Setup
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();
        OffChainStorageService offChainService = new OffChainStorageService();
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);

        // Authorize key
        blockchain.createBootstrapAdmin(
            publicKeyString,
            "DemoUser"
        );
        
        // 1. Plain text on-chain block
        blockchain.addBlockWithKeywords(
            "Public financial report: Q3 revenue increased by 15% to $2.5M",
            new String[]{"financial", "public", "revenue", "quarterly"},
            "FINANCIAL",
            privateKey, publicKey
        );
        
        // 2. Encrypted on-chain block
        blockchain.addEncryptedBlockWithKeywords(
            "Private account transfer: $50000 transferred to confidential recipient account ABC-789",
            DEMO_PASSWORD,
            new String[]{"financial", "private", "transfer", "confidential"},
            "FINANCIAL",
            privateKey, publicKey
        );
        
        // 3. Off-chain JSON file with financial data
        File jsonFile = new File(tempDir, "financial_data.json");
        String jsonContent = """
            {
              "company": "TechCorp Inc",
              "quarter": "Q3 2024",
              "revenue": 2500000,
              "expenses": 1800000,
              "profit": 700000,
              "transactions": [
                {"date": "2024-07-15", "amount": 125000, "type": "sale", "category": "software"},
                {"date": "2024-08-20", "amount": 89000, "type": "expense", "category": "marketing"},
                {"date": "2024-09-10", "amount": 234000, "type": "sale", "category": "consulting"}
              ],
              "summary": "Strong financial performance this quarter with revenue growth"
            }
            """;
        
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(jsonContent);
        }
        
        OffChainData jsonOffChain = offChainService.storeData(
            jsonContent.getBytes(), DEMO_PASSWORD, privateKey, publicKeyString, "application/json");
        
        Block jsonBlock = blockchain.addEncryptedBlockWithKeywords(
            "Financial summary with detailed transaction data",
            DEMO_PASSWORD,
            new String[]{"financial", "transactions", "summary", "revenue"},
            "FINANCIAL",
            privateKey, publicKey
        );
        jsonBlock.setOffChainData(jsonOffChain);
        
        // Index everything
        searchEngine.indexBlockchain(blockchain, DEMO_PASSWORD, privateKey);
        
        // Exhaustive search across all content types
        System.out.println("Searching for 'financial' across all content types...");
        SearchResult result = searchEngine.searchExhaustiveOffChain(
            "financial", DEMO_PASSWORD, privateKey, 20);
        
        // Analyze search distribution
        if (result.isSuccessful()) {
            int onChainCount = 0, offChainCount = 0;
            int publicCount = 0, encryptedCount = 0;
            
            System.out.println("✅ Found " + result.getResultCount() + " results");
            System.out.println("⏱️ Search time: " + String.format("%.2f", result.getTotalTimeMs()) + "ms");
            System.out.println();
            
            for (EnhancedSearchResult searchResult : result.getResults()) {
                if (searchResult.hasOffChainMatch()) {
                    offChainCount++;
                    OffChainMatch match = searchResult.getOffChainMatch();
                    System.out.println("📁 OFF-CHAIN: " + match.getFileName() + " (" + match.getContentType() + ")");
                    System.out.println("   Preview: " + match.getPreviewSnippet().substring(0, 
                        Math.min(80, match.getPreviewSnippet().length())) + "...");
                } else {
                    onChainCount++;
                    if (searchResult.getSource().toString().contains("ENCRYPTED")) {
                        encryptedCount++;
                        System.out.println("🔐 ON-CHAIN (Encrypted): " + searchResult.getSummary());
                    } else {
                        publicCount++;
                        System.out.println("📝 ON-CHAIN (Public): " + searchResult.getSummary());
                    }
                }
                System.out.println("   Block: " + searchResult.getBlockHash().substring(0, 8) + "... | Score: " + 
                    searchResult.getRelevanceScore());
                System.out.println();
            }
            
            System.out.println("📊 DISTRIBUTION SUMMARY:");
            System.out.println("   On-chain matches: " + onChainCount + " (Public: " + publicCount + ", Encrypted: " + encryptedCount + ")");
            System.out.println("   Off-chain matches: " + offChainCount);
        } else {
            System.out.println("❌ Search failed: " + result.getErrorMessage());
        }
        
        searchEngine.shutdown();
    }
    
    /**
     * Example 4: Security and Access Control
     * Demonstrates password protection and access control mechanisms
     */
    public static void securityValidationExample() throws Exception {
        System.out.println("Testing security and access control...");

        // Setup
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);

        // Authorize key
        blockchain.createBootstrapAdmin(
            publicKeyString,
            "DemoUser"
        );
        
        // Create blocks with different security levels
        blockchain.addBlockWithKeywords(
            "Public information: Company announces new product launch",
            new String[]{"public", "announcement", "product"},
            "PUBLIC",
            privateKey, publicKey
        );
        
        blockchain.addEncryptedBlockWithKeywords(
            "Confidential data: Trade secrets and internal strategic plans",
            DEMO_PASSWORD,
            new String[]{"confidential", "trade", "secrets", "strategic"},
            "CONFIDENTIAL",
            privateKey, publicKey
        );
        
        // Index blockchain
        searchEngine.indexBlockchain(blockchain, DEMO_PASSWORD, privateKey);
        
        // Test 1: Search with correct password
        System.out.println("🔑 Test 1: Search with CORRECT password");
        SearchResult correctResult = searchEngine.searchExhaustiveOffChain(
            "confidential", DEMO_PASSWORD, privateKey, 10);
        
        if (correctResult.isSuccessful()) {
            System.out.println("✅ Access granted - Found " + correctResult.getResultCount() + " results");
            for (EnhancedSearchResult result : correctResult.getResults()) {
                System.out.println("   📄 " + result.getSummary());
            }
        }
        System.out.println();
        
        // Test 2: Search with wrong password
        System.out.println("🚫 Test 2: Search with WRONG password");
        SearchResult wrongResult = searchEngine.searchExhaustiveOffChain(
            "confidential", "wrong_password", privateKey, 10);
        
        if (wrongResult.isSuccessful()) {
            System.out.println("⚠️ Limited access - Found " + wrongResult.getResultCount() + " results (public only)");
            for (EnhancedSearchResult result : wrongResult.getResults()) {
                System.out.println("   📄 " + result.getSummary());
            }
        } else {
            System.out.println("❌ Access denied");
        }
        System.out.println();
        
        // Test 3: Public-only search
        System.out.println("🌐 Test 3: Public-only search");
        SearchResult publicResult = searchEngine.searchPublicOnly("product", 10);
        
        if (publicResult.isSuccessful()) {
            System.out.println("✅ Public access - Found " + publicResult.getResultCount() + " results");
            for (EnhancedSearchResult result : publicResult.getResults()) {
                System.out.println("   📄 " + result.getSummary());
            }
        }
        
        searchEngine.shutdown();
    }
    
    /**
     * Example 5: Performance and Caching
     * Demonstrates cache behavior and performance optimization
     */
    public static void performanceExample() throws Exception {
        System.out.println("Testing performance and caching...");

        // Setup with multiple blocks
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);

        // Authorize key
        blockchain.createBootstrapAdmin(
            publicKeyString,
            "DemoUser"
        );
        
        // Create multiple blocks for performance testing
        for (int i = 0; i < 5; i++) {
            blockchain.addEncryptedBlockWithKeywords(
                "Performance test block " + i + " with medical data and patient information",
                DEMO_PASSWORD,
                new String[]{"performance", "test", "medical", "patient", "block" + i},
                "TEST",
                privateKey, publicKey
            );
        }
        
        // Index blockchain
        long indexStart = System.nanoTime();
        searchEngine.indexBlockchain(blockchain, DEMO_PASSWORD, privateKey);
        long indexEnd = System.nanoTime();
        double indexTime = (indexEnd - indexStart) / 1_000_000.0;
        System.out.println("📊 Indexing time: " + String.format("%.2f", indexTime) + "ms");
        
        // First search (populate cache)
        System.out.println("🔄 First search (populating cache)...");
        long firstStart = System.nanoTime();
        SearchResult firstResult = searchEngine.searchExhaustiveOffChain(
            "medical", DEMO_PASSWORD, privateKey, 10);
        long firstEnd = System.nanoTime();
        double firstTime = (firstEnd - firstStart) / 1_000_000.0;
        System.out.println("   Time: " + String.format("%.2f", firstTime) + "ms");
        System.out.println("   Results: " + firstResult.getResultCount());
        
        // Second search (use cache)
        System.out.println("⚡ Second search (using cache)...");
        long secondStart = System.nanoTime();
        SearchResult secondResult = searchEngine.searchExhaustiveOffChain(
            "medical", DEMO_PASSWORD, privateKey, 10);
        long secondEnd = System.nanoTime();
        double secondTime = (secondEnd - secondStart) / 1_000_000.0;
        System.out.println("   Time: " + String.format("%.2f", secondTime) + "ms");
        System.out.println("   Results: " + secondResult.getResultCount());
        
        // Cache performance analysis
        double speedup = firstTime / secondTime;
        System.out.println("📈 Cache speedup: " + String.format("%.1f", speedup) + "x faster");
        
        // Cache statistics
        Map<String, Object> stats = searchEngine.getOffChainSearchStats();
        System.out.println("💾 Cache statistics:");
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            System.out.println("   " + entry.getKey() + ": " + entry.getValue());
        }
        
        // Clear cache and test
        searchEngine.clearOffChainCache();
        Map<String, Object> clearedStats = searchEngine.getOffChainSearchStats();
        System.out.println("🧹 Cache cleared - size now: " + clearedStats.get("cacheSize"));
        
        searchEngine.shutdown();
    }
    
    /**
     * Example 6: Thread Safety
     * Demonstrates concurrent search operations
     */
    public static void threadSafetyExample() throws Exception {
        System.out.println("Testing thread safety with concurrent searches...");

        // Setup
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);

        // Authorize key
        blockchain.createBootstrapAdmin(
            publicKeyString,
            "DemoUser"
        );
        
        // Create test data
        for (int i = 0; i < 3; i++) {
            blockchain.addEncryptedBlockWithKeywords(
                "Thread safety test data " + i + " with concurrent access patterns",
                DEMO_PASSWORD,
                new String[]{"thread", "safety", "concurrent", "test" + i},
                "CONCURRENCY",
                privateKey, publicKey
            );
        }
        
        // Index blockchain
        searchEngine.indexBlockchain(blockchain, DEMO_PASSWORD, privateKey);
        
        // Concurrent search test
        int threadCount = 5;
        int searchesPerThread = 3;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 25 Virtual Threads
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        System.out.println("🧵 Running " + threadCount + " threads with " + searchesPerThread + " searches each...");
        
        long concurrentStart = System.nanoTime();
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int s = 0; s < searchesPerThread; s++) {
                        SearchResult result = searchEngine.searchExhaustiveOffChain(
                            "thread", DEMO_PASSWORD, privateKey, 5);
                        
                        System.out.println("   Thread " + threadId + "-" + s + ": " + 
                            result.getResultCount() + " results (" + 
                            String.format("%.1f", result.getTotalTimeMs()) + "ms)");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Thread " + threadId + " error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for completion
        latch.await();
        executor.shutdown();
        
        long concurrentEnd = System.nanoTime();
        double totalTime = (concurrentEnd - concurrentStart) / 1_000_000.0;
        
        System.out.println("✅ All concurrent searches completed");
        System.out.println("⏱️ Total time: " + String.format("%.2f", totalTime) + "ms");
        System.out.println("📊 Average per search: " + 
            String.format("%.2f", totalTime / (threadCount * searchesPerThread)) + "ms");
        
        searchEngine.shutdown();
    }
    
    // Utility methods
    
    private static void setupTempDirectory() throws IOException {
        tempDir = new File(System.getProperty("java.io.tmpdir"), 
            "exhaustive_search_examples_" + System.currentTimeMillis());
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        System.out.println("📁 Using temp directory: " + tempDir.getAbsolutePath());
        System.out.println();
    }
    
    private static String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\\n");
            }
        }
        return content.toString();
    }
    
    private static void cleanupTempDirectory() {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tempDir.delete();
        }
        System.out.println("🧹 Cleanup completed");
    }
}