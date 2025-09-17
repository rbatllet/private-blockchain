# Advanced Search Guide

## 🔍 Overview

The UserFriendlyEncryptionAPI provides powerful search capabilities that allow you to find data across your blockchain while maintaining privacy and security. This guide covers all search features from basic term searches to advanced AI-powered analysis.

## 🎯 Search Strategy Overview

The API implements a **two-tier search strategy** for maximum flexibility:

1. **Public Search** - Searches metadata and public information without requiring passwords
2. **Private Search** - Includes encrypted content using provided passwords for decryption

```java
// Public search (metadata only)
List<Block> publicResults = api.searchByTerms(new String[]{"medical"}, null, 10);

// Private search (includes encrypted content)
List<Block> privateResults = api.searchByTerms(new String[]{"medical"}, password, 10);
```

## 🔎 Basic Search Methods

### Simple Term Search

The most basic search functionality looks for terms in block metadata and content:

```java
// Search for single term
String[] terms = {"diabetes"};
List<Block> results = api.searchByTerms(terms, password, 10);

// Search for multiple terms (AND logic)
String[] multipleTerms = {"patient", "diabetes", "treatment"};
List<Block> results = api.searchByTerms(multipleTerms, password, 20);

// Search with no password limit (public metadata only)
List<Block> publicResults = api.searchByTerms(terms, null, 10);
```

### Search with Decryption

To search encrypted content and immediately decrypt results:

```java
String[] searchTerms = {"diagnosis", "medication"};
List<Block> encryptedResults = api.searchAndDecryptByTerms(searchTerms, password, 15);

// Results are automatically decrypted and ready to use
for (Block block : encryptedResults) {
    String decryptedData = block.getData(); // Already decrypted
    System.out.println("Found: " + decryptedData);
}
```

## 🚀 Advanced Search Features

### Comprehensive Search

For searching across all available data sources:

```java
// Search everything (respects access permissions)
List<Block> everything = api.searchEverything("medical records");

// Search everything with password (includes encrypted content)
List<Block> everythingWithAuth = api.searchEverythingWithPassword("patient data", password);
```

### Advanced Search with Criteria

The most powerful search method accepts complex search criteria:

```java
Map<String, Object> searchCriteria = new HashMap<>();

// Basic search parameters
searchCriteria.put("keywords", "diabetes treatment protocol");
searchCriteria.put("maxResults", 50);
searchCriteria.put("includeEncrypted", true);

// Time-based filtering
searchCriteria.put("startDate", LocalDateTime.of(2024, 1, 1, 0, 0));
searchCriteria.put("endDate", LocalDateTime.of(2024, 12, 31, 23, 59));

// Category and type filtering
searchCriteria.put("categories", Set.of("medical", "clinical"));
searchCriteria.put("dataTypes", Set.of("patient-record", "treatment-plan"));

// Content analysis options
searchCriteria.put("enableKeywordExtraction", true);
searchCriteria.put("confidenceThreshold", 0.75);

// Execute advanced search
AdvancedSearchResult result = api.performAdvancedSearch(searchCriteria, password, 100);

// Process results
System.out.println("📊 Search Statistics:");
System.out.println("Total Results: " + result.getTotalResults());
System.out.println("Execution Time: " + result.getExecutionTimeMs() + "ms");
System.out.println("Confidence Score: " + result.getOverallConfidence());

for (Block block : result.getBlocks()) {
    System.out.println("✅ Found: " + block.getId());
}
```

## 🎯 Identifier-Based Search

### Find by Specific Identifiers

When you need to find data with specific identifiers:

```java
// Find records by exact identifier
List<Block> patientRecords = api.findRecordsByIdentifier("PATIENT-001");

// Find multiple identifiers
Set<String> identifiers = Set.of("TXN-2024-001", "TXN-2024-002", "TXN-2024-003");
List<Block> transactions = identifiers.stream()
    .flatMap(id -> api.findRecordsByIdentifier(id).stream())
    .collect(Collectors.toList());
```

### Smart Search

AI-powered search that understands context and relationships:

```java
// Smart search with natural language
String naturalQuery = "Find all diabetes patients treated with insulin in 2024";
List<Block> smartResults = api.smartSearch(naturalQuery, password, 25);

// Advanced smart search across all data types
List<Block> advancedResults = api.smartAdvancedSearch(
    "cardiac surgery recovery protocols", password, 30);
```

