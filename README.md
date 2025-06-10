# Private Blockchain

A simple and secure private blockchain implementation using Java 21, SQLite, and Hibernate.

## 📋 Overview

This is a **private blockchain** for controlled environments where only authorized users can add blocks. Unlike public blockchains, there is no mining - blocks are added directly by authorized users.

## ✨ Main Features

### Core Blockchain Features
- **Genesis Block**: Created automatically when blockchain starts
- **Block Creation**: Add new blocks with data to the chain
- **Hash Verification**: SHA-256 hashing ensures data integrity
- **Digital Signatures**: RSA signatures verify block authenticity
- **Chain Validation**: Complete blockchain integrity checking

### Security Features
- **Authorized Keys**: Only approved users can add blocks
- **Cryptographic Protection**: Each block is cryptographically signed
- **Immutable Records**: Blocks cannot be changed once added
- **Sequential Validation**: Each block links to the previous block

### Advanced Features
- **Block Size Validation**: Prevents oversized blocks
- **Chain Export/Import**: Backup and restore complete blockchain
- **Block Rollback**: Safe removal of recent blocks
- **Advanced Search**: Find blocks by content, hash, or date range

### Technical Features
- **Persistent Storage**: SQLite database with Hibernate ORM
- **Auto-generated Tables**: Database schema created automatically
- **Clean Architecture**: Well-structured code with DAO pattern
- **Java 21**: Modern Java features and performance

## 🛠️ Technologies Used

- **Java 21** - Programming language
- **Maven** - Build and dependency management
- **SQLite** - Lightweight database for data storage
- **Hibernate** - Object-relational mapping (ORM)
- **SHA-256** - Cryptographic hash function
- **RSA** - Digital signature algorithm
- **JUnit 5** - Testing framework

## 📦 Project Structure

```
src/main/java/com/rbatllet/blockchain/
├── core/
│   └── Blockchain.java                           # Main blockchain logic
├── dao/
│   ├── BlockDAO.java                            # Database operations for blocks
│   └── AuthorizedKeyDAO.java                    # Database operations for keys
├── entity/
│   ├── Block.java                               # Block data model
│   └── AuthorizedKey.java                       # Authorized key data model
├── util/
│   ├── CryptoUtil.java                          # Cryptographic utilities
│   └── HibernateUtil.java                       # Database connection management
├── BlockchainDemo.java                          # Basic demo application
├── AdditionalAdvancedFunctionsDemo.java         # Advanced features demo
├── CoreFunctionsTest.java                       # Comprehensive core test
├── SimpleTest.java                              # Basic functionality test
└── QuickTest.java                               # Fast verification test

src/test/java/com/rbatllet/blockchain/core/
├── BlockchainAdditionalAdvancedFunctionsTest.java   # JUnit 5 test suite (22 tests)
├── BlockchainAdditionalAdvancedFunctionsTestRunner.java # Test runner
└── TestEnvironmentValidator.java                    # Environment validation

Scripts:
├── run_all_tests.sh                             # Run all tests (recommended)
├── run_advanced_tests.sh                        # Run advanced functions tests only
└── run_basic_tests.sh                           # Run basic core functions tests only
```

## 🚀 How to Run

### Prerequisites
- **Java 21** or higher
- **Maven 3.6** or higher

### Quick Start
```bash
# 1. Navigate to project directory
cd /path/to/privateBlockchain

# 2. Compile the project
mvn clean compile

# 3. Run the basic demo
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"

# 4. Run advanced features demo
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.core.AdditionalAdvancedFunctionsDemo"
```

### Expected Output (Basic Demo)
```
=== PRIVATE BLOCKCHAIN DEMO ===
Genesis block created successfully!
Authorized key added for: Alice
Authorized key added for: Bob
Block #1 added successfully!
Block #2 added successfully!
Block #3 added successfully!
Chain validation successful! Total blocks: 4
Blockchain is valid: true
=== BLOCKCHAIN STATUS ===
Total blocks: 4
Authorized keys: 2
=== DEMO COMPLETED ===
```

