# Block Number-Based Decryption Methods Guide

## Overview

This document describes the new block number-based decryption methods that were implemented to resolve the hash-to-block mapping issue discovered in the `UserFriendlyEncryptionAPIOptimizationTest`. These methods provide a clean separation between database ID-based and block number-based operations.

## Problem Solved

### Original Issue
The original decryption methods used **database IDs** internally, but the API exposed **block numbers** to users. This caused confusion where:

- Test code called `retrieveSecret(block.getBlockNumber())` passing block number `1`
- But `getBlockWithDecryption()` expected a database ID
- Genesis Block had database ID=1, causing wrong block retrieval
- Block #1 (with "medical" content) had database ID=2, not 1

### Solution
New methods were created that consistently use **block numbers** throughout the entire call chain, providing clean separation and avoiding ID confusion.

## New Methods Architecture

### 1. BlockDAO.getBlockByNumberWithDecryption()

**Purpose**: Database access layer method that queries by block number instead of database ID.

**Location**: `src/main/java/com/rbatllet/blockchain/dao/BlockDAO.java`

**Method Signature**:
```java
public String getBlockByNumberWithDecryption(Long blockNumber, String password)
```

**Implementation Details**:
- Queries database using `blockNumber` field instead of `id` field
- Includes comprehensive debug logging for troubleshooting
- Handles decryption using `SecureBlockEncryptionService`
- Returns decrypted content string or null if decryption fails

**Usage Example**:
```java
// Direct DAO usage (not recommended for user code)
BlockDAO blockDAO = blockchain.getBlockDAO();
String content = blockDAO.getBlockByNumberWithDecryption(1L, "password123");
```

### 2. Blockchain.getDecryptedBlockDataByNumber()

**Purpose**: Clean interface layer method that provides block number-based decryption.

**Location**: `src/main/java/com/rbatllet/blockchain/core/Blockchain.java`

**Method Signature**:
```java
public String getDecryptedBlockDataByNumber(Long blockNumber, String password)
```

**Implementation Details**:
- Acts as interface layer between API and DAO
- Delegates to `BlockDAO.getBlockByNumberWithDecryption()`
- Provides consistent error handling and logging
- Maintains architectural separation of concerns

**Usage Example**:
```java
// Blockchain layer usage
Blockchain blockchain = new Blockchain();
String decryptedData = blockchain.getDecryptedBlockDataByNumber(1L, "password123");
```

### 3. UserFriendlyEncryptionAPI.retrieveSecret() (Updated)

**Purpose**: User-facing API method that now correctly uses block number-based decryption.

**Location**: `src/main/java/com/rbatllet/blockchain/service/UserFriendlyEncryptionAPI.java`

**Method Signature**:
```java
public String retrieveSecret(Long blockId, String password)
```

**Implementation Details**:
- Now calls `blockchain.getDecryptedBlockDataByNumber()` instead of `getDecryptedBlockData()`
- Provides comprehensive debug logging for user troubleshooting
- Maintains backward compatibility - still accepts block numbers as `blockId`
- Enhanced error handling with detailed logging

**Usage Example**:
```java
// User-friendly API usage (recommended)
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, "user", keyPair);

// Store encrypted data
Block block = api.storeSecret("Sensitive medical data", "password123");

// Retrieve using block ID
String decrypted = api.retrieveSecret(block.getId(), "password123");
System.out.println("Retrieved: " + decrypted);
```

## Practical Examples

### Example 1: Medical Records Management

```java
// Initialize API
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, "doctor", keyPair);

// Store patient record
String patientData = "Patient: John Doe, Diagnosis: Diabetes Type 1, Treatment: Insulin";
Block medicalBlock = api.storeSecret(patientData, "medicalPassword123");

System.out.println("Medical record stored in block #" + medicalBlock.getBlockNumber());

// Later retrieve the record
String retrievedRecord = api.retrieveSecret(medicalBlock.getId(), "medicalPassword123");
if (retrievedRecord != null) {
    System.out.println("Retrieved medical record: " + retrievedRecord);
} else {
    System.out.println("Failed to decrypt - check password");
}
```

### Example 2: Financial Transaction Processing

```java
// Initialize API
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, "financial_officer", keyPair);

// Store transaction data with identifier
String transactionData = "Transfer $50,000 from Account A123 to Account B456";
Block txBlock = api.storeDataWithIdentifier(
    transactionData, 
    "financeKey456", 
    "transaction:TX-2025-001"
);

// Find and decrypt transaction by identifier
List<Block> transactions = api.findRecordsByIdentifier("transaction:TX-2025-001");
for (Block block : transactions) {
    String txData = api.retrieveSecret(block.getId(), "financeKey456");
    System.out.println("Transaction: " + txData);
}
```

