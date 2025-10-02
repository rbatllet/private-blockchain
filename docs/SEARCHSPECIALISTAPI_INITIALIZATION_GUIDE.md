# SearchSpecialistAPI Initialization Guide

## 📊 Current API Usage (v2.0+)

This guide shows how to properly initialize and use SearchSpecialistAPI.

## 📋 Problem: SearchSpecialistAPI Returns No Results

**Symptom:**
```java
// This pattern may not work as expected:
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey);
List<EnhancedSearchResult> results = searchAPI.searchSimple("medical");
// Results: 0 (even when data exists)
```

**Common Causes:**
- Incorrect parameter order or values
- Data not properly stored before search
- Password mismatch between storage and search

## ✅ CORRECT Usage Pattern

### Step 1: Store Searchable Data
```java
// Set up blockchain and data first
Blockchain blockchain = new Blockchain();
UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain);
KeyPair userKeys = dataAPI.createUser("test-user");
dataAPI.setDefaultCredentials("test-user", userKeys);

// Store data with searchable keywords
String[] keywords = {"medical", "patient", "diagnosis"};
dataAPI.storeSearchableData("Medical record data", "password123", keywords);
```

### Step 2: Create SearchSpecialistAPI

#### Standard Constructor
```java
// ✅ Create SearchSpecialistAPI with blockchain, password, and private key
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, "password123", userKeys.getPrivate());

// ⚡ PERFORMANCE: Constructor reuses blockchain's existing SearchFrameworkEngine
// ✅ READY: Ready to use immediately, no additional initialization needed

List<EnhancedSearchResult> results = searchAPI.searchSimple("medical");
System.out.println("Results: " + results.size()); // Will return > 0 if data exists
```

#### With Custom EncryptionConfig
```java
// ✅ ADVANCED: Use custom EncryptionConfig for specific security requirements
EncryptionConfig config = EncryptionConfig.createHighSecurityConfig();
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, "password123", userKeys.getPrivate(), config);

List<EnhancedSearchResult> results = searchAPI.searchSimple("medical");
```

## ⚠️ Common Issues

### Issue 1: Wrong Parameter Order
```java
// ❌ WRONG: Incorrect parameter order
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(password, blockchain, privateKey);
```

### Issue 2: Null Parameters
```java
// ❌ WRONG: Null parameters will cause IllegalArgumentException
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(null, password, privateKey);
```

### Issue 3: Critical Initialization Order Issue
```java
// ❌ WRONG: Storing searchable data BEFORE SearchSpecialistAPI initialization
// This causes blocks to be indexed with "public metadata only" mode
dataAPI.storeSearchableData("confidential medical data", password, keywords);
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey);
// Result: smartSearchWithPassword returns 0 results even though data exists

// ✅ CORRECT: Initialize SearchSpecialistAPI BEFORE storing searchable data
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey);
dataAPI.storeSearchableData("confidential medical data", password, keywords);
// Result: smartSearchWithPassword works correctly and finds the data
```

**Critical Rule:** Always initialize SearchSpecialistAPI or call `blockchain.initializeAdvancedSearch(password)` BEFORE creating any blocks with `storeSearchableData()` that you plan to search with `smartSearchWithPassword()` methods.

## 🔍 Troubleshooting Guide

### Problem: No Search Results
**Symptoms:**
- `searchAPI.searchSimple("term")` returns empty list
- No errors thrown, but results are always 0

**Diagnostic Steps:**
1. Verify data was stored with matching keywords before creating SearchSpecialistAPI
2. Check that the password used matches the one used to store the data
3. Ensure the private key has access to the stored data
4. Verify the blockchain contains the expected data

**Solution:**
```java
// Ensure correct parameter order and values
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey);
```

### Problem: IllegalStateException During Search
**Error Message:**
```
IllegalStateException: SearchSpecialistAPI is not initialized. 
Call initializeWithBlockchain() before performing search operations.
```

**Solutions:**
1. **If using preferred constructor:** This shouldn't happen - check constructor parameters are not null
2. **If using default constructor:** Use preferred constructor instead
3. **If using blockchain.getSearchSpecialistAPI():** Call `blockchain.initializeAdvancedSearch(password)` first

### Problem: IllegalArgumentException During Construction
**Common Error Messages:**
```
IllegalArgumentException: Blockchain cannot be null
IllegalArgumentException: Password cannot be null  
IllegalArgumentException: Private key cannot be null
IllegalArgumentException: EncryptionConfig cannot be null
```

