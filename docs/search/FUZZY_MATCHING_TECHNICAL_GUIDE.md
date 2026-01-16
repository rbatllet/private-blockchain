# Fuzzy Matching Technical Guide

## Overview

Fuzzy matching is implemented in the search system to allow finding keywords that are similar but not exactly the same. This guide covers the technical implementation, algorithm details, and security considerations.

## Security Policy

### Fuzzy Matching by Search Type

| Search Method | Fuzzy Enabled | Security Rationale |
|---------------|---------------|-------------------|
| `searchPublic()` | ❌ No | Prevents privacy leaks through similar keywords |
| `searchSecure()` | ✅ Yes | Allows authenticated users to find typos/variations |
| `searchComprehensive()` | ✅ Yes | Combines public (with fuzzy) + encrypted search |

**Key Principle**: Fuzzy matching is only enabled for authenticated searches to prevent accidental exposure of private keywords through similarity matches.

### Why Disable Fuzzy for Public Search?

Without this restriction, a public search for "salary" could accidentally match:
- "salary-admin-access" (private keyword)
- "salary-confidential" (private keyword)
- "salary-2024-internal" (private keyword)

Even with 30% edit distance threshold, similar keywords could leak sensitive information.

## Algorithm Implementation

### Location
- **Class**: `FastIndexSearch`
- **Method**: `searchFast(String query, int maxResults, boolean enableFuzzy)`
- **Package**: `com.rbatllet.blockchain.search.strategy`

### Levenshtein Edit Distance

The algorithm uses Levenshtein edit distance to calculate similarity between strings:

```java
private int calculateEditDistance(String s1, String s2) {
    int[][] dp = new int[s1.length() + 1][s2.length() + 1];

    for (int i = 0; i <= s1.length(); i++) {
        dp[i][0] = i;
    }
    for (int j = 0; j <= s2.length(); j++) {
        dp[0][j] = j;
    }

    for (int i = 1; i <= s1.length(); i++) {
        for (int j = 1; j <= s2.length(); j++) {
            if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                dp[i][j] = dp[i - 1][j - 1];
            } else {
                dp[i][j] = 1 + Math.min(
                    dp[i - 1][j],    // Deletion
                    Math.min(
                        dp[i][j - 1],    // Insertion
                        dp[i - 1][j - 1] // Substitution
                    )
                );
            }
        }
    }

    return dp[s1.length()][s2.length()];
}
```

### Matching Logic

```java
private boolean isFuzzyMatch(String query, String indexed, boolean enableFuzzy) {
    // Exact match already handled separately
    if (query.equals(indexed)) {
        return false;
    }

    // SECURITY: Fuzzy matching only for authenticated searches
    if (!enableFuzzy) {
        return false;
    }

    // Contains check (one word contains the other)
    if (indexed.contains(query) || query.contains(indexed)) {
        return true;
    }

    // Edit distance check for words > 3 characters
    if (query.length() > 3 && indexed.length() > 3) {
        int editDistance = calculateEditDistance(query, indexed);
        int maxLength = Math.max(query.length(), indexed.length());
        return (double) editDistance / maxLength < 0.3; // 30% threshold
    }

    return false;
}
```

### Fuzzy Score Calculation

```java
private double calculateFuzzyScore(String query, String indexed) {
    // Contains match gets higher score
    if (indexed.contains(query) || query.contains(indexed)) {
        return 1.5;
    }

    // Edit distance based score
    if (query.length() > 3 && indexed.length() > 3) {
        int editDistance = calculateEditDistance(query, indexed);
        int maxLength = Math.max(query.length(), indexed.length());
        return 1.0 - ((double) editDistance / maxLength);
    }

    return 0.5; // Default fuzzy score
}
```

## Matching Rules

### When Fuzzy Match Occurs

1. **Contains Relationship** (Priority 1)
   - `"cardio"` matches `"cardiology"`
   - `"admin"` matches `"administrator"`

2. **Edit Distance < 30%** (Priority 2)
   - Only for words > 3 characters
   - `"diabetis"` matches `"diabetes"` (1/8 = 12.5% difference)
   - `"finace"` matches `"finance"` (1/7 = 14.3% difference)

### When Fuzzy Match Does NOT Occur

1. **Short Words (≤3 characters)**
   - `"abc"` vs `"abd"` - NO match (too short)

2. **Very Different Words (>30% difference)**
   - `"financial"` vs `"medical"` - NO match
   - `"confidential"` vs `"public"` - NO match

