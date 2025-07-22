# Utility Classes Guide

This document describes the utility classes available in the Private Blockchain core project, which have been migrated from the CLI project to improve code reuse and maintain a clear separation of responsibilities.

## Table of Contents

1. [ExitUtil](#exitutil)
2. [Validation Utilities](#validation-utilities)
   - [BlockValidationUtil](#blockvalidationutil)
   - [BlockValidationResult](#blockvalidationresult)
3. [Format Utilities](#format-utilities)
   - [FormatUtil](#formatutil)
4. [Testing](#testing)

## ExitUtil

`ExitUtil` is a utility class for handling `System.exit()` calls in a test-compatible way.

### Location

```
com.rbatllet.blockchain.util.ExitUtil
```

### Main Methods

| Method | Description |
|--------|-------------|
| `disableExit()` | Disables calls to `System.exit()` (for testing) |
| `enableExit()` | Enables calls to `System.exit()` (normal operation) |
| `exit(int exitCode)` | Safe exit that can be disabled for testing |
| `isExitDisabled()` | Checks if exit is disabled |
| `getLastExitCode()` | Gets the last attempted exit code (only works in test mode) |

### Usage Example

```java
// In production code
ExitUtil.exit(1); // Will exit with code 1

// In test code
ExitUtil.disableExit();
ExitUtil.exit(1); // Won't exit, just logs the code
int exitCode = ExitUtil.getLastExitCode(); // Will get 1
ExitUtil.enableExit(); // Returns to normal behavior
```

## Validation Utilities

### BlockValidationUtil

`BlockValidationUtil` provides methods for validating blocks in the blockchain.

#### Location

```
com.rbatllet.blockchain.util.validation.BlockValidationUtil
```

#### Main Methods

| Method | Description |
|--------|-------------|
| `validateGenesisBlock(Block)` | Specifically validates the genesis block |
| `wasKeyAuthorizedAt(Blockchain, String, LocalDateTime)` | Checks if a key was authorized at a specific time |
| `truncateHash(String)` | Truncates a hash to display it in a more readable way |

#### Usage Example

```java
// Validate genesis block
Block genesisBlock = blockchain.getBlockByNumber(0L);
boolean isValid = BlockValidationUtil.validateGenesisBlock(genesisBlock);

// Check key authorization
boolean wasAuthorized = BlockValidationUtil.wasKeyAuthorizedAt(
    blockchain, 
    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...", 
    LocalDateTime.now().minusDays(1)
);

// Truncate hash for display
String truncatedHash = BlockValidationUtil.truncateHash(block.getHash());
```

### BlockValidationResult

`BlockValidationResult` represents the result of a block validation, with details about which aspects of the block are valid or invalid.

#### Location

```
com.rbatllet.blockchain.util.validation.BlockValidationResult
```

#### Main Methods

| Method | Description |
|--------|-------------|
| `getBlock()` | Gets the block being validated |
| `getStatus()` | Gets the validation status (VALID, INVALID, REVOKED) |
| `isValid()` | Returns true if block is valid or revoked (structurally sound) |
| `isFullyValid()` | Returns true only if block is completely valid |
| `isStructurallyValid()` | Checks if block structure is valid |
| `isCryptographicallyValid()` | Checks if cryptographic signatures are valid |
| `isAuthorizationValid()` | Checks if the signing key was authorized |
| `isOffChainDataValid()` | Checks if off-chain data is valid |
| `getErrorMessage()` | Gets detailed error message if validation failed |
| `getWarningMessage()` | Gets warning message if applicable |
| `toString()` | Gets a formatted summary of validation results |

#### Usage Example

```java
// BlockValidationResult is created by the validation process using Builder pattern
// You typically receive it from validation methods, not create it directly

// Example of working with validation results
BlockValidationResult result = blockchain.validateBlock(block);

// Check different aspects of validation
if (result.isFullyValid()) {
    System.out.println("Block is completely valid");
} else if (result.isValid()) {
    System.out.println("Block is structurally valid but may have authorization issues");
    if (!result.isAuthorizationValid()) {
        System.out.println("Warning: " + result.getWarningMessage());
    }
} else {
    System.out.println("Block validation failed: " + result.getErrorMessage());
}

// Get detailed information
System.out.println("Status: " + result.getStatus());
System.out.println("Structural: " + result.isStructurallyValid());
System.out.println("Cryptographic: " + result.isCryptographicallyValid());
System.out.println("Authorization: " + result.isAuthorizationValid());
System.out.println("Off-chain data: " + result.isOffChainDataValid());

// Get formatted summary
System.out.println("Summary: " + result.toString());
```

## Format Utilities

### FormatUtil

`FormatUtil` provides comprehensive methods for formatting blockchain data for display and storage. **Enhanced with new functions migrated from BlockchainDisplayUtils**.

#### Location

```
com.rbatllet.blockchain.util.format.FormatUtil
```

#### Core Display Methods

| Method | Description |
|--------|-------------|
| `truncateHash(String)` | **🔄 ENHANCED:** Truncates hash with symmetric 16+16 format (improved from 16+20) |
| `truncateKey(String)` | **✨ NEW:** Truncates public keys with 20+20 format for readability |
| `formatBlockInfo(Block)` | Formats complete block information with consistent hash truncation |
| `formatTimestamp(LocalDateTime)` | Formats timestamp using default format (yyyy-MM-dd HH:mm:ss) |
| `formatTimestamp(LocalDateTime, String)` | Formats timestamp using custom pattern |
| `fixedWidth(String, int)` | Formats string to fixed width with intelligent word-boundary truncation |

#### Data Formatting Methods

| Method | Description |
|--------|-------------|
| `formatBytes(long)` | **✨ NEW:** Convert bytes to human-readable format (B, KB, MB, GB) |
| `formatDate(LocalDateTime)` | **✨ NEW:** Format date-only display (yyyy-MM-dd) |
| `formatDuration(long)` | **✨ NEW:** Format nanosecond durations with appropriate precision |
| `formatPercentage(double)` | **✨ NEW:** Format percentage values with smart precision |
| `formatBlockchainState(long, boolean, LocalDateTime)` | **✨ NEW:** Format blockchain state summary |

#### Utility Methods

| Method | Description |
|--------|-------------|
| `escapeJson(String)` | **✨ NEW:** Safely escape strings for JSON output |
| `createSeparator(int)` | **✨ NEW:** Generate separator lines for formatted output |

#### Enhanced Usage Examples

```java
// 🔄 ENHANCED: Symmetric hash truncation (16+16)
String hash = "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456";
String truncated = FormatUtil.truncateHash(hash); 
// Result: "a1b2c3d4e5f67890...ef123456" (symmetric 16+16)

// ✨ NEW: Public key truncation (20+20)
String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7nJlL...";
String truncatedKey = FormatUtil.truncateKey(publicKey);
// Result: "MIIBIjANBgkqhkiG9w0B...Q8AMIIBCgKCAQEA7nJl"

// ✨ NEW: Byte formatting
FormatUtil.formatBytes(1536);          // "1.5 KB"
FormatUtil.formatBytes(2097152);       // "2.0 MB"
FormatUtil.formatBytes(3221225472L);   // "3.0 GB"

// ✨ NEW: Duration formatting with smart precision
FormatUtil.formatDuration(50_000L);      // "0.050 ms" (high precision)
FormatUtil.formatDuration(150_000_000L); // "150 ms" (low precision)

// ✨ NEW: Percentage formatting
FormatUtil.formatPercentage(0.25);   // "0.25%"
FormatUtil.formatPercentage(5.7);    // "5.7%"
FormatUtil.formatPercentage(100);    // "100%"

// ✨ NEW: JSON escaping
String input = "User said: \"Hello\nWorld!\"";
String escaped = FormatUtil.escapeJson(input);
// Result: "User said: \\\"Hello\\nWorld!\\\""

// ✨ NEW: Blockchain state formatting
String state = FormatUtil.formatBlockchainState(42, true, LocalDateTime.now());
/* Result:
Total Blocks        : 42
Chain Valid         : Yes
Last Block Time     : 2024-06-28 19:30:02
*/

// ✨ NEW: Separator lines
String separator = FormatUtil.createSeparator(50);
// Result: "=================================================="

// Original timestamp formatting
LocalDateTime timestamp = LocalDateTime.now();
String fullTimestamp = FormatUtil.formatTimestamp(timestamp);   // "2024-06-28 19:30:02"
String dateOnly = FormatUtil.formatDate(timestamp);             // "2024-06-28"
String custom = FormatUtil.formatTimestamp(timestamp, "dd/MM/yyyy HH:mm");  // "28/06/2024 19:30"

// Fixed-width formatting with intelligent truncation
String text = "Very long text that needs to be truncated";
String fixedWidthText = FormatUtil.fixedWidth(text, 20); // Truncates at word boundary when possible
```

#### Migration from BlockchainDisplayUtils

**🔄 MIGRATION COMPLETED:** All display utilities have been migrated from `BlockchainDisplayUtils` to `FormatUtil` for better code reusability and consistency.

**Key Improvements:**
- **Symmetric Hash Truncation**: Changed from asymmetric 16+20 to symmetric 16+16 for better visual consistency
- **Consolidated Functions**: All formatting functions now in one location
- **Enhanced Functionality**: Added 7 new utility functions for comprehensive formatting needs
- **CLI Integration**: All CLI commands updated to use FormatUtil instead of BlockchainDisplayUtils

**Files Updated in Migration:**
- `AddBlockCommand.java` - Updated import and method calls
- `SearchCommand.java` - Updated import and method calls  
- `OffChainCommand.java` - Updated import and method calls
- `EncryptCommand.java` - Updated import and method calls

#### Testing Best Practices

The FormatUtil class follows these testing best practices:

- Tests verify behavior rather than specific output formats
- No hardcoded special cases in implementation or tests
- Tests use assertions that allow implementation to evolve without breaking
- Edge cases are properly handled and tested (null inputs, empty strings, etc.)

## Testing

The utility classes are fully tested with JUnit to ensure their correct operation:

- `ExitUtilTest`: Tests the safe exit functionality
- `BlockValidationUtilTest`: Tests the block validation utilities
- `BlockValidationResultTest`: Tests the block validation result class
- `FormatUtilTest`: Tests the formatting utilities

Example of running tests:

```zsh
mvn test -Dtest=com.rbatllet.blockchain.util.*Test
```

For more details on testing, see the [TESTING.md](TESTING.md) file.
```
