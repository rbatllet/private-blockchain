# Search Framework Engine Guide

Comprehensive guide to the Private Blockchain's Search Framework Engine with two-layer metadata architecture, intelligent strategy routing, and privacy-preserving search capabilities.

## 📋 Table of Contents

- [Overview](#-overview)
- [Search Architecture](#-search-architecture)
- [Search Strategies](#-search-strategies)
- [Basic Usage](#-basic-usage)
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

### Quick Start with SearchSpecialistAPI

```java
// Initialize the API
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI();

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

```java
// Initialize the search engine
SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();

// Initialize with blockchain
searchEngine.initialize(blockchain, OffChainStorageService.getInstance());

// Perform public-only search
SearchResult result = searchEngine.searchPublicOnly("medical", 20);

// Perform authenticated search
Map<String, String> passwords = new HashMap<>();
passwords.put("block123", "password123");
SearchResult authResult = searchEngine.searchWithAuth(
    "confidential", passwords, 50);
```

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

### 1. **Choose the Right Search Strategy**
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

### 2. **Optimize for Performance**
```java
// Use caching for repeated searches
searchAPI.enableCaching(true);

// Limit result size appropriately
searchAPI.setDefaultLimit(50);

// Use async for non-blocking searches
CompletableFuture<List<EnhancedSearchResult>> future = 
    searchAPI.searchAsync("data");
```

### 3. **Maintain Security**
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

### 4. **Handle Errors Gracefully**
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

- [API Guide](../reference/API_GUIDE.md) - Complete API reference
- [Atomic Protection & Multi-Instance Coordination](../testing/ATOMIC_PROTECTION_MULTI_INSTANCE_GUIDE.md) - Thread-safe multi-instance operations
- [IndexingCoordinator Examples](../monitoring/INDEXING_COORDINATOR_EXAMPLES.md) - Practical examples for indexing coordination and loop prevention
- [Granular Term Visibility Demo](../scripts/run_granular_term_visibility_demo.zsh) - Interactive demonstration
- [Search Comparison](SEARCH_COMPARISON.md) - Performance benchmarks
- [Technical Details](../reference/TECHNICAL_DETAILS.md) - Implementation details

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