### Example 3: Search and Decrypt Workflow

```java
// Initialize API
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, "researcher", keyPair);

// Search for encrypted data by term
List<Block> encryptedBlocks = api.findEncryptedData("medical");
System.out.println("Found " + encryptedBlocks.size() + " encrypted blocks");

// Decrypt each found block
for (Block block : encryptedBlocks) {
    String decryptedContent = api.retrieveSecret(block.getId(), "medicalPassword");
    if (decryptedContent != null) {
        System.out.println("Block #" + block.getBlockNumber() + ": " + decryptedContent);
    }
}
```

## Architecture Benefits

### 1. Clear Separation of Concerns
- **Database Layer**: `BlockDAO.getBlockByNumberWithDecryption()` handles database queries
- **Business Logic**: `Blockchain.getDecryptedBlockDataByNumber()` provides clean interface
- **User API**: `UserFriendlyEncryptionAPI.retrieveSecret()` offers intuitive access

### 2. Consistent Block Number Usage
- All methods in the call chain now use block numbers consistently
- No more confusion between database IDs and block numbers
- Predictable behavior for developers and users

### 3. Enhanced Debugging
- Comprehensive logging at each layer
- Clear identification of which method is being called
- Detailed error messages for troubleshooting

### 4. Backward Compatibility
- Existing API signatures remain unchanged
- User code continues to work without modifications
- Internal implementation improved without breaking changes

## Debug Logging Features

### BlockDAO Layer Logging
```
🔧 DECRYPTION DEBUG: Getting block by blockNumber=1
🔧 DECRYPTION DEBUG: Found block with blockNumber=1, ID=2, data='[ENCRYPTED]...'
🔧 DECRYPTION DEBUG: Block #1 decrypted successfully. Content: 'Test data 0 category:medical...'
```

### API Layer Logging
```
🔓 DEBUG: retrieveSecret called for block #1
🔓 DEBUG: Block #1 decrypted successfully. Content: 'Test data 0 category:medical...'
```

## Performance Considerations

### Optimized Database Queries
- Direct query by `blockNumber` field avoids ID lookups
- Single database roundtrip per decryption operation
- Efficient indexing on `blockNumber` column

### Memory Management
- Decrypted data is handled efficiently
- No unnecessary object creation in call chain
- Proper cleanup of sensitive data after use

## Error Handling

### Common Error Scenarios
1. **Block Not Found**: Returns null, logs warning
2. **Wrong Password**: Returns null, logs decryption failure
3. **Invalid Block Number**: Throws `IllegalArgumentException`
4. **Database Error**: Returns null, logs database exception

### Error Recovery Patterns
```java
String decrypted = api.retrieveSecret(blockNumber, password);
if (decrypted == null) {
    // Handle decryption failure
    System.out.println("Decryption failed - check block number and password");
    return;
}
// Process successful decryption
processDecryptedData(decrypted);
```

## Migration Guide

### For Existing Code
No changes required - the API remains the same:
```java
// This code continues to work unchanged
String data = api.retrieveSecret(blockNumber, password);
```

### For New Development
Use the consistent block number approach:
```java
// Store data
Block block = api.storeSecret("data", "password");

// Retrieve using the same block number
String retrieved = api.retrieveSecret(block.getBlockNumber(), "password");
```

## Testing Verification

The fix was verified through the `UserFriendlyEncryptionAPIOptimizationTest`:

### Test Scenario
1. Create 5 encrypted blocks with different content
2. Block #1 contains "medical" content
3. Search for "medical" term
4. Verify correct block is found and decrypted

### Test Results
```
✅ Search finds correct hash: 682b6eda1e81062950a6b85abd49ae2a361cbe6b23e62400acb2b5fdfc80d16e
✅ Database returns correct Block #1 with blockNumber=1, ID=2
✅ Decryption returns correct content: "Test data 0 category:medical status:active priority:high index:0"
✅ No more Genesis Block confusion
```

## Conclusion

The new block number-based decryption methods provide:

- **Consistency**: Block numbers used throughout the entire call chain
- **Clarity**: Clear separation between database IDs and block numbers  
- **Reliability**: Predictable behavior with comprehensive error handling
- **Maintainability**: Clean architecture with proper debugging support
- **Compatibility**: Existing code continues to work without changes

This implementation resolves the hash-to-block mapping issue while maintaining API compatibility and improving the overall architecture of the decryption system.