## 🧠 AI-Powered Content Analysis

### Keyword Extraction

Extract meaningful keywords from content for better searchability:

```java
String medicalText = """
    Patient presents with Type 2 Diabetes Mellitus, requiring
    immediate intervention with Metformin therapy and dietary modifications.
    Blood glucose levels elevated at 180 mg/dL.
    """;

List<String> keywords = api.extractKeywords(medicalText);
System.out.println("📝 Extracted Keywords: " + keywords);
// Output: [diabetes, metformin, blood glucose, therapy, dietary, intervention]
```

### Content Classification

Automatically classify content into categories:

```java
// Analyze and classify content
Map<String, Object> analysisOptions = Map.of(
    "extractKeywords", true,
    "classifyContent", true,
    "confidenceThreshold", 0.8
);

ContentAnalysisResult analysis = api.analyzeContent(medicalText, analysisOptions);

System.out.println("📋 Content Categories: " + analysis.getCategories());
System.out.println("🔑 Keywords: " + analysis.getKeywords());
System.out.println("📊 Confidence: " + analysis.getConfidence());
```

## 🔧 Search Configuration and Optimization

### Search Performance Tuning

Configure search behavior for optimal performance:

```java
// Configure search cache for better performance
SearchCacheConfig cacheConfig = new SearchCacheConfig();
cacheConfig.setEnabled(true);
cacheConfig.setMaxCacheSize(1000);
cacheConfig.setCacheTtlMinutes(30);

api.configureSearchCache(cacheConfig);

// Performance-optimized search
Map<String, Object> performanceOptions = Map.of(
    "useCache", true,
    "enableParallelSearch", true,
    "maxExecutionTimeMs", 5000,
    "enableResultRanking", true
);

List<Block> optimizedResults = api.searchWithOptions(
    "patient medical records", password, 50, performanceOptions);
```

### Search Result Formatting

Format search results for different use cases:

```java
List<Block> searchResults = api.searchByTerms(new String[]{"diabetes"}, password, 10);

// Format for medical category
String medicalFormat = api.formatSearchResults("medical", searchResults);
System.out.println("🏥 Medical Format:\n" + medicalFormat);

// Format for financial category
String financialFormat = api.formatSearchResults("financial", searchResults);
System.out.println("💰 Financial Format:\n" + financialFormat);

// Custom formatting
String customFormat = searchResults.stream()
    .map(block -> String.format("📦 Block #%d: %s (Created: %s)", 
                                block.getBlockNumber(), 
                                block.getId(), 
                                block.getTimestamp()))
    .collect(Collectors.joining("\n"));
```

## 🔐 Security and Privacy in Search

### Access Control

Implement secure search with proper access controls:

```java
public class SecureSearchManager {
    private final UserFriendlyEncryptionAPI api;
    private final Map<String, Set<String>> userPermissions;
    
    public List<Block> secureSearch(String userId, String query, String password) {
        // Verify user permissions
        if (!hasSearchPermission(userId)) {
            throw new SecurityException("User not authorized for search operations");
        }
        
        // Perform search with user's access level
        List<Block> results = api.searchEverythingWithPassword(query, password);
        
        // Filter results based on user permissions
        return results.stream()
            .filter(block -> userCanAccess(userId, block))
            .collect(Collectors.toList());
    }
    
    private boolean userCanAccess(String userId, Block block) {
        Set<String> userCategories = userPermissions.get(userId);
        String blockCategory = extractCategory(block);
        return userCategories != null && userCategories.contains(blockCategory);
    }
}
```

### Audit Search Operations

Log search activities for security monitoring:

```java
public void auditedSearch(String userId, String query) {
    long startTime = System.currentTimeMillis();
    
    try {
        List<Block> results = api.searchEverything(query);
        long duration = System.currentTimeMillis() - startTime;
        
        // Log successful search
        logger.info("Search performed - User: {}, Query: [REDACTED], Results: {}, Duration: {}ms", 
                   userId, results.size(), duration);
        
    } catch (Exception e) {
        // Log failed search
        logger.warn("Search failed - User: {}, Error: {}", userId, e.getMessage());
    }
}
```

## 📊 Search Analytics and Monitoring

### Search Metrics Collection

Track search performance and usage patterns:

