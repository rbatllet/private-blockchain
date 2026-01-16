# Public/Private Keywords Technical Guide

## Overview

This guide covers the technical implementation of public and private keywords in the blockchain search system. The `public:` prefix allows fine-grained control over keyword visibility in encrypted blocks.

## Architecture

### Keyword Storage Model

```
Encrypted Block
├── manualKeywords (Public Metadata - Never Encrypted)
│   ├── "public:keyword1"     → Searchable WITHOUT password
│   ├── "public:keyword2"     → Searchable WITHOUT password
│   └── "public:keyword3"     → Searchable WITHOUT password
│
└── autoKeywords (Encrypted Metadata - Password Required)
    └── "encrypted_private_keywords" → Searchable ONLY with password
```

### Processing Flow

```
User Input: ["public:research", "funding", "confidential"]
                ↓
┌─────────────────────────────────────────────────────────────┐
│ Blockchain.processEncryptedBlockKeywords()                  │
│ - Separates keywords by "public:" prefix                   │
│ - Public keywords → manualKeywords (keep "public:" prefix) │
│ - Private keywords → autoKeywords (encrypted)              │
└─────────────────────────────────────────────────────────────┘
                ↓
┌─────────────────────────────────────────────────────────────┐
│ Block Storage                                              │
│ - manualKeywords: "public:research public:funding"         │
│ - autoKeywords: "ENCRYPTED:confidential"                   │
└─────────────────────────────────────────────────────────────┘
                ↓
┌─────────────────────────────────────────────────────────────┐
│ MetadataLayerManager.generatePublicMetadataLayer()         │
│ - Strips "public:" prefix for indexing                    │
│ - "research" → publicKeywordSet                            │
│ - Skips private keywords (already encrypted)               │
└─────────────────────────────────────────────────────────────┘
                ↓
┌─────────────────────────────────────────────────────────────┐
│ FastIndexSearch.indexBlock()                               │
│ - Strips "public:" prefix (second time, for safety)        │
│ - Indexes "research" in keywordIndex                       │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Details

### 1. Blockchain.processEncryptedBlockKeywords()

**Location**: `com.rbatllet.blockchain.core.Blockchain`
**Lines**: ~3024-3089

```java
private void processEncryptedBlockKeywords(
    Block block,
    String originalData,
    String[] manualKeywords,
    String category,
    String encryptionPassword
) {
    List<String> publicKeywords = new ArrayList<>();
    List<String> privateKeywords = new ArrayList<>();

    // Separate keywords by public: prefix
    for (String keyword : manualKeywords) {
        if (keyword.toLowerCase().startsWith("public:")) {
            // Keep the "public:" prefix for storage (required by isTermPublicInBlock)
            publicKeywords.add(keyword.toLowerCase());
        } else {
            privateKeywords.add(keyword.toLowerCase());
        }
    }

    // Store public keywords unencrypted in manualKeywords
    if (!publicKeywords.isEmpty()) {
        String publicKeywordString = String.join(" ", publicKeywords);
        block.setManualKeywords(publicKeywordString); // "public:research public:funding"
    }

    // Store private keywords encrypted in autoKeywords
    if (!privateKeywords.isEmpty()) {
        String privateKeywordString = String.join(" ", privateKeywords);
        String encryptedPrivateKeywords =
            SecureBlockEncryptionService.encryptToString(
                privateKeywordString,
                encryptionPassword
            );
        block.setAutoKeywords(encryptedPrivateKeywords); // ENCRYPTED
    }
}
```

**Key Points**:
- **Prefix Preserved**: `public:` prefix is kept during storage
- **Separation**: Public and private keywords stored in different fields
- **Encryption**: Private keywords encrypted with block password

### 2. MetadataLayerManager.generatePublicMetadataLayer()

**Location**: `com.rbatllet.blockchain.search.metadata.MetadataLayerManager`
**Lines**: ~190-250

```java
private PublicMetadata generatePublicMetadataLayer(
    Block block,
    Set<String> publicSearchTerms,
    Set<String> privateSearchTerms,
    String password
) {
    Set<String> publicKeywordSet = new HashSet<>();
    Set<String> privateKeywordSet = new HashSet<>();

    // Extract manual keywords
    if (block.getManualKeywords() != null) {
        String[] manualKeywords = block.getManualKeywords().split("\\s+");

        for (String keyword : manualKeywords) {
            String cleanKeyword = keyword.trim();

            // Check for explicit "public:" prefix
            if (cleanKeyword.toLowerCase().startsWith("public:")) {
                // Strip the prefix for indexing
                cleanKeyword = cleanKeyword.substring(7);
                publicKeywordSet.add(cleanKeyword); // "research"
            } else if (!block.isDataEncrypted()) {
                // Non-encrypted block: all keywords are public
                publicKeywordSet.add(cleanKeyword);
            } else {
                // Encrypted block without "public:" prefix = private
                // Skip here (already in autoKeywords encrypted)
                logger.debug("SKIPPING PRIVATE keyword: '{}'", cleanKeyword);
            }
        }
    }

    // Create PublicMetadata
    return new PublicMetadata(
        publicKeywordSet,           // ["research", "funding"]
        null,                       // No private terms in public layer
        block.getContentCategory(),
        // ... other fields
    );
}
```

**Key Points**:
- **Strip First**: Strips `public:` prefix when creating metadata
- **Non-Encrypted Blocks**: All keywords treated as public
- **Encrypted Blocks**: Only `public:` prefixed keywords in public layer

### 3. FastIndexSearch.indexBlock()

**Location**: `com.rbatllet.blockchain.search.strategy.FastIndexSearch`
**Lines**: ~50-85

```java
public void indexBlock(String blockHash, BlockMetadataLayers metadata) {
    PublicMetadata publicLayer = metadata.getPublicLayer();

    // Index keywords
    for (String keyword : publicLayer.getGeneralKeywords()) {
        // Strip "public:" prefix for intuitive searching
        String indexableKeyword = keyword.toLowerCase();
        if (indexableKeyword.startsWith("public:")) {
            indexableKeyword = indexableKeyword.substring("public:".length());
        }

        // Add to index
        keywordIndex.computeIfAbsent(indexableKeyword, k -> ConcurrentHashMap.newKeySet())
                   .add(blockHash);
    }
}
```

**Key Points**:
- **Double Strip**: Strips prefix again for safety
- **Natural Search**: Users search "research" not "public:research"

### 4. UserFriendlyEncryptionAPI.isTermPublicInBlock()

**Location**: `com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI`
**Lines**: ~5138-5180

```java
private boolean isTermPublicInBlock(Block block, String searchTerm) {
    // Check if the block has the public keyword marker
    String publicKeyword = "public:" + searchTerm.toLowerCase();

    // Check in manual keywords
    String manualKeywords = block.getManualKeywords();
    if (manualKeywords != null) {
        String[] keywords = manualKeywords.split("\\s+");
        for (String keyword : keywords) {
            String trimmedKeyword = keyword.trim();
            if (trimmedKeyword.equals(publicKeyword)) {
                return true; // Found "public:research"
            }
        }
    }

    return false;
}
```

**Key Points**:
- **Storage Format Check**: Looks for `public:research` in manualKeywords
- **Compatibility**: Works because prefix is preserved during storage

## Search Behavior

### Public Search (No Password)

```java
public List<Block> searchByTerms(String[] searchTerms, String password, int maxResults) {
    for (String term : searchTerms) {
        // Public term search (NO password)
        processPublicTermMatches(searchTerm.toLowerCase(), maxResults, resultConsumer);
    }
}
```

**Flow**:
```
User searches: "research"
    ↓
