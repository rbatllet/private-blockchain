# Quick Reference: Retroactive Encryption

## TL;DR

**Rule #1**: NEVER modify `block.data` after block creation - it breaks the hash chain.

**Solution**: Store encrypted data in `block.encryptionMetadata` field instead.

## Basic Usage

```java
// 1. Create unencrypted block
Block block = blockchain.addBlock("Sensitive data", privateKey, publicKey);

// 2. Later: Retroactively encrypt
blockchain.encryptExistingBlock(block.getBlockNumber(), "password");

// 3. Access requires password
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
String decrypted = api.retrieveSecret(block.getBlockNumber(), "password");
```

## Architecture

```
Block Structure After Retroactive Encryption:
├─ data: "Original data" ← UNCHANGED (for hash integrity)
├─ encryptionMetadata: "AES-encrypted..." ← Encrypted version
├─ isEncrypted: true ← Status flag
└─ hash: "87e70a4e..." ← SAME as before (chain valid)
```

## Why This Design?

```java
// Hash is calculated from original data:
hash = SHA-256(data + previousHash + timestamp + ...)

// CRITICAL: 'data' field must NEVER be modified
// Our solution stores encrypted data separately:
block.setEncryptionMetadata(encrypted);  // ✅ MAINTAINS CHAIN
// block.data remains unchanged for hash integrity
```

## Common Use Cases

### GDPR Right to Be Forgotten
```java
long blockNumber = findUserBlock("user@example.com");
blockchain.encryptExistingBlock(blockNumber, "gdpr-key-2024");
```

### HIPAA Compliance
```java
List<Block> medical = blockchain.searchByCategory("MEDICAL");
medical.forEach(b -> blockchain.encryptExistingBlock(b.getBlockNumber(), "hipaa-key"));
```

### Security Incident Response
```java
// Emergency encryption
blockchain.encryptExistingBlock(compromisedBlockNumber, generateEmergencyKey());
```

## Security Notes

- **Database**: Original data in `data` field (physical security required)
- **Application**: Password check enforced before data access
- **Cryptography**: AES-256-GCM with PBKDF2 key derivation
- **Blockchain**: Hash integrity maintained (chain validation works)

## Full Documentation

📖 [RETROACTIVE_ENCRYPTION_ARCHITECTURE.md](../security/RETROACTIVE_ENCRYPTION_ARCHITECTURE.md) - Complete technical details
