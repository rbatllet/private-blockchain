# Real-World Examples & Use Cases

Comprehensive real-world examples and practical use cases for the Private Blockchain implementation.

> **IMPORTANT NOTE**: The classes shown in these examples (DocumentVerificationSystem, SupplyChainTracker, MedicalRecordsSystem, FinancialAuditSystem, KeyRotationExample, KeyCleanupManager, BlockchainHealthMonitor, HealthStatus) are conceptual and designed to illustrate potential use cases. They are not part of the actual project code. Only the Blockchain, Block, AuthorizedKey, Blockchain.KeyDeletionImpact, CryptoUtil, and JPAUtil classes exist in the current implementation.
>
> For real working examples, refer to the following classes in the source code:
> - `BlockchainDemo.java` - Basic blockchain demonstration
> - `AdditionalAdvancedFunctionsDemo.java` - Advanced features demonstration
> - `ChainRecoveryDemo.java` - Chain recovery demonstration
> - `DangerousDeleteDemo.java` - Key deletion safety features demo
> - `EnhancedRecoveryExample.java` - Advanced recovery techniques example

## 📋 Table of Contents

- [Use Case Examples](#-use-case-examples)
- [Common Workflow Patterns](#-common-workflow-patterns)
- [Integration Examples](#-integration-examples)

## 🎯 Use Case Examples

### Use Case 1: Document Verification System

Track and verify document authenticity with immutable records.

```java
// Required imports
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public class DocumentVerificationSystem {
    public static void main(String[] args) {
        try {
            Blockchain blockchain = new Blockchain();
            
            // Setup document verification authorities
            KeyPair notary = CryptoUtil.generateKeyPair();
            KeyPair university = CryptoUtil.generateKeyPair();
            KeyPair government = CryptoUtil.generateKeyPair();
            
            String notaryKey = CryptoUtil.publicKeyToString(notary.getPublic());
            String universityKey = CryptoUtil.publicKeyToString(university.getPublic());
            String governmentKey = CryptoUtil.publicKeyToString(government.getPublic());
            
            blockchain.addAuthorizedKey(notaryKey, "Public Notary Office");
            blockchain.addAuthorizedKey(universityKey, "University of Barcelona");
            blockchain.addAuthorizedKey(governmentKey, "Government Registry");
            
            // Record document verifications
            blockchain.addBlock("Document: Birth Certificate #BC-2025-001 | Status: VERIFIED | Hash: sha256:a1b2c3...", 
                              government.getPrivate(), government.getPublic());
            
            blockchain.addBlock("Document: University Diploma #UB-CS-2025-456 | Status: AUTHENTIC | Graduate: John Doe", 
                              university.getPrivate(), university.getPublic());
            
            blockchain.addBlock("Document: Property Deed #PD-BCN-2025-789 | Property: Carrer Balmes 123 | Owner: Jane Smith", 
                              notary.getPrivate(), notary.getPublic());
            
            // Verify document authenticity
            List<Block> certificateRecords = blockchain.searchBlocksByContent("Birth Certificate");
            System.out.println("Birth certificates found: " + certificateRecords.size());
            
            // Export for external verification
            blockchain.exportChain("document_verification_backup.json");
            System.out.println("Document verification chain backed up successfully!");
            
        } catch (Exception e) {
            System.err.println("Document verification error: " + e.getMessage());
        }
    }
}
```

**Key Benefits:**
- **Immutable Records**: Documents cannot be altered once verified
- **Multi-Authority Verification**: Different authorities can verify different document types
- **Audit Trail**: Complete history of all document verifications
- **Export Capability**: Easy backup and sharing with external systems

### Use Case 2: Supply Chain Management

Track products through the supply chain with full traceability.

```java
// Required imports
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDate;
import java.util.List;

public class SupplyChainTracker {
    public static void main(String[] args) {
        try {
            Blockchain blockchain = new Blockchain();
            
            // Setup supply chain participants
            KeyPair manufacturer = CryptoUtil.generateKeyPair();
            KeyPair distributor = CryptoUtil.generateKeyPair();
            KeyPair retailer = CryptoUtil.generateKeyPair();
            KeyPair qualityControl = CryptoUtil.generateKeyPair();
            
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(manufacturer.getPublic()), "Barcelona Electronics Mfg");
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(distributor.getPublic()), "Iberian Distribution Ltd");
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(retailer.getPublic()), "TechStore Barcelona");
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(qualityControl.getPublic()), "EU Quality Assurance");
            
            // Track product lifecycle
            blockchain.addBlock("MANUFACTURED: Product #PS5-2025-001 | Location: Barcelona Factory | Date: 2025-06-10 | Batch: B2025-156", 
                              manufacturer.getPrivate(), manufacturer.getPublic());
            
            blockchain.addBlock("QUALITY_CHECK: Product #PS5-2025-001 | Status: PASSED | Tests: Safety, Performance | Inspector: QC-007", 
                              qualityControl.getPrivate(), qualityControl.getPublic());
            
            blockchain.addBlock("SHIPPED: Product #PS5-2025-001 | From: Barcelona | To: Madrid | Carrier: Express Logistics | Tracking: EL123456", 
                              distributor.getPrivate(), distributor.getPublic());
            
            blockchain.addBlock("RECEIVED: Product #PS5-2025-001 | Store: TechStore Madrid | Condition: Excellent | Shelf: A-15", 
                              retailer.getPrivate(), retailer.getPublic());
            
            blockchain.addBlock("SOLD: Product #PS5-2025-001 | Customer: [ANONYMIZED] | Date: 2025-06-15 | Warranty: 2 years", 
                              retailer.getPrivate(), retailer.getPublic());
            
            // Track product history
            List<Block> productHistory = blockchain.searchBlocksByContent("PS5-2025-001");
            System.out.println("Product lifecycle events: " + productHistory.size());
            
            // Generate compliance report
            LocalDate startDate = LocalDate.of(2025, 6, 1);
            LocalDate endDate = LocalDate.of(2025, 6, 30);
            List<Block> monthlyActivity = blockchain.getBlocksByDateRange(startDate, endDate);
            System.out.println("June 2025 supply chain activity: " + monthlyActivity.size() + " events");
            
        } catch (Exception e) {
            System.err.println("Supply chain tracking error: " + e.getMessage());
        }
    }
}
```

**Key Benefits:**
- **Full Traceability**: Track products from manufacture to sale
- **Quality Assurance**: Record all quality checks and certifications
- **Compliance Reporting**: Generate reports for regulatory requirements
- **Multi-Stakeholder**: Different participants can add relevant information

### Use Case 3: Medical Records Management

Secure and auditable medical record system with privacy protection.

```java
// Required imports
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public class MedicalRecordsSystem {
    public static void main(String[] args) {
        try {
            Blockchain blockchain = new Blockchain();
            
            // Setup medical system participants
            KeyPair hospital = CryptoUtil.generateKeyPair();
            KeyPair doctor = CryptoUtil.generateKeyPair();
            KeyPair pharmacy = CryptoUtil.generateKeyPair();
            KeyPair insurance = CryptoUtil.generateKeyPair();
            
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(hospital.getPublic()), "Hospital Clínic Barcelona");
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(doctor.getPublic()), "Dr. Maria Garcia - Cardiology");
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(pharmacy.getPublic()), "Farmacia Central");
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(insurance.getPublic()), "Sanitas Insurance");
            
            // Record medical events (anonymized)
            blockchain.addBlock("PATIENT_ADMISSION: ID: P-[HASH] | Department: Cardiology | Condition: Routine Checkup | Date: 2025-06-10", 
                              hospital.getPrivate(), hospital.getPublic());
            
            blockchain.addBlock("DIAGNOSIS: Patient: P-[HASH] | Condition: Hypertension Stage 1 | Treatment: Lifestyle + Medication | Doctor: Dr. Garcia", 
                              doctor.getPrivate(), doctor.getPublic());
            
            blockchain.addBlock("PRESCRIPTION: Patient: P-[HASH] | Medication: Lisinopril 10mg | Quantity: 30 tablets | Duration: 30 days", 
                              doctor.getPrivate(), doctor.getPublic());
            
            blockchain.addBlock("DISPENSED: Prescription: RX-2025-789 | Patient: P-[HASH] | Medication: Lisinopril 10mg | Pharmacist: PharmD Lopez", 
                              pharmacy.getPrivate(), pharmacy.getPublic());
            
            blockchain.addBlock("CLAIM_PROCESSED: Patient: P-[HASH] | Service: Cardiology Consultation | Amount: €120.00 | Status: APPROVED", 
                              insurance.getPrivate(), insurance.getPublic());
            
            // Audit medical records
            List<Block> patientRecords = blockchain.searchBlocksByContent("P-[HASH]");
            System.out.println("Patient record entries: " + patientRecords.size());
            
            // Compliance validation
            boolean chainValid = blockchain.validateChain();
            System.out.println("Medical records integrity: " + (chainValid ? "VERIFIED" : "COMPROMISED"));
            
            // Secure backup for regulatory compliance
            blockchain.exportChain("medical_records_backup_" + java.time.LocalDate.now() + ".json");
            
        } catch (Exception e) {
            System.err.println("Medical records system error: " + e.getMessage());
        }
    }
}
```

**Key Benefits:**
- **Privacy Protection**: Patient data is anonymized with hash identifiers
- **Multi-Provider Coordination**: Hospitals, doctors, pharmacies, and insurance work together
- **Regulatory Compliance**: Immutable audit trail for healthcare regulations
- **Data Integrity**: Cryptographic validation ensures medical record authenticity

### Use Case 4: Financial Audit Trail

Create an immutable audit trail for financial transactions.

```java
// Required imports
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public class FinancialAuditSystem {
    public static void main(String[] args) {
        try {
            Blockchain blockchain = new Blockchain();
            
            // Setup financial system participants
            KeyPair bank = CryptoUtil.generateKeyPair();
            KeyPair auditor = CryptoUtil.generateKeyPair();
            KeyPair compliance = CryptoUtil.generateKeyPair();
            KeyPair regulator = CryptoUtil.generateKeyPair();
            
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(bank.getPublic()), "Banco Santander España");
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(auditor.getPublic()), "PwC Auditing Services");
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(compliance.getPublic()), "Internal Compliance Dept");
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(regulator.getPublic()), "Banco de España");
            
            // Record financial transactions and audits
            blockchain.addBlock("TRANSACTION: ID: TXN-2025-001 | Type: Wire Transfer | Amount: €50,000.00 | From: [ENCRYPTED] | To: [ENCRYPTED] | Status: COMPLETED", 
                              bank.getPrivate(), bank.getPublic());
            
            blockchain.addBlock("AML_CHECK: Transaction: TXN-2025-001 | Status: CLEARED | Risk Score: LOW | Officer: Compliance-007 | Date: 2025-06-10", 
                              compliance.getPrivate(), compliance.getPublic());
            
            blockchain.addBlock("AUDIT_REVIEW: Transaction: TXN-2025-001 | Auditor: PwC-Team-Alpha | Finding: COMPLIANT | Documentation: COMPLETE", 
                              auditor.getPrivate(), auditor.getPublic());
            
            blockchain.addBlock("REGULATORY_FILING: Report: Q2-2025-WIRE-TRANSFERS | Transactions: 1,247 | Total Value: €12,500,000 | Status: SUBMITTED", 
                              regulator.getPrivate(), regulator.getPublic());
            
            // Generate audit reports
            List<Block> auditTrail = blockchain.searchBlocksByContent("TXN-2025-001");
            System.out.println("Transaction audit trail: " + auditTrail.size() + " entries");
            
            // Validate audit integrity
            boolean auditValid = blockchain.validateChain();
            System.out.println("Audit trail integrity: " + (auditValid ? "VALID" : "COMPROMISED"));
            
            // Export for regulatory submission
            blockchain.exportChain("financial_audit_Q2_2025.json");
            
        } catch (Exception e) {
            System.err.println("Financial audit system error: " + e.getMessage());
        }
    }
}
```

**Key Benefits:**
- **Regulatory Compliance**: Complete audit trail for financial regulations
- **Multi-Layer Verification**: Bank, internal compliance, external auditors, and regulators
- **Immutable Records**: Financial transactions cannot be altered or deleted
- **Export for Authorities**: Easy submission to regulatory bodies

## 🔄 Common Workflow Patterns

### Daily Backup Routine

```java
public void performDailyBackup(Blockchain blockchain) {
    try {
        // Generate timestamp-based backup filename
        String backupFile = "blockchain_backup_" + 
                           java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + 
                           ".json";
        
        // Validate chain before backup
        if (!blockchain.validateChain()) {
            System.err.println("WARNING: Chain validation failed - backup may contain corrupted data");
        }
        
        // Perform backup
        if (blockchain.exportChain(backupFile)) {
            System.out.println("Daily backup completed: " + backupFile);
            
            // Log backup statistics
            long blockCount = blockchain.getBlockCount();
            int keyCount = blockchain.getAuthorizedKeys().size();
            System.out.println("Backup contains: " + blockCount + " blocks, " + keyCount + " authorized keys");
        }
        
    } catch (Exception e) {
        System.err.println("Daily backup failed: " + e.getMessage());
    }
}
```

### Batch Transaction Processing

```java
public void processBatchTransactions(Blockchain blockchain, List<String> transactions, 
                                   PrivateKey signerKey, PublicKey signerPublic) {
    try {
        int successCount = 0;
        int failureCount = 0;
        
        for (String transaction : transactions) {
            // Validate transaction size
            if (transaction.length() > blockchain.getMaxBlockDataLength()) {
                System.err.println("Transaction too large, skipping: " + transaction.substring(0, 50) + "...");
                failureCount++;
                continue;
            }
            
            // Add transaction to blockchain
            if (blockchain.addBlock(transaction, signerKey, signerPublic)) {
                successCount++;
                if (successCount % 100 == 0) {
                    System.out.println("Processed " + successCount + " transactions...");
                }
            } else {
                failureCount++;
                System.err.println("Failed to add transaction: " + transaction.substring(0, 50) + "...");
            }
        }
        
        System.out.println("Batch processing complete: " + successCount + " successful, " + failureCount + " failed");
        
        // Validate chain after batch processing
        if (blockchain.validateChain()) {
            System.out.println("Chain validation successful after batch processing");
        } else {
            System.err.println("WARNING: Chain validation failed after batch processing");
        }
        
    } catch (Exception e) {
        System.err.println("Batch processing error: " + e.getMessage());
    }
}
```

For more detailed API information and technical specifications, see [API_GUIDE.md](API_GUIDE.md) and [TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md).

### Key Management Patterns

```java
// Required imports
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Example class demonstrating key rotation best practices using the Blockchain API.
 * This shows how to safely rotate keys with an overlap period to ensure continuity.
 */
public class KeyRotationExample {
    
    // Simple key rotation example with overlap period for security
    public boolean rotateKey(Blockchain blockchain, String oldPublicKey, String ownerName) {
        try {
            System.out.println("🔄 Starting key rotation for: " + ownerName);
            
            // Generate new key
            KeyPair newKeyPair = CryptoUtil.generateKeyPair();
            String newPublicKey = CryptoUtil.publicKeyToString(newKeyPair.getPublic());
            
            // Add new key to blockchain
            String fullOwnerName = ownerName + " (Rotated Key)";
            if (!blockchain.addAuthorizedKey(newPublicKey, fullOwnerName)) {
                System.err.println("❌ Failed to authorize new key");
                return false;
            }
            
            System.out.println("✅ New key authorized for: " + fullOwnerName);
            System.out.println("🔑 New public key: " + newPublicKey.substring(0, 32) + "...");
            
            // Allow overlap period for transition
            System.out.println("⏱️  Overlap period: Old and new keys both active");
            Thread.sleep(1000); // Simulate overlap period
            
            // Revoke old key
            if (blockchain.revokeAuthorizedKey(oldPublicKey)) {
                System.out.println("✅ Key rotation completed successfully");
                System.out.println("🗑️  Old key revoked: " + oldPublicKey.substring(0, 32) + "...");
                return true;
            } else {
                System.err.println("❌ Failed to revoke old key during rotation");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("💥 Key rotation error: " + e.getMessage());
            return false;
        }
    }
}
```

### Safe and Dangerous Key Deletion Examples

```java
// Required imports
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

/**
 * Example class demonstrating the multi-level key deletion API.
 * Shows proper usage of the KeyDeletionImpact analysis and various deletion methods
 * with appropriate safety checks.
 */
public class KeyCleanupManager {
    
    // RECOMMENDED: Safe key deletion with impact analysis
    public boolean safeDeleteKey(Blockchain blockchain, String publicKey, String reason) {
        try {
            System.out.println("🔍 SAFE KEY DELETION: Analyzing impact first");
            System.out.println("🔑 Key: " + publicKey.substring(0, 32) + "...");
            System.out.println("📝 Reason: " + reason);
            
            // Step 1: Analyze impact before deletion
            Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(publicKey);
            System.out.println("📊 Impact Analysis: " + impact);
            
            if (!impact.keyExists()) {
                System.out.println("❌ Key not found in database");
                return false;
            }
            
            if (!impact.canSafelyDelete()) {
                System.out.println("⚠️ UNSAFE: Key has signed " + impact.getAffectedBlocks() + " historical blocks");
                System.out.println("💡 Use dangerousDeleteKey() if deletion is absolutely necessary");
                return false;
            }
            
            // Step 2: Safe deletion (no blocks affected)
            boolean deleted = blockchain.deleteAuthorizedKey(publicKey);
            
            if (deleted) {
                System.out.println("✅ Key safely deleted - no historical blocks affected");
                return true;
            } else {
                System.err.println("❌ Safe deletion failed");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("💥 Safe deletion error: " + e.getMessage());
            return false;
        }
    }
    
    // DANGEROUS: Permanent key deletion for compliance scenarios
    public boolean dangerousDeleteKey(Blockchain blockchain, String publicKey, boolean force, String reason) {
        try {
            System.out.println("⚠️ DANGEROUS KEY DELETION: Use with extreme caution");
            System.out.println("🔑 Key: " + publicKey.substring(0, 32) + "...");
            System.out.println("📝 Reason: " + reason);
            System.out.println("⚡ Force mode: " + force);
            
            // Step 1: Always analyze impact first
            Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(publicKey);
            System.out.println("📊 Impact Analysis: " + impact);
            
            if (!impact.keyExists()) {
                System.out.println("❌ Key not found in database");
                return false;
            }
            
            // Step 2: Show warnings for severe impact
            if (impact.isSevereImpact()) {
                System.out.println("🚨 SEVERE IMPACT WARNING:");
                System.out.println("   - " + impact.getAffectedBlocks() + " historical blocks will be orphaned");
                System.out.println("   - Blockchain validation will FAIL for these blocks");
                System.out.println("   - This action is IRREVERSIBLE");
                
                if (!force) {
                    System.out.println("❌ Deletion blocked - use force=true to override safety");
                    return false;
                }
            }
            
            // Step 3: Perform dangerous deletion
            // Use the correct API method name and parameter order
            boolean deleted = blockchain.dangerouslyDeleteAuthorizedKey(publicKey, force, reason);
            
            if (deleted) {
                System.out.println("🗑️ ✅ Key permanently deleted from database");
                System.out.println("⚠️ WARNING: This action was IRREVERSIBLE!");
                
                if (impact.isSevereImpact()) {
                    System.out.println("💡 RECOMMENDED: Run blockchain.validateChain() to verify integrity");
                }
                return true;
            } else {
                System.err.println("❌ Dangerous deletion failed or was blocked by safety checks");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("💥 Dangerous deletion error: " + e.getMessage());
            return false;
        }
    }
    
    // Practical usage examples
    public void demonstrateKeyDeletionWorkflow(Blockchain blockchain) {
        // Example 1: Safe deletion workflow
        System.out.println("=== EXAMPLE 1: Safe Deletion Workflow ===");
        
        // Create a test key that hasn't signed any blocks
        KeyPair testKey = CryptoUtil.generateKeyPair();
        String testPublicKey = CryptoUtil.publicKeyToString(testKey.getPublic());
        blockchain.addAuthorizedKey(testPublicKey, "Test User - No Blocks");
        
        // Safe deletion - should succeed
        boolean safeResult = safeDeleteKey(blockchain, testPublicKey, "Cleanup test keys");
        System.out.println("Safe deletion result: " + (safeResult ? "SUCCESS ✅" : "FAILED ❌"));
        
        // Example 2: Blocked deletion workflow  
        System.out.println("\n=== EXAMPLE 2: Blocked Deletion Workflow ===");
        
        // Create a key and use it to sign blocks
        KeyPair activeKey = CryptoUtil.generateKeyPair();
        String activePublicKey = CryptoUtil.publicKeyToString(activeKey.getPublic());
        blockchain.addAuthorizedKey(activePublicKey, "Active User - Has Blocks");
        blockchain.addBlock("Important transaction", activeKey.getPrivate(), activeKey.getPublic());
        
        // Attempt safe deletion - should be blocked because key has signed blocks
        boolean blockedResult = safeDeleteKey(blockchain, activePublicKey, "Attempt to delete active key");
        System.out.println("Blocked deletion result: " + (blockedResult ? "UNEXPECTED SUCCESS ⚠️" : "CORRECTLY BLOCKED ✅"));
        
        // Example 3: Forced dangerous deletion (emergency)
        System.out.println("\n=== EXAMPLE 3: Emergency Forced Deletion ===");
        
        // This should only be done in emergencies (security incidents, GDPR)
        boolean forcedResult = dangerousDeleteKey(blockchain, activePublicKey, true,
                                                "Security incident: key compromised");
        System.out.println("Forced deletion result: " + (forcedResult ? "SUCCESS (DANGEROUS) ⚠️" : "FAILED ❌"));
        
        // Verify blockchain integrity after forced deletion
        boolean chainValid = blockchain.validateChain();
        System.out.println("Blockchain integrity after forced deletion: " + 
                          (chainValid ? "VALID ✅" : "COMPROMISED ❌ (expected)"));
    }
    
    // Bulk cleanup for security compliance (enhanced with new safety features)
    public void performSecurityCompliantCleanup(Blockchain blockchain, String complianceReason) {
        System.out.println("🔒 Starting ENHANCED security compliance cleanup");
        System.out.println("📋 Reason: " + complianceReason);
        
        List<AuthorizedKey> allKeys = blockchain.getAllAuthorizedKeys();
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusYears(7); // 7-year retention
        
        int safeDeleted = 0;
        int dangerousSkipped = 0;
        
        for (AuthorizedKey key : allKeys) {
            // Only process revoked keys older than cutoff
            if (!key.isActive() && key.getRevokedAt() != null && key.getRevokedAt().isBefore(cutoffDate)) {
                System.out.println("🧹 Analyzing old revoked key: " + key.getOwnerName());
                
                // Use new safety analysis
                Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(key.getPublicKey());
                
                if (impact.canSafelyDelete()) {
                    // Safe to delete
                    boolean deleted = blockchain.deleteAuthorizedKey(key.getPublicKey());
                    if (deleted) {
                        safeDeleted++;
                        System.out.println("✅ Safely deleted: " + key.getOwnerName());
                    }
                } else {
                    // Has historical blocks - skip for safety
                    dangerousSkipped++;
                    System.out.println("⚠️ Skipped (has " + impact.getAffectedBlocks() + " blocks): " + key.getOwnerName());
                    System.out.println("   Use manual review for forced deletion if absolutely necessary");
                }
            }
        }
        
        System.out.println("✅ Enhanced security compliance cleanup completed");
        System.out.println("📊 Results: " + safeDeleted + " safely deleted, " + dangerousSkipped + " skipped for safety");
    }
}
```

### Health Check and Monitoring

```java
// Required imports
import java.io.File;
import java.util.List;

/**
 * Example monitoring class for blockchain health checks.
 * This is a conceptual class to demonstrate monitoring patterns.
 */
public class BlockchainHealthMonitor {
    
    /**
     * Performs a comprehensive health check on the blockchain.
     * @param blockchain The blockchain instance to check
     * @return A HealthStatus object containing various health metrics
     */
public HealthStatus performHealthCheck(Blockchain blockchain) {
        // HealthStatus is a conceptual class for this example
        HealthStatus status = new HealthStatus();
        
        try {
            // Check basic blockchain properties
            long blockCount = blockchain.getBlockCount();
            status.setBlockCount(blockCount);
            
            // Validate chain integrity
            boolean chainValid = blockchain.validateChain();
            status.setChainValid(chainValid);
            
            // Check authorized keys
            List<AuthorizedKey> activeKeys = blockchain.getAuthorizedKeys();
            status.setActiveKeys(activeKeys.size());
            
            // Check database file properties
            // Note: In production, the database path should be configurable
            File dbFile = new File(blockchain.getDatabasePath());
            if (dbFile.exists()) {
                status.setDatabaseSize(dbFile.length());
                status.setFreeSpace(dbFile.getFreeSpace());
            }
            
            // Calculate average block size
            if (blockCount > 0) {
                long avgBlockSize = status.getDatabaseSize() / blockCount;
                status.setAverageBlockSize(avgBlockSize);
            }
            
            // Overall health assessment
            if (chainValid && status.getFreeSpace() > 1000000000 && status.getActiveKeys() > 0) {
                status.setOverallHealth("HEALTHY");
            } else if (chainValid) {
                status.setOverallHealth("WARNING");
            } else {
                status.setOverallHealth("CRITICAL");
            }
            
        } catch (Exception e) {
            status.setOverallHealth("ERROR");
            status.setError(e.getMessage());
        }
        
        return status;
    }
    
    /**
     * Conceptual class representing blockchain health metrics.
     * This is for example purposes only.
     */
    public static class HealthStatus {
        private long blockCount;
        private boolean chainValid;
        private int activeKeys;
        private long databaseSize;
        private long freeSpace;
        private long averageBlockSize;
        private String overallHealth;
        private String error;
        
        // Getters and setters
        public long getBlockCount() { return blockCount; }
        public void setBlockCount(long blockCount) { this.blockCount = blockCount; }
        public boolean isChainValid() { return chainValid; }
        public void setChainValid(boolean chainValid) { this.chainValid = chainValid; }
        public int getActiveKeys() { return activeKeys; }
        public void setActiveKeys(int activeKeys) { this.activeKeys = activeKeys; }
        public long getDatabaseSize() { return databaseSize; }
        public void setDatabaseSize(long databaseSize) { this.databaseSize = databaseSize; }
        public long getFreeSpace() { return freeSpace; }
        public void setFreeSpace(long freeSpace) { this.freeSpace = freeSpace; }
        public long getAverageBlockSize() { return averageBlockSize; }
        public void setAverageBlockSize(long averageBlockSize) { this.averageBlockSize = averageBlockSize; }
        public String getOverallHealth() { return overallHealth; }
        public void setOverallHealth(String overallHealth) { this.overallHealth = overallHealth; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
```

## 🔧 Technical Implementation Examples

### JPA Implementation Patterns

#### Direct JPA EntityManager Usage
```java
// Example using JPA EntityManager for custom operations
EntityManager em = JPAUtil.getEntityManager();
EntityTransaction transaction = null;

try {
    transaction = em.getTransaction();
    transaction.begin();
    
    // Persist a new block
    Block newBlock = new Block(1, "0", "Genesis", LocalDateTime.now(), "hash", null, null);
    em.persist(newBlock);
    
    transaction.commit();
} catch (Exception e) {
    if (transaction != null && transaction.isActive()) {
        transaction.rollback();
    }
    throw new RuntimeException("Error saving block", e);
} finally {
    em.close();
}
```

#### JPA Query Language (JPQL) Examples
```java
// Example using JPQL for custom queries
EntityManager em = JPAUtil.getEntityManager();
try {
    // Find blocks within date range
    TypedQuery<Block> query = em.createQuery(
        "SELECT b FROM Block b WHERE b.timestamp BETWEEN :startTime AND :endTime ORDER BY b.blockNumber ASC", 
        Block.class);
    query.setParameter("startTime", startTime);
    query.setParameter("endTime", endTime);
    
    List<Block> blocks = query.getResultList();
    
    // Count blocks by content
    TypedQuery<Long> countQuery = em.createQuery(
        "SELECT COUNT(b) FROM Block b WHERE LOWER(b.data) LIKE :content", 
        Long.class);
    countQuery.setParameter("content", "%" + searchTerm.toLowerCase() + "%");
    
    Long count = countQuery.getSingleResult();
} finally {
    em.close();
}
```

#### Entity Configuration with JPA Annotations
```java
// Example of JPA entity annotations and lifecycle callbacks
@Entity
@Table(name = "blocks")
public class Block {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "block_number", unique = true, nullable = false)
    private int blockNumber;
    
    @Column(name = "data", columnDefinition = "TEXT")
    private String data;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "hash", length = 64, nullable = false)
    private String hash;
    
    // JPA lifecycle callbacks
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    @PostLoad
    public void postLoad() {
        // Post-load operations if needed
    }
    
    // ... getters and setters
}
```

#### Advanced JPA Operations
```java
// Batch operations with JPA
public class AdvancedJPAOperations {
    
    public void batchInsertBlocks(List<BlockData> blockDataList, 
                                 PrivateKey signerKey, PublicKey signerPublic) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction transaction = null;
        
        try {
            transaction = em.getTransaction();
            transaction.begin();
            
            for (int i = 0; i < blockDataList.size(); i++) {
                Block block = createBlock(blockDataList.get(i), signerKey, signerPublic);
                em.persist(block);
                
                // Flush every 25 blocks for memory management
                if (i % 25 == 0) {
                    em.flush();
                    em.clear();
                }
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
    
    // Native SQL queries when JPQL is not sufficient
    public List<Object[]> getBlockStatistics() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Query nativeQuery = em.createNativeQuery(
                "SELECT strftime('%Y-%m', timestamp) as month, " +
                "COUNT(*) as block_count, " +
                "AVG(length(data)) as avg_data_size " +
                "FROM blocks " +
                "GROUP BY strftime('%Y-%m', timestamp) " +
                "ORDER BY month DESC");
            
            return nativeQuery.getResultList();
        } finally {
            em.close();
        }
    }
    
    // Criteria API for dynamic queries
    public List<Block> dynamicBlockSearch(String content, LocalDateTime fromDate, 
                                        LocalDateTime toDate, String signerPublicKey) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Block> query = cb.createQuery(Block.class);
            Root<Block> block = query.from(Block.class);
            
            List<Predicate> predicates = new ArrayList<>();
            
            if (content != null && !content.isEmpty()) {
                predicates.add(cb.like(cb.lower(block.get("data")), 
                                     "%" + content.toLowerCase() + "%"));
            }
            
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(block.get("timestamp"), fromDate));
            }
            
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(block.get("timestamp"), toDate));
            }
            
            if (signerPublicKey != null && !signerPublicKey.isEmpty()) {
                predicates.add(cb.equal(block.get("signerPublicKey"), signerPublicKey));
            }
            
            query.where(predicates.toArray(new Predicate[0]));
            query.orderBy(cb.desc(block.get("blockNumber")));
            
            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    }
}
```

#### JPA Configuration Example
```java
// JPAUtil - EntityManager factory management
public class JPAUtil {
    private static EntityManagerFactory entityManagerFactory;
    
    static {
        try {
            // Create the EntityManagerFactory from persistence.xml
            entityManagerFactory = Persistence.createEntityManagerFactory("blockchainPU");
        } catch (Throwable ex) {
            System.err.println("Initial EntityManagerFactory creation failed: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
    
    public static EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }
    
    public static EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }
    
    public static void shutdown() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }
    
    // Utility method for transaction management
    public static <T> T executeInTransaction(Function<EntityManager, T> operation) {
        EntityManager em = getEntityManager();
        EntityTransaction transaction = null;
        
        try {
            transaction = em.getTransaction();
            transaction.begin();
            
            T result = operation.apply(em);
            
            transaction.commit();
            return result;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Transaction failed", e);
        } finally {
            em.close();
        }
    }
}
```

## 🔧 Integration Examples

### Command Line Interface Integration

```java
public class BlockchainCLI {
    private Blockchain blockchain;
    
    public BlockchainCLI() {
        this.blockchain = new Blockchain();
    }
    
    public void processCommand(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "add-key":
                if (args.length >= 3) {
                    handleAddKey(args[1], args[2]);
                } else {
                    System.err.println("Usage: add-key <public_key> <owner_name>");
                }
                break;
                
            case "add-block":
                if (args.length >= 4) {
                    handleAddBlock(args[1], args[2], args[3]);
                } else {
                    System.err.println("Usage: add-block <data> <private_key_file> <public_key_file>");
                }
                break;
                
            case "validate":
                handleValidate();
                break;
                
            case "export":
                if (args.length >= 2) {
                    handleExport(args[1]);
                } else {
                    System.err.println("Usage: export <filename>");
                }
                break;
                
            case "search":
                if (args.length >= 2) {
                    handleSearch(args[1]);
                } else {
                    System.err.println("Usage: search <term>");
                }
                break;
                
            default:
                System.err.println("Unknown command: " + command);
                printUsage();
        }
    }
    
    private void handleAddKey(String publicKey, String ownerName) {
        try {
            if (blockchain.addAuthorizedKey(publicKey, ownerName)) {
                System.out.println("✅ Key authorized for: " + ownerName);
            } else {
                System.err.println("❌ Failed to authorize key");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private void handleValidate() {
        try {
            boolean isValid = blockchain.validateChain();
            System.out.println("Chain validation: " + (isValid ? "✅ VALID" : "❌ INVALID"));
            
            if (isValid) {
                long blockCount = blockchain.getBlockCount();
                int keyCount = blockchain.getAuthorizedKeys().size();
                System.out.println("Blocks: " + blockCount + ", Active keys: " + keyCount);
            }
        } catch (Exception e) {
            System.err.println("Validation error: " + e.getMessage());
        }
    }
    
    private void handleExport(String filename) {
        try {
            if (blockchain.exportChain(filename)) {
                System.out.println("✅ Chain exported to: " + filename);
            } else {
                System.err.println("❌ Export failed");
            }
        } catch (Exception e) {
            System.err.println("Export error: " + e.getMessage());
        }
    }
    
    private void handleSearch(String searchTerm) {
        try {
            List<Block> results = blockchain.searchBlocksByContent(searchTerm);
            System.out.println("Found " + results.size() + " blocks containing: " + searchTerm);
            
            for (Block block : results) {
                System.out.println("Block #" + block.getBlockNumber() + ": " + 
                                 block.getData().substring(0, Math.min(50, block.getData().length())) + "...");
            }
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
        }
    }
    
    private void printUsage() {
        System.out.println("Blockchain CLI Usage:");
        System.out.println("  add-key <public_key> <owner_name>     - Add authorized key");
        System.out.println("  add-block <data> <priv_key> <pub_key> - Add new block");
        System.out.println("  validate                              - Validate blockchain");
        System.out.println("  export <filename>                     - Export blockchain");
        System.out.println("  search <term>                         - Search blocks");
    }
}
```

For detailed API documentation, see [API_GUIDE.md](API_GUIDE.md).  
For production deployment guidance, see [PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md).  
For testing and troubleshooting, see [TESTING.md](TESTING.md).