isTermPublicInBlock(block, "research")
    ↓
Looks for: "public:research" in manualKeywords
    ↓
Returns: Blocks with "public:research" keyword
```

### Secure Search (With Password)

```java
public List<Block> searchAndDecryptByTerms(String[] searchTerms, String password, int maxResults) {
    for (String term : searchTerms) {
        // Public term search
        processPublicTermMatches(searchTerm.toLowerCase(), maxResults, resultConsumer);

        // Private term search (WITH password)
        if (password != null) {
            processPrivateTermMatches(searchTerm, password, maxResults, resultConsumer);
        }
    }
}
```

**Flow**:
```
User searches: "funding" with password
    ↓
1. Public search: looks for "public:funding" in manualKeywords
2. Private search: decrypts autoKeywords, looks for "funding"
    ↓
Returns: Blocks with either public or private "funding"
```

## Security Guarantee

### Private Keywords Are NEVER Exposed

1. **Storage**: Private keywords encrypted in `autoKeywords`
2. **Indexing**: Private keywords NOT indexed in `FastIndexSearch.keywordIndex`
3. **Access**: Only via `EncryptedContentSearch` with correct password

### Attack Prevention

**Without this system**:
```
Attacker searches: "patient"
Finds: "patient-001-CONFIDENTIAL" (stored without prefix)
```

**With this system**:
```
Attacker searches: "patient" (no password)
Finds: NOTHING (only "public:patient" would match)

Legitimate user searches: "patient" with password
Finds: "patient-001-CONFIDENTIAL" (autoKeywords decrypted)
```

## API Reference

### Blockchain.addEncryptedBlockWithKeywords()

```java
public Block addEncryptedBlockWithKeywords(
    String data,
    String password,
    String[] manualKeywords,    // ["public:research", "funding"]
    String category,
    PrivateKey signingKey,
    PublicKey publicKey
)
```

### SearchSpecialistAPI

```java
// Public search (no fuzzy, only public: keywords)
public List<EnhancedSearchResult> searchPublic(String query, int maxResults)