## 💻 How It Works

### Step 1: Initialize Blockchain
```java
Blockchain blockchain = new Blockchain();
// Creates genesis block automatically
```

### Step 2: Add Authorized Users
```java
KeyPair userKeys = CryptoUtil.generateKeyPair();
String publicKey = CryptoUtil.publicKeyToString(userKeys.getPublic());
blockchain.addAuthorizedKey(publicKey, "UserName");
```

### Step 3: Add Blocks
```java
boolean success = blockchain.addBlock(
    "Your data here", 
    userKeys.getPrivate(), 
    userKeys.getPublic()
);
```

### Step 4: Validate Chain
```java
boolean isValid = blockchain.validateChain();
```

## 🧪 Testing

The project includes comprehensive test suites to verify all functionality.

### Recommended Testing Order

#### 1. Run All Tests (Complete Validation) ⭐ **RECOMMENDED**
```bash
./run_all_tests.sh
```
This runs everything: basic core tests + advanced function tests.

**Expected output:**
```
=== COMPREHENSIVE BLOCKCHAIN TEST RUNNER ===
✅ Compilation successful!
🎉 JUnit 5 Additional Advanced Functions tests: PASSED (22/22)
✅ Basic Core Functions test: PASSED
✅ Blockchain Demo: PASSED
✅ Simple Test: PASSED
✅ Quick Test: PASSED

📊 Test suites passed: 5/5
🎉 ALL TESTS PASSED SUCCESSFULLY!
```

#### 2. Advanced Functions Only (JUnit 5 Tests)
```bash
./run_advanced_tests.sh
```
Runs 22 professional JUnit 5 tests for additional advanced functions only.

**Note**: You may see error messages like "Error exporting chain" or "Import file not found" during these tests. These are **intentional test cases** that verify proper error handling - they are not actual failures.

#### 3. Basic Core Functions Only
```bash
./run_basic_tests.sh
```
Runs the comprehensive basic core functions test that validates fundamental blockchain operations.

**Expected output:**
```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
🎉 ALL TESTS PASSED!
```

#### 4. Interactive Demonstrations
```bash
# Advanced features demo with practical examples
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.core.AdditionalAdvancedFunctionsDemo"

# Basic demo with multiple users
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"

# Core functions comprehensive test
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest"
```

#### 5. Quick Verification Tests
```bash
# Fast verification
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.QuickTest"

# Basic functionality
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.SimpleTest"
```

### What Gets Tested

#### Core Blockchain Functions
- ✅ Genesis block creation
- ✅ Add/revoke authorized keys  
- ✅ Add blocks to chain
- ✅ Chain validation and integrity
- ✅ Security controls and authorization
- ✅ Error handling and edge cases

#### Advanced Functions (22 JUnit 5 Tests)
- ✅ **Block Size Validation**: Prevents oversized blocks
- ✅ **Chain Export**: Complete blockchain backup to JSON
- ✅ **Chain Import**: Blockchain restore from backup
- ✅ **Block Rollback**: Safe removal of recent blocks
- ✅ **Advanced Search**: Content, hash, and date range search
- ✅ **Integration**: All functions working together
- ✅ **Error Handling**: Graceful failure handling
- ✅ **Performance**: Execution time validation

### Troubleshooting Tests

#### If Tests Fail
```bash
# Reset database and try again
rm blockchain.db
./run_all_tests.sh

# Check Java version
java -version  # Should be 21+

# Validate environment
mvn clean compile test-compile
```

#### Database Issues
```bash
# Reset database
rm blockchain.db

# Check permissions
ls -la blockchain.db
```

## 🎯 Core Functions Usage

### Basic Operations

#### Initialize and Setup
```java
// Create blockchain (automatic genesis block)
Blockchain blockchain = new Blockchain();

// Add authorized users
KeyPair alice = CryptoUtil.generateKeyPair();
String alicePublicKey = CryptoUtil.publicKeyToString(alice.getPublic());
blockchain.addAuthorizedKey(alicePublicKey, "Alice");
```

