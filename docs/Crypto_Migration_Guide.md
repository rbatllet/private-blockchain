# 🔐 Cryptographic Migration Guide: RSA/SHA-2 to ECDSA/SHA-3

## 📋 Overview

This document outlines the cryptographic migration from RSA/SHA-2 to ECDSA/SHA-3 in the private blockchain implementation. The changes include:

- **Hashing Algorithm**: SHA-2 → SHA3-256
- **Signature Algorithm**: RSA → ECDSA (secp256r1 curve)
- **Key Management**: Hierarchical key management system
- **Validation**: Enhanced chain validation with detailed reporting

## 🚀 Key Changes

### 1. Hashing Algorithm

| Before (Deprecated) | After (New) |
|---------------------|-------------|
| SHA-256 | SHA3-256 |
| Less resistant to length extension attacks | More secure against length extension |
| Slower on hardware | Better performance on modern hardware |

### 2. Digital Signatures

| Before (Deprecated) | After (New) |
|---------------------|-------------|
| RSA (2048-bit) | ECDSA (secp256r1) |
| Larger key sizes | Smaller key sizes for equivalent security |
| Slower verification | Faster verification |
| `SHA256withRSA` | `SHA3-256withECDSA` |

### 3. Key Management Hierarchy

The new implementation uses a three-tier key hierarchy:

1. **Root Keys**
   - Longest validity (5 years)
   - Used to sign intermediate keys
   - Stored most securely

2. **Intermediate Keys**
   - Medium validity (1 year)
   - Signed by root keys
   - Used to sign operational keys

3. **Operational Keys**
   - Shortest validity (90 days)
   - Used for daily operations
   - Signed by intermediate keys

## 🔄 Migration Steps

### 1. Update Dependencies

Ensure you have the latest version of the blockchain library that includes the ECDSA/SHA-3 implementation.

### 2. Key Generation

Old (RSA):
```java
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
keyGen.initialize(2048);
KeyPair keyPair = keyGen.generateKeyPair();
```

New (ECDSA):
```java
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;

// Generate a new root key
CryptoUtil.KeyInfo rootKey = CryptoUtil.createRootKey();

// Or generate a key using the utility method
KeyPair keyPair = CryptoUtil.generateKeyPair();
```

### 3. Signing Data

Old (RSA):
```java
Signature signature = Signature.getInstance("SHA256withRSA");
signature.initSign(privateKey);
signature.update(data.getBytes(StandardCharsets.UTF_8));
byte[] signedData = signature.sign();
```

New (ECDSA):
```java
import com.rbatllet.blockchain.util.CryptoUtil;

String signature = CryptoUtil.signData(data, privateKey);
```

### 4. Verifying Signatures

Old (RSA):
```java
Signature signature = Signature.getInstance("SHA256withRSA");
signature.initVerify(publicKey);
signature.update(data.getBytes(StandardCharsets.UTF_8));
boolean isValid = signature.verify(signatureBytes);
```

New (ECDSA):
```java
import com.rbatllet.blockchain.util.CryptoUtil;

boolean isValid = CryptoUtil.verifySignature(data, signature, publicKey);
```

### 5. Chain Validation

Modern approach:
```java
import com.rbatllet.blockchain.model.ChainValidationResult;

ChainValidationResult result = blockchain.validateChainDetailed();

if (result.isStructurallyIntact()) {
    if (result.isFullyCompliant()) {
        System.out.println("✅ Chain is fully valid");
    } else {
        System.out.println("⚠️ Chain has authorization issues");
        System.out.println("Revoked blocks: " + result.getRevokedBlocks());
    }
} else {
    System.out.println("❌ Chain has structural problems");
    System.out.println("Invalid blocks: " + result.getInvalidBlocks());
}
```

## 🔐 Security Considerations

1. **Key Rotation**:
   - Root keys should be rotated every 5 years
   - Intermediate keys should be rotated annually
   - Operational keys should be rotated every 90 days

2. **Key Storage**:
   - Root keys should be stored in a Hardware Security Module (HSM) or secure key vault
   - Private keys should never be stored in version control
   - Use environment variables or secure configuration management for key storage

3. **Migration Period**:
   - Support both old and new signatures during migration
   - Set a deadline for complete migration to the new algorithms
   - Monitor for any compatibility issues during the transition

## 📊 Performance Impact

- **Faster Verification**: ECDSA signature verification is significantly faster than RSA
- **Smaller Signatures**: ECDSA signatures are smaller than RSA signatures
- **Reduced Bandwidth**: Smaller signatures mean less data transfer

## 🧪 Testing

1. Unit tests have been updated to use the new cryptographic functions
2. Integration tests verify compatibility between components
3. Performance tests show improved throughput with the new algorithms

## 📅 Timeline

1. **Phase 1 (Month 1)**: Deploy new version with both old and new algorithms
2. **Phase 2 (Month 2)**: Update all components to use new algorithms
3. **Phase 3 (Month 3)**: Remove deprecated code and complete migration

## 📝 Additional Notes

- Use `validateChainDetailed()` for comprehensive validation with detailed reporting.
- Old RSA keys can be migrated to ECDSA by reissuing them using the new key generation methods.
- The new implementation is thread-safe and includes proper synchronization.

For any questions or issues, please contact the development team.
