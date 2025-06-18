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
| `isValid()` | Checks if the block is valid overall |
| `setPreviousHashValid(boolean)` | Sets the validation result for the previous hash |
| `setBlockNumberValid(boolean)` | Sets the validation result for the block number |
| `setHashIntegrityValid(boolean)` | Sets the validation result for hash integrity |
| `setTimestampValid(boolean)` | Sets the validation result for the timestamp |
| `setSignatureValid(boolean)` | Sets the validation result for the signature |
| `setDataValid(boolean)` | Sets the validation result for the data |
| `setAuthorKeyValid(boolean)` | Sets the validation result for the author key |
| `getValidationSummary()` | Gets a summary of all validation results |

#### Usage Example

```java
BlockValidationResult result = new BlockValidationResult();

// Set individual results
result.setPreviousHashValid(true);
result.setBlockNumberValid(true);
result.setHashIntegrityValid(false); // Hash doesn't match content
result.setTimestampValid(true);
result.setSignatureValid(true);
result.setDataValid(true);
result.setAuthorKeyValid(true);

// Check overall validity
if (!result.isValid()) {
    System.out.println("Block is not valid: " + result.getValidationSummary());
}
```

## Format Utilities

### FormatUtil

`FormatUtil` provides methods for formatting blockchain data for display and storage.

#### Location

```
com.rbatllet.blockchain.util.format.FormatUtil
```

#### Main Methods

| Method | Description |
|--------|-------------|
| `truncateHash(String)` | Truncates a hash for display purposes with ellipsis in the middle |
| `formatTimestamp(LocalDateTime)` | Formats a timestamp using the default format (yyyy-MM-dd HH:mm:ss) |
| `formatTimestamp(LocalDateTime, String)` | Formats a timestamp using a custom pattern |
| `formatBlockInfo(Block)` | Formats complete block information for display with proper truncation |
| `fixedWidth(String, int)` | Formats a string to a fixed width by truncating with ellipsis or padding with spaces |

#### Usage Example

```java
// Truncate a hash for display
String hash = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";
String truncatedHash = FormatUtil.truncateHash(hash); // "a1b2c3d4e5f6a1b2...4e5f6a1b2c3d4e5f6a1b2"

// Format a timestamp with default format
LocalDateTime timestamp = LocalDateTime.now();
String formattedTimestamp = FormatUtil.formatTimestamp(timestamp); // "2025-06-18 13:30:37"

// Format a timestamp with custom pattern
String customFormatted = FormatUtil.formatTimestamp(timestamp, "dd/MM/yyyy"); // "18/06/2025"

// Format complete block information
Block block = blockchain.getBlockByNumber(1L);
String blockInfo = FormatUtil.formatBlockInfo(block); // Multi-line formatted block info

// Format to fixed width with intelligent truncation
String text = "Very long text that needs to be truncated";
String fixedWidthText = FormatUtil.fixedWidth(text, 20); // Will truncate at word boundary if possible
```

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
