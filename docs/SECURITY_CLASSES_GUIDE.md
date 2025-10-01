# 🔐 Security Classes Guide

This guide provides detailed information about the security classes in the privateBlockchain project, which implement modern cryptographic standards including ECDSA with SHA3-256 and hierarchical key management.

## 📋 Table of Contents

- [Overview](#-overview)
- [ECKeyDerivation](#-eckeyderivation)
- [SecureKeyStorage](#-securekeystorage)
- [PasswordUtil](#-passwordutil)
- [KeyFileLoader](#-keyfileloader)
- [Usage Examples](#-usage-examples)
- [Testing](#-testing)

## 🚀 Overview

The security classes provide essential functionalities for:

- Secure storage of private keys with AES-256-GCM encryption
- Password validation with security requirements
- Loading keys from files
- Elliptic curve key derivation and validation

## 🔑 ECKeyDerivation

High-performance, thread-safe EC key derivation utility for secure cryptographic operations.

### Features

- **Thread Safety**: Fully thread-safe with synchronized provider initialization
- **Performance**: Uses `ConcurrentHashMap` for curve parameter caching and `ThreadLocal` for `KeyFactory` instances
- **Reliability**: Leverages BouncyCastle for proven EC point multiplication
- **Validation**: Comprehensive input validation and curve point verification
- **Flexibility**: Supports multiple EC curves and key types

### Key Methods

- `derivePublicKey(PrivateKey privateKey)`: Derives a public key from a private key
- `derivePublicKeyFromPrivate(PrivateKey privateKey, ECParameterSpec curveParams)`: Derives a public key with custom curve parameters
- `verifyKeyPair(PrivateKey privateKey, PublicKey publicKey)`: Verifies if a private and public key form a valid pair
- `isPointOnCurve(ECPoint point, ECParameterSpec curveParams)`: Validates if a point lies on the specified curve

### Thread Safety

All public methods are thread-safe with the following characteristics:
- No shared mutable state
- Thread-local `KeyFactory` instances to prevent contention
- Synchronized BouncyCastle provider initialization
- Concurrent caching of curve parameters

### Example Usage

```java
// Generate a key pair using the hierarchical key system
KeyPair keyPair = CryptoUtil.generateKeyPair();

// Derive public key from private key
ECKeyDerivation keyDerivation = new ECKeyDerivation();
PublicKey derivedPublic = keyDerivation.derivePublicKey(keyPair.getPrivate());

// Verify the key pair using modern ECDSA
boolean isValid = keyDerivation.verifyKeyPair(keyPair.getPrivate(), derivedPublic);

// Example of hierarchical key usage
KeyInfo rootKey = CryptoUtil.createRootKey();
KeyInfo intermediateKey = CryptoUtil.createIntermediateKey(rootKey.getKeyId());
KeyInfo operationalKey = CryptoUtil.createOperationalKey(intermediateKey.getKeyId());
```

### Performance Considerations

- Curve parameters are cached in a `ConcurrentHashMap` for efficient lookups
- Each thread gets its own `KeyFactory` instance via `ThreadLocal`
- BouncyCastle provider initialization is synchronized to prevent race conditions
- Point validation is optimized to fail fast with minimal computations

### Error Handling

- All methods throw `ECKeyDerivationException` for cryptographic operations
- Input validation throws `IllegalArgumentException` for invalid parameters
- Curve validation throws `IllegalStateException` for unsupported curves

## 🚀 Overview

The security classes provide essential cryptographic functionalities:

- **Key Management**: Hierarchical key management (Root/Intermediate/Operational)
- **Encryption**: AES-256-GCM encryption for private key storage
- **Hashing**: SHA3-256 for message digests
- **Digital Signatures**: ECDSA with secp256r1 curve
- **Key Derivation**: Secure key derivation with PBKDF2
- **Password Validation**: Strong password policies and secure input handling

These classes are available in the `com.rbatllet.blockchain.security` package.

## 🔒 SecureKeyStorage

### Description
`SecureKeyStorage` provides methods to store, load, and manage private keys securely using **AES-256-GCM** encryption with authenticated encryption and automatic IV generation.

### Security Features

- **AES-256-GCM**: Galois/Counter Mode with 256-bit keys for authenticated encryption
- **Random IV Generation**: Each encryption uses a cryptographically secure random 96-bit IV
- **Authentication Tag**: 128-bit authentication tag protects against tampering
- **SHA-3-256 Key Derivation**: Password-based key derivation using SHA-3-256
- **Memory Security**: Sensitive data cleared from memory after use
- **Thread-Safe**: All operations are thread-safe for concurrent access

### Encryption Specification

| Parameter | Value | Purpose |
|-----------|-------|---------|
| Algorithm | AES-256-GCM | Authenticated encryption |
| Key Size | 256 bits (32 bytes) | Maximum AES security |
| IV Size | 96 bits (12 bytes) | GCM recommended size |
| Auth Tag | 128 bits (16 bytes) | Data integrity protection |
| Key Derivation | SHA-3-256 | Secure hash algorithm |

### Main Methods

```java
// Save a private key encrypted with password using AES-256-GCM
public static boolean savePrivateKey(String ownerName, PrivateKey privateKey, String password)

// Load a stored private key (verifies authentication tag automatically)
public static PrivateKey loadPrivateKey(String ownerName, String password)

// Check if a private key exists for an owner
public static boolean hasPrivateKey(String ownerName)

// Delete a stored private key
public static boolean deletePrivateKey(String ownerName)

// List all stored key owners
public static String[] listStoredKeys()
```

### Usage Example

```java
import com.rbatllet.blockchain.security.SecureKeyStorage;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.security.PrivateKey;

// Generate an ECDSA key pair using secp256r1 curve
KeyPair keyPair = CryptoUtil.generateKeyPair();
PrivateKey privateKey = keyPair.getPrivate();

// Save the private key with AES-256-GCM encryption
String ownerName = "Alice";
String password = "SecureP@ssw0rd123!";
boolean saved = SecureKeyStorage.savePrivateKey(ownerName, privateKey, password);

if (saved) {
    System.out.println("✅ Private key saved with AES-256-GCM encryption");
} else {
    System.out.println("❌ Failed to save private key");
}

// Check if the key exists
boolean exists = SecureKeyStorage.hasPrivateKey(ownerName);
System.out.println("🔍 Key exists: " + exists);

// Load the private key (authentication tag verified automatically)
PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(ownerName, password);

if (loadedKey != null) {
    System.out.println("✅ Private key loaded and verified successfully");

    // Verify key integrity
    boolean keysMatch = java.util.Arrays.equals(
        privateKey.getEncoded(),
        loadedKey.getEncoded()
    );
    System.out.println("🔐 Key integrity verified: " + keysMatch);
} else {
    System.out.println("❌ Failed to load key (wrong password or corrupted data)");
}

// List all stored keys
String[] storedKeys = SecureKeyStorage.listStoredKeys();
System.out.println("📋 Stored keys (" + storedKeys.length + "):");
for (String key : storedKeys) {
    System.out.println("  - " + key);
}

// Delete the key when no longer needed
boolean deleted = SecureKeyStorage.deletePrivateKey(ownerName);
if (deleted) {
    System.out.println("🧹 Key deleted successfully");
}
```

### Security Best Practices

#### ✅ Secure Usage Patterns

```java
// Use strong passwords (recommended 16+ characters)
String strongPassword = CryptoUtil.deriveKeyFromPassword(
    "UserMasterPassword123!",
    "salt-" + ownerName
);

// Save key with strong password
SecureKeyStorage.savePrivateKey(ownerName, privateKey, strongPassword);

// Always verify key was loaded successfully
PrivateKey key = SecureKeyStorage.loadPrivateKey(ownerName, strongPassword);
if (key == null) {
    throw new SecurityException("Failed to load private key - authentication failed");
}

// Clear password from memory after use
strongPassword = null;
```

#### ❌ Insecure Usage Patterns to Avoid

```java
// DON'T: Use weak passwords
SecureKeyStorage.savePrivateKey(ownerName, privateKey, "123"); // Too weak!

// DON'T: Hardcode passwords
String password = "hardcoded"; // Security vulnerability!

// DON'T: Ignore null returns
PrivateKey key = SecureKeyStorage.loadPrivateKey(ownerName, password);
// Always check for null before using!

// DON'T: Store passwords in logs
logger.info("Password: " + password); // Security leak!
```

### Error Handling and Security

The storage system provides **fail-safe** behavior:

- **Wrong Password**: Returns `null` (authentication tag verification fails)
- **Corrupted Data**: Returns `null` (GCM authentication fails)
- **Invalid Input**: Returns `false` or `null` (input validation fails)
- **File Not Found**: Returns `null` (file doesn't exist)

```java
// Proper error handling with GCM authentication
PrivateKey key = SecureKeyStorage.loadPrivateKey(ownerName, password);

if (key == null) {
    // Could be:
    // 1. Wrong password
    // 2. Corrupted encrypted data (failed authentication tag)
    // 3. File not found
    // 4. Invalid key format

    logger.warn("❌ Failed to load key for owner: {}", ownerName);
    // Do NOT log the password or detailed error
} else {
    logger.info("✅ Key loaded and authenticated successfully");
}
```

### File Storage Format

Keys are stored in the `private-keys/` directory with the following format:

```
private-keys/
  ├── Alice.key          (Base64-encoded: IV + Ciphertext + Auth Tag)
  ├── Bob.key
  └── Charlie.key
```

**File Content Structure** (Base64-encoded):
```
[12-byte IV][Variable-length Ciphertext][16-byte Auth Tag]
```

The authentication tag ensures:
- **Integrity**: Data hasn't been modified
- **Authenticity**: Data was encrypted with the correct key
- **Tamper Detection**: Any modification causes decryption to fail

## 🔑 PasswordUtil

### Description
`PasswordUtil` provides utilities for secure password management, including validation and secure input.

### Main Methods

```java
// Securely reads a password from the console
public static String readPassword(String prompt)

// Validates that a password meets security requirements
public static boolean isValidPassword(String password)

// Gets a detailed error message if the password is not valid
public static String getPasswordValidationError(String password)
```

### Password Requirements

- **Length**: 8-128 characters
- **Complexity**: Must include at least:
  - One uppercase letter
  - One lowercase letter
  - One digit
  - One special character
- **Formatting**: No leading or trailing spaces
- **Security**: Uses PBKDF2 with HMAC-SHA256 for key derivation

### Usage Example

```java
import com.rbatllet.blockchain.security.PasswordUtil;

// Read a password securely
String password = PasswordUtil.readPassword("Enter your password: ");

// Validate the password
if (PasswordUtil.isValidPassword(password)) {
    System.out.println("Valid password!");
} else {
    String error = PasswordUtil.getPasswordValidationError(password);
    System.out.println("Error: " + error);
}
```

## 📄 KeyFileLoader

### Description
`KeyFileLoader` provides methods to load public and private keys from files.

### Main Methods

```java
// Load a public key from a file
public static PublicKey loadPublicKey(String filePath)

// Load a private key from a file
public static PrivateKey loadPrivateKey(String filePath, String password)

// Save a public key to a file
public static void savePublicKey(PublicKey key, String filePath)

// Save a private key to a file (encrypted with password)
public static void savePrivateKey(PrivateKey key, String filePath, String password)
```

### Usage Example

```java
import com.rbatllet.blockchain.security.KeyFileLoader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

// Generate a key pair for the example
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
keyGen.initialize(new ECGenParameterSpec("secp256r1"));
KeyPair keyPair = keyGen.generateKeyPair();
PrivateKey privateKey = keyPair.getPrivate();
PublicKey publicKey = keyPair.getPublic();

// Save the keys to files
String privateKeyPath = "alice_private.key";
String publicKeyPath = "alice_public.key";
String password = "SecureP@ssw0rd";

KeyFileLoader.savePrivateKey(privateKey, privateKeyPath, password);
KeyFileLoader.savePublicKey(publicKey, publicKeyPath);

// Load the keys from files
PrivateKey loadedPrivateKey = KeyFileLoader.loadPrivateKey(privateKeyPath, password);
PublicKey loadedPublicKey = KeyFileLoader.loadPublicKey(publicKeyPath);
```

## 📝 Usage Examples

### Complete Workflow

```java
import com.rbatllet.blockchain.security.*;
import java.security.*;

public class SecurityExample {
    public static void main(String[] args) throws Exception {
        // 1. Generate a key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyGen.generateKeyPair();
        
        // 2. Validate the password
        String password;
        do {
            password = PasswordUtil.readPassword("Enter a secure password: ");
            if (!PasswordUtil.isValidPassword(password)) {
                System.out.println("Error: " + PasswordUtil.getPasswordValidationError(password));
            }
        } while (!PasswordUtil.isValidPassword(password));
        
        // 3. Store the private key securely
        String ownerName = "Alice";
        SecureKeyStorage.savePrivateKey(ownerName, keyPair.getPrivate(), password);
        
        // 4. Save the public key to a file
        KeyFileLoader.savePublicKey(keyPair.getPublic(), ownerName + "_public.key");
        
        // 5. List stored keys
        System.out.println("Stored keys:");
        for (String key : SecureKeyStorage.listStoredKeys()) {
            System.out.println(" - " + key);
        }
        
        // 6. Load the private key when needed
        PrivateKey privateKey = SecureKeyStorage.loadPrivateKey(ownerName, password);
        
        // 7. Load the public key from file
        PublicKey publicKey = KeyFileLoader.loadPublicKey(ownerName + "_public.key");
        
        // 8. Use the keys for cryptographic operations
        // (sign blocks, verify signatures, etc.)
    }
}
```

## 🧪 Testing

The security classes are fully tested with JUnit to ensure their correct operation:

- `SecureKeyStorageTest`: Tests storage and retrieval of private keys
- `PasswordUtilTest`: Validates password validation functionality
- `KeyFileLoaderTest`: Checks loading and saving keys to files

Example of running tests:

```bash
mvn test -Dtest=com.rbatllet.blockchain.security.*Test
```

For more details on testing, see the [TESTING.md](TESTING.md) file.