3. **Fuzzy Disabled**
   - `searchPublic()` - NO fuzzy matches
   - `searchSecure()` - fuzzy matches enabled

## Scoring System

```java
// Exact match (handled before fuzzy)
score += 3.0

// Fuzzy contains match
score += 1.5

// Fuzzy edit distance match
score += (1.0 - editDistancePercentage)

// Default fuzzy match
score += 0.5
```

## Search Flow

### Public Search (No Fuzzy)
```
User Query → Parse Query → Find Exact Matches → Return Results
                          ↓
                    (No fuzzy matching)
```

### Secure Search (With Fuzzy)
```
User Query → Parse Query → Find Exact Matches → score += 3.0
                          ↓
                    Find Fuzzy Matches → score += 0.5-1.5
                          ↓
                    Sort by Score → Return Results
```

## API Reference

### FastIndexSearch

```java
// Public search (no fuzzy)
public List<FastSearchResult> searchFast(String query, int maxResults) {
    return searchFast(query, maxResults, false); // enableFuzzy = false
}

// Secure search (with fuzzy)
public List<FastSearchResult> searchFast(String query, int maxResults, boolean enableFuzzy) {
    // ... implementation
}
```

### SearchSpecialistAPI

```java
// Public search - no fuzzy
public List<EnhancedSearchResult> searchPublic(String query, int maxResults) {
    // Uses FastIndexSearch with enableFuzzy = false
}

// Secure search - with fuzzy (via searchComprehensive)
public List<EnhancedSearchResult> searchSecure(String query, String password, int maxResults) {
    // Uses searchComprehensive with fuzzy enabled
}
```

### SearchFrameworkEngine

```java
// Comprehensive search - combines public (with fuzzy) + encrypted
public SearchResult searchComprehensive(String query, String password, int maxResults) {
    // 1. Public search with fuzzy matching enabled
    List<FastSearchResult> publicResults = strategyRouter
        .getFastIndexSearch()
        .searchFast(query, maxResults, true); // enableFuzzy = true

    // 2. Encrypted content search
    List<EncryptedSearchResult> encryptedResults = strategyRouter
        .getEncryptedContentSearch()
        .searchEncryptedContent(query, password, maxResults);

    // 3. Combine and deduplicate
    // ...
}
```

## Performance Considerations

### Time Complexity
- **Edit Distance Calculation**: O(m × n) where m, n are string lengths
- **Fuzzy Search**: O(k × m × n) where k is number of indexed keywords

### Optimization
- Early termination for short strings (≤3 characters)
- Cached results for repeated queries
- Indexed keywords stored in `ConcurrentHashMap` for fast lookup

### Typical Performance
| Operation | Time |
|-----------|------|
| Exact match only | ~10-20ms |
| With fuzzy matching | ~15-30ms |

## Security Considerations

### Private Keyword Protection

Private keywords are NEVER exposed to fuzzy matching in public search because:

1. **Storage**: Private keywords stored encrypted in `autoKeywords` field
2. **Indexing**: Private keywords NOT indexed in `FastIndexSearch.keywordIndex`
3. **Access**: Only accessible via `EncryptedContentSearch` with correct password

### Attack Scenarios Prevented

1. **Brute Force via Typos**: Attacker cannot try variations of private keywords
2. **Similarity Attacks**: "salary" won't match "salary-confidential"
3. **Partial Match Attacks**: "patient" won't match "patient-001-CONFIDENTIAL"

## Testing

### Unit Tests
- `FastIndexSearchRobustnessTest.testIsFuzzyMatchEdgeCases()`
- Tests exact match, contains match, edit distance match

### Integration Tests
- `AdvancedPublicPrivateKeywordTest` - Verifies public/private separation
- `ComplexMultiPasswordSearchTest` - Verifies concurrent multi-password searches

### Security Tests
- `GranularTermVisibilityIntegrationTest` - Verifies public terms accessible without password
- `SearchCompatibilityTest` - Verifies backward compatibility

## References

- **Elasticsearch Best Practices**: Document/Field Level Security
- **Levenshtein Distance**: https://en.wikipedia.org/wiki/Levenshtein_distance
- **Related Classes**:
  - `FastIndexSearch` - Main search implementation
  - `SearchSpecialistAPI` - Public API
  - `SearchFrameworkEngine` - Orchestration
  - `EncryptedContentSearch` - Private keyword search
