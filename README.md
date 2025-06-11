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
- **Chain Export/Import**: Backup and restore complete blockchain with temporal consistency
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

// Add authorized key with specific timestamp (for CLI operations)
boolean added = blockchain.addAuthorizedKey(publicKeyString, "User Name", specificTimestamp);

// List authorized keys
List<AuthorizedKey> activeKeys = blockchain.getAuthorizedKeys();

// Get all authorized keys (including revoked ones) for export functionality
List<AuthorizedKey> allKeys = blockchain.getAllAuthorizedKeys();
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
 readPerformance) { this.readPerformance = readPerformance; }
        public long getDatabaseSize() { return databaseSize; }
        public void setDatabaseSize(long databaseSize) { this.databaseSize = databaseSize; }
        public long getAverageBlockSize() { return averageBlockSize; }
        public void setAverageBlockSize(long averageBlockSize) { this.averageBlockSize = averageBlockSize; }
        public String getOverallHealth() { return overallHealth; }
        public void setOverallHealth(String overallHealth) { this.overallHealth = overallHealth; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
```

### Key Management Best Practices
```java
public class KeyManagementPatterns {
    
    // Secure key generation with validation and timestamp control
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
            
            // Add to blockchain with validation (using current timestamp)
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
    
    // Add authorized key with specific timestamp for CLI or import operations
    public boolean addKeyWithSpecificTimestamp(Blockchain blockchain, String publicKeyString, 
                                             String ownerName, java.time.LocalDateTime specificTime) {
        try {
            // Add authorized key with specific creation time for import/CLI operations
            boolean success = blockchain.addAuthorizedKey(publicKeyString, ownerName, specificTime);
            
            if (success) {
                System.out.println("✅ Key authorized with timestamp: " + specificTime);
                System.out.println("🔑 Owner: " + ownerName);
                System.out.println("📅 Timestamp control ensures temporal consistency");
            } else {
                System.err.println("❌ Failed to authorize key with timestamp");
            }
            
            return success;
            
        } catch (Exception e) {
            System.err.println("💥 Timestamp key authorization error: " + e.getMessage());
            return false;
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
            
            // Allow overlap period for transition (in production, this might be hours/days)
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
    
    // Bulk key management for organization setup
    public OrganizationKeyResult setupOrganizationKeys(Blockchain blockchain, 
                                                       java.util.Map<String, String> roleAssignments) {
        OrganizationKeyResult result = new OrganizationKeyResult();
        
        try {
            System.out.println("🏢 Setting up organization keys for " + roleAssignments.size() + " roles");
            
            for (java.util.Map.Entry<String, String> assignment : roleAssignments.entrySet()) {
                String person = assignment.getKey();
                String role = assignment.getValue();
                
                // Generate keys for each person/role
                AuthorizedKeyInfo keyInfo = generateAndAuthorizeSecureKey(blockchain, person, 
                                                                        "Organization", role);
                if (keyInfo != null) {
                    result.addSuccessfulKey(person, keyInfo);
                    System.out.println("  ✅ " + person + " (" + role + ")");
                } else {
                    result.addFailedKey(person, "Key generation failed");
                    System.err.println("  ❌ " + person + " (" + role + ") - FAILED");
                }
            }
            
            System.out.println("📊 Organization setup summary:");
            System.out.println("  ✅ Successful: " + result.getSuccessfulKeys().size());
            System.out.println("  ❌ Failed: " + result.getFailedKeys().size());
            
        } catch (Exception e) {
            System.err.println("💥 Organization setup error: " + e.getMessage());
            result.setError(e.getMessage());
        }
        
        return result;
    }
    
    // Supporting classes
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
    
    public static class OrganizationKeyResult {
        private java.util.Map<String, AuthorizedKeyInfo> successfulKeys = new java.util.HashMap<>();
        private java.util.Map<String, String> failedKeys = new java.util.HashMap<>();
        private String error;
        
        public void addSuccessfulKey(String person, AuthorizedKeyInfo keyInfo) {
            successfulKeys.put(person, keyInfo);
        }
        
        public void addFailedKey(String person, String reason) {
            failedKeys.put(person, reason);
        }
        
        public java.util.Map<String, AuthorizedKeyInfo> getSuccessfulKeys() { return successfulKeys; }
        public java.util.Map<String, String> getFailedKeys() { return failedKeys; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
```

## 🔒 Security Model

### Block Security
- **Cryptographic Hashing**: Each block contains a SHA-256 hash of its content
- **Chain Linking**: Blocks are cryptographically linked by including the previous block's hash
- **Tamper Detection**: Any modification breaks the chain validation and is immediately detected
- **Temporal Consistency**: Historical validation ensures blocks were signed by authorized keys at the time of creation

### Access Control
- **Authorization Management**: Only users with authorized public keys can add blocks
- **Digital Signatures**: Each block is digitally signed with the user's private key using RSA
- **Signature Verification**: All signatures are verified before accepting blocks into the chain
- **Key Lifecycle**: Full tracking of key authorization, usage, and revocation history
- **Temporal Consistency**: Enhanced timestamp control ensures proper key lifecycle management during export/import operations

### Data Integrity
- **Complete Chain Validation**: All blocks are validated when checking the chain integrity
- **Hash Verification**: SHA-256 hash verification ensures no data has been modified
- **Sequential Validation**: Confirms proper block order and linking
- **Critical Consistency**: Advanced tests verify complex scenarios like concurrent operations and edge cases

### Advanced Security Features
- **Export/Import Security**: Complete blockchain state preservation including revoked keys for historical validation
- **Temporal Validation**: Verify that blocks were signed by keys that were authorized at the time of block creation
- **Rollback Protection**: Safe block removal with consistency checks and key state preservation
- **Concurrency Safety**: Thread-safe operations prevent race conditions and data corruption

## 📊 Database Schema

The application automatically creates these tables with full audit capabilities:

### blocks table
- `id` - Unique identifier (Primary Key)
- `block_number` - Sequential block number (starts from 0, indexed)
- `previous_hash` - Hash of the previous block (64 chars, indexed)
- `data` - Block content (user data, up to 10,000 characters)
- `hash` - SHA-256 hash of the block (64 chars, unique)
- `signature` - Digital signature of the block (Base64 encoded)
- `signer_public_key` - Public key of the block creator (indexed)
- `timestamp` - When the block was created (indexed for date range queries)

### authorized_keys table
- `id` - Unique identifier (Primary Key)
- `public_key` - User's public key (unique, indexed)
- `owner_name` - Human-readable name and role information
- `is_active` - Whether the key is currently active (indexed)
- `created_at` - When the key was authorized (indexed)
- `revoked_at` - When the key was revoked (nullable, indexed)

### Performance Indexes
- `idx_blocks_number` - Fast block number lookups
- `idx_blocks_hash` - Fast hash-based searches
- `idx_blocks_signer` - Fast searches by signer
- `idx_blocks_timestamp` - Fast date range queries
- `idx_keys_public_key` - Fast key authorization checks
- `idx_keys_active` - Fast active key listings

## 📝 Complete API Reference

### Core Blockchain Management
```java
// Initialization
Blockchain blockchain = new Blockchain();  // Auto-creates genesis block

// Basic information
long totalBlocks = blockchain.getBlockCount();
List<Block> allBlocks = blockchain.getAllBlocks();
Block lastBlock = blockchain.getLastBlock();
Block specificBlock = blockchain.getBlock(blockNumber);
boolean isValid = blockchain.validateChain();

// Configuration limits
int maxBytes = blockchain.getMaxBlockSizeBytes();    // 1,048,576 bytes (1MB)
int maxChars = blockchain.getMaxBlockDataLength();   // 10,000 characters
```

### Key Management Operations
```java
// Add/remove authorized keys
boolean added = blockchain.addAuthorizedKey(publicKeyString, "User Name");
boolean revoked = blockchain.revokeAuthorizedKey(publicKeyString);

// Query key information
List<AuthorizedKey> activeKeys = blockchain.getAuthorizedKeys();
boolean isAuthorized = blockchain.isKeyAuthorized(publicKeyString);

// Historical key validation (for audit purposes)
boolean wasAuthorizedAt = blockchain.wasKeyAuthorizedAt(publicKeyString, timestamp);
```

### Block Operations
```java
// Add blocks with full validation
boolean success = blockchain.addBlock(data, privateKey, publicKey);

// Validation operations
boolean isValid = blockchain.validateChain();
boolean blockValid = blockchain.validateBlock(block, previousBlock);

// Advanced block operations
boolean exported = blockchain.exportChain("backup.json");
boolean imported = blockchain.importChain("backup.json");
boolean rolledBack = blockchain.rollbackBlocks(numberOfBlocks);
boolean rolledBackTo = blockchain.rollbackToBlock(targetBlockNumber);
```

### Search and Query Operations
```java
// Content-based search (case-insensitive)
List<Block> contentResults = blockchain.searchBlocksByContent("searchTerm");

// Hash-based lookup
Block hashResult = blockchain.getBlockByHash("hashString");

// Date range queries
LocalDate startDate = LocalDate.of(2025, 1, 1);
LocalDate endDate = LocalDate.of(2025, 12, 31);
List<Block> dateResults = blockchain.getBlocksByDateRange(startDate, endDate);

// Advanced filtering (example implementation)
List<Block> filteredBlocks = blockchain.getAllBlocks().stream()
    .filter(block -> block.getData().contains("keyword"))
    .filter(block -> block.getTimestamp().isAfter(someDateTime))
    .collect(Collectors.toList());
```

### Utility and Helper Methods
```java
// Cryptographic utilities
KeyPair keyPair = CryptoUtil.generateKeyPair();
String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
PublicKey publicKey = CryptoUtil.stringToPublicKey(publicKeyString);
String hash = CryptoUtil.calculateHash(data);
boolean verified = CryptoUtil.verifySignature(data, signature, publicKey);

// Block analysis
String blockContent = blockchain.buildBlockContent(block);  // For custom validation
long chainSize = blockchain.getBlockCount();
Block genesisBlock = blockchain.getBlock(0);
```

## 🔧 Configuration

### Database Configuration
- **Location**: `blockchain.db` in project root directory
- **Type**: SQLite database with WAL mode for better concurrency
- **ORM**: Hibernate with automatic table creation and indexing
- **Connection Pooling**: HikariCP with optimized settings
- **Logging**: Configurable SQL query logging (hibernate.cfg.xml)

### Security Configuration
- **Hash Algorithm**: SHA-256 (256-bit)
- **Signature Algorithm**: RSA with SHA-256
- **Key Size**: 2048 bits (configurable to 4096 for high-security environments)
- **Encoding**: Base64 for signatures, UTF-8 for text data

### Performance Limits
- **Block Data**: 10,000 characters maximum per block
- **Block Size**: 1MB (1,048,576 bytes) total block size limit
- **Hash Length**: 64 characters (SHA-256 hex representation)
- **Signature Length**: Variable (typically ~344 characters for 2048-bit RSA)

### Advanced Configuration Options
```xml
<!-- hibernate.cfg.xml customization -->
<property name="hibernate.show_sql">false</property>          <!-- Disable SQL logging -->
<property name="hibernate.hbm2ddl.auto">update</property>     <!-- Auto-update schema -->
<property name="hibernate.connection.pool_size">10</property>  <!-- Connection pool size -->
```

## 🚨 Important Production Considerations

### Security Best Practices
- **Private Key Management**: Store private keys in secure hardware (HSMs) or encrypted storage
- **Database Encryption**: Consider transparent database encryption for sensitive data
- **Access Control**: Implement application-level authentication and authorization
- **Network Security**: Use TLS/SSL for any network communications
- **Audit Logging**: Log all blockchain operations for security monitoring

### Backup and Recovery
- **Regular Backups**: Implement automated daily/hourly backup schedules
- **Backup Validation**: Always verify backup integrity before relying on them
- **Offsite Storage**: Store backups in geographically separate locations
- **Recovery Testing**: Regularly test backup restoration procedures
- **Version Control**: Keep multiple backup versions for point-in-time recovery

### Performance Optimization
- **Database Maintenance**: Regular VACUUM and ANALYZE operations for SQLite
- **Index Optimization**: Monitor query performance and add indexes as needed
- **Connection Pooling**: Configure appropriate connection pool sizes
- **Memory Management**: Monitor Java heap usage for large blockchains
- **Concurrent Access**: Consider read replicas for high-read scenarios

### Monitoring and Alerting
- **Chain Validation**: Regular automated chain integrity checks
- **Performance Metrics**: Monitor block addition times and query performance
- **Storage Growth**: Track database size growth and plan capacity
- **Error Monitoring**: Implement comprehensive error logging and alerting
- **Health Checks**: Regular system health assessments

### Current Limitations and Considerations
- **Single Node**: Designed for single-application use (not distributed)
- **No Consensus**: No multi-node consensus mechanism (by design for private blockchain)
- **Storage Growth**: Database grows continuously (implement archiving for very large chains)
- **Key Recovery**: No built-in key recovery system (implement according to organizational policies)
- **Scalability**: Consider partitioning strategies for extremely large blockchains (millions of blocks)

## 🤝 Contributing

### Development Setup
1. **Environment**: Ensure Java 21+ and Maven 3.6+ are installed
2. **Clone**: Clone the repository to your local development environment
3. **Build**: Run `mvn clean compile` to build the project
4. **Test**: Run `./run_all_tests.sh` to verify everything works (41+ tests)
5. **IDE**: Import as Maven project in your preferred IDE

### Testing New Features
1. **Feature Development**: Add your feature to the appropriate class following existing patterns
2. **Unit Tests**: Create comprehensive JUnit 5 tests following existing test structures
3. **Integration Tests**: Ensure your feature works with existing functionality
4. **Consistency Tests**: Add critical consistency tests for complex scenarios
5. **Documentation**: Update README.md and add code comments
6. **Full Test Suite**: Run `./run_all_tests.sh` to ensure nothing is broken

### Code Quality Standards
- **Clear Naming**: Use descriptive variable and method names
- **Comments**: Add comprehensive comments for complex logic
- **Error Handling**: Implement proper exception handling and logging
- **Consistency**: Follow existing naming conventions and code style
- **Performance**: Consider performance implications of new features
- **Security**: Ensure new features maintain security properties

### Contribution Areas
- **Performance Optimization**: Database query optimization, caching strategies
- **Security Enhancements**: Additional cryptographic features, audit capabilities
- **Monitoring**: Health check improvements, metrics collection
- **Integration**: APIs for external systems, import/export formats
- **Documentation**: Examples, tutorials, best practices guides

## 📄 License

This project is provided as-is for educational and development purposes. Feel free to use, modify, and distribute according to your organization's requirements.

## 📞 Support and Troubleshooting

### Common Issues and Solutions

#### Build Issues
```bash
# Java version mismatch
java -version  # Should show 21+
mvn -version   # Verify Maven can see Java 21

# Clean build
mvn clean compile test-compile
```

#### Test Failures
```bash
# Reset environment
rm blockchain.db blockchain.db-*
./run_all_tests.sh

# Check specific test
mvn test -Dtest=CriticalConsistencyTest
```

#### Database Issues
```bash
# Check database permissions
ls -la blockchain.db*

# Reset database
rm blockchain.db blockchain.db-shm blockchain.db-wal
```

#### Performance Issues
```bash
# Monitor database size
du -h blockchain.db

# Check Java memory usage
jstat -gc [java_process_id]
```

### Getting Help
1. **Check this documentation** for common use cases and examples
2. **Review the test files** for comprehensive usage examples
3. **Run the health check** using the workflow patterns above
4. **Examine the console output** for specific error messages
5. **Verify your environment** meets the prerequisites

### Verification Steps
```bash
# Complete verification procedure
cd /path/to/privateBlockchain
mvn clean compile test-compile
./run_all_tests.sh
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"
```

---

**🚀 Ready to start?** 

1. Run `./run_all_tests.sh` to verify everything works perfectly
2. Try the practical examples above for your use case
3. Explore the comprehensive test suite to understand all features
4. Build your own blockchain application using the patterns provided!

**💡 Remember**: This blockchain includes **41+ comprehensive tests** covering everything from basic operations to critical consistency scenarios, ensuring enterprise-grade reliability for your applications.