**Solutions:**
- Ensure all constructor parameters are properly initialized
- Check that KeyPair generation succeeded: `KeyPair userKeys = dataAPI.createUser("username");`
- Verify blockchain instance is created: `Blockchain blockchain = new Blockchain();`

### Problem: Console Warnings About Direct Instantiation
**Warning Message:**
```
⚠️ SearchSpecialistAPI created directly. This is NOT recommended. 
Consider using blockchain.getSearchSpecialistAPI() instead. 
Direct instantiation may result in empty search results.
```

**Solution:**
Use the preferred constructor pattern instead of the default constructor:
```java
// Instead of this:
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI();

// Use this:
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey);
```

## 📚 Complete Working Example

```java
package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.config.EncryptionConfig;
import java.security.KeyPair;
import java.util.List;

public class SearchSpecialistAPIExample {
    
    public static void main(String[] args) {
        try {
            // 1. Setup blockchain and data
            Blockchain blockchain = new Blockchain();
            UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain);
            
            // 2. Create user and credentials
            KeyPair userKeys = dataAPI.createUser("example-user");
            dataAPI.setDefaultCredentials("example-user", userKeys);
            
            // 3. Store searchable data
            String password = "ExamplePassword123!";
            String[] keywords = {"medical", "patient", "diagnosis", "treatment"};
            dataAPI.storeSearchableData(
                "Medical record: Patient diagnosis and treatment plan", 
                password, 
                keywords
            );
            
            // 4. Initialize SearchSpecialistAPI (OPTIMIZED v2.0 - IMMEDIATE READY)
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate());
            
            // 4b. Alternative: Use custom EncryptionConfig for specific security requirements
            EncryptionConfig highSecConfig = EncryptionConfig.createHighSecurityConfig();
            SearchSpecialistAPI highSecSearchAPI = new SearchSpecialistAPI(
                blockchain, password, userKeys.getPrivate(), highSecConfig);
            
            // ⚡ PERFORMANCE NOTE: Constructor automatically reuses blockchain's SearchFrameworkEngine
            // 🔍 AUTOMATIC: blockchain.initializeAdvancedSearch(password) called internally
            
            // 5. Use the API immediately (ready to use, no need to check isReady())
            List<EnhancedSearchResult> results = searchAPI.searchSimple("medical");
            System.out.println("✅ Search results: " + results.size());
            
            // 6. Test different search methods
            List<EnhancedSearchResult> secureResults = searchAPI.searchSecure("patient", password, 10);
            System.out.println("✅ Secure search results: " + secureResults.size());
            
            // 7. Test intelligent search (adaptive strategy selection)
            List<EnhancedSearchResult> intelligentResults = searchAPI.searchIntelligent("diagnosis", password, 15);
            System.out.println("✅ Intelligent search results: " + intelligentResults.size());
            
            // 8. Test with custom configuration
            List<EnhancedSearchResult> highSecResults = highSecSearchAPI.searchSimple("treatment");
            System.out.println("✅ High security search results: " + highSecResults.size());
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Parameter Error: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.err.println("❌ Initialization Error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}```

## 📋 Constructor Reference

### Available Constructors (Actual Implementation)

#### 1. Default Constructor (NOT RECOMMENDED)
```java
public SearchSpecialistAPI()
```
- **Usage:** Creates isolated instance with warning
- **Warning Generated:** `"⚠️ SearchSpecialistAPI created directly. This is NOT recommended..."`
- **Requires:** Manual `initializeWithBlockchain()` call
- **Flag Set:** `isDirectlyInstantiated = true`

#### 2. Preferred Constructor (RECOMMENDED)
```java
public SearchSpecialistAPI(Blockchain blockchain, String password, PrivateKey privateKey)
```
- **Usage:** Auto-initializes with blockchain, immediately ready
- **Performance:** Uses blockchain's existing SearchFrameworkEngine (no double indexing)
- **Automatic:** Calls `blockchain.initializeAdvancedSearch(password)` internally
- **Ready:** `isInitialized = true` set automatically

#### 3. Advanced Constructor with Custom Config
```java
public SearchSpecialistAPI(Blockchain blockchain, String password, PrivateKey privateKey, EncryptionConfig config)
```
- **Usage:** Like preferred constructor but with custom security settings
- **Config Options:** HIGH_SECURITY, BALANCED, PERFORMANCE
- **Performance:** Same optimization as preferred constructor

