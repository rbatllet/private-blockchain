# Search System Guide

Comprehensive guide to the Private Blockchain's hybrid search system with multi-level search capabilities, automatic keyword extraction, and content categorization.

## 📋 Table of Contents

- [Overview](#-overview)
- [Search Levels](#-search-levels)
- [Basic Usage](#-basic-usage)
- [Advanced Search Features](#-advanced-search-features)
- [Search Validation](#-search-validation)
- [Automatic Keyword Extraction](#-automatic-keyword-extraction)
- [Content Categories](#-content-categories)
- [Performance Considerations](#-performance-considerations)
- [Thread Safety](#-thread-safety)
- [Complete Examples](#-complete-examples)
- [Best Practices](#-best-practices)

## 🔍 Overview

The Private Blockchain includes a sophisticated hybrid search system that enables efficient searching across blockchain content with multiple search modes:

- **Multi-Level Search**: Choose between speed and comprehensiveness
- **Automatic Keyword Extraction**: Universal language-independent elements
- **Content Categories**: Organize and filter blocks by type
- **Search Validation**: Intelligent term requirements with useful exceptions
- **Thread-Safe Operations**: Concurrent search support

## 🚀 Search Levels

The system provides three search levels through the `SearchLevel` enum:

### 1. FAST_ONLY
- **Purpose**: Maximum performance
- **Searches**: Keywords only (manual + automatic)
- **Use Case**: Quick lookups, real-time search suggestions
- **Performance**: Fastest ⚡

### 2. INCLUDE_DATA
- **Purpose**: Balanced performance and coverage
- **Searches**: Keywords + block data content
- **Use Case**: Standard search operations, most common use
- **Performance**: Medium ⚖️

### 3. EXHAUSTIVE_OFFCHAIN
- **Purpose**: Complete coverage
- **Searches**: Keywords + block data + off-chain content
- **Use Case**: Comprehensive searches, audit procedures
- **Performance**: Slowest but most thorough 🔍

## 📚 Basic Usage

### Quick Start

```java
// Initialize blockchain with search capabilities
Blockchain blockchain = new Blockchain();

// Create blocks with keywords and categories
String[] keywords = {"PATIENT-001", "CARDIOLOGY", "ECG-2024"};
blockchain.addBlockWithKeywords(
    "Patient examination data...",
    keywords,
    "MEDICAL",
    privateKey, publicKey
);

// Perform searches at different levels
List<Block> fastResults = blockchain.searchBlocks("medical", SearchLevel.FAST_ONLY);
List<Block> dataResults = blockchain.searchBlocks("patient", SearchLevel.INCLUDE_DATA);
List<Block> allResults = blockchain.searchBlocks("examination", SearchLevel.EXHAUSTIVE_OFFCHAIN);
```

### Convenience Methods

```java
// Fast search (keywords only)
List<Block> quickSearch = blockchain.searchBlocksFast("API");

// Complete search (all content)
List<Block> completeSearch = blockchain.searchBlocksComplete("John Doe");

// Category filtering
List<Block> medicalBlocks = blockchain.searchByCategory("MEDICAL");
```

## 🔧 Advanced Search Features

### Search with Keywords and Categories

```java
public void demonstrateAdvancedSearch() {
    // Medical block with comprehensive keywords
    String[] medicalKeywords = {
        "PATIENT-001",      // Patient ID
        "CARDIOLOGY",       // Department
        "ECG-2024-001",     // Test reference
        "DR-SMITH",         // Doctor code
        "URGENT"            // Priority
    };
    
    blockchain.addBlockWithKeywords(
        "Patient John Doe underwent ECG examination on 2024-01-15. " +
        "Results indicate normal sinus rhythm. Follow-up in 6 months. " +
        "Contact: cardiology@hospital.com for questions.",
        medicalKeywords,
        "MEDICAL",
        privateKey, publicKey
    );
    
    // Financial block with transaction data
    String[] financeKeywords = {
        "PROJECT-ALPHA",    // Project code
        "BUDGET-2024-Q1",   // Budget period
        "APPROVED",         // Status
        "EUR",              // Currency
        "INVOICE-001"       // Invoice reference
    };
    
    blockchain.addBlockWithKeywords(
        "Project Alpha Q1 budget: 50000 EUR approved. " +
        "Invoice sent to finance@company.com. " +
        "Payment due: 2024-02-15.",
        financeKeywords,
        "FINANCE",
        privateKey, publicKey
    );
}
```

### Search Combinations

```java
public void demonstrateSearchCombinations() {
    // Search for specific patient across all levels
    System.out.println("=== Patient Search Across Levels ===");
    
    // Fast: Check if patient exists in keywords
    List<Block> fastPatient = blockchain.searchBlocksFast("PATIENT-001");
    System.out.println("Fast search found: " + fastPatient.size() + " blocks");
    
    // Data: Find patient mentions in content
    List<Block> dataPatient = blockchain.searchBlocks("John Doe", SearchLevel.INCLUDE_DATA);
    System.out.println("Data search found: " + dataPatient.size() + " blocks");
    
    // Category: All medical records
    List<Block> medicalRecords = blockchain.searchByCategory("MEDICAL");
    System.out.println("Medical category: " + medicalRecords.size() + " blocks");
    
    // Combine results programmatically
    Set<Block> combinedResults = new HashSet<>();
    combinedResults.addAll(fastPatient);
    combinedResults.addAll(dataPatient);
    combinedResults.addAll(medicalRecords);
    System.out.println("Combined unique blocks: " + combinedResults.size());
}
```

## ✅ Search Validation

### Validation Rules

The system validates search terms with intelligent exceptions:

```java
public void demonstrateSearchValidation() {
    // Valid searches (4+ characters)
    assert SearchValidator.isValidSearchTerm("medical");     // ✅ 7 characters
    assert SearchValidator.isValidSearchTerm("blockchain");  // ✅ 10 characters
    
    // Valid short term exceptions
    assert SearchValidator.isValidSearchTerm("2024");        // ✅ Year
    assert SearchValidator.isValidSearchTerm("API");         // ✅ Acronym
    assert SearchValidator.isValidSearchTerm("SQL");         // ✅ Technical term
    assert SearchValidator.isValidSearchTerm("XML");         // ✅ Technical term
    assert SearchValidator.isValidSearchTerm("JSON");        // ✅ Technical term
    assert SearchValidator.isValidSearchTerm("123");         // ✅ Number
    assert SearchValidator.isValidSearchTerm("ID-001");      // ✅ ID format
    
    // Invalid searches (too short)
    assert !SearchValidator.isValidSearchTerm("hi");        // ❌ 2 characters
    assert !SearchValidator.isValidSearchTerm("a");         // ❌ 1 character
    assert !SearchValidator.isValidSearchTerm("");          // ❌ Empty
    assert !SearchValidator.isValidSearchTerm(null);        // ❌ Null
    assert !SearchValidator.isValidSearchTerm("   ");       // ❌ Whitespace only
    
    System.out.println("All validation tests passed!");
}
```

### Search Results with Validation

```java
public void searchWithValidation() {
    // These will return results
    List<Block> results1 = blockchain.searchBlocksFast("medical");  // Valid
    List<Block> results2 = blockchain.searchBlocksFast("2024");     // Valid exception
    List<Block> results3 = blockchain.searchBlocksFast("API");      // Valid exception
    
    // These will return empty lists (invalid terms)
    List<Block> empty1 = blockchain.searchBlocksFast("hi");         // Too short
    List<Block> empty2 = blockchain.searchBlocksFast("a");          // Too short
    List<Block> empty3 = blockchain.searchBlocksFast("");           // Empty
    
    assert empty1.isEmpty() && empty2.isEmpty() && empty3.isEmpty();
}
```

## 🤖 Automatic Keyword Extraction

### Universal Elements Extracted

The system automatically extracts language-independent keywords:

```java
public void demonstrateAutoExtraction() {
    String blockContent = """
        Project Meeting Minutes - 2024-01-15
        
        Attendees:
        - John Smith (john.smith@company.com)
        - Maria Garcia (maria.garcia@company.com)
        
        Topics Discussed:
        1. API-v2.1 release schedule
        2. Budget allocation: 75000 EUR
        3. Database migration to SQL-Server
        4. Contract reference: CONT-2024-001
        
        Action Items:
        - Review JSON schema by 2024-01-20
        - Update XML documentation
        - Schedule follow-up for 2024-02-01
        
        Document ID: DOC-2024-001
        Classification: CONFIDENTIAL
        """;
    
    // Add block with automatic extraction
    blockchain.addBlockWithKeywords(
        blockContent,
        null,  // No manual keywords - test auto extraction
        "TECHNICAL",
        privateKey, publicKey
    );
    
    // Search for automatically extracted elements
    System.out.println("=== Auto-Extracted Keyword Search ===");
    
    // Years and dates
    List<Block> yearSearch = blockchain.searchBlocksFast("2024");
    List<Block> dateSearch = blockchain.searchBlocksFast("2024-01-15");
    
    // Emails
    List<Block> emailSearch = blockchain.searchBlocks("john.smith@company.com", SearchLevel.INCLUDE_DATA);
    
    // Codes and references
    List<Block> apiSearch = blockchain.searchBlocksFast("API-v2.1");
    List<Block> contractSearch = blockchain.searchBlocksFast("CONT-2024-001");
    
    // Currency
    List<Block> currencySearch = blockchain.searchBlocksFast("EUR");
    
    // Technical terms
    List<Block> jsonSearch = blockchain.searchBlocksFast("JSON");
    List<Block> xmlSearch = blockchain.searchBlocksFast("XML");
    
    System.out.println("Year 2024 found in: " + yearSearch.size() + " blocks");
    System.out.println("Date found in: " + dateSearch.size() + " blocks");
    System.out.println("Email found in: " + emailSearch.size() + " blocks");
    System.out.println("API reference found in: " + apiSearch.size() + " blocks");
    System.out.println("Contract found in: " + contractSearch.size() + " blocks");
    System.out.println("EUR currency found in: " + currencySearch.size() + " blocks");
    System.out.println("JSON term found in: " + jsonSearch.size() + " blocks");
    System.out.println("XML term found in: " + xmlSearch.size() + " blocks");
}
```

### What Gets Extracted

- **Dates**: 2024-01-15, 01/15/2024, 15-Jan-2024
- **Years**: 2024, 2023, 1999-2099 range
- **Numbers**: 123, 50000, 3.14
- **Emails**: user@domain.com, admin@company.org
- **URLs**: https://example.com, www.company.com
- **Codes**: API-v2.1, PROJ-001, ID_123
- **Currency**: EUR, USD, GBP (3-letter codes)
- **File Extensions**: document.pdf, data.xlsx, file.json
- **Technical Terms**: API, SQL, XML, JSON, CSV, PDF

## 📁 Content Categories

### Standard Categories

```java
public void demonstrateCategoryUsage() {
    // Medical category
    blockchain.addBlockWithKeywords(
        "Patient consultation notes...",
        new String[]{"PATIENT-001", "CONSULTATION"},
        "MEDICAL",
        privateKey, publicKey
    );
    
    // Finance category
    blockchain.addBlockWithKeywords(
        "Quarterly budget report...",
        new String[]{"Q1-2024", "BUDGET"},
        "FINANCE",
        privateKey, publicKey
    );
    
    // Technical category
    blockchain.addBlockWithKeywords(
        "API documentation update...",
        new String[]{"API-v2.0", "DOCS"},
        "TECHNICAL",
        privateKey, publicKey
    );
    
    // Legal category
    blockchain.addBlockWithKeywords(
        "Contract amendment details...",
        new String[]{"CONTRACT-001", "AMENDMENT"},
        "LEGAL",
        privateKey, publicKey
    );
    
    // Search by categories
    List<Block> medical = blockchain.searchByCategory("MEDICAL");
    List<Block> finance = blockchain.searchByCategory("FINANCE");
    List<Block> technical = blockchain.searchByCategory("TECHNICAL");
    List<Block> legal = blockchain.searchByCategory("LEGAL");
    
    System.out.println("Medical blocks: " + medical.size());
    System.out.println("Finance blocks: " + finance.size());
    System.out.println("Technical blocks: " + technical.size());
    System.out.println("Legal blocks: " + legal.size());
}
```

### Custom Categories

```java
public void demonstrateCustomCategories() {
    // Custom categories for specific use cases
    String[] hrKeywords = {"EMPLOYEE-001", "PERFORMANCE"};
    blockchain.addBlockWithKeywords(
        "Employee performance review...",
        hrKeywords,
        "HR",  // Custom category
        privateKey, publicKey
    );
    
    String[] complianceKeywords = {"AUDIT-2024", "SOX"};
    blockchain.addBlockWithKeywords(
        "SOX compliance audit results...",
        complianceKeywords,
        "COMPLIANCE",  // Custom category
        privateKey, publicKey
    );
    
    // Search custom categories
    List<Block> hrBlocks = blockchain.searchByCategory("HR");
    List<Block> complianceBlocks = blockchain.searchByCategory("COMPLIANCE");
    
    System.out.println("HR blocks: " + hrBlocks.size());
    System.out.println("Compliance blocks: " + complianceBlocks.size());
}
```

## ⚡ Performance Considerations

### Search Level Performance

```java
public void compareSearchPerformance() {
    int iterations = 100;
    String searchTerm = "2024";
    
    // Warm up
    blockchain.searchBlocksFast(searchTerm);
    
    // Test FAST_ONLY performance
    long startTime = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
        blockchain.searchBlocks(searchTerm, SearchLevel.FAST_ONLY);
    }
    long fastTime = System.nanoTime() - startTime;
    
    // Test INCLUDE_DATA performance
    startTime = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
        blockchain.searchBlocks(searchTerm, SearchLevel.INCLUDE_DATA);
    }
    long dataTime = System.nanoTime() - startTime;
    
    // Test EXHAUSTIVE_OFFCHAIN performance
    startTime = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
        blockchain.searchBlocks(searchTerm, SearchLevel.EXHAUSTIVE_OFFCHAIN);
    }
    long exhaustiveTime = System.nanoTime() - startTime;
    
    System.out.println("Performance Results (" + iterations + " iterations):");
    System.out.println("FAST_ONLY: " + (fastTime / 1_000_000) + "ms");
    System.out.println("INCLUDE_DATA: " + (dataTime / 1_000_000) + "ms");
    System.out.println("EXHAUSTIVE_OFFCHAIN: " + (exhaustiveTime / 1_000_000) + "ms");
    
    // Performance ratios
    double dataRatio = (double) dataTime / fastTime;
    double exhaustiveRatio = (double) exhaustiveTime / fastTime;
    
    System.out.println("INCLUDE_DATA is " + String.format("%.1f", dataRatio) + "x slower than FAST_ONLY");
    System.out.println("EXHAUSTIVE_OFFCHAIN is " + String.format("%.1f", exhaustiveRatio) + "x slower than FAST_ONLY");
}
```

### Performance Recommendations

- **Real-time Search**: Use `FAST_ONLY` for autocomplete, suggestions
- **Standard Search**: Use `INCLUDE_DATA` for most search operations
- **Audit/Investigation**: Use `EXHAUSTIVE_OFFCHAIN` for comprehensive searches
- **Category Filtering**: Always fast, can be combined with any search level

## 🔒 Thread Safety

All search operations are fully thread-safe:

```java
public void demonstrateConcurrentSearch() {
    ExecutorService executor = Executors.newFixedThreadPool(8);
    CountDownLatch latch = new CountDownLatch(8);
    AtomicInteger totalResults = new AtomicInteger(0);
    
    // Launch concurrent searches
    for (int i = 0; i < 8; i++) {
        final int threadId = i;
        executor.submit(() -> {
            try {
                // Mix different search types
                List<Block> results = switch (threadId % 4) {
                    case 0 -> blockchain.searchBlocksFast("2024");
                    case 1 -> blockchain.searchBlocks("medical", SearchLevel.INCLUDE_DATA);
                    case 2 -> blockchain.searchByCategory("FINANCE");
                    case 3 -> blockchain.searchBlocksComplete("patient");
                    default -> throw new IllegalStateException("Unexpected value: " + (threadId % 4));
                };
                
                totalResults.addAndGet(results.size());
                System.out.println("Thread " + threadId + " found " + results.size() + " blocks");
                
            } finally {
                latch.countDown();
            }
        });
    }
    
    try {
        latch.await(30, TimeUnit.SECONDS);
        System.out.println("Total results across all threads: " + totalResults.get());
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        executor.shutdown();
    }
}
```

## 📚 Complete Examples

### Medical Records System

```java
public class MedicalRecordsSearchExample {
    public static void main(String[] args) throws Exception {
        Blockchain blockchain = new Blockchain();
        
        // Setup
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(publicKey), "MedicalSystem");
        
        // Add medical records
        addMedicalRecords(blockchain, privateKey, publicKey);
        
        // Demonstrate medical search scenarios
        demonstrateMedicalSearch(blockchain);
    }
    
    private static void addMedicalRecords(Blockchain blockchain, PrivateKey privateKey, PublicKey publicKey) {
        // Patient consultation
        blockchain.addBlockWithKeywords(
            "Patient ID: PAT-001, Name: John Doe, Date: 2024-01-15. " +
            "Chief Complaint: Chest pain. Examination: Normal heart rate and rhythm. " +
            "Diagnosis: Non-cardiac chest pain. Treatment: Rest and follow-up in 2 weeks. " +
            "Doctor: Dr. Smith (cardiology@hospital.com)",
            new String[]{"PAT-001", "JOHN-DOE", "CHEST-PAIN", "CARDIOLOGY", "DR-SMITH"},
            "MEDICAL",
            privateKey, publicKey
        );
        
        // Lab results
        blockchain.addBlockWithKeywords(
            "Lab Results for Patient PAT-001 (John Doe). " +
            "Test Date: 2024-01-16. Cholesterol: 195 mg/dL (Normal). " +
            "Glucose: 88 mg/dL (Normal). Blood pressure: 120/80 mmHg. " +
            "Lab Tech: tech@hospital.com",
            new String[]{"PAT-001", "LAB-RESULTS", "CHOLESTEROL", "GLUCOSE", "BP"},
            "MEDICAL",
            privateKey, publicKey
        );
        
        // Follow-up appointment
        blockchain.addBlockWithKeywords(
            "Follow-up appointment for PAT-001 scheduled for 2024-01-30. " +
            "Patient reports improvement in chest pain symptoms. " +
            "Vital signs stable. Continue current treatment plan. " +
            "Next appointment: 2024-03-01",
            new String[]{"PAT-001", "FOLLOW-UP", "IMPROVEMENT", "STABLE"},
            "MEDICAL",
            privateKey, publicKey
        );
    }
    
    private static void demonstrateMedicalSearch(Blockchain blockchain) {
        System.out.println("=== MEDICAL RECORDS SEARCH DEMO ===");
        
        // Search for specific patient
        System.out.println("\n1. Patient-specific search:");
        List<Block> patientRecords = blockchain.searchBlocksFast("PAT-001");
        System.out.println("Found " + patientRecords.size() + " records for PAT-001");
        
        // Search for specific condition
        System.out.println("\n2. Condition search:");
        List<Block> chestPainCases = blockchain.searchBlocks("chest pain", SearchLevel.INCLUDE_DATA);
        System.out.println("Found " + chestPainCases.size() + " records mentioning chest pain");
        
        // Search for doctor
        System.out.println("\n3. Doctor search:");
        List<Block> drSmithRecords = blockchain.searchBlocks("DR-SMITH", SearchLevel.FAST_ONLY);
        System.out.println("Found " + drSmithRecords.size() + " records from Dr. Smith");
        
        // Search by date range (would need additional implementation)
        System.out.println("\n4. Date-based search:");
        List<Block> recentRecords = blockchain.searchBlocks("2024-01", SearchLevel.INCLUDE_DATA);
        System.out.println("Found " + recentRecords.size() + " records from January 2024");
        
        // Category search
        System.out.println("\n5. All medical records:");
        List<Block> allMedical = blockchain.searchByCategory("MEDICAL");
        System.out.println("Total medical records: " + allMedical.size());
        
        // Email contact search
        System.out.println("\n6. Contact information search:");
        List<Block> contactRecords = blockchain.searchBlocks("@hospital.com", SearchLevel.INCLUDE_DATA);
        System.out.println("Found " + contactRecords.size() + " records with hospital contacts");
    }
}
```

### Financial Transaction System

```java
public class FinancialSearchExample {
    public static void main(String[] args) throws Exception {
        Blockchain blockchain = new Blockchain();
        
        // Setup
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(publicKey), "FinanceSystem");
        
        // Add financial records
        addFinancialRecords(blockchain, privateKey, publicKey);
        
        // Demonstrate financial search scenarios
        demonstrateFinancialSearch(blockchain);
    }
    
    private static void addFinancialRecords(Blockchain blockchain, PrivateKey privateKey, PublicKey publicKey) {
        // Project budget allocation
        blockchain.addBlockWithKeywords(
            "Project Alpha budget allocation approved. Amount: 250000 EUR. " +
            "Project ID: PROJ-ALPHA-2024. Budget period: Q1-2024. " +
            "Approved by: finance@company.com. Date: 2024-01-10. " +
            "Account: 1001-2024-ALPHA. Department: Engineering.",
            new String[]{"PROJ-ALPHA-2024", "Q1-2024", "BUDGET", "EUR", "ENGINEERING"},
            "FINANCE",
            privateKey, publicKey
        );
        
        // Invoice processing
        blockchain.addBlockWithKeywords(
            "Invoice INV-2024-001 processed. Vendor: TechCorp Ltd. " +
            "Amount: 15000 USD. Service: Cloud hosting services. " +
            "Payment due: 2024-02-15. PO: PO-2024-005. " +
            "Approved by: procurement@company.com",
            new String[]{"INV-2024-001", "TECHCORP", "USD", "CLOUD-HOSTING", "PO-2024-005"},
            "FINANCE",
            privateKey, publicKey
        );
        
        // Expense report
        blockchain.addBlockWithKeywords(
            "Employee expense report EXP-2024-001 submitted. " +
            "Employee: John Smith (john.smith@company.com). " +
            "Travel expenses: 2500 EUR. Conference: DevCon 2024. " +
            "Period: 2024-01-20 to 2024-01-25. Status: Approved.",
            new String[]{"EXP-2024-001", "JOHN-SMITH", "TRAVEL", "DEVCON", "APPROVED"},
            "FINANCE",
            privateKey, publicKey
        );
    }
    
    private static void demonstrateFinancialSearch(Blockchain blockchain) {
        System.out.println("=== FINANCIAL RECORDS SEARCH DEMO ===");
        
        // Search for specific project
        System.out.println("\n1. Project-specific search:");
        List<Block> alphaRecords = blockchain.searchBlocksFast("PROJ-ALPHA-2024");
        System.out.println("Found " + alphaRecords.size() + " records for Project Alpha");
        
        // Currency search
        System.out.println("\n2. Currency search:");
        List<Block> eurRecords = blockchain.searchBlocksFast("EUR");
        List<Block> usdRecords = blockchain.searchBlocksFast("USD");
        System.out.println("EUR transactions: " + eurRecords.size());
        System.out.println("USD transactions: " + usdRecords.size());
        
        // Amount search
        System.out.println("\n3. Large amount search:");
        List<Block> largeAmounts = blockchain.searchBlocks("250000", SearchLevel.INCLUDE_DATA);
        System.out.println("Found " + largeAmounts.size() + " records with amount 250000");
        
        // Vendor search
        System.out.println("\n4. Vendor search:");
        List<Block> vendorRecords = blockchain.searchBlocks("TechCorp", SearchLevel.INCLUDE_DATA);
        System.out.println("Found " + vendorRecords.size() + " records for TechCorp");
        
        // Employee search
        System.out.println("\n5. Employee expense search:");
        List<Block> employeeRecords = blockchain.searchBlocks("john.smith@company.com", SearchLevel.INCLUDE_DATA);
        System.out.println("Found " + employeeRecords.size() + " records for John Smith");
        
        // All financial records
        System.out.println("\n6. All financial records:");
        List<Block> allFinance = blockchain.searchByCategory("FINANCE");
        System.out.println("Total financial records: " + allFinance.size());
    }
}
```

## 🎯 Best Practices

### 1. Choose the Right Search Level

```java
// Use FAST_ONLY for:
// - Real-time search suggestions
// - Quick existence checks
// - High-frequency searches
List<Block> suggestions = blockchain.searchBlocksFast("API");

// Use INCLUDE_DATA for:
// - Standard search operations
// - Most user-initiated searches
// - Balanced performance needs
List<Block> standardSearch = blockchain.searchBlocks("patient data", SearchLevel.INCLUDE_DATA);

// Use EXHAUSTIVE_OFFCHAIN for:
// - Audit and compliance searches
// - Investigative procedures
// - Complete content analysis
List<Block> auditSearch = blockchain.searchBlocks("contract", SearchLevel.EXHAUSTIVE_OFFCHAIN);
```

### 2. Design Effective Keywords

```java
// Good keyword strategy
String[] effectiveKeywords = {
    "PROJECT-ALPHA",        // Specific project identifier
    "2024-Q1",             // Time period
    "BUDGET-APPROVED",     // Status indicator
    "FINANCE-DEPT",        // Department
    "EUR",                 // Currency
    "HIGH-PRIORITY"        // Priority level
};

// Poor keyword strategy
String[] poorKeywords = {
    "project",             // Too generic
    "money",               // Vague
    "important",           // Subjective
    "stuff"                // Meaningless
};
```

### 3. Use Consistent Categories

```java
// Define standard categories for your organization
public enum ContentCategory {
    MEDICAL("MEDICAL"),
    FINANCE("FINANCE"),
    TECHNICAL("TECHNICAL"),
    LEGAL("LEGAL"),
    HR("HR"),
    COMPLIANCE("COMPLIANCE"),
    OPERATIONS("OPERATIONS");
    
    private final String value;
    
    ContentCategory(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}

// Use consistently
blockchain.addBlockWithKeywords(data, keywords, ContentCategory.MEDICAL.getValue(), privateKey, publicKey);
```

### 4. Validate Search Terms

```java
public List<Block> safeSearch(String searchTerm, SearchLevel level) {
    // Always validate before searching
    if (!SearchValidator.isValidSearchTerm(searchTerm)) {
        System.out.println("Invalid search term: " + searchTerm);
        return Collections.emptyList();
    }
    
    return blockchain.searchBlocks(searchTerm, level);
}
```

### 5. Implement Search Caching

```java
public class SearchCache {
    private final Map<String, List<Block>> cache = new ConcurrentHashMap<>();
    private final int maxCacheSize = 1000;
    
    public List<Block> cachedSearch(String searchTerm, SearchLevel level) {
        String cacheKey = searchTerm.toLowerCase() + "_" + level.name();
        
        // Check cache first
        List<Block> cached = cache.get(cacheKey);
        if (cached != null) {
            return new ArrayList<>(cached); // Return copy
        }
        
        // Perform search
        List<Block> results = blockchain.searchBlocks(searchTerm, level);
        
        // Cache results (with size limit)
        if (cache.size() < maxCacheSize) {
            cache.put(cacheKey, new ArrayList<>(results));
        }
        
        return results;
    }
    
    public void clearCache() {
        cache.clear();
    }
}
```

### 6. Monitor Search Performance

```java
public class SearchMetrics {
    private final Map<SearchLevel, Long> searchTimes = new ConcurrentHashMap<>();
    private final AtomicLong searchCount = new AtomicLong(0);
    
    public List<Block> timedSearch(String searchTerm, SearchLevel level) {
        long startTime = System.nanoTime();
        List<Block> results = blockchain.searchBlocks(searchTerm, level);
        long duration = System.nanoTime() - startTime;
        
        // Record metrics
        searchTimes.merge(level, duration, Long::sum);
        searchCount.incrementAndGet();
        
        return results;
    }
    
    public void printMetrics() {
        System.out.println("Search Metrics:");
        System.out.println("Total searches: " + searchCount.get());
        searchTimes.forEach((level, totalTime) -> {
            double avgTime = totalTime / 1_000_000.0; // Convert to ms
            System.out.println(level + ": " + String.format("%.2f", avgTime) + "ms average");
        });
    }
}
```

---

For more information about the blockchain implementation, see:
- [API Guide](API_GUIDE.md) - Complete API reference
- [Examples](EXAMPLES.md) - Practical usage examples  
- [Technical Details](TECHNICAL_DETAILS.md) - Implementation details
- [Testing Guide](TESTING.md) - Testing procedures