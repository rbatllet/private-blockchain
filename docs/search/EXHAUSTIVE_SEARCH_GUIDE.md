# TRUE Exhaustive Search Guide

## 📋 Overview

The Advanced Search Engine now supports **TRUE exhaustive search** that combines:
- **On-chain content search**: Searches inside block.getData() content (encrypted and plain text)
- **Off-chain file search**: Searches external files referenced by blocks
- **Metadata search**: Searches public and private metadata layers
- **Thread-safe operations**: Concurrent search with cache management

## 🔍 Search Levels

### INCLUDE_ENCRYPTED (TRUE Exhaustive)
Searches across ALL content types:
```java
SearchResult result = searchEngine.searchExhaustiveOffChain(
    "medical", password, privateKey, maxResults);
```

**What it searches:**
1. ✅ On-chain block content (encrypted with password)
2. ✅ On-chain block content (plain text) 
3. ✅ Off-chain files (encrypted and plain text)
4. ✅ Public metadata (keywords, categories)
5. ✅ Private metadata (encrypted keywords, sensitive terms)

### Other Search Levels
- `FAST_ONLY`: Keywords only (fastest, ~10-20ms) - Searches public metadata keywords
- `INCLUDE_METADATA`: Keywords + block data (~30-60ms) - Searches on-chain content with password
- `INCLUDE_ENCRYPTED`: Everything including off-chain (~50-600ms) - Searches all content with signer key

## 📝 Practical Examples

### Example 1: Basic On-Chain Content Search

```java
// Setup
Blockchain blockchain = new Blockchain();
SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();
KeyPair keyPair = CryptoUtil.generateKeyPair();
String password = "MySecurePassword123!";

// Authorize key
String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
blockchain.addAuthorizedKey(publicKeyString, "User", null);

// Create blocks with searchable content
blockchain.addBlockWithKeywords(
    "Public medical announcement: New treatment protocol available",
    new String[]{"medical", "public", "treatment"},
    "MEDICAL",
    keyPair.getPrivate(),
    keyPair.getPublic()
);

blockchain.addEncryptedBlockWithKeywords(
    "Confidential patient diagnosis: John Doe has diabetes type 2",
    password,
    new String[]{"patient", "diagnosis", "confidential", "diabetes"},
    "MEDICAL",
    keyPair.getPrivate(),
    keyPair.getPublic()
);

// Index blockchain
searchEngine.indexBlockchain(blockchain, password, keyPair.getPrivate());

// Search on-chain content
SearchResult result = searchEngine.searchExhaustiveOffChain(
    "diabetes", password, keyPair.getPrivate(), 10);

// Process results
if (result.isSuccessful()) {
    System.out.println("Found " + result.getResultCount() + " results");
    for (EnhancedSearchResult searchResult : result.getResults()) {
        System.out.println("Block: " + searchResult.getBlockHash().substring(0, 8) + "...");
        System.out.println("Source: " + searchResult.getSource());
        System.out.println("Content: " + searchResult.getSummary());
        System.out.println("Relevance: " + searchResult.getRelevanceScore());
    }
}
```

### Example 2: Off-Chain File Search

```java
// Create off-chain file
File medicalFile = new File("patient_record.txt");
try (FileWriter writer = new FileWriter(medicalFile)) {
    writer.write("MEDICAL RECORD\n" +
                "Patient: Jane Smith\n" +
                "Diagnosis: Hypertension and cardiac arrhythmia\n" +
                "Treatment: Beta-blockers and ACE inhibitors\n" +
                "Notes: Patient shows excellent response to medication");
}

// Store off-chain data
OffChainStorageService offChainService = new OffChainStorageService();
OffChainData offChainData = offChainService.storeData(
    Files.readAllBytes(medicalFile.toPath()),
    password,
    keyPair.getPrivate(),
    publicKeyString,
    "text/plain"
);

// Create block with off-chain reference
Block block = blockchain.addEncryptedBlockWithKeywords(
    "Medical record with off-chain detailed patient information",
    password,
    new String[]{"medical", "patient", "record", "offchain"},
    "MEDICAL",
    keyPair.getPrivate(),
    keyPair.getPublic()
);
block.setOffChainData(offChainData);

// Re-index to include off-chain content
searchEngine.indexBlockchain(blockchain, password, keyPair.getPrivate());

// Search will now find content in off-chain file
SearchResult result = searchEngine.searchExhaustiveOffChain(
    "hypertension", password, keyPair.getPrivate(), 10);

// Check for off-chain matches
for (EnhancedSearchResult searchResult : result.getResults()) {
    if (searchResult.hasOffChainMatch()) {
        OffChainMatch match = searchResult.getOffChainMatch();
        System.out.println("Off-chain file: " + match.getFileName());
        System.out.println("Content type: " + match.getContentType());
        System.out.println("Matches found: " + match.getMatchCount());
        System.out.println("Preview: " + match.getPreviewSnippet());
    }
}
```

