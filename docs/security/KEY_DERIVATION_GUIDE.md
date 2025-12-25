# 🔐 Key Derivation Guide - PBKDF2-HMAC-SHA512

Complete guide to password-based key derivation using the `KeyDerivationUtil` class with quantum-resistant PBKDF2-HMAC-SHA512.

## 📋 Table of Contents

- [Overview](#-overview)
- [Quick Start](#-quick-start)
- [API Reference](#-api-reference)
- [Security Guarantees](#-security-guarantees)
- [Quantum Resistance](#-quantum-resistance)
- [Best Practices](#-best-practices)
- [Performance Considerations](#-performance-considerations)
- [Migration from SHA-3-256](#-migration-from-sha-3-256)

---

## 🚀 Overview

`KeyDerivationUtil` provides **quantum-resistant password-based key derivation** using PBKDF2-HMAC-SHA512, replacing the vulnerable SHA-3-256 direct hashing approach **for password-based key derivation**.

**Note**: SHA-3-256 is still used correctly for **content hashing** (block integrity verification) - it's only vulnerable when misused for password-to-key derivation without salt and iterations.

### Why PBKDF2 Over Direct Hashing for Passwords?

**Context**: Comparison is for **password-to-key derivation**, NOT for general hashing.

| Attack Vector | SHA-3-256 Direct (for passwords) | PBKDF2-HMAC-SHA512 |
|---------------|------------------|---------------------|
| **Rainbow Tables** | ❌ Vulnerable | ✅ Immune (unique salt) |
| **GPU Brute Force** | ❌ ~1 billion hash/s | ✅ ~10,000 hash/s |
| **Dictionary Attack** | ❌ Fast (~2 hours for 8 chars) | ✅ Slow (~200 days for 8 chars) |
| **Pre-computation** | ❌ Possible | ✅ Impossible (salt-based) |
| **Quantum Attacks** | ⚠️ 128-bit security | ✅ 256-bit security |

### Core Features

- ✅ **OWASP 2023 Compliant**: 210,000 iterations for PBKDF2-HMAC-SHA512
- ✅ **NIST SP 800-132**: Follows NIST recommendations
- ✅ **Quantum-Resistant**: 256-bit post-quantum security
- ✅ **Cryptographically Secure**: Uses `SecureRandom` for salt generation
- ✅ **Memory Safety**: Clears sensitive data after use
- ✅ **Configurable**: Iteration count and key length adjustable

---

## ⚡ Quick Start

### Basic Key Derivation

```java
import com.rbatllet.blockchain.security.KeyDerivationUtil;
import javax.crypto.spec.SecretKeySpec;

// 1. Generate cryptographically secure salt
byte[] salt = KeyDerivationUtil.generateSalt();  // 128-bit (16 bytes)

// 2. Derive AES-256 key from password
byte[] key = KeyDerivationUtil.deriveKey("user-password", salt);
// Result: 256-bit (32 bytes) key derived with 210,000 iterations

// 3. Or get SecretKeySpec directly for AES encryption
SecretKeySpec secretKey = KeyDerivationUtil.deriveSecretKey("user-password", salt);

// 4. Use with AES-256-GCM encryption
GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
byte[] ciphertext = cipher.doFinal(plaintext);
```

### Encrypting and Storing Data

```java
// COMPLETE EXAMPLE: Password-protected file encryption

// 1. Generate salt and IV
byte[] salt = KeyDerivationUtil.generateSalt();  // 16 bytes
byte[] iv = new byte[12];  // 12 bytes for GCM
new SecureRandom().nextBytes(iv);

// 2. Derive encryption key
SecretKeySpec key = KeyDerivationUtil.deriveSecretKey("user-password", salt);

// 3. Encrypt data
GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
byte[] ciphertext = cipher.doFinal(plaintext);

// 4. Store: [salt][iv][ciphertext]
// CRITICAL: Salt MUST be stored with ciphertext for decryption
byte[] encrypted = new byte[salt.length + iv.length + ciphertext.length];
System.arraycopy(salt, 0, encrypted, 0, salt.length);
System.arraycopy(iv, 0, encrypted, salt.length, iv.length);
System.arraycopy(ciphertext, 0, encrypted, salt.length + iv.length, ciphertext.length);

Files.write(Path.of("encrypted.dat"), encrypted);
```

### Decrypting Stored Data

```java
// COMPLETE EXAMPLE: Decrypting password-protected file

// 1. Read encrypted file
byte[] encrypted = Files.readAllBytes(Path.of("encrypted.dat"));

// 2. Extract salt and IV
byte[] salt = Arrays.copyOfRange(encrypted, 0, 16);  // First 16 bytes
byte[] iv = Arrays.copyOfRange(encrypted, 16, 28);   // Next 12 bytes
byte[] ciphertext = Arrays.copyOfRange(encrypted, 28, encrypted.length);

// 3. Derive same key from password + salt
SecretKeySpec key = KeyDerivationUtil.deriveSecretKey("user-password", salt);

// 4. Decrypt
GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
byte[] plaintext = cipher.doFinal(ciphertext);

// ✅ Success: Correct password returns plaintext
// ❌ Failure: Wrong password throws AEADBadTagException
```

---

## 📚 API Reference

### Salt Generation

#### `generateSalt()`
```java
public static byte[] generateSalt()
```

Generates cryptographically secure 128-bit (16-byte) random salt.

**Returns**: Random salt bytes (16 bytes)

**Example**:
```java
byte[] salt = KeyDerivationUtil.generateSalt();
// Result: 16 random bytes from SecureRandom
```

#### `generateSalt(int length)`
```java
public static byte[] generateSalt(int length)
```

Generates cryptographically secure salt of specified length.

**Parameters**:
- `length` - Salt length in bytes (recommended: ≥16)

**Returns**: Random salt bytes

**Example**:
```java
byte[] customSalt = KeyDerivationUtil.generateSalt(32);  // 256-bit salt
```

---

### Key Derivation

#### `deriveKey(String password, byte[] salt)`
```java
public static byte[] deriveKey(String password, byte[] salt)
```

Derives 256-bit AES key using default parameters (210,000 iterations).

**Parameters**:
- `password` - Password to derive key from
- `salt` - Salt bytes (minimum 16 bytes recommended)

**Returns**: 32-byte (256-bit) derived key

**Throws**: `RuntimeException` if key derivation fails

**Example**:
```java
byte[] key = KeyDerivationUtil.deriveKey("my-password", salt);
// Result: 32 bytes derived with PBKDF2-HMAC-SHA512, 210k iterations
```

#### `deriveKey(String password, byte[] salt, int iterations, int keyLength)`
```java
public static byte[] deriveKey(String password, byte[] salt, int iterations, int keyLength)
```

Derives key with custom iteration count and length.

**Parameters**:
- `password` - Password to derive key from
- `salt` - Salt bytes
- `iterations` - Number of PBKDF2 iterations (recommended: ≥210,000)
- `keyLength` - Derived key length in bytes (e.g., 32 for AES-256)

**Returns**: Derived key bytes

**Example**:
```java
// High-security: 500k iterations
byte[] key = KeyDerivationUtil.deriveKey("password", salt, 500000, 32);

// Argon2-equivalent memory-hard parameters (if needed)
byte[] key = KeyDerivationUtil.deriveKey("password", salt, 100000, 64);
```

---

### SecretKeySpec Derivation

#### `deriveSecretKey(String password, byte[] salt)`
```java
public static SecretKeySpec deriveSecretKey(String password, byte[] salt)
```

Derives `SecretKeySpec` ready for AES cipher use.

**Parameters**:
- `password` - Password to derive key from
- `salt` - Salt bytes

**Returns**: `SecretKeySpec` for AES-256

**Cleanup**: Automatically clears intermediate key bytes from memory

**Example**:
```java
SecretKeySpec secretKey = KeyDerivationUtil.deriveSecretKey("password", salt);

// Direct use with Cipher
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
```

#### `deriveSecretKey(String password, byte[] salt, int iterations, int keyLength)`
```java
public static SecretKeySpec deriveSecretKey(String password, byte[] salt, int iterations, int keyLength)
```

Derives `SecretKeySpec` with custom parameters.

**Example**:
```java
SecretKeySpec key = KeyDerivationUtil.deriveSecretKey("password", salt, 300000, 32);
```

---

### Utility Methods

#### `getDefaultSaltLength()`
```java
public static int getDefaultSaltLength()
```

Returns default salt length in bytes (16 bytes = 128 bits).

#### `getDefaultIterations()`
```java
public static int getDefaultIterations()
```

Returns default iteration count (210,000 - OWASP 2023 recommendation).

#### `getDefaultKeyLength()`
```java
public static int getDefaultKeyLength()
```

Returns default key length in bytes (32 bytes = 256 bits for AES-256).

---

## 🛡️ Security Guarantees

### Cryptographic Standards

| Standard | Compliance | Details |
|----------|-----------|---------|
| **NIST SP 800-132** | ✅ Full | Salt length, iteration count, algorithm |
| **OWASP 2023** | ✅ Full | 210,000 iterations for PBKDF2-HMAC-SHA512 |
| **FIPS 140-2** | ✅ Compatible | PBKDF2 has FIPS-validated implementations |
| **RFC 2898** | ✅ Full | PKCS #5 v2.0 standard |

### Security Properties

#### Rainbow Table Immunity
```
Old (SHA-3-256 for password-to-key):
  password → SHA3(password) → Same key always
  ❌ Pre-computed table: "password123" → 0x7f83b1... (instant crack)
  ⚠️  Note: This is only a problem for password derivation, NOT for content hashing

New (PBKDF2 for password-to-key):
  password + unique salt → 210k iterations → Different key each time
  ✅ Unique salt prevents pre-computation (rainbow tables useless)
```

#### Brute Force Resistance
```
Password: "Test1234" (8 characters)

SHA-3-256 Direct:
  GPU: 1 billion hashes/second
  Time: ~2 hours to crack

PBKDF2-HMAC-SHA512 (210k iterations):
  GPU: ~10,000 derived keys/second (210k iterations slowdown)
  Time: ~200 days to crack

  Ratio: 100x slower brute force
```

#### Memory Safety
```java
// KeyDerivationUtil automatically clears sensitive data
public static SecretKeySpec deriveSecretKey(String password, byte[] salt) {
    byte[] keyBytes = deriveKey(password, salt);
    try {
        return new SecretKeySpec(keyBytes, "AES");
    } finally {
        Arrays.fill(keyBytes, (byte) 0);  // ✅ Cleared from memory
    }
}
```

---

## 🔮 Quantum Resistance

### Grover's Algorithm Impact

Grover's algorithm (quantum computer attack) reduces effective hash security by **half**:

| Hash Function | Classical Bits | Quantum Bits | Status |
|---------------|----------------|--------------|--------|
| SHA-256 | 256 bits | 128 bits | ⚠️ Borderline |
| SHA-384 | 384 bits | 192 bits | ✅ Quantum-safe |
| **SHA-512** | **512 bits** | **256 bits** | ✅✅ **Very safe** |
| SHA3-256 | 256 bits | 128 bits | ⚠️ Borderline |
| SHA3-512 | 512 bits | 256 bits | ✅✅ Very safe |

### PBKDF2-HMAC-SHA512 Quantum Security

```
PBKDF2-HMAC-SHA512:
  Base: HMAC-SHA-512 (512-bit output)
  Quantum Security: 256 bits (after Grover's algorithm)

  ✅ Exceeds 128-bit minimum for quantum resistance
  ✅ Provides comfortable security margin
  ✅ Future-proof for post-quantum era
```

### Why Not SHA3?

While SHA3-512 offers same quantum security, PBKDF2WithHmacSHA3-512 is **not available in Java standard library** (requires Bouncy Castle). HMAC-SHA512 provides identical quantum security with zero dependencies.

---

## 💡 Best Practices

### 1. Always Use Unique Salt Per Encryption

```java
// ✅ CORRECT: Unique salt per encryption
for (String data : dataList) {
    byte[] salt = KeyDerivationUtil.generateSalt();  // NEW salt each time
    SecretKeySpec key = KeyDerivationUtil.deriveSecretKey(password, salt);
    // Encrypt and store [salt][iv][ciphertext]
}

// ❌ WRONG: Reusing salt defeats the purpose
byte[] salt = KeyDerivationUtil.generateSalt();  // Only once
for (String data : dataList) {
    SecretKeySpec key = KeyDerivationUtil.deriveSecretKey(password, salt);  // Same salt!
    // Rainbow tables still possible
}
```

### 2. Store Salt With Ciphertext

```java
// ✅ CORRECT: Salt stored alongside encrypted data
byte[] encrypted = new byte[salt.length + iv.length + ciphertext.length];
System.arraycopy(salt, 0, encrypted, 0, salt.length);
System.arraycopy(iv, 0, encrypted, salt.length, iv.length);
System.arraycopy(ciphertext, 0, encrypted, salt.length + iv.length, ciphertext.length);

// Format: [16 bytes salt][12 bytes IV][N bytes ciphertext]
```

### 3. Validate Password Strength

```java
// Enforce minimum password requirements
if (password == null || password.length() < 12) {
    throw new IllegalArgumentException("Password must be at least 12 characters");
}

// Use PasswordUtil for strong validation
if (!PasswordUtil.validateStrongPassword(password)) {
    throw new IllegalArgumentException("Password must meet complexity requirements");
}
```

### 4. Handle Decryption Failures Securely

```java
try {
    SecretKeySpec key = KeyDerivationUtil.deriveSecretKey(password, salt);
    cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
    byte[] plaintext = cipher.doFinal(ciphertext);
    return plaintext;
} catch (AEADBadTagException e) {
    // ✅ CORRECT: Generic error message (don't reveal why it failed)
    throw new SecurityException("Decryption failed - incorrect password or corrupted data");
} catch (Exception e) {
    // ✅ CORRECT: Don't expose cryptographic details
    throw new SecurityException("Decryption failed");
}

// ❌ WRONG: Exposing details helps attackers
catch (AEADBadTagException e) {
    throw new SecurityException("Wrong password!");  // Reveals it's not corruption
}
```

### 5. Adjust Iterations for Hardware

```java
// Measure time on YOUR hardware
long start = System.currentTimeMillis();
KeyDerivationUtil.deriveKey(password, salt);
long time = System.currentTimeMillis() - start;

// Target: ~0.5 seconds
if (time < 100) {
    // Too fast - increase iterations
    KeyDerivationUtil.deriveKey(password, salt, 400000, 32);
} else if (time > 2000) {
    // Too slow - decrease iterations (minimum 100k)
    KeyDerivationUtil.deriveKey(password, salt, 150000, 32);
}
```

---

## ⚡ Performance Considerations

### Iteration Count vs Time

| Iterations | Time (CPU) | Security Level |
|-----------|------------|----------------|
| 100,000 | ~0.3s | ⚠️ Below OWASP 2023 |
| 210,000 | ~0.6s | ✅ OWASP 2023 recommended |
| 500,000 | ~1.5s | ✅✅ High security |
| 1,000,000 | ~3.0s | ✅✅✅ Maximum security |

**Recommendation**: Start with 210,000 (OWASP 2023) and adjust based on hardware and use case.

### Caching Derived Keys

```java
// For applications with frequent re-derivation
private final Map<String, SecretKeySpec> keyCache = new ConcurrentHashMap<>();

public SecretKeySpec getCachedKey(String password, byte[] salt) {
    String cacheKey = password + Base64.getEncoder().encodeToString(salt);

    return keyCache.computeIfAbsent(cacheKey, k ->
        KeyDerivationUtil.deriveSecretKey(password, salt)
    );
}

// ⚠️ WARNING: Caching reduces security - only for performance-critical applications
// Clear cache when no longer needed to minimize exposure
```

### Batch Processing

```java
// Process multiple encryptions efficiently
List<byte[]> salts = new ArrayList<>();
List<SecretKeySpec> keys = new ArrayList<>();

// Parallel key derivation (if CPU-bound)
IntStream.range(0, dataList.size()).parallel().forEach(i -> {
    byte[] salt = KeyDerivationUtil.generateSalt();
    SecretKeySpec key = KeyDerivationUtil.deriveSecretKey(password, salt);
    synchronized (salts) {
        salts.add(salt);
        keys.add(key);
    }
});
```

---

## 🔄 Migration from SHA-3-256 (Password-based Key Derivation Only)

**Important**: This migration applies ONLY to password-based key derivation. SHA-3-256 is still used correctly for content hashing (block integrity).

### Old Code (Vulnerable for Password-to-Key Derivation)
```java
// OLD: Direct SHA-3-256 hashing for password → key (DEPRECATED)
MessageDigest digest = MessageDigest.getInstance("SHA3-256");
byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
byte[] keyBytes = new byte[32];
System.arraycopy(hash, 0, keyBytes, 0, 32);
SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

// ❌ Vulnerable to rainbow tables (no salt)
// ❌ No brute-force resistance (no iterations)
// ❌ Same password = same key always
```

### New Code (Secure)
```java
// NEW: PBKDF2-HMAC-SHA512 with salt
byte[] salt = KeyDerivationUtil.generateSalt();
SecretKeySpec key = KeyDerivationUtil.deriveSecretKey(password, salt);

// ✅ Rainbow table immune
// ✅ 210k iterations slow brute force
// ✅ Unique salt = different key each time
// ✅ Quantum-resistant (256-bit security)
```

### Breaking Change Notice

**IMPORTANT**: Old encrypted data **CANNOT** be decrypted with new version. Migration required:

1. Decrypt all existing data with OLD code
2. Upgrade to new version
3. Re-encrypt with NEW code (generates salt automatically)

**No backward compatibility by design** - security takes priority over convenience.

---

## 📖 References

- [NIST SP 800-132](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-132.pdf) - Recommendation for Password-Based Key Derivation
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) - Current best practices
- [RFC 2898](https://www.rfc-editor.org/rfc/rfc2898) - PKCS #5: Password-Based Cryptography Specification
- [Quantum-Safe Cryptography Guide](https://cryptobook.nakov.com/quantum-safe-cryptography) - Post-quantum security

---

## 🎯 Summary

**Use `KeyDerivationUtil` for:**
- ✅ Password-protected file encryption
- ✅ Secure key storage (SecureKeyStorage)
- ✅ Off-chain data encryption (OffChainStorageService)
- ✅ Any password-to-key derivation needs

**Key Takeaways:**
1. Always generate unique salt per encryption
2. Store salt with ciphertext
3. Use 210k+ iterations (OWASP 2023)
4. 256-bit quantum security (Grover-resistant)
5. No backward compatibility - regenerate all keys

**Migration**: Replace `SHA3-256(password)` with `KeyDerivationUtil.deriveKey(password, salt)`