#### Add Blocks
```java
// Add data to blockchain
boolean success = blockchain.addBlock(
    "Transaction: Alice sends payment to Bob",  // Your data
    alice.getPrivate(),                         // Private key (for signing)
    alice.getPublic()                          // Public key (for verification)
);
```

#### Validate Chain
```java
// Check if blockchain is valid
boolean isValid = blockchain.validateChain();
System.out.println("Blockchain is valid: " + isValid);
```

### Advanced Operations

#### Block Size Validation
```java
// Get size limits
int maxBytes = blockchain.getMaxBlockSizeBytes();      // 1MB limit
int maxChars = blockchain.getMaxBlockDataLength();     // 10K characters limit

// Size validation happens automatically when adding blocks
// Large blocks are rejected automatically
```

#### Chain Export/Import (Backup/Restore)
```java
// Export blockchain to JSON file
boolean exported = blockchain.exportChain("backup.json");

// Import blockchain from JSON file
boolean imported = blockchain.importChain("backup.json");
```

#### Block Rollback
```java
// Remove last 3 blocks
boolean success = blockchain.rollbackBlocks(3);

// Rollback to specific block (keep blocks 0-5)
boolean success = blockchain.rollbackToBlock(5);
```

#### Advanced Search
```java
// Search blocks by content (case-insensitive)
List<Block> paymentBlocks = blockchain.searchBlocksByContent("payment");

// Find block by hash
Block block = blockchain.getBlockByHash("a1b2c3d4...");

// Find blocks by date range
LocalDate start = LocalDate.of(2024, 1, 1);
LocalDate end = LocalDate.of(2024, 1, 31);
List<Block> monthlyBlocks = blockchain.getBlocksByDateRange(start, end);
```

### Complete Example
```java
public class BlockchainExample {
    public static void main(String[] args) {
        try {
            // 1. Initialize blockchain
            Blockchain blockchain = new Blockchain();
            
            // 2. Add users
            KeyPair alice = CryptoUtil.generateKeyPair();
            KeyPair bob = CryptoUtil.generateKeyPair();
            
            String aliceKey = CryptoUtil.publicKeyToString(alice.getPublic());
            String bobKey = CryptoUtil.publicKeyToString(bob.getPublic());
            
            blockchain.addAuthorizedKey(aliceKey, "Alice");
            blockchain.addAuthorizedKey(bobKey, "Bob");
            
            // 3. Add blocks
            blockchain.addBlock("Alice registers", alice.getPrivate(), alice.getPublic());
            blockchain.addBlock("Bob joins network", bob.getPrivate(), bob.getPublic());
            blockchain.addBlock("Alice sends payment", alice.getPrivate(), alice.getPublic());
            
            // 4. Search and validate
            List<Block> payments = blockchain.searchBlocksByContent("payment");
            System.out.println("Payment blocks found: " + payments.size());
            
            boolean isValid = blockchain.validateChain();
            System.out.println("Blockchain is valid: " + isValid);
            
            // 5. Backup
            blockchain.exportChain("blockchain_backup.json");
            System.out.println("Blockchain backed up successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

## 💼 Real-World Use Cases & Examples

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

### Common Workflow Patterns

#### Daily Backup Routine
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

#### Batch Transaction Processing
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

## 🔒 Security Model

### Block Security
- Each block contains a SHA-256 hash of its content
- Blocks are linked by including the previous block's hash
- Any tampering breaks the chain validation

### Access Control
- Only users with authorized public keys can add blocks
- Each block is digitally signed with the user's private key
- Signatures are verified before accepting blocks

### Data Integrity
- All blocks are validated when checking the chain
- Hash verification ensures no data has been modified
- Sequential validation confirms proper block order

## 📊 Database Schema

The application automatically creates these tables:

### blocks table
- `id` - Unique identifier
- `block_number` - Sequential block number (starts from 0)
- `previous_hash` - Hash of the previous block
- `data` - Block content (user data)
- `hash` - SHA-256 hash of the block
- `signature` - Digital signature of the block
- `signer_public_key` - Public key of the block creator
- `timestamp` - When the block was created

### authorized_keys table
- `id` - Unique identifier
- `public_key` - User's public key (unique)
- `owner_name` - Human-readable name
- `is_active` - Whether the key is currently active
- `created_at` - When the key was added

## 📝 API Reference

### Core Methods

#### Blockchain Management
```java
// Basic information
long totalBlocks = blockchain.getBlockCount();
List<Block> allBlocks = blockchain.getAllBlocks();
Block lastBlock = blockchain.getLastBlock();
Block specificBlock = blockchain.getBlock(blockNumber);