#### 4. Config-Only Constructor (ADVANCED USERS)
```java
public SearchSpecialistAPI(EncryptionConfig config)
```
- **Usage:** Advanced constructor with custom encryption settings
- **Warning Generated:** Custom warning about needing `initializeWithBlockchain()`
- **Requires:** Manual initialization before use

#### 5. Internal Constructor (FRAMEWORK USE)
```java
SearchSpecialistAPI(boolean internal, SearchFrameworkEngine searchEngine)
```
- **Usage:** Package-private, used internally by blockchain
- **Performance:** Reuses existing SearchFrameworkEngine instance
- **Purpose:** Framework optimization, not for public use

#### 6. Legacy Internal Constructor (DEPRECATED)
```java
@Deprecated
SearchSpecialistAPI(boolean internal)
```
- **Status:** Deprecated, creates new SearchFrameworkEngine
- **Replacement:** Use constructor with SearchFrameworkEngine parameter

## 🎯 Key Takeaways

### Recommended Patterns (Ranked by Performance)

1. **BEST (Optimal Performance):**
   ```java
   SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey);
   ```
   - ⚡ Uses blockchain's existing SearchFrameworkEngine
   - 🔄 Automatic initialization with `blockchain.initializeAdvancedSearch(password)`
   - ✅ Ready to use immediately

2. **ADVANCED (Custom Security):**
   ```java
   EncryptionConfig config = EncryptionConfig.createHighSecurityConfig();
   SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey, config);
   ```
   - Same performance benefits as #1
   - 🔒 Custom security configuration

3. **ALTERNATIVE (Legacy Support):**
   ```java
   blockchain.initializeAdvancedSearch(password);
   SearchSpecialistAPI searchAPI = blockchain.getSearchSpecialistAPI();
   ```
   - Still supported and functional
   - May require `isReady()` check

### Performance Optimization Details

From the actual implementation:
```java
// Constructor logs this optimization:
logger.info("🔄 Using blockchain's existing SearchFrameworkEngine with {} blocks", 
           this.searchEngine.getSearchStats().getTotalBlocksIndexed());

// No need to re-index, the blockchain's engine is already indexed
logger.info("✅ SearchSpecialistAPI created and initialized with blockchain - {} blocks indexed, config: {}", 
           this.searchEngine.getSearchStats().getTotalBlocksIndexed(), config.getSecurityLevel());
```

### Validation and Error Handling

The implementation includes extensive parameter validation:
```java
// Actual validation code from implementation:
if (blockchain == null) {
    throw new IllegalArgumentException("Blockchain cannot be null");
}
if (password == null) {
    throw new IllegalArgumentException("Password cannot be null");
}
if (privateKey == null) {
    throw new IllegalArgumentException("Private key cannot be null");
}
if (config == null) {
    throw new IllegalArgumentException("EncryptionConfig cannot be null");
}
```

### Search Method Availability

After proper initialization, these search methods are available:
- `searchSimple(String query)` - Fast public metadata search (limit: 20)
- `searchSimple(String query, int maxResults)` - Fast public metadata with custom limit
- `searchSecure(String query, String password)` - Encrypted content search (limit: 20)
- `searchSecure(String query, String password, int maxResults)` - Encrypted with custom limit
- `searchIntelligent(String query, String password, int maxResults)` - Adaptive strategy
- `searchAdvanced(String query, String password, EncryptionConfig config, int maxResults)` - Full control

### Status Checking Methods

- `isReady()` - Returns `true` if initialized and ready for operations
- `getEncryptionConfig()` - Returns current encryption configuration

## 🔗 Related Documentation

- [SEARCHSPECIALISTAPI_IMPROVEMENTS_V2.md](SEARCHSPECIALISTAPI_IMPROVEMENTS_V2.md) - v2.0 performance improvements
- [SEARCHSPECIALISTAPI_INDEX.md](SEARCHSPECIALISTAPI_INDEX.md) - Central navigation
- [API_GUIDE.md](API_GUIDE.md) - Complete API reference
- [SEARCH_FRAMEWORK_GUIDE.md](SEARCH_FRAMEWORK_GUIDE.md) - Search framework overview
- [EXAMPLES.md](EXAMPLES.md) - More usage examples

---

**Last Updated:** September 9, 2025  
**Version:** 2.0.0 (Implementation-Faithful)  
**Author:** Development Team

**📋 Implementation Fidelity:**
- ✅ All constructor signatures match actual code
- ✅ Warning messages match exact implementation text
- ✅ Exception messages reflect actual validation code
- ✅ Performance optimizations documented from actual logs
- ✅ Method availability confirmed from implementation
- ✅ Parameter validation reflects actual checks