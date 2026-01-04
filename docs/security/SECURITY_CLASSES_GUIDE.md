# 🔐 Security Classes Guide

This guide provides detailed information about the security classes in the privateBlockchain project, which implement post-quantum cryptographic standards including ML-DSA-87 (NIST FIPS 204) with SHA3-256 and hierarchical key management.

## 📋 Table of Contents

- [Overview](#-overview)
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
- Post-quantum key management with ML-DSA-87

**Note:** The `ECKeyDerivation` class has been removed as it is not compatible with ML-DSA-87 post-quantum cryptography. ML-DSA (Module-Lattice Digital Signature Algorithm) does not support public key derivation from private keys due to its lattice-based mathematical structure.

These classes are available in the `com.rbatllet.blockchain.security` package.

- **Key Management**: Hierarchical key management (Root/Intermediate/Operational)
- **Encryption**: AES-256-GCM encryption for private key storage
- **Hashing**: SHA3-256 for message digests (quantum-resistant)
- **Digital Signatures**: ML-DSA-87 (NIST FIPS 204, 256-bit quantum-resistant, Module-Lattice Digital Signature Algorithm)
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
- **PBKDF2-HMAC-SHA512 Key Derivation**: Quantum-resistant password-based key derivation (210,000 iterations)
- **Unique Salt**: 128-bit cryptographically secure random salt per encryption
- **Memory Security**: Sensitive data cleared from memory after use
- **Thread-Safe**: All operations are thread-safe for concurrent access

### Encryption Specification

| Parameter | Value | Purpose |
|-----------|-------|---------|
| Algorithm | AES-256-GCM | Authenticated encryption |
| Key Size | 256 bits (32 bytes) | Maximum AES security |
| IV Size | 96 bits (12 bytes) | GCM recommended size |
| Auth Tag | 128 bits (16 bytes) | Data integrity protection |
| Key Derivation | PBKDF2-HMAC-SHA512 | Quantum-resistant KDF (210k iterations) |
| Salt | 128 bits (16 bytes) | Unique per encryption |

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

// Generate an ML-DSA-87 key pair (NIST FIPS 204, 256-bit quantum-resistant)
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

Keys are stored in the `keys/` directory with the following format:

```
keys/
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
```

### Password Requirements

- **Length**: 8-128 characters
- **Complexity**: Must include at least:
  - One uppercase letter
  - One lowercase letter
  - One digit
  - One special character
- **Formatting**: No leading or trailing spaces
- **Security**: Uses PBKDF2 with HMAC-SHA512 for quantum-resistant key derivation (210,000 iterations)

### Usage Example

```java
import com.rbatllet.blockchain.security.PasswordUtil;

// Read a password securely
String password = PasswordUtil.readPassword("Enter your password: ");

// Validate the password
if (PasswordUtil.isValidPassword(password)) {
    System.out.println("Valid password!");
} else {
    System.out.println("Error: Password does not meet security requirements");
    System.out.println("Required: 8-128 characters with uppercase, lowercase, digit, and special character");
}
```

## 📄 KeyFileLoader

### Description
`KeyFileLoader` provides methods to load public and private keys from files.

### Main Methods

```java
// Load a public key from a file (PEM/DER/Base64 formats)
public static PublicKey loadPublicKeyFromFile(String filePath)

// Load a private key from a file (PEM/DER/Base64 formats, unencrypted)
public static PrivateKey loadPrivateKeyFromFile(String filePath)

// Load a complete KeyPair from two separate files (required for ML-DSA-87)
public static KeyPair loadKeyPairFromFiles(String privateKeyPath, String publicKeyPath)

// Save a complete KeyPair to two separate files (PEM format)
public static boolean saveKeyPairToFiles(KeyPair keyPair, String privateKeyPath, String publicKeyPath)
```

### Usage Example

```java
import com.rbatllet.blockchain.security.KeyFileLoader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

// Generate an ML-DSA-87 key pair (NIST FIPS 204, 256-bit quantum-resistant)
KeyPair keyPair = CryptoUtil.generateKeyPair();
PrivateKey privateKey = keyPair.getPrivate();
PublicKey publicKey = keyPair.getPublic();

// Option 1: Save to files WITHOUT password (KeyFileLoader)
String privateKeyPath = "alice_private.key";
String publicKeyPath = "alice_public.key";

KeyFileLoader.saveKeyPairToFiles(keyPair, privateKeyPath, publicKeyPath);

// Load from files WITHOUT password
KeyPair loadedKeyPair = KeyFileLoader.loadKeyPairFromFiles(privateKeyPath, publicKeyPath);

// Option 2: Save WITH password protection (SecureKeyStorage)
String ownerName = "alice";
String password = "SecureP@ssw0rd";

SecureKeyStorage.saveKeyPair(ownerName, keyPair, password);

// Load WITH password (complete KeyPair)
KeyPair loadedKeyPair = SecureKeyStorage.loadKeyPair(ownerName, password);
PrivateKey loadedPrivateKey = loadedKeyPair.getPrivate();
PublicKey loadedPublicKey = loadedKeyPair.getPublic();
```

## 📝 Usage Examples

### Complete Workflow

```java
import com.rbatllet.blockchain.security.*;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.*;

public class SecurityExample {
    public static void main(String[] args) throws Exception {
        // 1. Generate an ML-DSA-87 key pair (NIST FIPS 204, 256-bit quantum-resistant)
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        
        // 2. Validate the password
        String password;
        do {
            password = PasswordUtil.readPassword("Enter a secure password: ");
            if (!PasswordUtil.isValidPassword(password)) {
                System.out.println("Error: Password does not meet security requirements");
                System.out.println("Required: 8-128 characters with uppercase, lowercase, digit, and special character");
            }
        } while (!PasswordUtil.isValidPassword(password));
        
        // 3. Store the keys securely
        String ownerName = "Alice";
        SecureKeyStorage.saveKeyPair(ownerName, keyPair, password);

        // 4. List stored keys
        System.out.println("Stored keys:");
        for (String key : SecureKeyStorage.listStoredKeys()) {
            System.out.println(" - " + key);
        }
        
        // 5. Load the KeyPair when needed
        KeyPair loadedKeyPair = SecureKeyStorage.loadKeyPair(ownerName, password);
        PrivateKey privateKey = loadedKeyPair.getPrivate();
        PublicKey publicKey = loadedKeyPair.getPublic();

        // 6. Use the keys for cryptographic operations
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

For more details on testing, see the [TESTING.md](../testing/TESTING.md) file.