// Configuration
int maxBytes = blockchain.getMaxBlockSizeBytes();
int maxChars = blockchain.getMaxBlockDataLength();
```

#### Key Management
```java
// Add/remove authorized keys
boolean added = blockchain.addAuthorizedKey(publicKeyString, "User Name");
boolean revoked = blockchain.revokeAuthorizedKey(publicKeyString);

// List authorized keys
List<AuthorizedKey> activeKeys = blockchain.getAuthorizedKeys();
```

#### Block Operations
```java
// Add block
boolean success = blockchain.addBlock(data, privateKey, publicKey);

// Validate
boolean isValid = blockchain.validateChain();

// Advanced operations
boolean exported = blockchain.exportChain("backup.json");
boolean imported = blockchain.importChain("backup.json");
boolean rolledBack = blockchain.rollbackBlocks(numberOfBlocks);
```

#### Search Operations
```java
// Search methods
List<Block> contentResults = blockchain.searchBlocksByContent("searchTerm");
Block hashResult = blockchain.getBlockByHash("hashString");
List<Block> dateResults = blockchain.getBlocksByDateRange(startDate, endDate);
```

## 🔧 Configuration

### Database Configuration
- **Location**: `blockchain.db` in project root directory
- **Type**: SQLite database
- **ORM**: Hibernate with automatic table creation
- **Logging**: SQL queries logged (can be disabled in hibernate.cfg.xml)

### Size Limits
- **Block Data**: 10,000 characters maximum
- **Block Size**: 1MB (1,048,576 bytes) maximum
- **Hash Length**: 64 characters (SHA-256)

### Security Configuration
- **Hash Algorithm**: SHA-256
- **Signature Algorithm**: RSA
- **Key Size**: 2048 bits (default)

## 🚨 Important Notes

### Production Considerations
- **Key Management**: Store private keys securely
- **Database Security**: Consider encryption for sensitive data
- **Backup Strategy**: Regular database backups recommended
- **Access Control**: Implement proper user authentication

### Current Limitations
- **Single Database**: Uses one SQLite file
- **No Network**: Designed for single-application use
- **No Consensus**: No multi-node consensus mechanism
- **Key Recovery**: No built-in key recovery system

### Performance Notes
- **Block Size**: Large blocks may affect performance
- **Search Operations**: Content search is case-insensitive but may be slow with many blocks
- **Rollback Operations**: Large rollbacks may take time
- **Database Size**: Consider regular maintenance for large blockchains

## 🤝 Contributing

### Development Setup
1. Ensure Java 21+ and Maven 3.6+ are installed
2. Clone the repository
3. Run `mvn clean compile` to build
4. Run `./run_all_tests.sh` to verify everything works

### Testing New Features
1. Add your feature to the appropriate class
2. Create tests following the existing patterns
3. Run all tests to ensure nothing is broken
4. Update documentation as needed

### Code Style
- Use clear, descriptive variable names
- Add comments for complex logic
- Follow existing naming conventions
- Ensure proper error handling

## 📄 License

This project is provided as-is for educational and development purposes.

## 📞 Support

For issues or questions:
1. Check the troubleshooting section above
2. Verify your Java and Maven versions
3. Run `./run_all_tests.sh` to identify problems
4. Check the console output for specific error messages

---

**Ready to start?** Run `./run_all_tests.sh` to verify everything works, then try the demos!
