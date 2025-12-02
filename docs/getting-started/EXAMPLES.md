# Real-World Examples & Use Cases

Comprehensive real-world examples and practical use cases for the Private Blockchain implementation.

> **IMPORTANT NOTE**: The classes shown in these examples (DocumentVerificationSystem, SupplyChainTracker, MedicalRecordsSystem, FinancialAuditSystem, KeyRotationExample, KeyCleanupManager, BlockchainHealthMonitor, HealthStatus) are conceptual and designed to illustrate potential use cases. They are not part of the actual project code. Only the Blockchain, Block, AuthorizedKey, Blockchain.KeyDeletionImpact, CryptoUtil, and JPAUtil classes exist in the current implementation.
>
> For real working examples, refer to the following classes in the source code:
> - `/src/main/java/demo/BlockchainDemo.java` - Basic blockchain demonstration
> - `/src/main/java/demo/SearchFrameworkDemo.java` - Advanced search system demonstration
> - `/src/main/java/demo/GranularTermVisibilityDemo.java` - **NEW**: Granular term visibility control demo
> - `/src/main/java/demo/UserFriendlyEncryptionDemo.java` - User-friendly encryption API demo
> - `/src/main/java/demo/EncryptionConfigDemo.java` - Encryption configuration examples
> - `/src/main/java/demo/AdditionalAdvancedFunctionsDemo.java` - Advanced features demonstration
> - `/src/main/java/demo/ChainRecoveryDemo.java` - Chain recovery demonstration
> - `/src/main/java/demo/EncryptedChainExportImportDemo.java` - Chain export/import with encryption
> - `/src/main/java/demo/DangerousDeleteDemo.java` - Key deletion safety features demo
> - `/src/main/java/demo/EnhancedRecoveryExample.java` - Advanced recovery techniques example
> - `/src/main/java/demo/CoreFunctionsDemo.java` - Comprehensive core test
> - `/src/main/java/demo/SimpleDemo.java` - Basic functionality test
> - `/src/main/java/demo/QuickDemo.java` - Fast verification test
> - `/src/main/java/demo/CryptoSecurityDemo.java` - Cryptographic security demo

---

## ⚠️ SECURITY UPDATE (v1.0.6)

> **CRITICAL**: All examples in this document have been updated to reflect the new **mandatory pre-authorization** security model introduced in v1.0.6.

### 🔑 Prerequisites

**Before running any examples**, generate genesis-admin keys:

```bash
./tools/generate_genesis_keys.zsh
```

This creates `./keys/genesis-admin.*` required for bootstrap authorization. **Backup these keys securely!**

> **For tests only**: Tests auto-generate keys if missing. See [AUTO_GENESIS_KEY_GENERATION.md](../testing/AUTO_GENESIS_KEY_GENERATION.md).

### Required Initialization Pattern

All code examples now require this initialization pattern:

```java
// 1. Create blockchain (only genesis block is automatic)
Blockchain blockchain = new Blockchain();

// 2. Load genesis admin keys (generated via ./tools/generate_genesis_keys.zsh)
KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// 3. Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// 4. Create API with bootstrap admin credentials
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

// 5. Create user for your application
KeyPair appKeys = api.createUser("app-user");
api.setDefaultCredentials("app-user", appKeys);
```

> **💡 NOTE**: All examples below assume you have completed this initialization. See [GETTING_STARTED.md](GETTING_STARTED.md) for detailed initialization instructions.

### Exception-Based Error Handling (v1.0.6+)

> **🔄 BREAKING CHANGE**: Critical security and operational methods now throw **exceptions** instead of returning `false`. This fail-fast pattern ensures security violations cannot be silently ignored.
>
> **Affected methods**: `revokeAuthorizedKey()`, `deleteAuthorizedKey()`, `rollbackBlocks()`, `rollbackToBlock()`, `exportChain()`, `importChain()`.
>
> **Example:**
> ```java
> // Before v1.0.6:
> boolean result = blockchain.exportChain("backup.json");
> if (!result) { System.err.println("Export failed"); }
>
> // After v1.0.6:
> try {
>     blockchain.exportChain("backup.json");
>     System.out.println("✅ Export successful");
> } catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
>     System.err.println("❌ Export failed: " + e.getMessage());
> }
> ```
>
> See [Exception-Based Error Handling Guide](../security/EXCEPTION_BASED_ERROR_HANDLING_V1_0_6.md) for complete migration guide.

---

## 📋 Table of Contents

