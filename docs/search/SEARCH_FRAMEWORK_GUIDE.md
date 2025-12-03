# Search Framework Engine Guide

Comprehensive guide to the Private Blockchain's Search Framework Engine with two-layer metadata architecture, intelligent strategy routing, and privacy-preserving search capabilities.

## 📋 Table of Contents

- [Overview](#-overview)
- [Search Architecture](#-search-architecture)
- [Search Strategies](#-search-strategies)
- [Basic Usage](#-basic-usage)
- [Blockchain Indexing](#-blockchain-indexing)
  - [Asynchronous Indexing (Default)](#asynchronous-indexing-default)
  - [Synchronous Indexing (Blocking)](#synchronous-indexing-blocking)
  - [Practical Examples](#practical-examples)
  - [Privacy-by-Design: Keyword Requirement](#privacy-by-design-keyword-requirement)
  - [Comparison: Async vs Sync](#comparison-async-vs-sync)
- [Advanced Search Features](#-advanced-search-features)
- [Granular Term Visibility Control](#-granular-term-visibility-control)
- [Specialized Domain Searches](#-specialized-domain-searches)
- [Performance Metrics](#-performance-metrics)
- [Security and Privacy](#-security-and-privacy)
- [Complete Examples](#-complete-examples)
- [Best Practices](#-best-practices)

## 🚀 Overview

The Private Blockchain features a **Search Framework Engine** that represents a breakthrough in blockchain search technology. This system combines:

- **Two-Layer Metadata Architecture**: Public and Private layers with privacy protection
- **Intelligent Strategy Routing**: Automatic selection of optimal search strategy
- **Privacy-Preserving Search**: Encrypted content search with cryptographic protection
- **Enterprise Performance**: Sub-50ms search times for public metadata
- **Universal Security**: AES-256-GCM encryption with cryptographic privacy protection

## 🏗️ Search Architecture

### Two-Layer Metadata System

#### 1. **Public Layer** 🌍
- **Always Searchable**: No authentication required
- **Content**: General keywords, categories, approximate timestamps
- **Privacy**: No sensitive information exposed
- **Performance**: Lightning-fast (<50ms)

#### 2. **Private Layer** 🔐
- **Password Protected**: Requires authentication
- **Content**: Exact timestamps, detailed metadata, owner information
- **Privacy**: AES-256-GCM encrypted storage
- **Performance**: Moderate (100-500ms)

## ⚡ Search Strategies

The Search Framework Engine automatically selects the optimal strategy:

### 1. Fast Public Search
- **Target**: <50ms response time
- **Scope**: Public metadata only
- **Use Case**: Quick discovery, general searches

### 2. Encrypted Private Search  
- **Target**: 100-500ms response time
- **Scope**: Private metadata with decryption
- **Use Case**: Authenticated searches, sensitive data

### 3. Deep Content Search
- **Target**: 500ms-2s response time  
- **Scope**: Full block content analysis
- **Use Case**: Forensic analysis, detailed searches

## 🔍 Basic Usage

### ⚠️ Phase 5.4 Initialization Pattern (IMPORTANT!)

**CRITICAL:** With Phase 5.4 (Async/Background Indexing), you MUST follow this initialization order:

```java
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.api.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;

// Step 1: Create blockchain
Blockchain blockchain = new Blockchain();

// Step 2: Load genesis admin keys (generated via ./tools/generate_genesis_keys.zsh)
KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// Step 3: Register bootstrap admin (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// Step 4: Create API with genesis admin credentials
UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain);
dataAPI.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

// Step 5: Create user (only authorized users can create new users)
KeyPair userKeys = dataAPI.createUser("search-user");
dataAPI.setDefaultCredentials("search-user", userKeys);

// Step 2: Store searchable data (triggers async indexing in background)
String password = "SearchPassword123!";
String[] keywords = {"medical", "patient", "diagnosis"};
dataAPI.storeSearchableData("Medical record data", password, keywords);

// Step 3: ⚡ CRITICAL - Wait for async indexing to complete
System.out.println("⏳ Waiting for async indexing...");
IndexingCoordinator.getInstance().waitForCompletion();
System.out.println("✅ Async indexing completed");

// Step 4: NOW initialize SearchSpecialistAPI (blockchain has indexed blocks)
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
    blockchain, password, userKeys.getPrivate());

// Step 5: Search works correctly
List<EnhancedSearchResult> results = searchAPI.searchAll("medical");
System.out.println("Found " + results.size() + " results");
```

**Key Points (Phase 5.4):**
- ✅ **Create blocks FIRST** with `storeSearchableData()`
- ✅ **Wait for indexing** using `IndexingCoordinator.getInstance().waitForCompletion()`
- ✅ **Initialize SearchSpecialistAPI AFTER** blocks are indexed
- ❌ **DO NOT** initialize SearchSpecialistAPI on empty blockchain (throws IllegalStateException)
- ❌ **DO NOT** skip `waitForCompletion()` - you'll get 0 search results

**Common Mistake:**
```java
// ❌ WRONG - Empty blockchain
Blockchain blockchain = new Blockchain();  // Only genesis block
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, pass, key);
// Throws: IllegalStateException: "Blockchain is empty (0 blocks)"
```

**See Also:**
- [SearchSpecialistAPI Initialization Guide](SEARCHSPECIALISTAPI_INITIALIZATION_GUIDE.md)
- [SearchSpecialistAPI Initialization Order Issue](../reports/SEARCHSPECIALISTAPI_INITIALIZATION_ORDER_ISSUE.md)

### Quick Start with SearchSpecialistAPI

Once properly initialized (see Phase 5.4 pattern above), use these search methods:

```java
// Fast public-only search (sub-50ms)
List<EnhancedSearchResult> publicResults = searchAPI.searchPublic("medical records");

// Hybrid search (public + private with default password)
List<EnhancedSearchResult> hybridResults = searchAPI.searchAll("patient data");

// Encrypted-only search with explicit password
List<EnhancedSearchResult> privateResults = searchAPI.searchSecure(
    "patient diagnosis", "myPassword123", 50);

// Smart search with automatic strategy selection
List<EnhancedSearchResult> smartResults = searchAPI.searchIntelligent(
    "blockchain transaction", "password123", 100);
```

### Direct Engine Usage

For lower-level control, you can use `SearchFrameworkEngine` directly:

```java
import com.rbatllet.blockchain.search.SearchFrameworkEngine;
import com.rbatllet.blockchain.search.SearchResult;
import com.rbatllet.blockchain.config.EncryptionConfig;
import java.util.Map;
import java.util.HashMap;

// Create search engine with configuration
EncryptionConfig config = EncryptionConfig.createHighSecurityConfig();
SearchFrameworkEngine searchEngine = new SearchFrameworkEngine(config);

// Index blockchain (requires user's private key)
searchEngine.indexBlockchainSync(blockchain, password, userKeys.getPrivate());

// Perform public-only search
SearchResult result = searchEngine.searchPublicOnly("medical", 20);

// Perform authenticated search with password
SearchResult authResult = searchEngine.search("patient", password, userKeys.getPrivate());
```

## 📇 Blockchain Indexing

The Search Framework Engine provides two methods for indexing blockchain data for search:

### Asynchronous Indexing (Default)

**Use when:** Background jobs, scheduled tasks, UI operations - any scenario where you don't need to wait for completion.

```java
SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();

// Trigger async indexing - returns immediately
IndexingResult result = searchEngine.indexBlockchain(
    blockchain,
    password,
    privateKey
);

// Indexing happens in background
// Result contains initial counts (may not reflect final state)
System.out.println("Indexing started: " + result.getTotalProcessed() + " blocks queued");

// Optional: Wait for completion manually if needed later
IndexingCoordinator.getInstance().waitForCompletion();
System.out.println("✅ Background indexing completed");
```

**Characteristics:**
- ✅ **Non-blocking**: Returns immediately, doesn't block caller thread
- ✅ **Parallel processing**: Uses background thread pool for efficiency
- ✅ **Fire-and-forget**: Ideal for scheduled tasks and background jobs
- ⚠️ **Delayed availability**: Search index not immediately available
- ⚠️ **Approximate counts**: Initial result may not reflect final state

### Synchronous Indexing (Blocking)

**Use when:** Unit tests, demo scripts, CLI tools, integration tests - any scenario where you need immediate completion confirmation.

```java
SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();

// Trigger sync indexing - BLOCKS until complete
IndexingResult result = searchEngine.indexBlockchainSync(
    blockchain,
    password,
    privateKey
);

// Returns ONLY after all blocks are indexed
// Result contains accurate final counts
System.out.println("✅ Indexing completed: " + result.getTotalIndexed() + " blocks indexed");
System.out.println("⏱️ Duration: " + result.getDurationMs() + "ms");

// Safe to search immediately - all blocks are indexed
SearchResult searchResult = searchEngine.search("medical", password, privateKey);
```

**Characteristics:**
- ✅ **Blocking**: Waits for completion before returning
- ✅ **Accurate counts**: Returns precise final indexed block count
- ✅ **Immediate availability**: Search index ready for queries after return
- ✅ **Thread-safe**: Multiple threads can call simultaneously
- ⚠️ **Blocks caller**: Thread waits for indexing completion
- ⚠️ **InterruptedException**: Wrapped in RuntimeException if interrupted

### Practical Examples

#### Example 1: Unit Test Pattern

```java
@Test
public void testSearchAfterIndexing() throws Exception {
    // Setup blockchain with bootstrap admin
    Blockchain blockchain = new Blockchain();
    
    // Load genesis admin keys
    KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
        "./keys/genesis-admin.private",
        "./keys/genesis-admin.public"
    );
    
    // Register bootstrap admin (REQUIRED!)
    blockchain.createBootstrapAdmin(
        CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
        "BOOTSTRAP_ADMIN"
    );
    
    // Create API with genesis admin
    UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
    api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);
    
    // Create test user
    KeyPair userKeys = api.createUser("testuser");
    api.setDefaultCredentials("testuser", userKeys);
    
    String[] keywords = {"medical", "patient", "diagnosis"};
    
    // Add searchable block (skip auto-indexing for manual SYNC indexing)
    blockchain.addBlockWithKeywords(
        "Patient diagnosis record",
        keywords,
        "MEDICAL",
        userKeys.getPrivate(),
        userKeys.getPublic(),
        true  // skipAutoIndexing - we'll use SYNC indexing
    );
    
    // Use SYNC indexing for tests - ensure completion before assertions
    SearchFrameworkEngine engine = new SearchFrameworkEngine();
    IndexingResult indexResult = engine.indexBlockchainSync(
        blockchain,
        null,  // No password - block is unencrypted
        userKeys.getPrivate()
    );
    
    // Now safe to assert - indexing is complete
    assertEquals(2, indexResult.getTotalIndexed()); // Genesis + test block
    
    // Search works immediately
    SearchResult searchResult = engine.search("medical", null, userKeys.getPrivate());
    assertEquals(1, searchResult.getResults().size());
}
```

#### Example 2: Demo Script Pattern

```java
public static void main(String[] args) throws Exception {
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
    
    System.out.println("✅ Indexed " + result.getTotalIndexed() + 
                       " blocks in " + duration + "ms");
    
    // Immediate search demonstration
    System.out.println("\n🔍 Searching for 'medical' records...");
    SearchResult medicalResults = engine.search("medical", null, 10);
    
    System.out.println("📊 Found " + medicalResults.getResultCount() + " results");
    medicalResults.getResults().forEach(r -> 
        System.out.println("  - " + r.getBlockHash().substring(0, 8) + 
                           " (relevance: " + String.format("%.2f", r.getRelevanceScore()) + ")")
    );
}

// Helper class to return blockchain and user keys
static class BlockchainWithKeys {
    Blockchain blockchain;
    PrivateKey userPrivateKey;
    
    BlockchainWithKeys(Blockchain blockchain, PrivateKey userPrivateKey) {
        this.blockchain = blockchain;
        this.userPrivateKey = userPrivateKey;
    }
}

private static BlockchainWithKeys createSampleBlockchain() throws Exception {
    Blockchain blockchain = new Blockchain();
    
    // Load genesis admin keys
    KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
        "./keys/genesis-admin.private",
        "./keys/genesis-admin.public"
    );
    
    // Register bootstrap admin
    blockchain.createBootstrapAdmin(
        CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
        "BOOTSTRAP_ADMIN"
    );
    
    // Create API with genesis admin
    UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
    api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);
    
    // Create demo user
    KeyPair keys = api.createUser("demo-user");
    api.setDefaultCredentials("demo-user", keys);
    
    // Add searchable blocks (UNENCRYPTED with public keywords, skip auto-indexing)
    blockchain.addBlockWithKeywords(
        "Medical research paper",
        new String[]{"medical", "research", "clinical"},
        "RESEARCH",
        keys.getPrivate(),
        keys.getPublic(),
        true  // skipAutoIndexing
    );
    
    blockchain.addBlockWithKeywords(
        "Financial transaction log",
        new String[]{"financial", "transaction", "payment"},
        "FINANCE",
        keys.getPrivate(),
        keys.getPublic(),
        true  // skipAutoIndexing
    );
    
    blockchain.addBlockWithKeywords(
        "Legal contract agreement",
        new String[]{"legal", "contract", "agreement"},
        "LEGAL",
        keys.getPrivate(),
        keys.getPublic(),
        true  // skipAutoIndexing
    );
    
    return new BlockchainWithKeys(blockchain, keys.getPrivate());
}
```

#### Example 3: Background Job Pattern

```java
public class BlockchainIndexingJob {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public void scheduleIndexing() {
        // Schedule async indexing every hour
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("⏰ Starting scheduled blockchain indexing...");
                
                SearchFrameworkEngine engine = new SearchFrameworkEngine();
                
                // Use ASYNC for background jobs - non-blocking
                IndexingResult result = engine.indexBlockchain(
                    blockchain,
                    indexingPassword,
                    indexingPrivateKey
                );
                
                System.out.println("📨 Indexing job submitted: " + 
                                   result.getTotalProcessed() + " blocks queued");
                
                // Job continues, indexing happens in background
                // No need to wait - next job will pick up new blocks
                
            } catch (Exception e) {
                System.err.println("❌ Indexing job failed: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.HOURS);
    }
}
```

#### Example 4: CLI Tool Pattern

```java
public class SearchCLI {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: search-cli <blockchain-file> <search-term>");
            System.exit(1);
        }
        
        String blockchainFile = args[0];
        String searchTerm = args[1];
        
        // Load blockchain from file
        Blockchain blockchain = loadBlockchain(blockchainFile);
        
        // CLI tools need SYNC - user waits for completion
        System.out.println("⏳ Indexing blockchain from " + blockchainFile + "...");
        SearchFrameworkEngine engine = new SearchFrameworkEngine();
        
        IndexingResult indexResult = engine.indexBlockchainSync(
            blockchain,
            getPassword(),
            getPrivateKey()
        );
        
        System.out.println("✅ Indexed " + indexResult.getTotalIndexed() + 
                           " blocks in " + indexResult.getDurationMs() + "ms");
        
        // Now search
        System.out.println("\n🔍 Searching for: " + searchTerm);
        SearchResult result = engine.search(searchTerm, getPassword(), getPrivateKey());
        
        System.out.println("📊 Results: " + result.getResults().size() + " blocks found");
        result.getResults().forEach(r -> displayResult(r));
    }
}
```

### Privacy-by-Design: Keyword Requirement

**IMPORTANT:** The Search Framework Engine follows a **privacy-by-design** model:

- ✅ **Blocks WITH keywords** → Indexed and searchable
- ❌ **Blocks WITHOUT keywords** → NOT indexed (remains private)

This applies to both encrypted and unencrypted blocks. If you want a block to be searchable, you MUST provide explicit keywords:

```java
// ✅ SEARCHABLE - Block will be indexed
String[] keywords = {"medical", "patient", "diagnosis"};
blockchain.addBlockWithKeywords(
    "Patient medical record",
    keywords,
    "MEDICAL",  // Category
    privateKey,
    publicKey
);

// ❌ NOT SEARCHABLE - Block remains private (no keywords provided)
blockchain.addBlock(
    "Confidential internal memo",
    privateKey,
    publicKey
);
```

**Rationale:** In a **private blockchain**, the default should be privacy, not convenience. Users must explicitly opt-in to searchability by providing keywords. This prevents accidental exposure of sensitive data and aligns with privacy-first principles.

### Comparison: Async vs Sync

| Feature | `indexBlockchain()` (Async) | `indexBlockchainSync()` (Sync) |
|---------|----------------------------|--------------------------------|
| **Blocking** | ❌ Returns immediately | ✅ Blocks until complete |
| **Return Counts** | ⚠️ Initial/approximate | ✅ Accurate final counts |
| **Search Availability** | ⏳ Delayed (background) | ✅ Immediate after return |
| **Use Cases** | Background jobs, scheduled tasks | Tests, demos, CLI tools |
| **Thread Pool** | ✅ Uses background threads | ✅ Uses background + waits |
| **Interruption** | ➖ N/A | ⚠️ Throws RuntimeException |
| **Performance** | 🚀 Non-blocking caller | ⏸️ Caller waits |

**Choose async when:**
- Background indexing jobs
- Scheduled maintenance tasks
- UI operations (avoid blocking interface)
- Fire-and-forget scenarios

**Choose sync when:**
- Unit/integration tests
- Demo scripts needing sequential execution
- CLI tools where user expects completion
- Scenarios requiring immediate search after indexing

## 🎯 Advanced Search Features

### 1. Multi-Term Complex Searches

```java
// Search with multiple terms and filters
String[] terms = {"medical", "2024", "cardiology"};
List<EnhancedSearchResult> results = searchAPI.searchComplex(terms, filters);

// Boolean search operations
String query = "medical AND (cardiology OR neurology) NOT pediatric";
List<EnhancedSearchResult> booleanResults = searchAPI.searchBoolean(query);
```

### 2. Time-Range Searches

```java
// Search within specific time range
LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
LocalDateTime endDate = LocalDateTime.now();

List<EnhancedSearchResult> timeResults = searchAPI.searchByTimeRange(
    "transactions", startDate, endDate, 100);
```

### 3. Category-Based Searches

```java
// Search within specific categories
Set<String> categories = Set.of("MEDICAL", "CONFIDENTIAL");
List<EnhancedSearchResult> categoryResults = searchAPI.searchByCategory(
    "patient data", categories, 50);
```

### 4. Owner-Based Searches

```java
// Search blocks by specific owner
String ownerPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj...";
List<EnhancedSearchResult> ownerResults = searchAPI.searchByOwner(
    ownerPublicKey, "contract", 100);
```

## 🔐 Granular Term Visibility Control

### Setting Term Visibility

```java
// Create term visibility map
TermVisibilityMap visibilityMap = new TermVisibilityMap();

// Public terms (visible without authentication)
visibilityMap.addPublicTerm("medical");
visibilityMap.addPublicTerm("research");
visibilityMap.addPublicTerm("2024");

// Private terms (require authentication)
visibilityMap.addPrivateTerm("patient-id");
visibilityMap.addPrivateTerm("diagnosis-code");
visibilityMap.addPrivateTerm("treatment-plan");

// Store block with visibility control
Block block = blockchain.addBlockWithVisibility(
    data, privateKey, publicKey, visibilityMap);
```

### Searching with Visibility Awareness

```java
// Public-only search returns only public content
List<EnhancedSearchResult> publicResults = searchAPI.searchPublic("medical");
// Will find only public blocks - private content completely ignored

// Hybrid search uses default credentials to access both public and private content  
List<EnhancedSearchResult> hybridResults = searchAPI.searchAll("patient");
// Will find both public and private content using default password

// Encrypted-only search returns only private/encrypted content
List<EnhancedSearchResult> authResults = searchAPI.searchSecure(
    "patient-id", "password123", 50);
// Will find only encrypted blocks with the provided password
```

## 🏥 Specialized Domain Searches

### Content Type Filtered Search

The Search Framework Engine provides specialized methods to filter results by content type:

```java
// Search for medical-related blocks only
SearchResult medicalResults = searchEngine.searchByContentType(
    "patient diagnosis",  // Search query
    "medical",            // Content type filter
    20                    // Maximum results
);

// Search for financial transactions only
SearchResult financialResults = searchEngine.searchByContentType(
    "transaction payment",
    "financial",
    50
);

// Search for legal documents only
SearchResult legalResults = searchEngine.searchByContentType(
    "contract agreement",
    "legal",
    30
);

// Process results
for (EnhancedSearchResult result : medicalResults.getResults()) {
    System.out.println("Block: " + result.getBlockHash());
    System.out.println("Relevance: " + result.getRelevanceScore());
    System.out.println("Context: " + result.getSummary());
}
```

**Key Features:**
- ✅ Fast public metadata filtering
- ✅ Automatic content type classification
- ✅ Sub-100ms performance for most queries
- ✅ Supports custom content types
- ✅ No authentication required

### Time Range Filtered Search

Filter search results by time range for temporal analysis:

```java
// Search within current month
String currentMonth = java.time.YearMonth.now().toString(); // e.g., "2025-09"
SearchResult thisMonthResults = searchEngine.searchByTimeRange(
    "medical record",     // Search query
    currentMonth,         // Time range (YYYY-MM format)
    50                    // Maximum results
);

// Search within specific month
SearchResult januaryResults = searchEngine.searchByTimeRange(
    "financial audit",
    "2025-01",           // January 2025
    100
);

// Search within quarter (if indexed)
SearchResult q1Results = searchEngine.searchByTimeRange(
    "quarterly report",
    "2025-Q1",           // Q1 2025
    75
);

// Analyze temporal distribution
System.out.println("📅 Results for " + currentMonth + ":");
System.out.println("  Total results: " + thisMonthResults.getResultCount());
System.out.println("  Search time: " + thisMonthResults.getTotalTimeMs() + "ms");

for (EnhancedSearchResult result : thisMonthResults.getResults()) {
    PublicMetadata metadata = result.getPublicMetadata();
    System.out.println("  - " + result.getBlockHash() +
                       " | Keywords: " + metadata.getGeneralKeywords());
}
```

**Key Features:**
- ✅ Temporal filtering by month/quarter/year
- ✅ Fast time-indexed search
- ✅ Useful for audit trails and compliance
- ✅ Supports custom time range formats
- ✅ No authentication required

**Supported Time Range Formats:**
- `YYYY-MM` - Specific month (e.g., "2025-09")
- `YYYY-QN` - Specific quarter (e.g., "2025-Q1", "2025-Q2")
- `YYYY` - Entire year (e.g., "2025")
- Custom formats supported by your indexing strategy

### Medical Records Search

```java
public class MedicalSearchExample {
    
    public List<EnhancedSearchResult> searchPatientRecords(
            String patientId, String condition) {
        
        // Build search criteria
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("category", "MEDICAL");
        criteria.put("mustContain", Arrays.asList(patientId, condition));
        criteria.put("timeRange", "last-6-months");
        
        // Perform authenticated medical search
        return searchAPI.searchMedical(criteria, medicalPassword, 100);
    }
    
    public List<EnhancedSearchResult> searchByDiagnosis(String diagnosisCode) {
        // Use ICD-10 code search
        return searchAPI.searchByCode("ICD10", diagnosisCode, 50);
    }
}
```

### Financial Transaction Search

```java
public class FinancialSearchExample {
    
    public List<EnhancedSearchResult> searchTransactions(
            String accountId, BigDecimal minAmount) {
        
        // Build financial search criteria
        FinancialSearchCriteria criteria = FinancialSearchCriteria.builder()
            .accountId(accountId)
            .minAmount(minAmount)
            .transactionType("TRANSFER")
            .dateRange(DateRange.lastMonth())
            .build();
            
        return searchAPI.searchFinancial(criteria, financePassword);
    }
}
```

### Legal Document Search

```java
public class LegalSearchExample {
    
    public List<EnhancedSearchResult> searchContracts(
            String partyName, String contractType) {
        
        // Search with legal metadata
        LegalSearchFilter filter = new LegalSearchFilter();
        filter.addParty(partyName);
        filter.setDocumentType(contractType);
        filter.setJurisdiction("US");
        filter.setDateRange(DateRange.lastYear());
        
        return searchAPI.searchLegal(filter, legalPassword);
    }
}
```

## 📊 Performance Metrics

### Search Performance by Strategy

| Strategy | Target Time | Actual (Avg) | Use Case |
|----------|------------|--------------|----------|
| Fast Public | <50ms | 35ms | Quick discovery |
| Encrypted Private | 100-500ms | 250ms | Authenticated search |
| Deep Content | 500ms-2s | 1.2s | Detailed analysis |
| Exhaustive | 2s-10s | 4.5s | Complete search |

### Optimization Techniques

```java
// Enable search caching
searchEngine.enableCaching(true);
searchEngine.setCacheSize(1000); // Cache last 1000 searches

// Parallel search execution
searchEngine.setParallelism(4); // Use 4 threads

// Index pre-warming
searchEngine.prewarmIndex(commonSearchTerms);

// Async search with callbacks
CompletableFuture<List<EnhancedSearchResult>> futureResults = 
    searchAPI.searchAsync("medical", resultHandler);
```

## 🔒 Security and Privacy

### Authentication and Authorization

```java
// Set up authenticated search session
SearchSession session = searchAPI.createAuthenticatedSession(userCredentials);

// Perform searches within session
List<EnhancedSearchResult> results = session.search("confidential data");

// Session automatically handles password management
session.close();
```

### Privacy-Preserving Features

```java
// Anonymous search (no user tracking)
List<EnhancedSearchResult> anonResults = searchAPI.searchAnonymous("data");

// Redacted results (sensitive data masked)
List<EnhancedSearchResult> redactedResults = searchAPI.searchWithRedaction(
    "patient records", RedactionLevel.HIGH);

// Audit-compliant search (with logging)
AuditableSearchResult auditResult = searchAPI.searchWithAudit(
    "financial records", auditContext);
```

## 💡 Complete Examples

### Example 1: Medical Research Platform

```java
public class MedicalResearchPlatform {
    private final SearchSpecialistAPI searchAPI;
    private final Map<String, String> researchPasswords;
    
    public List<ResearchResult> findRelatedStudies(String condition) {
        // Multi-strategy search for research data
        
        // 1. Quick public search for general studies
        List<EnhancedSearchResult> publicStudies = 
            searchAPI.searchPublic(condition + " study");
            
        // 2. Authenticated search for detailed data
        List<EnhancedSearchResult> detailedStudies = 
            searchAPI.searchSecure(condition + " clinical trial", 
                                 researchPasswords, 100);
                                 
        // 3. Deep content search for specific mentions
        SearchResult deepResults = 
            searchAPI.searchAdvanced(condition + " treatment outcomes",
                                   researchPassword, researchConfig, 500);
                                     
        // Combine and rank results
        return combineAndRankResults(publicStudies, detailedStudies, deepResults);
    }
}
```

### Example 2: Compliance Audit System

```java
public class ComplianceAuditSystem {
    private final SearchSpecialistAPI searchAPI;
    
    public ComplianceReport auditTimeframe(LocalDateTime start, LocalDateTime end) {
        // Comprehensive audit search
        
        ComplianceSearchCriteria criteria = ComplianceSearchCriteria.builder()
            .startTime(start)
            .endTime(end)
            .includeCategories(Set.of("FINANCIAL", "LEGAL", "MEDICAL"))
            .requiredTerms(Arrays.asList("transaction", "contract", "record"))
            .complianceLevel(ComplianceLevel.STRICT)
            .build();
            
        // Perform exhaustive authenticated search
        List<EnhancedSearchResult> auditResults = 
            searchAPI.searchForCompliance(criteria, auditPasswords);
            
        // Generate compliance report
        return generateComplianceReport(auditResults);
    }
}
```

### Example 3: Real-Time Monitoring Dashboard

```java
public class MonitoringDashboard {
    private final SearchSpecialistAPI searchAPI;
    private final ScheduledExecutorService scheduler;
    
    public void startMonitoring(String[] keywords, Consumer<List<Block>> handler) {
        // Real-time search monitoring
        
        scheduler.scheduleAtFixedRate(() -> {
            // Fast public search for real-time updates
            List<EnhancedSearchResult> newResults = 
                searchAPI.searchSince(keywords, lastCheckTime, 50);
                
            if (!newResults.isEmpty()) {
                // Convert to blocks and notify handler
                List<Block> blocks = extractBlocks(newResults);
                handler.accept(blocks);
            }
            
            lastCheckTime = Instant.now();
        }, 0, 5, TimeUnit.SECONDS);
    }
}
```

## 📊 Result Analysis Methods

The `SearchResult` class provides powerful analysis methods to help you process search results efficiently:

### Available Analysis Methods

#### 1. **Get Top Results by Relevance**
```java
SearchResult result = searchEngine.searchPublicOnly("medical", 100);

// Get top 10 results sorted by relevance score (highest first)
List<EnhancedSearchResult> topResults = result.getTopResults(10);

for (EnhancedSearchResult r : topResults) {
    System.out.printf("Score: %.2f - %s%n",
        r.getRelevanceScore(), r.getSummary());
}
```

**Use Cases:**
- Display most relevant results first
- Prioritize high-quality matches
- Implement pagination with quality filtering

#### 2. **Calculate Average Relevance Score**
```java
SearchResult result = searchEngine.searchIntelligent("blockchain", passwords, 50);

// Get average relevance score across all results
double avgScore = result.getAverageRelevanceScore();

System.out.printf("Search quality: %.2f (0.0-1.0)%n", avgScore);

// Use for quality assessment
if (avgScore < 0.3) {
    System.out.println("⚠️ Low-quality results, consider refining search");
}
```

**Use Cases:**
- Assess overall search quality
- Trigger query refinement suggestions
- Track search performance metrics

#### 3. **Group Results by Source**
```java
SearchResult result = searchEngine.searchHybrid("transaction", passwords, 100);

// Group results by their source (PUBLIC_METADATA, ENCRYPTED_CONTENT, OFF_CHAIN_CONTENT)
Map<SearchResultSource, List<EnhancedSearchResult>> grouped = result.groupBySource();

// Process by source
List<EnhancedSearchResult> publicMatches = grouped.get(SearchResultSource.PUBLIC_METADATA);
List<EnhancedSearchResult> encryptedMatches = grouped.get(SearchResultSource.ENCRYPTED_CONTENT);
List<EnhancedSearchResult> offChainMatches = grouped.get(SearchResultSource.OFF_CHAIN_CONTENT);

System.out.printf("Public: %d, Encrypted: %d, Off-chain: %d%n",
    publicMatches.size(), encryptedMatches.size(), offChainMatches.size());
```

**Use Cases:**
- Analyze search coverage (public vs private vs off-chain)
- Display results grouped by source type
- Optimize search strategies based on source distribution

### Complete Analysis Example

```java
// Perform comprehensive search
SearchResult result = searchEngine.searchIntelligent("medical records", passwords, 100);

// Analysis workflow
if (result.isSuccessful()) {
    System.out.printf("📊 Search Analysis for: '%s'%n", "medical records");
    System.out.printf("Total results: %d%n", result.getResultCount());
    System.out.printf("Search time: %.2f ms%n", result.getTotalTimeMs());
    System.out.printf("Average relevance: %.2f%n", result.getAverageRelevanceScore());

    // Top 5 results
    System.out.println("\n🔝 Top 5 Results:");
    for (EnhancedSearchResult top : result.getTopResults(5)) {
        System.out.printf("  %.2f - %s (%s)%n",
            top.getRelevanceScore(),
            top.getSummary(),
            top.getSource());
    }

    // Source distribution
    System.out.println("\n📈 Source Distribution:");
    Map<SearchResultSource, List<EnhancedSearchResult>> bySource = result.groupBySource();
    bySource.forEach((source, matches) ->
        System.out.printf("  %s: %d matches%n", source, matches.size())
    );
}
```

## ✅ Best Practices

### 1. **Choose the Right Indexing Method**
```java
// ✅ Use SYNC for tests - ensure completion before assertions
@Test
public void testBlockchainSearch() throws Exception {
    // Create block with skipAutoIndexing
    blockchain.addBlockWithKeywords(
        "Test data",
        new String[]{"test", "data"},
        "TEST",
        privateKey,
        publicKey,
        true  // skipAutoIndexing
    );
    
    IndexingResult result = searchEngine.indexBlockchainSync(
        blockchain, null, privateKey);  // null for unencrypted blocks
    // Safe to search immediately
    assertEquals(2, result.getTotalIndexed());  // Genesis + test block
}

// ✅ Use ASYNC for background jobs - non-blocking execution
public void scheduleIndexing() {
    scheduler.scheduleAtFixedRate(() -> {
        searchEngine.indexBlockchain(blockchain, null, privateKey);
        // Job continues, indexing in background
    }, 0, 1, TimeUnit.HOURS);
}

// ✅ Use SYNC for CLI tools - user expects completion
public static void main(String[] args) throws Exception {
    System.out.println("⏳ Indexing...");
    IndexingResult result = searchEngine.indexBlockchainSync(
        blockchain, null, privateKey);  // null for unencrypted blocks
    System.out.println("✅ Indexed " + result.getTotalIndexed() + " blocks");
    // Now search
}

// ✅ Always provide keywords for searchable blocks (privacy-by-design)
String[] keywords = {"medical", "patient", "diagnosis"};
blockchain.addBlockWithKeywords(
    data, 
    keywords, 
    "MEDICAL",  // Category
    privateKey, 
    publicKey,
    false  // Don't skip auto-indexing (default behavior)
);
// Block WITHOUT keywords will NOT be searchable (privacy default)
```

### 2. **Choose the Right Search Strategy**
```java
// For fast public-only discovery (<50ms)
searchAPI.searchPublic("keyword");

// For convenient hybrid search (public + private with default credentials)
searchAPI.searchAll("keyword");

// For encrypted-only sensitive data
searchAPI.searchSecure("confidential", password, limit);

// For comprehensive analysis with auto-strategy
searchAPI.searchIntelligent("complex query", password, limit);

// For expert-level control
searchAPI.searchAdvanced("detailed search", password, config, maxResults);
```

### 3. **Optimize for Performance**
```java
// Use caching for repeated searches
searchAPI.enableCaching(true);

// Limit result size appropriately
searchAPI.setDefaultLimit(50);

// Use async for non-blocking searches
CompletableFuture<List<EnhancedSearchResult>> future = 
    searchAPI.searchAsync("data");
```

### 4. **Maintain Security**
```java
// Always validate passwords
if (!isValidPassword(password)) {
    throw new SecurityException("Invalid password");
}

// Use time-limited sessions
SearchSession session = searchAPI.createSession(Duration.ofMinutes(30));

// Clear sensitive data
searchAPI.clearCaches();
```

### 5. **Handle Errors Gracefully**
```java
try {
    List<EnhancedSearchResult> results = searchAPI.searchSecure(
        "data", password, 100);
} catch (AuthenticationException e) {
    // Handle auth failure
    logger.warn("Authentication failed for search");
} catch (SearchException e) {
    // Handle search errors
    logger.error("Search failed: " + e.getMessage());
}
```

## 🔗 Related Documentation

- **[Indexing Guide](INDEXING_GUIDE.md)** - Complete blockchain indexing documentation (async vs sync)
- **[IndexingSyncDemo](../../src/main/java/demo/IndexingSyncDemo.java)** - Working demo with 4 practical examples
- [API Guide](../reference/API_GUIDE.md) - Complete API reference
- [Thread Safety & Concurrent Indexing](../monitoring/INDEXING_COORDINATOR_EXAMPLES.md#thread-safety--concurrent-indexing) - Per-block semaphore coordination
- [Semaphore-Based Block Indexing Implementation](../development/SEMAPHORE_INDEXING_IMPLEMENTATION.md) - Complete technical guide
- [IndexingCoordinator Examples](../monitoring/INDEXING_COORDINATOR_EXAMPLES.md) - Practical examples for indexing coordination and loop prevention
- [Granular Term Visibility Demo](../scripts/run_granular_term_visibility_demo.zsh) - Interactive demonstration
- [Search Comparison](SEARCH_COMPARISON.md) - Performance benchmarks
- [Technical Details](../reference/TECHNICAL_DETAILS.md) - Implementation details

### 🎬 Complete Working Examples

For complete, runnable implementations of all indexing patterns, see:

📁 **`src/main/java/demo/IndexingSyncDemo.java`**

This comprehensive demo includes:
- ✅ Example 1: Hospital Medical Records System (100 patient records)
- ✅ Example 2: Demo Script Pattern (sequential execution)
- ✅ Example 3: Background Job Pattern (async indexing)
- ✅ Example 4: CLI Tool Pattern (sync for user expectations)

**Key features demonstrated:**
- `skipAutoIndexing` parameter for explicit control
- SYNC vs ASYNC indexing patterns
- Unencrypted blocks with public keywords
- Multi-department security isolation
- Bootstrap admin initialization
- IndexingCoordinator coordination

**Run the demo:**
```bash
./scripts/run_indexing_sync_demo.zsh
```

## 🎯 Quick Reference

### Search Methods Comparison

| Method | Speed | Privacy | Use Case |
|--------|-------|---------|----------|
| `searchPublic()` | ⚡ Ultra-Fast (<50ms) | Public only | Fast discovery, anonymous access |
| `searchAll()` | ⚡ Fast | Public + Private (default) | Convenient hybrid search |
| `searchSecure()` | 🔒 Moderate | Encrypted only | Sensitive data with explicit password |
| `searchIntelligent()` | 🧠 Adaptive | Auto-detect | General purpose with smart routing |
| `searchAdvanced()` | 🔍 Comprehensive | Complete control | Expert configuration |

### Common Search Patterns

```java
// Pattern 1: Progressive search (public → hybrid → encrypted)
List<EnhancedSearchResult> fast = searchAPI.searchPublic(term);
if (needMoreScope) {
    List<EnhancedSearchResult> hybrid = searchAPI.searchAll(term);
}
if (needEncryptedOnly) {
    List<EnhancedSearchResult> encrypted = searchAPI.searchSecure(term, pass, 100);
}

// Pattern 2: Filtered search
SearchFilter filter = SearchFilter.builder()
    .categories(Set.of("MEDICAL"))
    .timeRange(TimeRange.lastMonth())
    .build();
List<EnhancedSearchResult> filtered = searchAPI.searchWithFilter(term, filter);

// Pattern 3: Batch search
List<String> terms = Arrays.asList("medical", "patient", "treatment");
Map<String, List<EnhancedSearchResult>> batchResults = 
    searchAPI.batchSearch(terms, 50);
```