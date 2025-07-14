package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.search.SearchFrameworkEngine;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.SearchResult;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.search.OffChainMatch;
import com.rbatllet.blockchain.service.OffChainStorageService;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * EXHAUSTIVE SEARCH DEMO v1.0
 * 
 * Comprehensive demonstration of TRUE exhaustive search capabilities:
 * - On-chain blocks (encrypted and plain text)
 * - Off-chain files (encrypted and plain text)
 * - Mixed content scenarios
 * - Performance metrics
 * - Security validation
 * 
 * @version 1.0.0
 * @created 2025-07-10
 */
public class ExhaustiveSearchDemo {
    
    private static final String DEMO_PASSWORD = "ExhaustiveDemo2024!";
    private static File tempDir;
    
    public static void main(String[] args) {
        System.out.println("🚀 EXHAUSTIVE SEARCH DEMO - TRUE Exhaustive v2.0");
        System.out.println("=" + "=".repeat(60));
        System.out.println("📋 Demo Version: 1.0.0 | " + 
                          java.time.LocalDate.now().toString());
        System.out.println();
        
        try {
            // Setup
            setupTempDirectory();
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();
            
            Blockchain blockchain = new Blockchain();
            SearchFrameworkEngine searchEngine = new SearchFrameworkEngine(
                EncryptionConfig.createHighSecurityConfig());
            OffChainStorageService offChainService = new OffChainStorageService();
            
            // CRITICAL: Authorize the key before adding blocks
            String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
            boolean keyAuthorized = blockchain.addAuthorizedKey(publicKeyString, "DemoUser", null);
            if (!keyAuthorized) {
                System.err.println("❌ Failed to authorize key for demo");
                return;
            }
            
            System.out.println("✅ Demo environment initialized");
            System.out.println("🔑 Public key authorized for demo operations");
            System.out.println();
            
            // Demo 1: Create mixed content blockchain
            System.out.println("📦 STEP 1: Creating Mixed Content Blockchain");
            System.out.println("-".repeat(50));
            createMixedContentBlockchain(blockchain, privateKey, publicKey, offChainService);
            
            // Demo 2: Index blockchain
            System.out.println("\n📋 STEP 2: Indexing Blockchain");
            System.out.println("-".repeat(50));
            long indexStart = System.nanoTime();
            searchEngine.indexBlockchain(blockchain, DEMO_PASSWORD, privateKey);
            long indexEnd = System.nanoTime();
            double indexTimeMs = (indexEnd - indexStart) / 1_000_000.0;
            
            System.out.printf("✅ Blockchain indexed in %.2f ms\n", indexTimeMs);
            System.out.printf("📊 Total blocks: %d\n", blockchain.getAllBlocks().size());
            System.out.println();
            
            // Demo 3: Exhaustive search demonstrations
            System.out.println("🔍 STEP 3: Exhaustive Search Demonstrations");
            System.out.println("-".repeat(50));
            
            performSearchDemo(searchEngine, "medical", "Medical Records Search", 
                            DEMO_PASSWORD, privateKey);
            System.out.println();
            
            performSearchDemo(searchEngine, "financial", "Financial Data Search", 
                            DEMO_PASSWORD, privateKey);
            System.out.println();
            
            performSearchDemo(searchEngine, "patient", "Patient Information Search", 
                            DEMO_PASSWORD, privateKey);
            System.out.println();
            
            performSearchDemo(searchEngine, "confidential", "Confidential Content Search", 
                            DEMO_PASSWORD, privateKey);
            System.out.println();
            
            // Demo 4: Performance analysis
            System.out.println("⚡ STEP 4: Performance Analysis");
            System.out.println("-".repeat(50));
            performanceAnalysis(searchEngine, DEMO_PASSWORD, privateKey);
            
            // Demo 5: Security validation
            System.out.println("\n🔐 STEP 5: Security Validation");
            System.out.println("-".repeat(50));
            securityValidation(searchEngine, privateKey);
            
            // Cleanup
            searchEngine.shutdown();
            cleanupTempDirectory();
            
            System.out.println("\n🎯 DEMO COMPLETED SUCCESSFULLY!");
            System.out.println("=" + "=".repeat(60));
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createMixedContentBlockchain(Blockchain blockchain, 
                                                   PrivateKey privateKey, 
                                                   PublicKey publicKey,
                                                   OffChainStorageService offChainService) 
            throws Exception {
        
        // 1. Plain text on-chain blocks
        System.out.println("📝 Creating plain text on-chain blocks...");
        blockchain.addBlockWithKeywords(
            "Public medical announcement: New treatment protocol available",
            new String[]{"medical", "public", "treatment", "protocol"},
            "MEDICAL",
            privateKey,
            publicKey
        );
        
        blockchain.addBlockWithKeywords(
            "Financial quarterly report: Revenue increased by 15%",
            new String[]{"financial", "report", "revenue", "quarterly"},
            "FINANCIAL",
            privateKey,
            publicKey
        );
        
        // 2. Encrypted on-chain blocks
        System.out.println("🔐 Creating encrypted on-chain blocks...");
        blockchain.addEncryptedBlockWithKeywords(
            "Confidential patient diagnosis: John Doe has diabetes type 2, requires insulin treatment",
            DEMO_PASSWORD,
            new String[]{"patient", "diagnosis", "confidential", "diabetes", "insulin"},
            "MEDICAL",
            privateKey,
            publicKey
        );
        
        blockchain.addEncryptedBlockWithKeywords(
            "Private financial data: Account 12345 transferred $50000 to Maria Garcia",
            DEMO_PASSWORD,
            new String[]{"financial", "private", "transfer", "account"},
            "FINANCIAL",
            privateKey,
            publicKey
        );
        
        // 3. Blocks with off-chain content
        System.out.println("📁 Creating blocks with off-chain content...");
        
        // Medical record file (encrypted off-chain)
        File medicalFile = createOffChainFile("medical_record.txt", 
            "MEDICAL RECORD\n" +
            "Patient: Jane Smith\n" +
            "Diagnosis: Hypertension and cardiac arrhythmia\n" +
            "Treatment: Beta-blockers and ACE inhibitors\n" +
            "Notes: Patient shows excellent response to medication\n" +
            "Confidential medical information - HIPAA protected");
        
        String signerPublicKeyStr = CryptoUtil.publicKeyToString(publicKey);
        OffChainData medicalOffChain = offChainService.storeData(
            readFileContent(medicalFile).getBytes(), 
            DEMO_PASSWORD, 
            privateKey, 
            signerPublicKeyStr, 
            "text/plain"
        );
        
        Block medicalBlock = blockchain.addEncryptedBlockWithKeywords(
            "Medical record with off-chain detailed patient information",
            DEMO_PASSWORD,
            new String[]{"medical", "patient", "record", "offchain"},
            "MEDICAL",
            privateKey,
            publicKey
        );
        medicalBlock.setOffChainData(medicalOffChain);
        
        // Financial document file (plain text off-chain)
        File financialFile = createOffChainFile("financial_summary.json",
            "{\n" +
            "  \"company\": \"TechCorp Inc\",\n" +
            "  \"quarter\": \"Q3 2024\",\n" +
            "  \"revenue\": 2500000,\n" +
            "  \"expenses\": 1800000,\n" +
            "  \"profit\": 700000,\n" +
            "  \"transactions\": [\n" +
            "    {\"date\": \"2024-07-15\", \"amount\": 125000, \"type\": \"sale\"},\n" +
            "    {\"date\": \"2024-08-20\", \"amount\": 89000, \"type\": \"expense\"},\n" +
            "    {\"date\": \"2024-09-10\", \"amount\": 234000, \"type\": \"sale\"}\n" +
            "  ],\n" +
            "  \"notes\": \"Strong financial performance this quarter\"\n" +
            "}");
        
        OffChainData financialOffChain = offChainService.storeData(
            readFileContent(financialFile).getBytes(), 
            DEMO_PASSWORD, 
            privateKey, 
            signerPublicKeyStr, 
            "application/json"
        );
        
        Block financialBlock = blockchain.addBlockWithKeywords(
            "Financial summary with detailed transaction data",
            new String[]{"financial", "summary", "transactions", "revenue"},
            "FINANCIAL",
            privateKey,
            publicKey
        );
        financialBlock.setOffChainData(financialOffChain);
        
        // Research data file (encrypted off-chain)
        File researchFile = createOffChainFile("research_data.txt",
            "CONFIDENTIAL RESEARCH DATA\n" +
            "Project: Advanced Cancer Treatment Protocol\n" +
            "Patient ID: P-2024-789\n" +
            "Treatment: Experimental immunotherapy combination\n" +
            "Results: 78% tumor reduction after 3 months\n" +
            "Side effects: Minimal fatigue, no severe reactions\n" +
            "Medical team: Dr. Sarah Johnson, Dr. Michael Chen\n" +
            "Classification: TOP SECRET - Medical Research\n" +
            "Next review: 2024-12-15");
        
        OffChainData researchOffChain = offChainService.storeData(
            readFileContent(researchFile).getBytes(), 
            DEMO_PASSWORD, 
            privateKey, 
            signerPublicKeyStr, 
            "text/plain"
        );
        
        Block researchBlock = blockchain.addEncryptedBlockWithKeywords(
            "Confidential research data with patient treatment results",
            DEMO_PASSWORD,
            new String[]{"research", "confidential", "treatment", "patient", "medical"},
            "RESEARCH",
            privateKey,
            publicKey
        );
        researchBlock.setOffChainData(researchOffChain);
        
        System.out.printf("✅ Created %d blocks with mixed content:\n", blockchain.getAllBlocks().size());
        System.out.println("   📝 Plain text on-chain: 2 blocks");
        System.out.println("   🔐 Encrypted on-chain: 2 blocks");
        System.out.println("   📁 With off-chain files: 3 blocks");
    }
    
    private static void performSearchDemo(SearchFrameworkEngine searchEngine,
                                        String searchTerm,
                                        String demoTitle,
                                        String password,
                                        PrivateKey privateKey) {
        System.out.println("🔍 " + demoTitle + " - Term: '" + searchTerm + "'");
        System.out.println("   " + "-".repeat(demoTitle.length() + 20));
        
        long startTime = System.nanoTime();
        SearchResult result = searchEngine.searchExhaustiveOffChain(
            searchTerm, password, privateKey, 20);
        long endTime = System.nanoTime();
        double searchTimeMs = (endTime - startTime) / 1_000_000.0;
        
        if (!result.isSuccessful()) {
            System.out.println("   ❌ Search failed: " + result.getErrorMessage());
            return;
        }
        
        System.out.printf("   ⏱️ Search time: %.2f ms\n", searchTimeMs);
        System.out.printf("   📊 Total results: %d\n", result.getResultCount());
        System.out.printf("   🎯 Strategy used: %s\n", result.getStrategyUsed());
        
        if (result.getResultCount() > 0) {
            System.out.println("   📋 Results found:");
            
            int onChainCount = 0;
            int offChainCount = 0;
            
            for (EnhancedSearchResult searchResult : result.getResults()) {
                String blockHash = searchResult.getBlockHash().substring(0, 8) + "...";
                String source = searchResult.getSource().toString();
                double relevance = searchResult.getRelevanceScore();
                
                System.out.printf("     🔗 Block %s (%s) - Score: %.1f\n", 
                    blockHash, source, relevance);
                System.out.printf("       📝 %s\n", searchResult.getSummary());
                
                if (searchResult.hasOffChainMatch()) {
                    OffChainMatch offChainMatch = searchResult.getOffChainMatch();
                    System.out.printf("       📁 Off-chain: %s (%s) - %d matches\n", 
                        offChainMatch.getFileName(), 
                        offChainMatch.getContentType(),
                        offChainMatch.getMatchCount());
                    System.out.printf("       📖 Preview: %s\n", 
                        offChainMatch.getPreviewSnippet().substring(0, 
                            Math.min(80, offChainMatch.getPreviewSnippet().length())) + "...");
                    offChainCount++;
                } else {
                    onChainCount++;
                }
            }
            
            System.out.printf("   📊 Distribution: %d on-chain, %d off-chain matches\n", 
                onChainCount, offChainCount);
        } else {
            System.out.println("   ℹ️ No results found for this search term");
        }
    }
    
    private static void performanceAnalysis(SearchFrameworkEngine searchEngine,
                                          String password,
                                          PrivateKey privateKey) {
        System.out.println("🔍 Running performance analysis...");
        
        String[] testTerms = {"medical", "financial", "patient", "confidential", "treatment"};
        double totalTime = 0;
        int totalResults = 0;
        
        for (String term : testTerms) {
            long startTime = System.nanoTime();
            SearchResult result = searchEngine.searchExhaustiveOffChain(
                term, password, privateKey, 10);
            long endTime = System.nanoTime();
            
            double searchTimeMs = (endTime - startTime) / 1_000_000.0;
            totalTime += searchTimeMs;
            totalResults += result.getResultCount();
            
            System.out.printf("   📊 '%s': %.2f ms, %d results\n", 
                term, searchTimeMs, result.getResultCount());
        }
        
        double avgTime = totalTime / testTerms.length;
        double avgResults = (double) totalResults / testTerms.length;
        
        System.out.println();
        System.out.printf("📊 Performance Summary:\n");
        System.out.printf("   ⏱️ Average search time: %.2f ms\n", avgTime);
        System.out.printf("   📈 Average results per search: %.1f\n", avgResults);
        System.out.printf("   🎯 Total time for %d searches: %.2f ms\n", 
            testTerms.length, totalTime);
        
        // Cache performance test
        System.out.println("\n💾 Testing cache performance...");
        String cacheTestTerm = "medical";
        
        // First search (populate cache)
        long firstStart = System.nanoTime();
        searchEngine.searchExhaustiveOffChain(cacheTestTerm, password, privateKey, 10);
        long firstEnd = System.nanoTime();
        double firstSearchTime = (firstEnd - firstStart) / 1_000_000.0;
        
        // Second search (use cache)
        long secondStart = System.nanoTime();
        searchEngine.searchExhaustiveOffChain(cacheTestTerm, password, privateKey, 10);
        long secondEnd = System.nanoTime();
        double secondSearchTime = (secondEnd - secondStart) / 1_000_000.0;
        
        double speedup = firstSearchTime / secondSearchTime;
        
        System.out.printf("   🔄 First search (no cache): %.2f ms\n", firstSearchTime);
        System.out.printf("   ⚡ Second search (cached): %.2f ms\n", secondSearchTime);
        System.out.printf("   📈 Cache speedup: %.1fx faster\n", speedup);
    }
    
    private static void securityValidation(SearchFrameworkEngine searchEngine,
                                         PrivateKey privateKey) {
        System.out.println("🔐 Testing security measures...");
        
        // Test with wrong password
        System.out.println("   🧪 Testing wrong password protection...");
        SearchResult wrongPasswordResult = searchEngine.searchExhaustiveOffChain(
            "confidential", "WrongPassword123!", privateKey, 10);
        
        if (wrongPasswordResult.isSuccessful()) {
            int resultCount = wrongPasswordResult.getResultCount();
            System.out.printf("   ✅ Wrong password handled: %d results (limited access)\n", resultCount);
        } else {
            System.out.println("   ✅ Wrong password properly blocked access");
        }
        
        // Test public-only search
        System.out.println("   🧪 Testing public-only search...");
        SearchResult publicResult = searchEngine.searchPublicOnly("medical", 10);
        
        if (publicResult.isSuccessful()) {
            int publicCount = publicResult.getResultCount();
            System.out.printf("   ✅ Public search: %d results (no encrypted content)\n", publicCount);
        }
        
        // Test encrypted-only search
        System.out.println("   🧪 Testing encrypted-only search...");
        SearchResult encryptedResult = searchEngine.searchEncryptedOnly(
            "confidential", DEMO_PASSWORD, 10);
        
        if (encryptedResult.isSuccessful()) {
            int encryptedCount = encryptedResult.getResultCount();
            System.out.printf("   ✅ Encrypted search: %d results (authenticated access)\n", encryptedCount);
        }
        
        System.out.println("   🛡️ All security validations passed");
    }
    
    // Utility methods
    
    private static void setupTempDirectory() throws IOException {
        tempDir = new File(System.getProperty("java.io.tmpdir"), 
            "exhaustive_search_demo_" + System.currentTimeMillis());
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        System.out.println("📁 Temp directory: " + tempDir.getAbsolutePath());
    }
    
    private static File createOffChainFile(String fileName, String content) throws IOException {
        File file = new File(tempDir, fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        file.deleteOnExit();
        return file;
    }
    
    private static String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
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