```java
public class SearchAnalytics {
    
    public void analyzeSearchPerformance() {
        // Collect search metrics
        SearchMetrics metrics = api.getSearchMetrics();
        SearchMetrics.PerformanceSnapshot snapshot = metrics.getPerformanceSnapshot();
        
        System.out.println("📊 Search Performance Report:");
        System.out.println("Total Searches: " + snapshot.getTotalSearches());
        System.out.println("Average Response Time: " + snapshot.getAverageDuration() + "ms");
        System.out.println("Cache Hit Rate: " + snapshot.getCacheHitRate() + "%");
        System.out.println("Runtime: " + snapshot.getRuntimeMinutes() + " minutes");
        System.out.println("Search Rate: " + snapshot.getRecentSearchRate() + " searches/min");
        
        // Enhanced analysis with PerformanceSnapshot
        String mostActive = snapshot.getMostActiveSearchType();
        if (mostActive != null) {
            System.out.println("Most Used Search Type: " + mostActive);
        }
        
        // Search type distribution
        Map<String, Long> typeCounts = snapshot.getSearchTypeCounts();
        if (!typeCounts.isEmpty()) {
            System.out.println("\n🔍 Search Type Distribution:");
            typeCounts.forEach((type, count) -> 
                System.out.println("  " + type + ": " + count + " searches"));
        }
        
        // Data validation and summary
        if (snapshot.hasValidData()) {
            System.out.println("\n📋 " + snapshot.getSummary());
        } else {
            System.out.println("⚠️  Insufficient data for analysis");
        }
        
        // Performance recommendations with enhanced metrics
        if (snapshot.getAverageDuration() > 2000) {
            System.out.println("⚠️  Recommendation: Enable search caching to improve performance");
        }
        
        if (snapshot.getCacheHitRate() < 50) {
            System.out.println("⚠️  Recommendation: Increase cache size or TTL");
        }
        
        if (snapshot.getRecentSearchRate() > 10) {
            System.out.println("💡 Info: High search activity detected (" + 
                             snapshot.getRecentSearchRate() + " searches/min)");
        }
    }
    
    public void generateDetailedReport() {
        SearchMetrics.PerformanceSnapshot snapshot = api.getSearchMetrics().getPerformanceSnapshot();
        
        if (!snapshot.hasValidData()) {
            System.out.println("❌ Cannot generate report - insufficient data");
            return;
        }
        
        System.out.println("📈 Detailed Performance Analysis");
        System.out.println("================================");
        System.out.println(snapshot.getSummary());
        System.out.println("\n🎯 Key Insights:");
        
        // Performance analysis
        double avgDuration = snapshot.getAverageDuration();
        if (avgDuration < 100) {
            System.out.println("✅ Excellent response time (" + avgDuration + "ms)");
        } else if (avgDuration < 500) {
            System.out.println("🟡 Good response time (" + avgDuration + "ms)");
        } else {
            System.out.println("🔴 Poor response time (" + avgDuration + "ms) - optimization needed");
        }
        
        // Cache efficiency
        double cacheRate = snapshot.getCacheHitRate();
        if (cacheRate > 80) {
            System.out.println("✅ Excellent cache performance (" + cacheRate + "%)");
        } else if (cacheRate > 50) {
            System.out.println("🟡 Moderate cache performance (" + cacheRate + "%)");  
        } else {
            System.out.println("🔴 Poor cache performance (" + cacheRate + "%) - review cache strategy");
        }
    }
}
```

## 🔍 Practical Search Examples

### Medical Records Search

```java
public class MedicalRecordsSearch {
    
    public void searchPatientHistory(String patientId, String condition) {
        // Search for specific patient's medical history
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("keywords", condition + " " + patientId);
        criteria.put("categories", Set.of("medical", "patient-records"));
        criteria.put("includeEncrypted", true);
        
        AdvancedSearchResult results = api.performAdvancedSearch(
            criteria, medicalPassword, 50);
        
        System.out.println("🏥 Medical Records for Patient " + patientId + ":");
        for (Block record : results.getBlocks()) {
            String data = api.retrieveSecret(record.getId(), medicalPassword);
            System.out.println("📋 " + record.getTimestamp() + ": " + data.substring(0, 100) + "...");
        }
    }
    
    public void searchBySymptoms(String[] symptoms) {
        // Find patients with similar symptoms
        String symptomsQuery = String.join(" OR ", symptoms);
        
        List<Block> similarCases = api.smartSearch(
            "patients with symptoms: " + symptomsQuery, medicalPassword, 25);
        
        // Group by diagnosis for pattern analysis
        Map<String, List<Block>> diagnosisGroups = similarCases.stream()
            .collect(Collectors.groupingBy(this::extractDiagnosis));
        
        diagnosisGroups.forEach((diagnosis, cases) -> {
            System.out.println("🔍 Diagnosis: " + diagnosis + " (" + cases.size() + " cases)");
        });
    }
}
```

