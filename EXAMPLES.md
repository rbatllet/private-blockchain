# Real-World Examples & Use Cases

Comprehensive real-world examples and practical use cases for the Private Blockchain implementation.

## 📋 Table of Contents

- [Use Case Examples](#-use-case-examples)
- [Common Workflow Patterns](#-common-workflow-patterns)
- [Integration Examples](#-integration-examples)

## 🎯 Use Case Examples

### Use Case 1: Document Verification System

Track and verify document authenticity with immutable records.

```java
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
public class KeyManagementPatterns {
    
    // Secure key generation with validation
    public AuthorizedKeyInfo generateAndAuthorizeSecureKey(Blockchain blockchain, String ownerName, 
                                                         String department, String role) {
        try {
            // Generate cryptographically secure key pair
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            
            // Create comprehensive owner identification
            String fullOwnerName = String.format("%s | Dept: %s | Role: %s | Generated: %s", 
                                                ownerName, department, role, 
                                                java.time.LocalDateTime.now().format(
                                                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            
            // Add to blockchain with validation
            if (blockchain.addAuthorizedKey(publicKeyString, fullOwnerName)) {
                System.out.println("✅ Key authorized for: " + ownerName);
                System.out.println("🔑 Public key fingerprint: " + publicKeyString.substring(0, 32) + "...");
                
                // Return secure key information
                return new AuthorizedKeyInfo(keyPair, publicKeyString, fullOwnerName, 
                                           java.time.LocalDateTime.now());
            } else {
                System.err.println("❌ Failed to authorize key for: " + ownerName);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("💥 Key generation error: " + e.getMessage());
            return null;
        }
    }
    
    // Key rotation with overlap period
    public boolean rotateKey(Blockchain blockchain, AuthorizedKeyInfo oldKey, String ownerName) {
        try {
            System.out.println("🔄 Starting key rotation for: " + ownerName);
            
            // Generate new key
            AuthorizedKeyInfo newKey = generateAndAuthorizeSecureKey(blockchain, 
                                                                   ownerName + " (Rotated)", 
                                                                   "Security", "Updated");
            if (newKey == null) {
                return false;
            }
            
            // Allow overlap period for transition
            System.out.println("⏱️  Overlap period: Old and new keys both active");
            Thread.sleep(1000); // Simulate overlap period
            
            // Revoke old key
            if (blockchain.revokeAuthorizedKey(oldKey.getPublicKeyString())) {
                System.out.println("✅ Key rotation completed successfully");
                System.out.println("🗑️  Old key revoked: " + oldKey.getPublicKeyString().substring(0, 32) + "...");
                System.out.println("🆕 New key active: " + newKey.getPublicKeyString().substring(0, 32) + "...");
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
    
    // Supporting class
    public static class AuthorizedKeyInfo {
        private final KeyPair keyPair;
        private final String publicKeyString;
        private final String ownerName;
        private final java.time.LocalDateTime creationTime;
        
        public AuthorizedKeyInfo(KeyPair keyPair, String publicKeyString, String ownerName, 
                               java.time.LocalDateTime creationTime) {
            this.keyPair = keyPair;
            this.publicKeyString = publicKeyString;
            this.ownerName = ownerName;
            this.creationTime = creationTime;
        }
        
        // Getters
        public KeyPair getKeyPair() { return keyPair; }
        public String getPublicKeyString() { return publicKeyString; }
        public String getOwnerName() { return ownerName; }
        public java.time.LocalDateTime getCreationTime() { return creationTime; }
        
        // Security methods
        public PrivateKey getPrivateKey() { return keyPair.getPrivate(); }
        public PublicKey getPublicKey() { return keyPair.getPublic(); }
        
        // Utility methods
        public String getKeyFingerprint() { 
            return publicKeyString.substring(0, Math.min(32, publicKeyString.length())); 
        }
    }
}
```

### Permanent Key Cleanup and Security Compliance

```java
public class KeyCleanupManager {
    
    // Secure permanent key deletion for compliance scenarios
    public boolean permanentlyDeleteKey(Blockchain blockchain, String publicKey, String reason) {
        try {
            System.out.println("⚠️  CRITICAL: Initiating permanent key deletion");
            System.out.println("🔑 Key: " + publicKey.substring(0, 32) + "...");
            System.out.println("📝 Reason: " + reason);
            
            // Security verification - check current authorized status
            List<AuthorizedKey> allKeys = blockchain.getAllAuthorizedKeys();
            boolean keyExists = allKeys.stream()
                .anyMatch(key -> key.getPublicKey().equals(publicKey));
            
            if (!keyExists) {
                System.out.println("ℹ️  Key not found in database - nothing to delete");
                return false;
            }
            
            // Log key information before deletion for audit trail
            allKeys.stream()
                .filter(key -> key.getPublicKey().equals(publicKey))
                .forEach(key -> {
                    System.out.println("📋 Deleting key record:");
                    System.out.println("   - Owner: " + key.getOwnerName());
                    System.out.println("   - Created: " + key.getCreatedAt());
                    System.out.println("   - Status: " + (key.isActive() ? "ACTIVE" : "REVOKED"));
                    if (key.getRevokedAt() != null) {
                        System.out.println("   - Revoked: " + key.getRevokedAt());
                    }
                });
            
            // Perform permanent deletion
            boolean deleted = blockchain.deleteAuthorizedKey(publicKey);
            
            if (deleted) {
                System.out.println("🗑️  ✅ Key permanently deleted from database");
                System.out.println("⚠️  WARNING: This action is irreversible!");
                System.out.println("📝 Audit log: Key deletion completed at " + 
                                 java.time.LocalDateTime.now().format(
                                     java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                return true;
            } else {
                System.err.println("❌ Failed to delete key from database");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("💥 Key deletion error: " + e.getMessage());
            return false;
        }
    }
    
    // Bulk cleanup for security compliance
    public void performSecurityCompliantCleanup(Blockchain blockchain, String complianceReason) {
        System.out.println("🔒 Starting security compliance cleanup");
        System.out.println("📋 Reason: " + complianceReason);
        
        List<AuthorizedKey> allKeys = blockchain.getAllAuthorizedKeys();
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusYears(7); // 7-year retention
        
        allKeys.stream()
            .filter(key -> !key.isActive()) // Only process revoked keys
            .filter(key -> key.getRevokedAt() != null && key.getRevokedAt().isBefore(cutoffDate))
            .forEach(key -> {
                System.out.println("🧹 Cleaning up old revoked key: " + key.getOwnerName());
                permanentlyDeleteKey(blockchain, key.getPublicKey(), 
                                   "Security compliance: " + complianceReason + " (Revoked " + key.getRevokedAt() + ")");
            });
        
        System.out.println("✅ Security compliance cleanup completed");
    }
}
```

### Health Check and Monitoring

```java
public class BlockchainHealthMonitor {
    
    public HealthStatus performHealthCheck(Blockchain blockchain) {
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
            File dbFile = new File("blockchain.db");
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
    
    // Supporting class for health status
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
                System.out.println("Block #" + block.getBlockIndex() + ": " + 
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