### Example 3: Mixed Content Scenario

```java
// Create a blockchain with mixed content types
public void createMixedContentDemo() throws Exception {
    // 1. Plain text on-chain block
    blockchain.addBlockWithKeywords(
        "Public financial report: Q3 revenue increased by 15%",
        new String[]{"financial", "public", "revenue", "quarterly"},
        "FINANCIAL",
        privateKey, publicKey
    );
    
    // 2. Encrypted on-chain block
    blockchain.addEncryptedBlockWithKeywords(
        "Private account transfer: $50000 to confidential recipient",
        password,
        new String[]{"financial", "private", "transfer", "confidential"},
        "FINANCIAL",
        privateKey, publicKey
    );
    
    // 3. Block with encrypted off-chain JSON file
    File jsonFile = new File("financial_data.json");
    String jsonContent = """
        {
          "transactions": [
            {"date": "2024-07-15", "amount": 125000, "type": "sale"},
            {"date": "2024-08-20", "amount": 89000, "type": "expense"}
          ],
          "summary": "Strong financial performance this quarter"
        }
        """;
    Files.write(jsonFile.toPath(), jsonContent.getBytes());
    
    OffChainData jsonOffChain = offChainService.storeData(
        jsonContent.getBytes(), password, privateKey, publicKeyString, "application/json");
    
    Block jsonBlock = blockchain.addEncryptedBlockWithKeywords(
        "Financial summary with detailed transaction data",
        password,
        new String[]{"financial", "transactions", "summary"},
        "FINANCIAL",
        privateKey, publicKey
    );
    jsonBlock.setOffChainData(jsonOffChain);
    
    // Index everything
    searchEngine.indexBlockchain(blockchain, password, privateKey);
    
    // Exhaustive search finds content across all types
    SearchResult result = searchEngine.searchExhaustiveOffChain(
        "financial", password, privateKey, 20);
    
    // Analyze search distribution
    int onChainCount = 0, offChainCount = 0;
    for (EnhancedSearchResult searchResult : result.getResults()) {
        if (searchResult.isOffChainResult()) {
            offChainCount++;
        } else {
            onChainCount++;
        }
    }
    
    System.out.println("Total results: " + result.getResultCount());
    System.out.println("On-chain matches: " + onChainCount);
    System.out.println("Off-chain matches: " + offChainCount);
    System.out.println("Search time: " + result.getTotalTimeMs() + "ms");
}
```

## 🔐 Security Considerations

### Password-Protected Content
- Encrypted blocks require the correct password for content search
- Wrong passwords limit search to public content only
- Off-chain files can be individually encrypted

### Key Authorization
- All block creation requires authorized public keys
- Search operations validate key permissions
- Private metadata access requires correct private key

### Example: Security Validation
```java
// Test with wrong password
SearchResult wrongPasswordResult = searchEngine.searchExhaustiveOffChain(
    "confidential", "wrong_password", privateKey, 10);
// Returns limited results (public content only)

// Test with correct password  
SearchResult correctResult = searchEngine.searchExhaustiveOffChain(
    "confidential", correctPassword, privateKey, 10);
// Returns full results including encrypted content
```

## ⚡ Performance Guidelines

### Cache Management
The search engine includes intelligent caching:
```java
// Cache statistics
Map<String, Object> stats = searchEngine.getOffChainSearchStats();
System.out.println("Cache size: " + stats.get("cacheSize"));

// Clear cache when needed
searchEngine.clearOffChainCache();
```

### Optimization Tips
1. **Limit search scope**: Use specific keywords rather than broad terms
2. **Set reasonable maxResults**: Don't request more results than needed
3. **Use appropriate search level**: PUBLIC_ONLY for faster searches when encryption not needed
4. **Monitor cache hit rates**: Check performance statistics regularly