### Financial Transaction Search

```java
public class FinancialSearch {
    
    public void searchTransactionsByAmount(double minAmount, double maxAmount) {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("keywords", "transaction amount");
        criteria.put("categories", Set.of("financial", "transactions"));
        criteria.put("customFilters", Map.of(
            "amountRange", Map.of("min", minAmount, "max", maxAmount)
        ));
        
        AdvancedSearchResult results = api.performAdvancedSearch(
            criteria, financialPassword, 100);
        
        System.out.println("💰 Transactions between $" + minAmount + " and $" + maxAmount + ":");
        
        double totalAmount = 0;
        for (Block transaction : results.getBlocks()) {
            String data = api.retrieveSecret(transaction.getId(), financialPassword);
            double amount = extractTransactionAmount(data);
            totalAmount += amount;
            
            System.out.println("💳 " + transaction.getTimestamp() + ": $" + amount);
        }
        
        System.out.println("📊 Total Amount: $" + totalAmount);
    }
}
```

### Research Data Search

```java
public class ResearchDataSearch {
    
    public void searchByResearchTopic(String topic, LocalDateTime startDate) {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("keywords", topic);
        criteria.put("categories", Set.of("research", "scientific"));
        criteria.put("startDate", startDate);
        criteria.put("confidenceThreshold", 0.8);
        
        AdvancedSearchResult results = api.performAdvancedSearch(
            criteria, researchPassword, 75);
        
        // Analyze research trends
        Map<String, Integer> yearlyDistribution = results.getBlocks().stream()
            .collect(Collectors.groupingBy(
                block -> String.valueOf(block.getTimestamp().getYear()),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        System.out.println("📊 Research on '" + topic + "' by year:");
        yearlyDistribution.forEach((year, count) -> 
            System.out.println("📅 " + year + ": " + count + " publications"));
    }
}
```

## ⚠️ Search Best Practices

### Performance Optimization

```java
// ✅ GOOD: Limit search scope
Map<String, Object> optimizedCriteria = Map.of(
    "keywords", "specific terms",
    "maxResults", 25,              // Reasonable limit
    "categories", Set.of("medical"), // Specific category
    "startDate", recentDate        // Time constraint
);

// ❌ AVOID: Open-ended searches
Map<String, Object> inefficientCriteria = Map.of(
    "keywords", "*",               // Too broad
    "maxResults", 10000,          // Too many results
    "includeEncrypted", true      // Without constraints
);
```

### Security Considerations

```java
// ✅ SECURE: Validate search input
public List<Block> secureSearch(String userInput, String password) {
    // Sanitize input
    String sanitizedInput = userInput.replaceAll("[^\\w\\s-]", "");
    
    if (sanitizedInput.length() > 100) {
        throw new IllegalArgumentException("Search query too long");
    }
    
    // Log search (without sensitive data)
    logger.info("Search performed: {} characters", sanitizedInput.length());
    
    return api.searchByTerms(sanitizedInput.split("\\s+"), password, 50);
}

// ❌ INSECURE: Direct user input without validation
public List<Block> unsecureSearch(String userInput, String password) {
    return api.searchByTerms(userInput.split("\\s+"), password, 1000);
}
```

## 📚 Additional Resources

- [Getting Started Guide](GETTING_STARTED.md) - Basic usage and quick start examples
- [Security Best Practices](SECURITY_GUIDE.md) - Search security guidelines and access control
- [Key Management Guide](KEY_MANAGEMENT_GUIDE.md) - Managing search credentials and passwords
- [Troubleshooting Guide](TROUBLESHOOTING_GUIDE.md) - Search performance and error solutions
- [API Reference](API_GUIDE.md) - Complete search method documentation
- [Examples Guide](EXAMPLES.md) - Real-world search implementation examples
- [Technical Details](TECHNICAL_DETAILS.md) - Search architecture and implementation details