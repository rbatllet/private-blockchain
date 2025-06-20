# 🔐 Security Classes Guide

This guide provides detailed information about the security classes available in the main privateBlockchain project, which were migrated from the CLI project to improve reusability and maintain a clear separation of responsibilities.

## 📋 Table of Contents

- [Overview](#-overview)
- [SecureKeyStorage](#-securekeystorage)
- [PasswordUtil](#-passwordutil)
- [KeyFileLoader](#-keyfileloader)
- [Usage Examples](#-usage-examples)
- [Testing](#-testing)

## 🚀 Overview

The security classes provide essential functionalities for:

- Secure storage of private keys with AES encryption
- Password validation with security requirements
- Loading keys from files
- Complete key lifecycle management

These classes are now available in the `com.rbatllet.blockchain.security` package of the main project.

## 🔒 SecureKeyStorage

### Description
`SecureKeyStorage` provides methods to store, load, and manage private keys securely using AES encryption.

### Main Methods

```java
// Save a private key encrypted with password
public static boolean savePrivateKey(String ownerName, PrivateKey privateKey, String password)

// Load a stored private key
public static PrivateKey loadPrivateKey(String ownerName, String password)

// Check if a private key exists for an owner
public static boolean hasPrivateKey(String ownerName)

// Delete a stored private key
public static boolean deletePrivateKey(String ownerName)

// List all stored keys
public static List<String> listStoredKeys()
```

### Usage Example

```java
import com.rbatllet.blockchain.security.SecureKeyStorage;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;

// Generate a key pair for the example
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
keyGen.initialize(new ECGenParameterSpec("secp256r1"));
KeyPair keyPair = keyGen.generateKeyPair();
PrivateKey privateKey = keyPair.getPrivate();

// Save the private key
String ownerName = "Alice";
String password = "SecureP@ssw0rd";
boolean saved = SecureKeyStorage.savePrivateKey(ownerName, privateKey, password);

// Check if the key exists
boolean exists = SecureKeyStorage.hasPrivateKey(ownerName);

// Load the private key
PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(ownerName, password);

// List all stored keys
List<String> storedKeys = SecureKeyStorage.listStoredKeys();
for (String key : storedKeys) {
    System.out.println("Stored key: " + key);
}

// Delete the key when no longer needed
boolean deleted = SecureKeyStorage.deletePrivateKey(ownerName);
```

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

### Requisitos de Contraseña

- Minimum 8 characters
- At least one letter
- At least one number
- Maximum 128 characters
- No leading or trailing spaces

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