### Performance Example
```java
// Performance benchmark
long startTime = System.nanoTime();
SearchResult result = searchEngine.searchExhaustiveOffChain(
    "medical", password, privateKey, 10);
long endTime = System.nanoTime();
double searchTimeMs = (endTime - startTime) / 1_000_000.0;

System.out.println("Search completed in: " + searchTimeMs + "ms");
System.out.println("Results found: " + result.getResultCount());
System.out.println("Strategy used: " + result.getStrategyUsed());
```

## 🧵 Thread Safety

The search engine is fully thread-safe:
```java
// Concurrent search example (Java 25 Virtual Threads)
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
CountDownLatch latch = new CountDownLatch(10);

for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
        try {
            SearchResult result = searchEngine.searchExhaustiveOffChain(
                "medical", password, privateKey, 5);
            System.out.println("Thread search: " + result.getResultCount() + " results");
        } finally {
            latch.countDown();
        }
    });
}

latch.await(); // Wait for all searches to complete
executor.shutdown();
```

## 📊 Result Analysis

### Enhanced Search Results
```java
for (EnhancedSearchResult result : searchResults) {
    // Basic information
    System.out.println("Block Hash: " + result.getBlockHash());
    System.out.println("Relevance Score: " + result.getRelevanceScore());
    System.out.println("Source: " + result.getSource());
    
    // On-chain vs off-chain
    if (result.isOffChainResult()) {
        OffChainMatch match = result.getOffChainMatch();
        System.out.println("Off-chain file: " + match.getFileName());
        System.out.println("Content preview: " + match.getPreviewSnippet());
    } else {
        System.out.println("On-chain content: " + result.getSummary());
    }
}
```

## 🚀 Running the Demo

A complete demonstration is available:
```bash
# Run the interactive demo
./scripts/run_exhaustive_search_demo.zsh

# Or run programmatically
mvn exec:java -Dexec.mainClass="demo.ExhaustiveSearchDemo"
```

The demo showcases:
- Mixed content blockchain creation
- Performance analysis across search types
- Security validation with different passwords
- Cache behavior demonstration

## 📋 API Reference

### Core Methods
- `searchExhaustiveOffChain(query, password, privateKey, maxResults)`: TRUE exhaustive search
- `searchPublicOnly(query, maxResults)`: Public content only
- `searchEncryptedOnly(query, password, maxResults)`: Encrypted content only
- `indexBlockchain(blockchain, password, privateKey)`: Index blockchain for search

### Utility Methods
- `getOffChainSearchStats()`: Cache and performance statistics
- `clearOffChainCache()`: Clear search cache
- `shutdown()`: Clean shutdown of search engine

## 🔧 Troubleshooting

### Common Issues
1. **No results found**: Check password, key authorization, and indexing
2. **Slow searches**: Monitor cache usage, reduce maxResults, check file sizes
3. **Thread safety**: Ensure proper cleanup in concurrent environments

### Debug Information
Enable debug logging to see detailed search operations:
```java
// Debug output shows:
// - Cache hits/misses
// - Encryption/decryption operations  
// - File access patterns
// - Performance metrics
```

---

## 📚 Related Documents

### 🔍 Search Guides
- **[SEARCH_COMPARISON.md](SEARCH_COMPARISON.md)** - Complete comparison of all 5 available search types
- **[USER_FRIENDLY_SEARCH_GUIDE.md](USER_FRIENDLY_SEARCH_GUIDE.md)** - Traditional hybrid search system guide

### 📖 Main Documentation  
- **[README.md](../../README.md)** - Main documentation with quick start
- **[API_GUIDE.md](../reference/API_GUIDE.md)** - Complete API reference
- **[EXAMPLES.md](../getting-started/EXAMPLES.md)** - Real use cases and workflows

### 🚀 Code and Demos
- **[ExhaustiveSearchDemo.java](../src/main/java/demo/ExhaustiveSearchDemo.java)** - Complete demo with mixed scenarios
- **[ExhaustiveSearchExamples.java](../src/main/java/demo/ExhaustiveSearchExamples.java)** - 6 detailed practical examples
- **[Execution scripts](../scripts/)** - Zsh scripts to run demos and examples

### 🔐 Security and Technical
- **[ENCRYPTION_GUIDE.md](../security/ENCRYPTION_GUIDE.md)** - Encryption and metadata management
- **[TESTING.md](../testing/TESTING.md)** - Testing guide and troubleshooting
- **[TECHNICAL_DETAILS.md](../reference/TECHNICAL_DETAILS.md)** - Architecture and database schema

---

*Generated with TRUE Exhaustive Search v2.0 - Advanced Blockchain Search Engine*