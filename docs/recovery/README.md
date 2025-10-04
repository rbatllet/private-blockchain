# Recovery & Validation

Chain validation, recovery, and off-chain integrity verification guides.

## 📚 Documents (4 files)

| Document | Description |
|----------|-------------|
| **[ENHANCED_VALIDATION_GUIDE.md](ENHANCED_VALIDATION_GUIDE.md)** | Advanced chain validation techniques |
| **[RECOVERY_CHECKPOINT_USAGE_GUIDE.md](RECOVERY_CHECKPOINT_USAGE_GUIDE.md)** | Recovery checkpoints and rollback |
| **[OFFCHAIN_INTEGRITY_REPORT_GUIDE.md](OFFCHAIN_INTEGRITY_REPORT_GUIDE.md)** | Comprehensive off-chain integrity verification |
| **[OFFCHAIN_INTEGRITY_REPORT_QUICK_START.md](OFFCHAIN_INTEGRITY_REPORT_QUICK_START.md)** | Quick start with integrity reports |

## 🎯 Quick Reference

### Validate Chain
```java
ChainValidationResult result = blockchain.validateChainDetailed();
```

### Create Recovery Checkpoint
```java
RecoveryCheckpoint checkpoint = blockchain.createRecoveryCheckpoint();
```

### Verify Off-Chain Integrity
```java
OffChainIntegrityReport report = blockchain.verifyAllOffChainIntegrity();
```

---
**Directory**: `docs/recovery/` | **Files**: 4 | **Updated**: 2025-10-04
