# SearchSpecialistAPI Initialization Guide

## ⚠️ IMPORTANT: Common Initialization Issues

This guide addresses the most common problems developers encounter when using SearchSpecialistAPI and provides solutions to prevent initialization failures.

## 📋 Problem: SearchSpecialistAPI Returns No Results

**Symptom:**
```java
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI();
List<EnhancedSearchResult> results = searchAPI.searchSimple("medical");
// Results: 0 (even when data exists)
```

**Root Cause:**
Creating `SearchSpecialistAPI` directly bypasses the blockchain's initialization process, resulting in an isolated instance that cannot access blockchain data.

## ✅ CORRECT Initialization Pattern

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

### Step 2: Initialize SearchSpecialistAPI CORRECTLY - NEW APPROACH
```java
// ✅ BEST: Use the improved constructor (RECOMMENDED)
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, "password123", userKeys.getPrivate());

// Optional: Use custom EncryptionConfig for specific security requirements
EncryptionConfig config = EncryptionConfig.createHighSecurityConfig();
SearchSpecialistAPI highSecSearchAPI = new SearchSpecialistAPI(blockchain, "password123", userKeys.getPrivate(), config);

// That's it! The API is ready to use immediately
List<EnhancedSearchResult> results = searchAPI.searchSimple("medical");
System.out.println("Results: " + results.size()); // Should return > 0
```

### Step 2 Alternative: Initialize through blockchain (OLD APPROACH)
```java
// ✅ CORRECT: Initialize through blockchain (still works)
blockchain.initializeAdvancedSearch("password123");
SearchSpecialistAPI searchAPI = blockchain.getSearchSpecialistAPI();

// Check if additional initialization is needed
if (!searchAPI.isReady()) {
    searchAPI.initializeWithBlockchain(blockchain, "password123", userKeys.getPrivate());
}

// Verify it's ready
if (!searchAPI.isReady()) {
    throw new IllegalStateException("SearchSpecialistAPI failed to initialize");
}
```

## ❌ INCORRECT Patterns (Avoid These)

### Anti-pattern 1: Direct Instantiation
```java
// ❌ WRONG - Creates isolated instance
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI();
searchAPI.initializeWithBlockchain(blockchain, "password123", privateKey);
// This will likely fail or return no results
```

### Anti-pattern 2: Missing Blockchain Initialization
```java
// ❌ WRONG - Skips blockchain initialization
SearchSpecialistAPI searchAPI = blockchain.getSearchSpecialistAPI();
// Missing: blockchain.initializeAdvancedSearch("password123");
```

### Anti-pattern 3: Wrong Parameter Order
```java
// ❌ WRONG - Incorrect parameter sequence
searchAPI.initializeWithBlockchain(privateKey, blockchain, "password123");
```

## 🔍 Troubleshooting Guide

### Problem: No Search Results
**Symptoms:**
- `searchAPI.searchSimple("term")` returns empty list
- No errors thrown, but results are always 0

**Solutions:**
1. Verify `blockchain.initializeAdvancedSearch(password)` was called
2. Check that data was stored with matching keywords
3. Ensure you're using `blockchain.getSearchSpecialistAPI()` not `new SearchSpecialistAPI()`

### Problem: IllegalStateException
**Symptoms:**
- "SearchSpecialistAPI not initialized" exception
- Methods fail with initialization errors

**Solutions:**
1. Call `blockchain.initializeAdvancedSearch(password)` first
2. Check `isReady()` before using the API
3. Verify the password matches the one used to store data

### Problem: NullPointerException
**Symptoms:**
- NPE when calling search methods
- Blockchain or API instances are null

**Solutions:**
1. Ensure blockchain is properly initialized
2. Check that `getSearchSpecialistAPI()` returns non-null
3. Verify user keys are properly generated

## 📚 Complete Working Example

```java
package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
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
            
            // 4. Initialize SearchSpecialistAPI (SIMPLE AND CORRECT)
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate());
            
            // 4b. Optional: Use custom EncryptionConfig for specific security requirements
            EncryptionConfig highSecConfig = EncryptionConfig.createHighSecurityConfig();
            SearchSpecialistAPI highSecSearchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), highSecConfig);
            
            // 5. Use the API immediately (no need to check isReady())
            List<EnhancedSearchResult> results = searchAPI.searchSimple("medical");
            System.out.println("✅ Search results: " + results.size());
            
            // 6. Test different search methods
            List<EnhancedSearchResult> secureResults = searchAPI.searchSecure("patient", password, 10);
            System.out.println("✅ Secure search results: " + secureResults.size());
            
            // 7. Test with custom configuration
            List<EnhancedSearchResult> highSecResults = highSecSearchAPI.searchSimple("treatment");
            System.out.println("✅ High security search results: " + highSecResults.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## 🎯 Key Takeaways

1. **BEST:** Use `new SearchSpecialistAPI(blockchain, password, privateKey)` for immediate initialization
2. **ADVANCED:** Use `new SearchSpecialistAPI(blockchain, password, privateKey, encryptionConfig)` for custom security
3. **ALTERNATIVE:** Use `blockchain.getSearchSpecialistAPI()` with proper initialization
4. **NEVER:** Use `new SearchSpecialistAPI()` without parameters
5. **Use the same password** for data storage and search initialization
6. **Store data with proper keywords** for search to work
7. **Choose appropriate EncryptionConfig** for your security requirements

## 📝 Recommended API Improvements

For future versions, consider these improvements:

1. **Factory Method:**
   ```java
   SearchSpecialistAPI.createFromBlockchain(blockchain, password, privateKey)
   ```

2. **Better Error Messages:**
   ```java
   throw new IllegalStateException(
       "SearchSpecialistAPI not initialized. " +
       "Call blockchain.initializeAdvancedSearch() first."
   );
   ```

3. **Validation in Constructor:**
   ```java
   public SearchSpecialistAPI() {
       logger.warn("Consider using blockchain.getSearchSpecialistAPI() instead");
   }
   ```

## 🔗 Related Documentation

- [API_GUIDE.md](API_GUIDE.md) - Complete API reference
- [SEARCH_FRAMEWORK_GUIDE.md](SEARCH_FRAMEWORK_GUIDE.md) - Search framework overview
- [EXAMPLES.md](EXAMPLES.md) - More usage examples

---

**Last Updated:** 2025-07-18  
**Version:** 1.0.0  
**Author:** Development Team