// Secure search (with fuzzy, public + private keywords)
public List<EnhancedSearchResult> searchSecure(String query, String password, int maxResults)

// Comprehensive (public with fuzzy + encrypted)
public SearchResult searchComprehensive(String query, String password, int maxResults)
```

### UserFriendlyEncryptionAPI

```java
// Store with layers
public Block storeSearchableDataWithLayers(
    String data,
    String password,
    String[] publicTerms,       // Auto-prefixed with "public:"
    String[] privateTerms       // Stored encrypted
)

// Search public
public List<Block> searchByTerms(String[] terms, null, int maxResults)

// Search secure
public List<Block> searchAndDecryptByTerms(String[] terms, String password, int maxResults)
```

## Data Structures

### Block Entity

```java
@Entity
public class Block {
    @Column(name = "manual_keywords", length = 1000)
    private String manualKeywords;     // "public:research public:funding"

    @Column(name = "auto_keywords", length = 2000)
    private String autoKeywords;       // ENCRYPTED private keywords

    // ... other fields
}
```

### PublicMetadata

```java
public class PublicMetadata {
    private Set<String> generalKeywords;  // ["research", "funding"] (prefix stripped)
    // ... other fields
}
```

### FastIndexSearch

```java
public class FastIndexSearch {
    private Map<String, Set<String>> keywordIndex;  // "research" -> [blockHash1, blockHash2]
    // ... other fields
}
```

## Testing

### Unit Tests

- `BlockchainTest.testProcessEncryptedBlockKeywords()` - Verifies keyword separation
- `MetadataLayerManagerTest.testPublicMetadataGeneration()` - Verifies prefix stripping
- `FastIndexSearchTest.testIndexBlock()` - Verifies indexing

### Integration Tests

- `AdvancedPublicPrivateKeywordTest` - Comprehensive public/private testing
  - `testPublicPrivateKeywordCombinations()` - Mixed visibility blocks
  - `testMultiTermPublicPrivateMix()` - Multi-term searches
  - `testCrossDepartmentPublicSharing()` - Cross-department sharing

- `GranularTermVisibilityIntegrationTest` - Term visibility verification
  - `testMedicalRecordGranularPrivacy()` - Medical records privacy
  - `testFinancialDataGranularPrivacy()` - Financial data privacy

- `ComplexMultiPasswordSearchTest` - Multi-password scenarios
  - `testPublicTermsAccessibility()` - Public terms without password
  - `testConcurrentMultiPasswordSearches()` - Concurrent searches

## Common Issues

### Issue: Public keyword not found in public search

**Cause**: Keyword not prefixed with `public:` during storage

**Solution**:
```java
// Wrong
blockchain.addEncryptedBlockWithKeywords(data, password,
    new String[]{"research"}, category, key);

// Correct
blockchain.addEncryptedBlockWithKeywords(data, password,
    new String[]{"public:research"}, category, key);
```

### Issue: Private keyword found in public search

**Cause**: Fuzzy matching enabled for public search (should NOT happen)

**Verification**:
```java
// Check that enableFuzzy is false for public search
searchAPI.searchPublic(query, maxResults); // Uses enableFuzzy=false
```

### Issue: Prefix stripping not working

**Cause**: Prefix case mismatch

**Solution**:
```java
// Correct - prefix check is case-insensitive
if (keyword.toLowerCase().startsWith("public:")) {
    // ...
}

// Both "public:research" and "PUBLIC:research" work
```

## Best Practices

### For Developers

1. **Always use `public:` prefix** for public keywords in encrypted blocks
2. **Never strip prefix** before calling `addEncryptedBlockWithKeywords()`
3. **Test both search types**: public (no password) and secure (with password)
4. **Use `storeSearchableDataWithLayers()`** for explicit control

### For Users

1. **Mark classification terms as public**: `public:diabetes`, `public:finance`
2. **Keep identifiers private**: `patient-001`, `account-123` (no prefix)
3. **Use meaningful keywords**: `public:contract` not `public:c`
4. **Test searches**: Verify both public and secure searches work as expected

## References

- **Elasticsearch Best Practices**: Document/Field Level Security (DLS)
- **Related Classes**:
  - `Blockchain.processEncryptedBlockKeywords()`
  - `MetadataLayerManager.generatePublicMetadataLayer()`
  - `FastIndexSearch.indexBlock()`
  - `UserFriendlyEncryptionAPI.isTermPublicInBlock()`
  - `SearchSpecialistAPI.searchPublic()` / `searchSecure()`

- **Related Documentation**:
  - `FUZZY_MATCHING_TECHNICAL_GUIDE.md` - Fuzzy matching details
  - `SEARCH_FRAMEWORK_GUIDE.md` - Overall search architecture
  - `USER_FRIENDLY_SEARCH_GUIDE.md` - High-level search guide
