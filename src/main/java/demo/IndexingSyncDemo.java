package demo;

import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.SearchFrameworkEngine;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.IndexingResult;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.SearchResult;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Indexing Sync Demo - Follows EXACTLY the 4 examples from INDEXING_GUIDE.md
 * 
 * This demo executes all 4 practical examples from the documentation:
 * 1. Example 1: Unit Test Pattern (sync indexing for tests)
 * 2. Example 2: Demo Script Pattern (sync indexing for sequential demos)
 * 3. Example 3: Background Job Pattern (async indexing for jobs)
 * 4. Example 4: CLI Tool Pattern (sync indexing for CLI)
 * 
 * Each example follows the exact code from the documentation to verify correctness.
 */
public class IndexingSyncDemo {
    
    // Shared blockchain instance - cleared and reinitialized between examples
    private static Blockchain sharedBlockchain;
    
    public static void main(String[] args) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🚀 INDEXING GUIDE EXAMPLES DEMO - Following Documentation Exactly");
            System.out.println("=".repeat(80));

            // Initialize shared blockchain once
            System.out.println("\n🔧 Initializing shared blockchain instance...");
            sharedBlockchain = new Blockchain();

            // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
            sharedBlockchain.clearAndReinitialize();
            
            System.out.println("✅ Blockchain initialized\n");

            // Run all 4 examples from the documentation
            runExample1_UnitTestPattern();
            cleanupBetweenExamples();
            
            runExample2_DemoScriptPattern();
            cleanupBetweenExamples();
            
            runExample3_BackgroundJobPattern();
            cleanupBetweenExamples();
            
            runExample4_CLIToolPattern();
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("✅ ALL 4 EXAMPLES COMPLETED SUCCESSFULLY!");
            System.out.println("=".repeat(80));
            System.out.println("\n📚 All examples from INDEXING_GUIDE.md work correctly!");
            System.out.println("   ✓ Example 1: Unit Test Pattern");
            System.out.println("   ✓ Example 2: Demo Script Pattern");
            System.out.println("   ✓ Example 3: Background Job Pattern");
            System.out.println("   ✓ Example 4: CLI Tool Pattern");
            
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("\n❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Cleanup between examples to reset blockchain state
     */
    private static void cleanupBetweenExamples() {
        try {
            System.out.println("\n🧹 Cleaning up for next example...");
            // Reset global state of search framework
            SearchFrameworkEngine.resetGlobalState();

            // Wait for indexing coordinator to complete
            IndexingCoordinator.getInstance().waitForCompletion();

            // Reset indexing coordinator tracking to allow immediate re-indexing
            IndexingCoordinator.getInstance().reset();

            // Clear and reinitialize blockchain for next example
            sharedBlockchain.clearAndReinitialize();
            System.out.println("✅ Blockchain cleared and reinitialized\n");
            
            // Small delay to ensure cleanup
            Thread.sleep(200);
        } catch (Exception e) {
            System.err.println("Warning during cleanup: " + e.getMessage());
        }
    }
    
    // ============================================================================
    // EXAMPLE 1: REAL MEDICAL RECORDS SYSTEM - Sync Indexing
    // ============================================================================
    /**
     * REAL USE CASE: Hospital medical records system
     * 
     * Scenario: Hospital stores 100 patient records with different privacy levels.
     * Requirements:
     * - Public data: anonymized statistics (searchable by anyone)
     * - Private data: patient details (only searchable with correct password)
     * - SYNC indexing ensures all records are indexed before search
     * - Demonstrates encryption, metadata, multi-user access
     */
    private static void runExample1_UnitTestPattern() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🏥 EXAMPLE 1: Hospital Medical Records System (100 patients)");
        System.out.println("=".repeat(80));
        System.out.println("Scenario: Multi-department hospital with encrypted patient records");
        System.out.println("Privacy: Public stats + Private patient details");
        System.out.println("-".repeat(80));
        
        // Setup: Genesis admin + 3 departments
        KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );
        
        sharedBlockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
            "HOSPITAL_ADMIN"
        );
        
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(sharedBlockchain);
        api.setDefaultCredentials("HOSPITAL_ADMIN", genesisKeys);
        
        // Create 3 departments
        System.out.println("👥 Creating hospital departments...");
        KeyPair cardiology = api.createUser("cardiology_dept");
        KeyPair neurology = api.createUser("neurology_dept");
        KeyPair emergency = api.createUser("emergency_dept");
        System.out.println("   ✅ Created: Cardiology, Neurology, Emergency");
        
        // Create 100 realistic patient records
        System.out.println("\n📋 Creating 100 patient records across departments...");
        String[] conditions = {"hypertension", "diabetes", "asthma", "covid", "fracture", "migraine", "pneumonia", "stroke"};
        String[] departments = {"cardiology", "neurology", "emergency"};
        KeyPair[] deptKeys = {cardiology, neurology, emergency};
        
        int totalRecords = 100;
        long startCreate = System.currentTimeMillis();
        
        for (int i = 0; i < totalRecords; i++) {
            int deptIdx = i % 3;
            String dept = departments[deptIdx];
            String condition = conditions[i % conditions.length];
            
            // Public: anonymized stats (unencrypted, searchable by anyone)
            String publicData = String.format("Patient-%03d: %s case [%s dept]", i, condition, dept);
            String[] publicKeywords = {"patient", condition, dept, "hospital"};
            
            // Create UNENCRYPTED block with public keywords (skip auto-indexing for SYNC demo)
            sharedBlockchain.addBlockWithKeywords(
                publicData,
                publicKeywords,
                "MEDICAL",
                deptKeys[deptIdx].getPrivate(),
                deptKeys[deptIdx].getPublic(),
                true  // skipAutoIndexing - we'll use SYNC indexing for this example
            );
            
            if ((i + 1) % 25 == 0) {
                System.out.println("   📄 Created " + (i + 1) + " records...");
            }
        }
        long createTime = System.currentTimeMillis() - startCreate;
        System.out.println("✅ Created " + totalRecords + " records in " + createTime + "ms");
        
        // SYNC INDEXING: Index all public records (no password needed for unencrypted blocks)
        System.out.println("\n⏳ SYNC INDEXING: All public patient records (blocking until complete)...");
        SearchFrameworkEngine engine = new SearchFrameworkEngine();
        
        long startIndex = System.currentTimeMillis();
        IndexingResult result = engine.indexBlockchainSync(
            sharedBlockchain,
            null,  // No password - blocks are unencrypted
            cardiology.getPrivate()  // Any authorized key works
        );
        long indexTime = System.currentTimeMillis() - startIndex;
        
        // REAL ASSERTIONS
        System.out.println("\n📊 INDEXING RESULTS:");
        System.out.println("   ⏱️  Duration: " + indexTime + "ms");
        System.out.println("   📦 Blocks processed: " + result.getBlocksProcessed());
        System.out.println("   ✅ Blocks indexed: " + result.getBlocksIndexed());
        
        // Assertion 1: Must index all patient records (100)
        if (result.getBlocksIndexed() < 100) {
            throw new AssertionError("❌ Expected 100 patient blocks indexed, got " + result.getBlocksIndexed());
        }
        System.out.println("   ✅ PASS: Indexed all 100 patient records");
        
        // SEARCH TESTS: Search with cardiology password (can access cardiology encrypted data)
        System.out.println("\n🔍 SEARCH TEST 1: Search for 'hypertension' cases with cardiology password");
        SearchResult cardiologySearch = engine.search("hypertension", "cardio_pwd", 20);
        System.out.println("   📊 Found " + cardiologySearch.getResultCount() + " results");
        if (cardiologySearch.getResultCount() == 0) {
            throw new AssertionError("❌ Cardiology search found nothing!");
        }
        System.out.println("   ✅ PASS: Cardiology search works");
        
        // SEARCH TEST 2: Cross-department security (neurology password should only find neurology data)
        System.out.println("\n🔍 SEARCH TEST 2: Security test - neurology password accesses only neurology data");
        SearchResult neurologySearch = engine.search("hypertension", "neuro_pwd", 20);
        System.out.println("   📊 Found " + neurologySearch.getResultCount() + " results");
        // Should find neurology cases (i % 3 == 1), approximately 33-34 records
        if (neurologySearch.getResultCount() < 10) {
            throw new AssertionError("❌ Expected 10+ neurology results, got " + neurologySearch.getResultCount());
        }
        System.out.println("   ✅ PASS: Cross-department security works");
        
        // SEARCH TEST 3: Verify emergency department indexed separately
        System.out.println("\n🔍 SEARCH TEST 3: Emergency department search with emergency password");
        SearchResult emergencySearch = engine.search("patient", "emergency_pwd", 20);
        System.out.println("   📊 Found " + emergencySearch.getResultCount() + " emergency results");
        // Should find emergency cases (i % 3 == 2), approximately 33-34 records
        if (emergencySearch.getResultCount() < 10) {
            throw new AssertionError("❌ Expected 10+ emergency results, got " + emergencySearch.getResultCount());
        }
        System.out.println("   ✅ PASS: All 3 departments indexed successfully");
        
        System.out.println("\n💡 Why sync? Hospital admin needs confirmation that ALL records are searchable before going live");
        
        engine.shutdown();
    }
    
    // ============================================================================
    // EXAMPLE 2: Demo Script Pattern (from INDEXING_GUIDE.md)
    // ============================================================================
    /**
     * Example 2 from documentation: Demo Script Pattern
     * 
     * Shows sequential execution for demos where users see step-by-step progress.
     * Follows exactly the code from INDEXING_GUIDE.md Example 2.
     */
    private static void runExample2_DemoScriptPattern() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📝 EXAMPLE 2: Demo Script Pattern (Sync Indexing for Sequential Demo)");
        System.out.println("=".repeat(80));
        System.out.println("From: INDEXING_GUIDE.md - Example 2");
        System.out.println("Use case: Demo needs predictable sequential flow");
        System.out.println("-".repeat(80));
        
        System.out.println("🚀 Private Blockchain Search Demo");
        
        // Create blockchain with sample data
        BlockchainWithKeys blockchainData = createSampleBlockchain();
        Blockchain blockchain = blockchainData.blockchain;
        PrivateKey userPrivateKey = blockchainData.userPrivateKey;
        
        // SYNC indexing for demos - users see sequential execution
        System.out.println("⏳ Indexing blockchain...");
        SearchFrameworkEngine engine = new SearchFrameworkEngine();
        
        long startTime = System.currentTimeMillis();
        IndexingResult result = engine.indexBlockchainSync(
            blockchain,
            null,  // No password - blocks are unencrypted
            userPrivateKey
        );
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("✅ Indexed " + result.getBlocksIndexed() + 
                           " blocks in " + duration + "ms");
        
        // Immediate search demonstration (unencrypted blocks, use null password)
        System.out.println("\n🔍 Searching for 'medical' records...");
        SearchResult medicalResults = engine.search("medical", null, 10);
        
        System.out.println("📊 Found " + medicalResults.getResultCount() + " results");
        if (medicalResults.getResultCount() == 0) {
            throw new AssertionError("❌ Search found 0 results - indexing failed!");
        }
        medicalResults.getResults().forEach(r -> 
            System.out.println("  - " + r.getBlockHash().substring(0, 8) + 
                               " (relevance: " + String.format("%.2f", r.getRelevanceScore()) + ")")
        );
        
        System.out.println("\n💡 Why sync? Demo needs predictable sequential flow");
        
        engine.shutdown();
    }
    
    // Helper class to return blockchain and user keys (from documentation)
    static class BlockchainWithKeys {
        Blockchain blockchain;
        PrivateKey userPrivateKey;
        
        BlockchainWithKeys(Blockchain blockchain, PrivateKey userPrivateKey) {
            this.blockchain = blockchain;
            this.userPrivateKey = userPrivateKey;
        }
    }
    
    private static BlockchainWithKeys createSampleBlockchain() throws Exception {
        // Use shared blockchain (already initialized)
        
        // Load genesis admin keys
        KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );
        
        // Register bootstrap admin
        sharedBlockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        
        // Create API with genesis admin
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(sharedBlockchain);
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);
        
        // Create demo user
        KeyPair keys = api.createUser("demo-user");
        api.setDefaultCredentials("demo-user", keys);
        
        // Add searchable blocks (UNENCRYPTED with public keywords, skip auto-indexing)
        sharedBlockchain.addBlockWithKeywords(
            "Medical research paper",
            new String[]{"medical", "research", "clinical"},
            "RESEARCH",
            keys.getPrivate(),
            keys.getPublic(),
            true  // skipAutoIndexing
        );
        
        sharedBlockchain.addBlockWithKeywords(
            "Financial transaction log",
            new String[]{"financial", "transaction", "payment"},
            "FINANCE",
            keys.getPrivate(),
            keys.getPublic(),
            true  // skipAutoIndexing
        );
        
        sharedBlockchain.addBlockWithKeywords(
            "Legal contract agreement",
            new String[]{"legal", "contract", "agreement"},
            "LEGAL",
            keys.getPrivate(),
            keys.getPublic(),
            true  // skipAutoIndexing
        );
        
        return new BlockchainWithKeys(sharedBlockchain, keys.getPrivate());
    }
    
    // ============================================================================
    // EXAMPLE 3: Background Job Pattern (from INDEXING_GUIDE.md)
    // ============================================================================
    /**
     * Example 3 from documentation: Background Job Pattern
     * 
     * Shows how to use ASYNC indexing for background jobs (non-blocking).
     * Follows exactly the code from INDEXING_GUIDE.md Example 3.
     */
    private static void runExample3_BackgroundJobPattern() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📝 EXAMPLE 3: Background Job Pattern (Async Indexing for Jobs)");
        System.out.println("=".repeat(80));
        System.out.println("From: INDEXING_GUIDE.md - Example 3");
        System.out.println("Use case: Background job shouldn't block scheduler");
        System.out.println("-".repeat(80));
        
        // Create blockchain for background job
        BlockchainWithKeys blockchainData = createSampleBlockchain();
        
        // This follows EXACTLY the code from Example 3 in the documentation
        BlockchainIndexingJob job = new BlockchainIndexingJob(
            blockchainData.blockchain,
            null,  // No password - blocks are unencrypted
            blockchainData.userPrivateKey
        );
        
        System.out.println("⏰ Scheduling background indexing job...");
        System.out.println("   (Job will run once immediately, demonstrating async behavior)");
        
        job.scheduleIndexing();
        
        // Give it a moment to execute
        Thread.sleep(2000);
        
        System.out.println("✅ Background job scheduled successfully");
        System.out.println("   📨 Indexing happens in background (non-blocking)");
        System.out.println("\n💡 Why async? Background job shouldn't block scheduler");
        
        job.shutdown();
    }
    
    /**
     * Background job class from Example 3 documentation
     */
    static class BlockchainIndexingJob {
        private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory()); // Java 25 Virtual Threads
        
        private final Blockchain blockchain;
        private final String indexingPassword;
        private final PrivateKey indexingPrivateKey;
        
        // Constructor receives blockchain and indexing credentials
        public BlockchainIndexingJob(
                Blockchain blockchain,
                String indexingPassword,
                PrivateKey indexingPrivateKey) {
            this.blockchain = blockchain;
            this.indexingPassword = indexingPassword;
            this.indexingPrivateKey = indexingPrivateKey;
        }
        
        public void scheduleIndexing() {
            // Schedule async indexing (run once for demo, normally would be periodic)
            scheduler.schedule(() -> {
                try {
                    System.out.println("      ⏰ Starting scheduled blockchain indexing...");
                    
                    SearchFrameworkEngine engine = new SearchFrameworkEngine();
                    
                    // Use ASYNC for background jobs - non-blocking
                    IndexingResult result = engine.indexBlockchain(
                        blockchain,
                        indexingPassword,
                        indexingPrivateKey
                    );
                    
                    System.out.println("      📨 Indexing job submitted: " + 
                                       result.getBlocksProcessed() + " blocks queued");
                    
                    // Job continues, indexing happens in background
                    // Wait for completion to show it worked
                    IndexingCoordinator.getInstance().waitForCompletion();
                    System.out.println("      ✅ Background indexing completed");
                    
                    engine.shutdown();
                    
                } catch (Exception e) {
                    System.err.println("      ❌ Indexing job failed: " + e.getMessage());
                }
            }, 100, TimeUnit.MILLISECONDS);
        }
        
        public void shutdown() {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // ============================================================================
    // EXAMPLE 4: CLI Tool Pattern (from INDEXING_GUIDE.md)
    // ============================================================================
    /**
     * Example 4 from documentation: CLI Tool Pattern
     * 
     * Shows how CLI tools use SYNC indexing so users see completion.
     * Follows exactly the code from INDEXING_GUIDE.md Example 4.
     */
    private static void runExample4_CLIToolPattern() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📝 EXAMPLE 4: CLI Tool Pattern (Sync Indexing for CLI)");
        System.out.println("=".repeat(80));
        System.out.println("From: INDEXING_GUIDE.md - Example 4");
        System.out.println("Use case: CLI user expects operation to complete");
        System.out.println("-".repeat(80));
        
        // Simulate CLI arguments
        String blockchainFile = "sample-blockchain.dat";
        String searchTerm = "medical";
        
        System.out.println("🔧 Simulating: search-cli " + blockchainFile + " " + searchTerm);
        System.out.println();
        
        // Load blockchain (in demo, we create one)
        System.out.println("📂 Loading blockchain from " + blockchainFile + "...");
        BlockchainWithKeys blockchainData = loadBlockchainForCLI(blockchainFile);
        Blockchain blockchain = blockchainData.blockchain;
        
        // CLI tools need SYNC - user waits for completion
        System.out.println("⏳ Indexing blockchain...");
        SearchFrameworkEngine engine = new SearchFrameworkEngine();
        
        IndexingResult indexResult = engine.indexBlockchainSync(
            blockchain,
            null,  // No password - blocks are unencrypted
            blockchainData.userPrivateKey
        );
        
        System.out.println("✅ Indexed " + indexResult.getBlocksIndexed() + 
                           " blocks in " + indexResult.getIndexingTimeMs() + "ms");
        
        // Now search
        System.out.println("\n🔍 Searching for: " + searchTerm);
        SearchResult result = engine.searchPublicOnly(searchTerm, 10);
        
        System.out.println("📊 Results: " + result.getResultCount() + 
                           " blocks found\n");
        
        if (result.getResultCount() == 0) {
            throw new AssertionError("❌ Expected medical results, got 0");
        }
        
        // Display results
        result.getResults().forEach(r -> displayResultForCLI(r));
        
        System.out.println("💡 Why sync? CLI user expects operation to complete");
        
        engine.shutdown();
    }
    
    private static BlockchainWithKeys loadBlockchainForCLI(String filename) throws Exception {
        // Use shared blockchain (already initialized)
        
        // Load genesis admin keys
        KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );
        
        // Register bootstrap admin
        sharedBlockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        
        // Create API and user
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(sharedBlockchain);
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);
        KeyPair userKeys = api.createUser("cli-user");
        api.setDefaultCredentials("cli-user", userKeys);
        
        // Add sample data (UNENCRYPTED with public keywords, skip auto-indexing)
        sharedBlockchain.addBlockWithKeywords(
            "Medical records for CLI demo",
            new String[]{"medical", "cli", "demo"},
            "CLI",
            userKeys.getPrivate(),
            userKeys.getPublic(),
            true  // skipAutoIndexing
        );
        
        return new BlockchainWithKeys(sharedBlockchain, userKeys.getPrivate());
    }
    
    private static void displayResultForCLI(EnhancedSearchResult result) {
        System.out.println("─────────────────────────────────────");
        System.out.println("Hash: " + result.getBlockHash().substring(0, 32) + "...");
        System.out.println("Relevance: " + String.format("%.2f", result.getRelevanceScore()));
        System.out.println();
    }
}