- [Use Case Examples](#-use-case-examples)
- [Common Workflow Patterns](#-common-workflow-patterns)
- [Integration Examples](#-integration-examples)

## 🎯 Use Case Examples

### 🔐 Use Case 1: UserFriendlyEncryptionAPI - Medical Records Management ⭐ **NEW**

Modern medical records system with encrypted storage, advanced search, and HIPAA compliance features.

```java
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.util.*;

public class MedicalRecordsEncryptionSystem {
    public static void main(String[] args) throws Exception {
        // Secure initialization (see security section above)
        Blockchain blockchain = new Blockchain();
        KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

        // Setup hierarchical security system (requires SUPER_ADMIN)
        System.out.println("🔐 Setting up medical-grade security...");
        KeyManagementResult security = api.setupHierarchicalKeys("MedicalMaster2025!");

        // Create doctor user AFTER security setup
        KeyPair doctorKeys = api.createUser("dr.smith");
        api.setDefaultCredentials("dr.smith", doctorKeys);
        if (security.isSuccess()) {
            System.out.println("✅ HIPAA-compliant security established");
        }
        
        // Generate secure passwords for patient records
        String patientPassword = api.generateValidatedPassword(16, true);
        System.out.println("🔑 Generated secure patient password: " + patientPassword.substring(0, 4) + "****");
        
        // Store encrypted patient records with granular search control
        System.out.println("\n📋 Storing patient records...");
        
        // Patient 1: John Doe - Diabetes diagnosis
        String[] publicTerms = {"diabetes", "endocrinology", "consultation"};
        String[] privateTerms = {"john-doe", "patient-001", "insulin-dependent"};
        
        Block patient1 = api.storeSearchableDataWithLayers(
            "Patient: John Doe (ID: P-001). Diagnosis: Type 1 Diabetes. Treatment: Insulin therapy 4x daily. " +
            "Blood glucose monitoring required. Next appointment: 2025-02-15. Dr. Smith, Endocrinology Dept.",
            patientPassword, publicTerms, privateTerms
        );
        System.out.println("✅ Patient 1 record stored in block #" + patient1.getBlockNumber());
        
        // Patient 2: Jane Smith - Cardiology
        String[] cardioPublic = {"cardiology", "ecg", "examination"};  
        String[] cardioPrivate = {"jane-smith", "patient-002", "arrhythmia"};
        
        Block patient2 = api.storeSearchableDataWithLayers(
            "Patient: Jane Smith (ID: P-002). ECG examination shows minor arrhythmia. " +
            "Cardiology consultation recommended. Holter monitor prescribed for 24h. Dr. Johnson, Cardiology.",
            patientPassword, cardioPublic, cardioPrivate
        );
        System.out.println("✅ Patient 2 record stored in block #" + patient2.getBlockNumber());
        
        // Store large medical file (MRI scan)
        String mriData = "MRI SCAN DATA: " + "A".repeat(600 * 1024); // 600KB file
        Block mriBlock = api.storeWithSmartTiering(mriData, patientPassword, 
            Map.of("fileType", "MRI", "patient", "P-001", "priority", "high"));
        System.out.println("✅ MRI scan stored with smart tiering in block #" + mriBlock.getBlockNumber());
        
        // Demonstrate search capabilities
        System.out.println("\n🔍 Medical records search demonstration...");
        
        // Public search (no password needed) - only finds general medical terms
        List<Block> publicResults = api.searchByTerms(new String[]{"diabetes"}, null, 10);
        System.out.println("📋 Public search for 'diabetes': " + publicResults.size() + " results");
        
        // Private search (password required) - finds patient-specific data
        List<Block> privateResults = api.searchWithAdaptiveDecryption("john-doe", patientPassword, 10);
        System.out.println("🔐 Private search for 'john-doe': " + privateResults.size() + " results");
        
        // Advanced search with multiple criteria (language-independent)
        Map<String, Object> advancedCriteria = new HashMap<>();
        advancedCriteria.put("terms", Arrays.asList("cardiology", "heart", "ecg"));
        advancedCriteria.put("includeEncrypted", true);
        AdvancedSearchResult advancedResults = api.performAdvancedSearch(advancedCriteria, patientPassword, 50);
        System.out.println("🔍 Advanced search for heart-related records: " + advancedResults.getTotalResults() + " results");
        
        // Generate comprehensive health report
        System.out.println("\n📊 System health and compliance check...");
        HealthReport health = api.performHealthDiagnosis();
        ValidationReport validation = api.performComprehensiveValidation();
        
        if (health.isHealthy() && validation.isFullyValid()) {
            System.out.println("✅ Medical records system: HIPAA COMPLIANT");
            System.out.println("✅ Data integrity: VERIFIED");
            System.out.println("✅ Security status: OPTIMAL");
        }
        
        // Export search results for medical research (anonymized)
        Map<String, Object> researchCriteria = new HashMap<>();
        researchCriteria.put("terms", Arrays.asList("diabetes", "treatment"));
        researchCriteria.put("anonymize", true);
        
        AdvancedSearchResult research = api.performAdvancedSearch(researchCriteria, patientPassword, 50);
        String researchData = api.exportSearchResults(research, "csv");
        System.out.println("📈 Research data exported: " + researchData.length() + " characters");
        
        // Demonstrate recovery capabilities
        System.out.println("\n🔧 Testing medical data recovery...");
        RecoveryCheckpoint checkpoint = api.createRecoveryCheckpoint(
            RecoveryCheckpoint.CheckpointType.MANUAL, "Daily medical backup");
        System.out.println("💾 Recovery checkpoint created: " + checkpoint.getDescription());
        
        // Storage analytics for compliance reporting
        String analytics = api.getStorageAnalytics();
        System.out.println("\n📊 Medical storage analytics summary:");
        System.out.println(analytics.substring(0, Math.min(200, analytics.length())) + "...");
        
        System.out.println("\n🎉 Medical Records Encryption System Demo Complete!");
        System.out.println("📋 Features demonstrated:");
        System.out.println("   • HIPAA-compliant hierarchical security");
        System.out.println("   • Encrypted patient data with granular search");
        System.out.println("   • Smart storage tiering for large medical files");
        System.out.println("   • Multi-level search (public/private/advanced)");
        System.out.println("   • Health monitoring and compliance validation");
        System.out.println("   • Research data export with anonymization");
        System.out.println("   • Automated backup and recovery systems");
    }
}
```

**Key Features Demonstrated:**
- **🔐 Medical-Grade Security**: Hierarchical key management with strong passwords
- **🔍 HIPAA-Compliant Search**: Public medical terms vs. private patient identifiers
- **💾 Smart Storage**: Automatic tiering for large medical files (MRI, CT scans)
- **🔍 Advanced Search**: Multi-criteria search with encrypted content access
- **📊 Compliance Monitoring**: Real-time health checks and validation reports
- **📈 Research Export**: Anonymized data export for medical research
- **🔧 Data Recovery**: Automated backup and recovery for critical medical data

### 🔐 Use Case 2: UserFriendlyEncryptionAPI - Financial Transaction Security ⭐ **NEW**

Enterprise-grade financial transaction system with audit trails, compliance reporting, and fraud detection.

```java
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import java.time.LocalDateTime;
import java.util.*;

public class FinancialTransactionSystem {
    public static void main(String[] args) throws Exception {
        // Secure initialization (see security section above)
        Blockchain blockchain = new Blockchain();
        KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

        // Setup financial-grade security (requires SUPER_ADMIN)
        System.out.println("🏦 Initializing financial security system...");
        KeyManagementResult security = api.setupHierarchicalKeys("FinancialSecure2025#");

        // Create bank system user AFTER security setup
        KeyPair bankKeys = api.createUser("bank-system");
        api.setDefaultCredentials("bank-system", bankKeys);
        String transactionPassword = api.generateValidatedPassword(20, true);
        
        // Store high-value transactions with visibility control
        System.out.println("\n💰 Recording financial transactions...");
        
        // Transaction 1: Large transfer (sensitive details private)
        Set<String> allTerms = Set.of("transfer", "wire", "international", "account-1234", 
                                    "account-5678", "250000", "EUR", "suspicious-flag");
        
        TermVisibilityMap visibility = new TermVisibilityMap()
            .setPublic("transfer", "wire", "international", "EUR")  // Transaction type public
            .setPrivate("account-1234", "account-5678", "250000", "suspicious-flag"); // Details private
            
        Block transaction1 = api.storeDataWithGranularTermControl(
            "WIRE TRANSFER: €250,000 from Account-1234 to Account-5678. " +
            "International transfer to Swiss bank. KYC verified. AML checked. " +
            "Reference: TXN-2025-001. Flagged for review due to amount.",
            transactionPassword, allTerms, visibility
        );
        System.out.println("✅ High-value transaction recorded in block #" + transaction1.getBlockNumber());
        
        // Transaction 2: Regular business payment
        String[] businessPublic = {"payment", "business", "invoice", "processed"};
        String[] businessPrivate = {"acme-corp", "invoice-12345", "25000"};
        
        Block transaction2 = api.storeSearchableDataWithLayers(
            "BUSINESS PAYMENT: €25,000 payment to ACME Corp for Invoice-12345. " +
            "Regular supplier payment. Auto-processed. No flags.",
            transactionPassword, businessPublic, businessPrivate
        );
        System.out.println("✅ Business payment recorded in block #" + transaction2.getBlockNumber());
        
        // Fraud detection search
        System.out.println("\n🚨 Fraud detection and compliance checks...");
        
        // Search for suspicious transactions (public search - no sensitive data exposed)
        List<Block> suspiciousPublic = api.searchByTerms(new String[]{"international"}, null, 10);
        System.out.println("🔍 Public search for international transactions: " + suspiciousPublic.size());
        
        // Deep fraud investigation (password required for sensitive details)
        List<Block> fraudInvestigation = api.searchWithAdaptiveDecryption("suspicious-flag", transactionPassword, 10);
        System.out.println("🚨 Fraud investigation results: " + fraudInvestigation.size() + " flagged transactions");
        
        // Time-based compliance audit
        LocalDateTime auditStart = LocalDateTime.now().minusDays(1);
        LocalDateTime auditEnd = LocalDateTime.now();
        Map<String, Object> auditFilters = Map.of("includeEncrypted", true, "compliance", true);
        
        AdvancedSearchResult audit = api.performTimeRangeSearch(auditStart, auditEnd, auditFilters);
        System.out.println("📋 24-hour compliance audit: " + audit.getTotalResults() + " transactions reviewed");
        
        // Generate regulatory reports
        System.out.println("\n📊 Regulatory compliance reporting...");
        
        String blockchainReport = api.generateBlockchainStatusReport();
        String offChainReport = api.generateOffChainStorageReport();
        HealthReport systemHealth = api.performHealthDiagnosis();
        
        if (systemHealth.isHealthy()) {
            System.out.println("✅ Financial system health: COMPLIANT");
            System.out.println("✅ Audit trail integrity: VERIFIED");
            System.out.println("✅ Regulatory status: READY FOR INSPECTION");
        }
        
        // Export compliance data for regulators
        Map<String, Object> regulatoryCriteria = new HashMap<>();
        regulatoryCriteria.put("terms", Arrays.asList("transfer", "payment", "international"));
        regulatoryCriteria.put("compliance_level", "full");
        regulatoryCriteria.put("regulator_access", true);
        
        AdvancedSearchResult regulatoryData = api.performAdvancedSearch(regulatoryCriteria, transactionPassword, 1000);
        String complianceExport = api.exportSearchResults(regulatoryData, "json");
        System.out.println("📋 Regulatory export generated: " + complianceExport.length() + " characters");
        
        // Automated backup for financial compliance
        RecoveryCheckpoint financialBackup = api.createRecoveryCheckpoint(
            RecoveryCheckpoint.CheckpointType.AUTOMATIC, "Daily financial compliance backup");
        
        System.out.println("\n🎉 Financial Transaction Security Demo Complete!");
        System.out.println("🏦 Features demonstrated:");
        System.out.println("   • Banking-grade hierarchical security");
        System.out.println("   • Granular transaction data visibility control");
        System.out.println("   • Fraud detection with encrypted search");
        System.out.println("   • Time-based compliance auditing");
        System.out.println("   • Regulatory reporting and data export");
        System.out.println("   • Automated compliance backup systems");
    }
}
```

**Key Features Demonstrated:**
- **🏦 Banking Security**: Multi-tier key management with financial-grade passwords
- **🔍 Compliance Search**: Public transaction types vs. private sensitive details
- **🚨 Fraud Detection**: Encrypted search for suspicious transaction flags
- **📋 Audit Trails**: Time-based transaction auditing and compliance reports
- **📊 Regulatory Export**: Structured data export for regulatory authorities
- **💾 Compliance Backup**: Automated backup systems for financial compliance

### Use Case 3: Document Verification System

> ⚠️ **IMPORTANT NOTE**: This `DocumentVerificationSystem` class is a **conceptual example** for illustration purposes. It does not exist in the actual project code. For real working examples, see the demo files listed at the beginning of this document, particularly `/src/main/java/demo/BlockchainDemo.java`.

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

            // Load bootstrap admin keys (RBAC v1.0.6+)
            KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            // Register bootstrap admin in blockchain (REQUIRED!)
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            // Setup document verification authorities
            KeyPair notary = CryptoUtil.generateKeyPair();
            KeyPair university = CryptoUtil.generateKeyPair();
            KeyPair government = CryptoUtil.generateKeyPair();

            String notaryKey = CryptoUtil.publicKeyToString(notary.getPublic());
            String universityKey = CryptoUtil.publicKeyToString(university.getPublic());
            String governmentKey = CryptoUtil.publicKeyToString(government.getPublic());

            blockchain.addAuthorizedKey(
                notaryKey,
                "Public Notary Office",
                bootstrapKeys,      // Caller: bootstrap admin
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                universityKey,
                "University of Barcelona",
                bootstrapKeys,
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                governmentKey,
                "Government Registry",
                bootstrapKeys,
                UserRole.USER
            );

            // Record document verifications
            blockchain.addBlock("Document: Birth Certificate #BC-2025-001 | Status: VERIFIED | Hash: sha3-256:a1b2c3...", 
                              government.getPrivate(), government.getPublic());
            
            blockchain.addBlock("Document: University Diploma #UB-CS-2025-456 | Status: AUTHENTIC | Graduate: John Doe", 
                              university.getPrivate(), university.getPublic());
            
            blockchain.addBlock("Document: Property Deed #PD-BCN-2025-789 | Property: Carrer Balmes 123 | Owner: Jane Smith", 
                              notary.getPrivate(), notary.getPublic());
            
            // Verify document authenticity
            List<Block> certificateRecords = blockchain.searchBlocksByContent("Birth Certificate");
            System.out.println("Birth certificates found: " + certificateRecords.size());
            
            // Validate the blockchain integrity before export
            ChainValidationResult validationResult = blockchain.validateChainDetailed();
            if (!validationResult.isStructurallyIntact()) {
                System.err.println("WARNING: Blockchain structure is compromised! Invalid blocks: " + 
                                validationResult.getInvalidBlocks());
            } else if (!validationResult.isFullyCompliant()) {
                System.out.println("WARNING: Some blocks have authorization issues. Revoked blocks: " + 
                                 validationResult.getRevokedBlocks());
            } else {
                System.out.println("Blockchain validation: All documents are properly verified and authorized");
            }
            
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

> ⚠️ **IMPORTANT NOTE**: This `SupplyChainTracker` class is a **conceptual example** for illustration purposes. It does not exist in the actual project code. For real working examples, see the demo files listed at the beginning of this document, particularly `/src/main/java/demo/BlockchainDemo.java`.

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

            // Load bootstrap admin keys (RBAC v1.0.6+)
            KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            // Register bootstrap admin in blockchain (REQUIRED!)
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            // Setup supply chain participants
            KeyPair manufacturer = CryptoUtil.generateKeyPair();
            KeyPair distributor = CryptoUtil.generateKeyPair();
            KeyPair retailer = CryptoUtil.generateKeyPair();
            KeyPair qualityControl = CryptoUtil.generateKeyPair();

            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(manufacturer.getPublic()),
                "Barcelona Electronics Mfg",
                bootstrapKeys,
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(distributor.getPublic()),
                "Iberian Distribution Ltd",
                bootstrapKeys,
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(retailer.getPublic()),
                "TechStore Barcelona",
                bootstrapKeys,
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(qualityControl.getPublic()),
                "EU Quality Assurance",
                bootstrapKeys,
                UserRole.USER
            );

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
            
            // Validate supply chain integrity before generating compliance report
            ChainValidationResult validation = blockchain.validateChainDetailed();
            if (!validation.isStructurallyIntact()) {
                System.err.println("CRITICAL: Supply chain integrity compromised! " +
                                validation.getInvalidBlocks() + " invalid blocks found.");
                // In a real application, you might want to trigger an alert or stop further processing
            } else if (!validation.isFullyCompliant()) {
                System.out.println("WARNING: Some supply chain events have authorization issues. " +
                                 validation.getRevokedBlocks() + " blocks affected.");
                // Log the issue but continue with the report
            } else {
                System.out.println("Supply chain validation: All events are properly authorized and verified");
            }
            
            // Generate compliance report for the specified date range
            LocalDate startDate = LocalDate.of(2025, 6, 1);
            LocalDate endDate = LocalDate.of(2025, 6, 30);
            List<Block> monthlyActivity = blockchain.getBlocksByDateRange(startDate, endDate);
            
            // Add a verification block to the chain
            blockchain.addBlock("COMPLIANCE_CHECK: Supply chain verified | Date: " + LocalDate.now() + 
                             " | Status: " + (validation.isStructurallyIntact() ? "VALID" : "INVALID") + 
                             " | Blocks: " + blockchain.getBlockCount(),
                             qualityControl.getPrivate(), qualityControl.getPublic());
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

> ⚠️ **IMPORTANT NOTE**: This `MedicalRecordsSystem` class is a **conceptual example** for illustration purposes. It does not exist in the actual project code. For real working examples, see the demo files listed at the beginning of this document, particularly `/src/main/java/demo/BlockchainDemo.java`.

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

            // Load bootstrap admin keys (RBAC v1.0.6+)
            KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            // Register bootstrap admin in blockchain (REQUIRED!)
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            // Setup medical system participants
            KeyPair hospital = CryptoUtil.generateKeyPair();
            KeyPair doctor = CryptoUtil.generateKeyPair();
            KeyPair pharmacy = CryptoUtil.generateKeyPair();
            KeyPair insurance = CryptoUtil.generateKeyPair();

            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(hospital.getPublic()),
                "Hospital Clínic Barcelona",
                bootstrapKeys,
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(doctor.getPublic()),
                "Dr. Maria Garcia - Cardiology",
                bootstrapKeys,
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(pharmacy.getPublic()),
                "Farmacia Central",
                bootstrapKeys,
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(insurance.getPublic()),
                "Sanitas Insurance",
                bootstrapKeys,
                UserRole.USER
            );

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
            ChainValidationResult validationResult = blockchain.validateChainDetailed();
            boolean structurallyIntact = validationResult.isStructurallyIntact();
            boolean fullyCompliant = validationResult.isFullyCompliant();
            System.out.println("Medical records structural integrity: " + (structurallyIntact ? "VERIFIED" : "COMPROMISED"));
            System.out.println("Medical records full compliance: " + (fullyCompliant ? "VERIFIED" : "COMPROMISED"));
            
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

> ⚠️ **IMPORTANT NOTE**: This `FinancialAuditSystem` class is a **conceptual example** for illustration purposes. It does not exist in the actual project code. For real working examples, see the demo files listed at the beginning of this document, particularly `/src/main/java/demo/BlockchainDemo.java`.

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

            // Load bootstrap admin keys (RBAC v1.0.6+)
            KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            // Register bootstrap admin in blockchain (REQUIRED!)
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            // Setup financial system participants
            KeyPair bank = CryptoUtil.generateKeyPair();
            KeyPair auditor = CryptoUtil.generateKeyPair();
            KeyPair compliance = CryptoUtil.generateKeyPair();
            KeyPair regulator = CryptoUtil.generateKeyPair();

            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(bank.getPublic()),
                "Banco Santander España",
                bootstrapKeys,
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(auditor.getPublic()),
                "PwC Auditing Services",
                bootstrapKeys,
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(compliance.getPublic()),
                "Internal Compliance Dept",
                bootstrapKeys,
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(regulator.getPublic()),
                "Banco de España",
                bootstrapKeys,
                UserRole.USER
            );

            // Record financial transactions and audits
            blockchain.addBlock("TRANSACTION: ID: TXN-2025-001 | Type: Wire Transfer | Amount: €50,000.00 | From: ACME Corp | To: Global Services Ltd | Status: COMPLETED", 
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
            ChainValidationResult auditResult = blockchain.validateChainDetailed();
            boolean structurallyIntact = auditResult.isStructurallyIntact();
            boolean fullyCompliant = auditResult.isFullyCompliant();
            System.out.println("Audit trail structural integrity: " + (structurallyIntact ? "VALID" : "COMPROMISED"));
            System.out.println("Audit trail full compliance: " + (fullyCompliant ? "VALID" : "COMPROMISED"));
            
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

> ⚠️ **MEMORY SAFETY NOTE**: The examples below use `validateChainDetailed()` which has memory limits:
> - ⚠️ **100,000 blocks**: Warning threshold (logs warning message)
> - ❌ **500,000 blocks**: Hard limit (throws `IllegalStateException`)  
> - ✅ **Solution**: For blockchains with >100K blocks, use `validateChainStreaming()` instead
>
> See the [Memory-Safe Validation for Large Blockchains](#-memory-safe-validation-for-large-blockchains) section for streaming validation examples.

```java
public void performDailyBackup(Blockchain blockchain) {
    try {
        // Generate timestamp-based backup filename
        String backupFile = "blockchain_backup_" + 
                           java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + 
                           ".json";
        
        // Validate chain before backup
        // Note: For blockchains with >100K blocks, use validateChainStreaming() instead
        ChainValidationResult validationResult = blockchain.validateChainDetailed();
        if (!validationResult.isStructurallyIntact()) {
            System.err.println("WARNING: Chain structural validation failed - backup may contain corrupted data");
            System.err.println("Invalid blocks: " + validationResult.getInvalidBlocks());
        } else if (!validationResult.isFullyCompliant()) {
            System.err.println("WARNING: Chain contains authorization issues - backup contains revoked blocks");
            System.err.println("Revoked blocks: " + validationResult.getRevokedBlocks());
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
            
            // Add transaction to blockchain (thread-safe operation)
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
        ChainValidationResult result = blockchain.validateChainDetailed();
        if (result.isStructurallyIntact()) {
            if (result.isFullyCompliant()) {
                System.out.println("Chain validation fully successful after batch processing");
            } else {
                System.out.println("Chain is structurally intact but has authorization issues after batch processing");
                System.out.println("Revoked blocks: " + result.getRevokedBlocks());
            }
        } else {
            System.err.println("WARNING: Chain has structural problems after batch processing");
            System.err.println("Invalid blocks: " + result.getInvalidBlocks());
        }
        
    } catch (Exception e) {
        System.err.println("Batch processing error: " + e.getMessage());
    }
}

// ✅ THREAD-SAFE: Concurrent Batch Processing
public void processConcurrentBatchTransactions(Blockchain blockchain, List<String> transactions, 
                                             PrivateKey signerKey, PublicKey signerPublic) {
    try {
        ExecutorService executor = Executors.newFixedThreadPool(4); // Limit concurrent threads
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        for (String transaction : transactions) {
            // Validate transaction size before submitting
            if (transaction.length() > blockchain.getMaxBlockDataLength()) {
                System.err.println("Transaction too large, skipping: " + transaction.substring(0, 50) + "...");
                failureCount.incrementAndGet();
                continue;
            }
            
            // Submit transaction to thread pool
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                return blockchain.addBlock(transaction, signerKey, signerPublic);
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all transactions to complete and collect results
        for (CompletableFuture<Boolean> future : futures) {
            try {
                if (future.get()) {
                    int current = successCount.incrementAndGet();
                    if (current % 100 == 0) {
                        System.out.println("Processed " + current + " transactions...");
                    }
                } else {
                    failureCount.incrementAndGet();
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
                System.err.println("Transaction failed: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        
        System.out.println("Concurrent batch processing complete: " + successCount.get() + " successful, " + failureCount.get() + " failed");
        
        // Validate chain after concurrent processing
        ChainValidationResult result = blockchain.validateChainDetailed();
        if (result.isStructurallyIntact()) {
            if (result.isFullyCompliant()) {
                System.out.println("Chain validation fully successful after concurrent batch processing");
            } else {
                System.out.println("Chain is structurally intact but has authorization issues after concurrent batch processing");
                System.out.println("Revoked blocks: " + result.getRevokedBlocks());
            }
        } else {
            System.err.println("WARNING: Chain has structural problems after concurrent batch processing");
            System.err.println("Invalid blocks: " + result.getInvalidBlocks());
        }
        
    } catch (Exception e) {
        System.err.println("Concurrent batch processing error: " + e.getMessage());
    }
}
```

For more detailed API information and technical specifications, see [API_GUIDE.md](../reference/API_GUIDE.md) and [TECHNICAL_DETAILS.md](../reference/TECHNICAL_DETAILS.md).

### 🚀 Memory-Safe Validation for Large Blockchains

> ⚠️ **IMPORTANT**: `validateChainDetailed()` has memory limits:
> - **Warning threshold**: 100,000 blocks (logs warning message)
> - **Hard limit**: 500,000 blocks (throws `IllegalStateException`)
> - **Recommendation**: Use `validateChainStreaming()` for blockchains with >100K blocks

For very large blockchains (millions of blocks), use the streaming validation API to avoid memory issues:

```java
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.core.Blockchain.ValidationSummary;
import com.rbatllet.blockchain.validation.BlockValidationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LargeBlockchainValidation {
    
    /**
     * Demonstrates memory-safe validation for blockchains with millions of blocks.
     * Uses streaming validation to process the chain in batches without loading 
     * all validation results into memory at once.
     */
    public void validateLargeBlockchain(Blockchain blockchain) {
        System.out.println("🔍 Starting streaming validation for large blockchain...");
        System.out.println("📊 Total blocks: " + blockchain.getBlockCount());
        
        // Track statistics across all batches
        final AtomicInteger batchesProcessed = new AtomicInteger(0);
        final List<Long> criticalInvalidBlocks = new ArrayList<>();
        
        // Validate chain in batches of 1000 blocks
        ValidationSummary summary = blockchain.validateChainStreaming(
            batchResults -> {
                // This consumer is called for each batch of validation results
                int batchNumber = batchesProcessed.incrementAndGet();
                int invalidInBatch = 0;
                int revokedInBatch = 0;
                
                // Process each validation result in the batch
                for (BlockValidationResult result : batchResults) {
                    if (!result.isValid()) {
                        invalidInBatch++;
                        long blockNumber = result.getBlock().getBlockNumber();
                        
                        // Store critical invalid blocks for detailed investigation
                        if (criticalInvalidBlocks.size() < 100) {
                            criticalInvalidBlocks.add(blockNumber);
                        }
                        
                        // Log first few invalid blocks in each batch
                        if (invalidInBatch <= 3) {
                            System.err.println("  ❌ Invalid block #" + blockNumber + 
                                             " - Reason: " + result.getErrorMessage());
                        }
                    } else if (result.isRevoked()) {
                        revokedInBatch++;
                    }
                }
                
                // Report batch statistics
                if (invalidInBatch > 0 || revokedInBatch > 0 || batchNumber % 10 == 0) {
                    System.out.println("📦 Batch " + batchNumber + ": " + 
                                     batchResults.size() + " blocks processed | " +
                                     invalidInBatch + " invalid, " + 
                                     revokedInBatch + " revoked");
                }
            },
            MemorySafetyConstants.DEFAULT_BATCH_SIZE // Process 1000 blocks per batch
        );
        
        // Print final validation summary
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 VALIDATION SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Total blocks validated:  " + summary.getTotalBlocks());
        System.out.println("Valid blocks:            " + summary.getValidBlocks());
        System.out.println("Invalid blocks:          " + summary.getInvalidBlocks());
        System.out.println("Revoked blocks:          " + summary.getRevokedBlocks());
        System.out.println("Overall status:          " + (summary.isValid() ? "✅ VALID" : "❌ INVALID"));
        System.out.println("Batches processed:       " + batchesProcessed.get());
        
        if (!criticalInvalidBlocks.isEmpty()) {
            System.out.println("\n⚠️  Critical invalid blocks (first 100):");
            criticalInvalidBlocks.forEach(blockNum -> 
                System.out.println("  - Block #" + blockNum)
            );
        }
        
        // Recommended actions based on results
        if (!summary.isValid()) {
            System.out.println("\n🔧 RECOMMENDED ACTIONS:");
            if (summary.getInvalidBlocks() > 0) {
                System.out.println("  1. Investigate structural issues in invalid blocks");
                System.out.println("  2. Consider using recovery mechanisms if corruption detected");
                System.out.println("  3. Check for hash chain breaks or signature failures");
            }
            if (summary.getRevokedBlocks() > 0) {
                System.out.println("  1. Review revoked authorization keys");
                System.out.println("  2. Consider if revoked blocks should be removed");
                System.out.println("  3. Update access control policies if needed");
            }
        } else {
            System.out.println("\n✅ Blockchain is fully valid and compliant!");
        }
    }
    
    /**
     * Alternative approach: Store validation results to database instead of memory.
     * This is useful for auditing or compliance reporting.
     */
    public void validateAndAuditToDatabase(Blockchain blockchain) {
        System.out.println("🔍 Starting validation with database audit trail...");
        
        // Simulate database connection (replace with actual DB logic)
        // Connection dbConnection = DriverManager.getConnection(...);
        
        ValidationSummary summary = blockchain.validateChainStreaming(
            batchResults -> {
                for (BlockValidationResult result : batchResults) {
                    if (!result.isValid() || result.isRevoked()) {
                        // Store to database for audit trail
                        // storeAuditRecord(dbConnection, result);
                        
                        // Example: Log to file instead
                        System.out.println("AUDIT: Block #" + result.getBlock().getBlockNumber() + 
                                         " | Valid: " + result.isValid() + 
                                         " | Revoked: " + result.isRevoked() +
                                         " | Error: " + result.getErrorMessage());
                    }
                }
            },
            1000
        );
        
        System.out.println("✅ Validation complete. Audit trail saved to database.");
        System.out.println("   Total: " + summary.getTotalBlocks() + " | " +
                         "Invalid: " + summary.getInvalidBlocks() + " | " +
                         "Revoked: " + summary.getRevokedBlocks());
    }
    
    /**
     * Performance comparison: validateChainDetailed vs validateChainStreaming
     */
    public void compareValidationMethods(Blockchain blockchain) {
        long blockCount = blockchain.getBlockCount();
        
        System.out.println("📊 Validation method comparison for " + blockCount + " blocks:");
        System.out.println();
        
        if (blockCount < 100_000) {
            // Safe to use validateChainDetailed for small chains
            long startTime = System.currentTimeMillis();
            ChainValidationResult detailedResult = blockchain.validateChainDetailed();
            long detailedTime = System.currentTimeMillis() - startTime;
            
            System.out.println("validateChainDetailed():");
            System.out.println("  Time: " + detailedTime + "ms");
            System.out.println("  Memory: Loads all results into memory");
            System.out.println("  Invalid blocks: " + detailedResult.getInvalidBlocks());
            System.out.println("  ✅ Recommended for chains < 100K blocks");
            System.out.println();
        } else if (blockCount < 500_000) {
            System.out.println("validateChainDetailed():");
            System.out.println("  ⚠️  WARNING: Chain has " + blockCount + " blocks (>100K)");
            System.out.println("  ⚠️  High memory usage expected");
            System.out.println("  ⚠️  Consider using validateChainStreaming() instead");
            System.out.println();
        } else {
            System.out.println("validateChainDetailed():");
            System.out.println("  ❌ BLOCKED: Chain has " + blockCount + " blocks (>500K)");
            System.out.println("  ❌ Would throw IllegalStateException");
            System.out.println("  ✅ MUST use validateChainStreaming() for this chain");
            System.out.println();
        }
        
        // Always safe to use streaming validation
        long startTime = System.currentTimeMillis();
        ValidationSummary streamingSummary = blockchain.validateChainStreaming(
            batchResults -> { /* Process batch */ },
            1000
        );
        long streamingTime = System.currentTimeMillis() - startTime;
        
        System.out.println("validateChainStreaming():");
        System.out.println("  Time: " + streamingTime + "ms");
        System.out.println("  Memory: Constant (processes in batches)");
        System.out.println("  Invalid blocks: " + streamingSummary.getInvalidBlocks());
        System.out.println("  ✅ Recommended for all chain sizes, especially >100K blocks");
    }
}
```

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
    
    // Simple key rotation example with overlap period for security and thread-safety
    // RBAC v1.0.6+: Requires admin credentials to authorize new keys
    public boolean rotateKey(Blockchain blockchain, String oldPublicKey, String ownerName, KeyPair adminKeyPair) {
        try {
            System.out.println("🔄 Starting key rotation for: " + ownerName);

            // Generate new key
            KeyPair newKeyPair = CryptoUtil.generateKeyPair();
            String newPublicKey = CryptoUtil.publicKeyToString(newKeyPair.getPublic());

            // Add new key to blockchain (RBAC v1.0.6+)
            String fullOwnerName = ownerName + " (Rotated Key)";
            if (!blockchain.addAuthorizedKey(
                    newPublicKey,
                    fullOwnerName,
                    adminKeyPair,       // Caller: requires ADMIN or SUPER_ADMIN
                    UserRole.USER
                )) {
                System.err.println("❌ Failed to authorize new key");
                return false;
            }
            
            System.out.println("✅ New key authorized for: " + fullOwnerName);
            System.out.println("🔑 New public key: " + newPublicKey.substring(0, 32) + "...");
            
            // ✅ THREAD-SAFE: Verify new key is properly activated before revoking old key
            System.out.println("⏱️  Verifying new key activation...");
            
            // Verify the new key is active in the blockchain
            List<AuthorizedKey> activeKeys = blockchain.getAuthorizedKeys();
            boolean newKeyActive = activeKeys.stream()
                .anyMatch(k -> k.getPublicKey().equals(newPublicKey) && k.isActive());
            
            if (!newKeyActive) {
                System.err.println("❌ New key not properly activated, aborting rotation");
                // Attempt to clean up the failed key addition
                blockchain.revokeAuthorizedKey(newPublicKey);
                return false;
            }
            
            // Test the new key by adding a test block (optional but recommended)
            try {
                Block testBlock = blockchain.addBlockAndReturn("Key rotation test block", 
                                                              newKeyPair.getPrivate(), 
                                                              newKeyPair.getPublic());
                if (testBlock == null) {
                    System.err.println("❌ New key failed functional test, aborting rotation");
                    blockchain.revokeAuthorizedKey(newPublicKey);
                    return false;
                }
                System.out.println("✅ New key passed functional test (Block #" + testBlock.getBlockNumber() + ")");
            } catch (Exception e) {
                System.err.println("❌ New key functional test failed: " + e.getMessage());
                blockchain.revokeAuthorizedKey(newPublicKey);
                return false;
            }
            
            // Now it's safe to revoke the old key
            if (blockchain.revokeAuthorizedKey(oldPublicKey)) {
                System.out.println("✅ Key rotation completed successfully");
                System.out.println("🗑️  Old key revoked: " + oldPublicKey.substring(0, 32) + "...");
                return true;
            } else {
                System.err.println("❌ Failed to revoke old key during rotation");
                // Both keys remain active - this is safer than leaving no keys
                System.out.println("⚠️  Both old and new keys remain active for safety");
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
    
    // SECURE DANGEROUS: Admin-authorized permanent key deletion for compliance scenarios
    public boolean secureAdminDeleteKey(Blockchain blockchain, String publicKey, boolean force, String reason, PrivateKey adminPrivateKey, String adminPublicKey) {
        try {
            System.out.println("🔐 SECURE ADMIN-AUTHORIZED KEY DELETION");
            System.out.println("🔑 Key: " + publicKey.substring(0, 32) + "...");
            System.out.println("📝 Reason: " + reason);
            System.out.println("⚡ Force mode: " + force);
            System.out.println("👤 Admin: " + adminPublicKey.substring(0, 32) + "...");

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

            // Step 3: Create admin signature for authorization
            String adminSignature = CryptoUtil.createAdminSignature(publicKey, force, reason, adminPrivateKey);
            System.out.println("🔐 Admin signature created for authorization");

            // Step 4: Perform secure admin-authorized deletion (v1.0.6+: throws exceptions)
            try {
                boolean deleted = blockchain.dangerouslyDeleteAuthorizedKey(publicKey, force, reason, adminSignature, adminPublicKey);

                System.out.println("🗑️ ✅ Key permanently deleted from database");
                System.out.println("⚠️ WARNING: This action was IRREVERSIBLE!");

                // Validate the chain after deletion
                ChainValidationResult result = blockchain.validateChainDetailed();
                if (!result.isStructurallyIntact()) {
                    System.err.println("❌ CRITICAL: Chain validation failed after key deletion!");
                    System.err.println("   - Invalid blocks: " + result.getInvalidBlocks());
                    // In a real application, you might want to trigger a rollback or alert here
                } else if (!result.isFullyCompliant()) {
                    System.out.println("⚠️ WARNING: Chain has compliance issues after deletion");
                    System.out.println("   - Revoked blocks: " + result.getRevokedBlocks());
                } else {
                    System.out.println("✅ Chain validation passed after key deletion");
                }
            } catch (SecurityException e) {
                System.err.println("❌ SECURITY VIOLATION: " + e.getMessage());
                System.err.println("   Admin authorization invalid - operation blocked");
            } catch (IllegalStateException e) {
                System.err.println("❌ DELETION BLOCKED: " + e.getMessage());
                System.err.println("   Safety checks or backup creation failed");
            } catch (IllegalArgumentException e) {
                System.err.println("❌ INVALID KEY: " + e.getMessage());
                System.err.println("   The specified key does not exist");
            }
                
                // Log the deletion event to the blockchain itself
                try {
                    blockchain.addBlock("SECURITY_EVENT: Key permanently deleted | Key ID: " + 
                                    publicKey.substring(0, 32) + "... | Reason: " + reason,
                                    null, null); // Using system account
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to log deletion event: " + e.getMessage());
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
    // RBAC v1.0.6+: Requires admin credentials to authorize new keys
    public void demonstrateKeyDeletionWorkflow(Blockchain blockchain, KeyPair adminKeyPair) {
        // Example 1: Safe deletion workflow
        System.out.println("=== EXAMPLE 1: Safe Deletion Workflow ===");

        // Create a test key that hasn't signed any blocks
        KeyPair testKey = CryptoUtil.generateKeyPair();
        String testPublicKey = CryptoUtil.publicKeyToString(testKey.getPublic());
        blockchain.addAuthorizedKey(
            testPublicKey,
            "Test User - No Blocks",
            adminKeyPair,       // Caller: requires ADMIN or SUPER_ADMIN
            UserRole.USER
        );

        // Safe deletion - should succeed
        boolean safeResult = safeDeleteKey(blockchain, testPublicKey, "Cleanup test keys");
        System.out.println("Safe deletion result: " + (safeResult ? "SUCCESS ✅" : "FAILED ❌"));

        // Example 2: Blocked deletion workflow
        System.out.println("\n=== EXAMPLE 2: Blocked Deletion Workflow ===");

        // Create a key and use it to sign blocks
        KeyPair activeKey = CryptoUtil.generateKeyPair();
        String activePublicKey = CryptoUtil.publicKeyToString(activeKey.getPublic());
        blockchain.addAuthorizedKey(
            activePublicKey,
            "Active User - Has Blocks",
            adminKeyPair,
            UserRole.USER
        );
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
        ChainValidationResult validationResult = blockchain.validateChainDetailed();
        boolean structurallyIntact = validationResult.isStructurallyIntact();
        boolean fullyCompliant = validationResult.isFullyCompliant();
        System.out.println("Blockchain structural integrity after forced deletion: " + 
                           (structurallyIntact ? "INTACT ✅" : "COMPROMISED ❌"));
        System.out.println("Blockchain full compliance after forced deletion: " + 
                           (fullyCompliant ? "COMPLIANT ✅" : "NON-COMPLIANT ❌ (expected)"));
        if (!fullyCompliant) {
            System.out.println("Revoked blocks: " + validationResult.getRevokedBlocks());
        }
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
            ChainValidationResult validationResult = blockchain.validateChainDetailed();
            boolean structurallyIntact = validationResult.isStructurallyIntact();
            boolean fullyCompliant = validationResult.isFullyCompliant();
            status.setStructurallyIntact(structurallyIntact);
            status.setFullyCompliant(fullyCompliant);
            if (!structurallyIntact) {
                status.setInvalidBlocks(validationResult.getInvalidBlocks());
            }
            if (!fullyCompliant) {
                status.setRevokedBlocks(validationResult.getRevokedBlocks());
            }
            
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
            if (structurallyIntact && status.getFreeSpace() > 1000000000 && status.getActiveKeys() > 0) {
                status.setOverallHealth("HEALTHY");
                System.out.println("✅ Blockchain is healthy and fully operational");
            } else if (structurallyIntact) {
                status.setOverallHealth("WARNING");
                System.out.println("⚠️  Warning: " + 
                    (status.getFreeSpace() <= 1000000000 ? "Low disk space. " : "") +
                    (status.getActiveKeys() == 0 ? "No active keys configured. " : ""));
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
        "SELECT b FROM Block b WHERE b.timestamp BETWEEN :startTime AND :endTime",
        Block.class);
    // Note: blockNumber ordering is automatic (PRIMARY KEY creates unique index, ASC order guaranteed)
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
// Phase 5.0: Manual ID assignment with JDBC batching
@Entity
@Table(name = "blocks")
public class Block {
    /**
     * Block number (position in the chain).
     * Phase 5.0: Manually assigned before persist() to allow hash calculation.
     * JDBC batching enabled via persistence.xml configuration.
     */
    @Id
    @Column(name = "block_number", unique = true, nullable = false, updatable = false)
    private Long blockNumber;
    
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
    private KeyPair adminKeyPair;  // RBAC v1.0.6+

    public BlockchainCLI() {
        this.blockchain = new Blockchain();
        // Load bootstrap admin keys (RBAC v1.0.6+)
        try {
            this.adminKeyPair = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            // Register bootstrap admin in blockchain (REQUIRED!)
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(this.adminKeyPair.getPublic()),
                "BOOTSTRAP_ADMIN"
            );
        } catch (Exception e) {
            System.err.println("Failed to load admin keys: " + e.getMessage());
            throw new RuntimeException("Cannot initialize CLI without admin keys", e);
        }
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
            // RBAC v1.0.6+: Requires admin credentials
            if (blockchain.addAuthorizedKey(
                    publicKey,
                    ownerName,
                    adminKeyPair,       // Caller: bootstrap admin
                    UserRole.USER
                )) {
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
            System.out.println("\n🔍 Starting blockchain validation...");
            long startTime = System.currentTimeMillis();
            
            ChainValidationResult result = blockchain.validateChainDetailed();
            boolean structurallyIntact = result.isStructurallyIntact();
            boolean fullyCompliant = result.isFullyCompliant();
            
            long validationTime = System.currentTimeMillis() - startTime;
            
            // Print summary header
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🔗 BLOCKCHAIN VALIDATION REPORT");
            System.out.println("📊 " + blockchain.getBlockCount() + " blocks analyzed in " + validationTime + "ms");
            System.out.println("-".repeat(60));
            
            // Structural validation results
            System.out.println("1. STRUCTURAL INTEGRITY: " + 
                (structurallyIntact ? "✅ INTACT" : "❌ COMPROMISED"));
            
            // Compliance validation results
            System.out.println("2. AUTHORIZATION COMPLIANCE: " + 
                (fullyCompliant ? "✅ FULLY COMPLIANT" : "⚠️  PARTIALLY COMPLIANT"));
            
            // Detailed issues
            if (!structurallyIntact) {
                System.out.println("\n❌ INVALID BLOCKS DETECTED (" + result.getInvalidBlocks() + "):");
                result.getInvalidBlocksList().forEach(block -> 
                    System.out.println("   - Block " + block.getIndex() + 
                                    " | Hash: " + block.getHash().substring(0, 16) + "..." +
                                    " | Issue: " + block.getValidationMessage()));
            }
            
            if (!fullyCompliant) {
                System.out.println("\n⚠️  REVOKED KEYS DETECTED (" + result.getRevokedBlocks() + " blocks affected):");
                result.getOrphanedBlocks().stream()
                    .limit(5) // Show first 5 for brevity
                    .forEach(block -> 
                        System.out.println("   - Block " + block.getIndex() + 
                                        " | Signed by: " + block.getSignedBy().substring(0, 16) + "..."));
                
                if (result.getRevokedBlocks() > 5) {
                    System.out.println("   ... and " + (result.getRevokedBlocks() - 5) + " more");
                }
            }
            
            // Chain statistics
            System.out.println("\n📊 CHAIN STATISTICS:");
            System.out.println("   • Total blocks: " + blockchain.getBlockCount());
            System.out.println("   • Active keys: " + blockchain.getAuthorizedKeys().size());
            
            // Recommendations
            System.out.println("\n💡 RECOMMENDATIONS:");
            if (!structurallyIntact) {
                System.out.println("   • Run recovery procedures to fix invalid blocks");
            }
            if (!fullyCompliant) {
                System.out.println("   • Review and update authorized keys configuration");
            }
            if (structurallyIntact && fullyCompliant) {
                System.out.println("   • No issues detected. Your blockchain is healthy!");
            }
            
            System.out.println("=".repeat(60) + "\n")
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

For detailed API documentation, see [API_GUIDE.md](../reference/API_GUIDE.md).  
For production deployment guidance, see [PRODUCTION_GUIDE.md](../deployment/PRODUCTION_GUIDE.md).  
For testing and troubleshooting, see [TESTING.md](../testing/TESTING.md).
        
        // Wait for all read operations to complete
        CompletableFuture.allOf(readOperations.toArray(new CompletableFuture[0]))
            .thenRun(() -> System.out.println("✅ All concurrent read operations completed"))
            .join();
    }
    
    /**
     * ✅ THREAD-SAFE: Safe search with concurrent caching
     */
    private final Map<String, List<Block>> searchCache = new ConcurrentHashMap<>();
    
    public List<Block> searchWithThreadSafeCache(String searchTerm) {
        // Check cache first (thread-safe)
        String cacheKey = searchTerm.toLowerCase();
        List<Block> cachedResults = searchCache.get(cacheKey);
        if (cachedResults != null) {
            System.out.println("Cache hit for: " + searchTerm);
            return new ArrayList<>(cachedResults); // Return copy to avoid external modification
        }
        
        // Perform search (blockchain.searchBlocksByContent is thread-safe)
        List<Block> results = blockchain.searchBlocksByContent(searchTerm);
        
        // Cache results with size limit (atomic operation)
        if (searchCache.size() < 100) {
            searchCache.put(cacheKey, new ArrayList<>(results)); // Store copy
        }
        
        System.out.println("Search completed for: " + searchTerm + " (" + results.size() + " results)");
        return results;
    }
    
    /**
     * ✅ THREAD-SAFE: Cleanup method
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Simple data class for transaction information
     */
    public static class TransactionData {
        private final String data;
        private final PrivateKey privateKey;
        private final PublicKey publicKey;
        
        public TransactionData(String data, PrivateKey privateKey, PublicKey publicKey) {
            this.data = data;
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }
        
        public String getData() { return data; }
        public PrivateKey getPrivateKey() { return privateKey; }
        public PublicKey getPublicKey() { return publicKey; }
    }
}
```

### Producer-Consumer Pattern Example

```java
/**
 * ✅ THREAD-SAFE: Producer-Consumer pattern with blockchain
 * Multiple producers add transactions, single consumer processes them
 */
public class BlockchainProducerConsumer {
    private final Blockchain blockchain;
    private final BlockingQueue<TransactionData> transactionQueue;
    private final ExecutorService producerPool;
    private volatile boolean running = true;
    
    public BlockchainProducerConsumer() {
        this.blockchain = new Blockchain();
        this.transactionQueue = new LinkedBlockingQueue<>(1000); // Bounded queue
        this.producerPool = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Start multiple producer threads
     */
    public void startProducers(int numberOfProducers, KeyPair signerKeys) {
        for (int i = 0; i < numberOfProducers; i++) {
            final int producerId = i;
            producerPool.submit(() -> {
                while (running) {
                    try {
                        // Generate transaction data
                        String transactionData = "Producer-" + producerId + "-Transaction-" + System.currentTimeMillis();
                        TransactionData transaction = new TransactionData(
                            transactionData, 
                            signerKeys.getPrivate(), 
                            signerKeys.getPublic()
                        );
                        
                        // Add to queue (blocking if queue is full)
                        transactionQueue.put(transaction);
                        System.out.println("Producer " + producerId + " queued transaction");
                        
                        // Simulate work
                        Thread.sleep(100 + (int)(Math.random() * 200));
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }
    
    /**
     * Consumer thread that processes transactions
     */
    public void startConsumer() {
        Thread consumerThread = new Thread(() -> {
            while (running || !transactionQueue.isEmpty()) {
                try {
                    // Take from queue (blocking if queue is empty)
                    TransactionData transaction = transactionQueue.poll(1, TimeUnit.SECONDS);
                    if (transaction != null) {
                        // Process transaction using thread-safe blockchain
                        Block createdBlock = blockchain.addBlockAndReturn(
                            transaction.getData(),
                            transaction.getPrivateKey(),
                            transaction.getPublicKey()
                        );
                        
                        if (createdBlock != null) {
                            System.out.println("✅ Consumer processed transaction -> Block #" + createdBlock.getBlockNumber());
                        } else {
                            System.err.println("❌ Consumer failed to process transaction");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("Consumer thread finished");
        });
        
        consumerThread.start();
    }
    
    public void stop() {
        running = false;
        producerPool.shutdown();
    }
}
```

### High-Performance Concurrent Testing Example

```java
/**
 * ✅ THREAD-SAFE: High-performance concurrent testing
 * Demonstrates blockchain performance under heavy concurrent load
 */
public class ConcurrentPerformanceTest {
    
    public static void testHighConcurrency() {
        Blockchain blockchain = new Blockchain();

        // Load bootstrap admin keys (RBAC v1.0.6+)
        KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Setup test keys
        KeyPair testKeys = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(testKeys.getPublic());
        blockchain.addAuthorizedKey(
            publicKeyString,
            "Performance Test User",
            bootstrapKeys,      // Caller: bootstrap admin
            UserRole.USER
        );

        // Test parameters
        int numberOfThreads = 20;
        int blocksPerThread = 10;
        int expectedBlocks = numberOfThreads * blocksPerThread;
        
        System.out.println("🧪 Starting high-concurrency test:");
        System.out.println("   Threads: " + numberOfThreads);
        System.out.println("   Blocks per thread: " + blocksPerThread);
        System.out.println("   Expected total blocks: " + expectedBlocks);
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // Submit concurrent tasks
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < blocksPerThread; j++) {
                        String data = "Thread-" + threadId + "-Block-" + j + "-" + System.currentTimeMillis();
                        
                        // Use addBlockAndReturn for better testing
                        Block createdBlock = blockchain.addBlockAndReturn(
                            data, 
                            testKeys.getPrivate(), 
                            testKeys.getPublic()
                        );
                        
                        if (createdBlock != null) {
                            int current = successCount.incrementAndGet();
                            if (current % 50 == 0) {
                                System.out.println("Progress: " + current + "/" + expectedBlocks + " blocks created");
                            }
                        } else {
                            failureCount.incrementAndGet();
                        }
                        
                        // Small random delay to increase concurrency stress
                        Thread.sleep((int)(Math.random() * 10));
                    }
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " failed: " + e.getMessage());
                    failureCount.addAndGet(blocksPerThread);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            // Wait for all threads to complete
            latch.await();
            executor.shutdown();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Final validation
            ChainValidationResult validationResult = blockchain.validateChainDetailed();
            boolean structurallyIntact = validationResult.isStructurallyIntact();
            boolean fullyCompliant = validationResult.isFullyCompliant();
            long actualBlockCount = blockchain.getBlockCount();
            
            System.out.println("\n📊 Performance Test Results:");
            System.out.println("   Duration: " + duration + "ms");
            System.out.println("   Successful blocks: " + successCount.get());
            System.out.println("   Failed blocks: " + failureCount.get());
            System.out.println("   Actual block count: " + actualBlockCount + " (including genesis)");
            System.out.println("   Expected count: " + (expectedBlocks + 1));
            System.out.println("   Chain structurally intact: " + (structurallyIntact ? "✅ YES" : "❌ NO"));
            System.out.println("   Chain fully compliant: " + (fullyCompliant ? "✅ YES" : "❌ NO"));
            System.out.println("   Throughput: " + (successCount.get() * 1000.0 / duration) + " blocks/second");
            
            if (structurallyIntact && fullyCompliant && actualBlockCount == (expectedBlocks + 1)) {
                System.out.println("🎉 HIGH-CONCURRENCY TEST PASSED!");
            } else {
                System.out.println("💥 HIGH-CONCURRENCY TEST FAILED!");
            }
            
        } catch (InterruptedException e) {
            System.err.println("Test interrupted: " + e.getMessage());
        }
    }
}
```

### Best Practices for Concurrent Usage

```java
/**
 * ✅ THREAD-SAFE: Best practices for concurrent blockchain usage
 */
public class ConcurrentBestPractices {
    
    /**
     * ✅ GOOD: Use addBlockAndReturn() for concurrent scenarios where you need immediate block info
     */
    public void goodConcurrentBlockAddition(Blockchain blockchain, String data, 
                                          PrivateKey privateKey, PublicKey publicKey) {
        CompletableFuture<Block> future = CompletableFuture.supplyAsync(() -> {
            return blockchain.addBlockAndReturn(data, privateKey, publicKey);
        });
        
        future.thenAccept(block -> {
            if (block != null) {
                System.out.println("Block created: #" + block.getBlockNumber() + " with hash: " + block.getHash());
            } else {
                System.err.println("Block creation failed");
            }
        });
    }
    
    /**
     * ✅ GOOD: Proper error handling in concurrent scenarios
     */
    public void robustConcurrentOperation(Blockchain blockchain, List<String> dataList, 
                                        PrivateKey privateKey, PublicKey publicKey) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        for (String data : dataList) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Block block = blockchain.addBlockAndReturn(data, privateKey, publicKey);
                    return block != null ? "SUCCESS: Block #" + block.getBlockNumber() : "FAILED: " + data;
                } catch (Exception e) {
                    return "ERROR: " + data + " - " + e.getMessage();
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Collect results with proper timeout
        CompletableFuture<List<String>> allResults = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        ).thenApply(v -> futures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()));
        
        try {
            List<String> results = allResults.get(30, TimeUnit.SECONDS);
            results.forEach(System.out::println);
        } catch (TimeoutException e) {
            System.err.println("Operations timed out");
        } catch (Exception e) {
            System.err.println("Error collecting results: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * ✅ GOOD: Concurrent read operations take advantage of ReadWriteLock
     */
    public void efficientConcurrentReads(Blockchain blockchain) {
        // Multiple read operations can execute simultaneously
        CompletableFuture<Long> blockCountFuture = CompletableFuture.supplyAsync(blockchain::getBlockCount);
        CompletableFuture<List<AuthorizedKey>> keysFuture = CompletableFuture.supplyAsync(blockchain::getAuthorizedKeys);
        CompletableFuture<ChainValidationResult> validationFuture = CompletableFuture.supplyAsync(blockchain::validateChainDetailed);

        // Combine results
        CompletableFuture.allOf(blockCountFuture, keysFuture, validationFuture)
            .thenRun(() -> {
                try {
                    long blockCount = blockCountFuture.get();
                    System.out.println("Blockchain Status:");
                    System.out.println("  Block count: " + blockCount);
                    System.out.println("  Active keys: " + keysFuture.get().size());
                    ChainValidationResult result = validationFuture.get();
                    System.out.println("  Chain structurally intact: " + result.isStructurallyIntact());
                    System.out.println("  Chain fully compliant: " + result.isFullyCompliant());
                } catch (Exception e) {
                    System.err.println("Error retrieving results: " + e.getMessage());
                }
            })
            .join();
    }
}
```

### Thread-Safety Guarantees

The Private Blockchain provides the following thread-safety guarantees:

- ✅ **Global Synchronization**: All operations are protected by a global `StampedLock`
- ✅ **Optimistic Reads**: Core methods use lock-free optimistic reads for best performance
- ✅ **Multiple Reader Support**: Multiple threads can read simultaneously (conservative read lock)
- ✅ **Exclusive Writer Access**: Write operations have exclusive access
- ✅ **Transaction Consistency**: All database operations use JPA transactions
- ✅ **Atomic Block Numbers**: Block numbering is atomic and prevents duplicates
- ✅ **Safe Key Management**: Key operations are fully synchronized
- ✅ **Chain Integrity**: Validation operations are consistent during concurrent access

### Performance Characteristics

- **Read Operations**: Scale well with multiple concurrent threads
- **Write Operations**: Serialized for safety, but optimized for performance
- **Mixed Workloads**: Read-heavy workloads perform excellently
- **High Concurrency**: Tested with 20+ concurrent threads
- **Memory Efficiency**: ThreadLocal EntityManager management
- **Database Optimization**: WAL mode enabled for better concurrent access

For more technical details about the thread-safety implementation, see [TECHNICAL_DETAILS.md](../reference/TECHNICAL_DETAILS.md).

For detailed API information including the thread-safe `addBlockAndReturn()` method, see [API_GUIDE.md](../reference/API_GUIDE.md).

For testing concurrent scenarios, see [TESTING.md](../testing/TESTING.md) and run `./scripts/run_thread_safety_test.zsh` or `./scripts/run_advanced_thread_safety_tests.zsh` for more complex concurrency tests. For race condition testing, use `./scripts/test_race_condition_fix.zsh`.

To explore cryptographic security features, run `./scripts/run_crypto_security_demo.zsh` which demonstrates the security mechanisms in action.

For security analysis and testing, use `./scripts/run_security_analysis.zsh` and `./scripts/run_security_tests.zsh`.

### 🏢 Use Case 8: Multi-Department Enterprise Blockchain ⭐ **NEW**

Enterprise-level multi-tenant blockchain system with department-specific encryption and secure cross-department isolation.

```java
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.util.*;

public class MultiDepartmentEnterpriseExample {
    public static void main(String[] args) throws Exception {
        System.out.println("🏢 MULTI-DEPARTMENT ENTERPRISE BLOCKCHAIN DEMO");
        System.out.println("==============================================");

        // Initialize enterprise blockchain
        Blockchain blockchain = new Blockchain();
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);

        // Setup enterprise user with hierarchical keys
        KeyPair enterpriseKeys = api.createUser("enterprise.admin");
        api.setDefaultCredentials("enterprise.admin", enterpriseKeys);

        // Department-specific passwords (realistic enterprise naming)
        String medicalPassword = "Medical_Dept_2024!SecureKey_" + generateSecureRandomSuffix();
        String financePassword = "Finance_Dept_2024!SecureKey_" + generateSecureRandomSuffix();
        String legalPassword = "Legal_Dept_2024!SecureKey_" + generateSecureRandomSuffix();
        String itPassword = "IT_Dept_2024!SecureKey_" + generateSecureRandomSuffix();

        String[] departmentPasswords = {
            medicalPassword, financePassword, legalPassword, itPassword
        };

        System.out.println("🔑 Generated department-specific passwords:");
        System.out.println("   🏥 Medical Department: [PROTECTED]");
        System.out.println("   💰 Finance Department: [PROTECTED]");
        System.out.println("   ⚖️ Legal Department: [PROTECTED]");
        System.out.println("   🔐 IT Department: [PROTECTED]");

        // 📊 EFFICIENT INITIALIZATION FOR ALL DEPARTMENTS
        System.out.println("\n📊 Initializing search system for multi-department operations...");
        blockchain.initializeAdvancedSearchWithMultiplePasswords(departmentPasswords);
        System.out.println("✅ Search system optimized for " + departmentPasswords.length + " departments");

        // Medical Department - Store patient data
        System.out.println("\n🏥 Medical Department - Storing patient records...");
        var medicalBlock1 = api.storeSearchableData(
            "Patient: John Doe, DOB: 1985-03-15, Diagnosis: Hypertension, Treatment: Lisinopril 10mg",
            medicalPassword,
            new String[]{"patient", "hypertension", "PATIENT_001"}
        );

        var medicalBlock2 = api.storeSearchableData(
            "Patient: Sarah Wilson, DOB: 1990-07-22, Diagnosis: Diabetes Type 2, HbA1c: 8.2%",
            medicalPassword,
            new String[]{"patient", "diabetes", "PATIENT_002"}
        );

        // Finance Department - Store transaction data
        System.out.println("💰 Finance Department - Storing financial records...");
        var financeBlock1 = api.storeSearchableData(
            "Account: CHK_123456789, Balance: $45,230.50, Transaction: Payroll $85,000, Date: 2024-01-15",
            financePassword,
            new String[]{"account", "payroll", "ACC_123456"}
        );

        var financeBlock2 = api.storeSearchableData(
            "Contract Payment: Vendor ABC Corp, Amount: $25,000, Invoice: INV-2024-001, Approved by: CFO",
            financePassword,
            new String[]{"contract", "payment", "vendor", "INV_2024_001"}
        );

        // Legal Department - Store contract data
        System.out.println("⚖️ Legal Department - Storing legal documents...");
        var legalBlock1 = api.storeSearchableData(
            "Contract Agreement: ABC Corp License Deal, Terms: 3-year, Annual fee: $50,000, Jurisdiction: NY",
            legalPassword,
            new String[]{"contract", "license", "CASE_2024_001"}
        );

        var legalBlock2 = api.storeSearchableData(
            "Employment Agreement: John Smith, Position: Senior Developer, Salary: $120,000/year",
            legalPassword,
            new String[]{"employment", "agreement", "EMP_2024_015"}
        );

        // IT Department - Store system data
        System.out.println("🔐 IT Department - Storing system information...");
        var itBlock1 = api.storeSecret(
            "API Key: sk_live_123abc456def789, Database: postgresql://prod.db:5432/main, Backup: daily",
            itPassword
        );

        System.out.println("\n📊 Blockchain Status:");
        System.out.println("   📦 Total Blocks: " + api.getBlockchainSummary());
        System.out.println("   🔒 Encrypted Blocks: " + api.getEncryptedBlockCount());

        // 🔍 DEPARTMENT-SPECIFIC SEARCHES (SECURITY DEMONSTRATION)
        System.out.println("\n🔍 Department-Specific Search Operations:");

        // Medical Department searches (should find medical data only)
        var medicalPatients = api.searchAndDecryptByTerms(
            new String[]{"patient"}, medicalPassword, 10
        );
        System.out.println("🏥 Medical Dept found " + medicalPatients.size() + " patient records");

        // Finance Department searches (should find financial data only)
        var financeAccounts = api.searchAndDecryptByTerms(
            new String[]{"account"}, financePassword, 10
        );
        System.out.println("💰 Finance Dept found " + financeAccounts.size() + " account records");

        // Legal Department searches (should find legal data only)
        var legalContracts = api.searchAndDecryptByTerms(
            new String[]{"contract"}, legalPassword, 10
        );
        System.out.println("⚖️ Legal Dept found " + legalContracts.size() + " contract documents");

        // 🔒 SECURITY VALIDATION (Cross-department access should be blocked)
        System.out.println("\n🔒 Security Validation - Cross-Department Access Tests:");

        // Medical trying to access Finance data (should fail)
        String crossAccessResult = api.retrieveSecret(financeBlock1.getId(), medicalPassword);
        System.out.println("❌ Medical→Finance access: " +
            (crossAccessResult == null ? "CORRECTLY BLOCKED ✅" : "SECURITY BREACH ❌"));

        // Finance trying to access Legal data (should fail)
        crossAccessResult = api.retrieveSecret(legalBlock1.getId(), financePassword);
        System.out.println("❌ Finance→Legal access: " +
            (crossAccessResult == null ? "CORRECTLY BLOCKED ✅" : "SECURITY BREACH ❌"));

        // Legal trying to access Medical data (should fail)
        crossAccessResult = api.retrieveSecret(medicalBlock1.getId(), legalPassword);
        System.out.println("❌ Legal→Medical access: " +
            (crossAccessResult == null ? "CORRECTLY BLOCKED ✅" : "SECURITY BREACH ❌"));

        // ✅ AUTHORIZED ACCESS DEMONSTRATION
        System.out.println("\n✅ Authorized Access Verification:");

        // Medical accessing own data (should succeed)
        String medicalData = api.retrieveSecret(medicalBlock1.getId(), medicalPassword);
        System.out.println("✅ Medical→Medical: " +
            (medicalData != null ? "ACCESS GRANTED ✅" : "ACCESS DENIED ❌"));

        // Finance accessing own data (should succeed)
        String financeData = api.retrieveSecret(financeBlock1.getId(), financePassword);
        System.out.println("✅ Finance→Finance: " +
            (financeData != null ? "ACCESS GRANTED ✅" : "ACCESS DENIED ❌"));

        // 📈 PERFORMANCE SUMMARY
        System.out.println("\n📈 Multi-Department Performance Summary:");
        System.out.println("✅ Single initialization prevented " + (departmentPasswords.length * 2) + " redundant indexing operations");
        System.out.println("✅ All departments maintain complete data isolation");
        System.out.println("✅ Cross-department access correctly blocked by encryption");
        System.out.println("✅ Thread-safe operations across all departments");

        // 🎯 ENTERPRISE BENEFITS
        System.out.println("\n🎯 Enterprise Benefits Demonstrated:");
        System.out.println("   🏢 Multi-tenant architecture with complete isolation");
        System.out.println("   🔐 Department-specific encryption prevents data leakage");
        System.out.println("   ⚡ Optimized initialization reduces system overhead");
        System.out.println("   🛡️ Enterprise-grade security with audit trails");
        System.out.println("   📊 Scalable to unlimited departments/tenants");

        System.out.println("\n🎉 Multi-Department Enterprise Demo completed successfully!");
    }

    private static String generateSecureRandomSuffix() {
        return CryptoUtil.generateRandomString(12); // Assuming this method exists
    }
}
```

**Key Benefits Demonstrated:**

- ✅ **Department Isolation**: Complete encryption-based separation between departments
- ✅ **Optimized Initialization**: Single call to `initializeAdvancedSearchWithMultiplePasswords()` prevents redundant operations
- ✅ **Enterprise Security**: Cross-department access is cryptographically blocked
- ✅ **Scalable Architecture**: Supports unlimited departments with unique passwords
- ✅ **Performance Optimization**: Eliminates Tag mismatch errors during setup
- ✅ **Realistic Scenarios**: Demonstrates medical, finance, legal, and IT department workflows

**Security Features:**
- 🔐 Each department can only decrypt its own data
- ❌ Cross-department access attempts are automatically blocked
- ✅ Thread-safe operations ensure data consistency
- 📊 Audit trail maintains complete transaction history

## 📚 Additional Resources

### 🚀 Getting Started & Fundamentals
For getting started with the blockchain system, see [GETTING_STARTED.md](GETTING_STARTED.md).  
For complete API reference and core functions, see [API_GUIDE.md](../reference/API_GUIDE.md).  
For comprehensive testing procedures, see [TESTING.md](../testing/TESTING.md).

### 🔐 Security & Key Management  
For security best practices and production guidelines, see [SECURITY_GUIDE.md](../security/SECURITY_GUIDE.md).  
For hierarchical key management and rotation, see [KEY_MANAGEMENT_GUIDE.md](../security/KEY_MANAGEMENT_GUIDE.md).  
For encryption and metadata management, see [ENCRYPTION_GUIDE.md](../security/ENCRYPTION_GUIDE.md).

### 🔍 Search & Data Operations
For UserFriendlyEncryptionAPI search functionality, see [USER_FRIENDLY_SEARCH_GUIDE.md](../search/USER_FRIENDLY_SEARCH_GUIDE.md).  
For Advanced Search Engine guide, see [SEARCH_FRAMEWORK_GUIDE.md](../search/SEARCH_FRAMEWORK_GUIDE.md).  
For complete search system comparison, see [EXHAUSTIVE_SEARCH_GUIDE.md](../search/EXHAUSTIVE_SEARCH_GUIDE.md).  
For search performance benchmarks, see [SEARCH_COMPARISON.md](../search/SEARCH_COMPARISON.md).

### 🛠️ Operations & Troubleshooting
For troubleshooting common issues and diagnostics, see [TROUBLESHOOTING_GUIDE.md](TROUBLESHOOTING_GUIDE.md).  
For production deployment guidance, see [PRODUCTION_GUIDE.md](../deployment/PRODUCTION_GUIDE.md).  
For technical implementation details, see [TECHNICAL_DETAILS.md](../reference/TECHNICAL_DETAILS.md).
