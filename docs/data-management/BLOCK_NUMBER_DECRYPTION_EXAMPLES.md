# Block Number-Based Decryption - Practical Examples

## Quick Start Examples

### Example 1: Simple Secret Storage and Retrieval

```java
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import java.security.KeyPair;

public class SimpleSecretExample {
    public static void main(String[] args) throws Exception {
        // Initialize blockchain and API
        Blockchain blockchain = new Blockchain();
        KeyPair userKeys = generateUserKeyPair(); // Your key generation logic
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(
            blockchain, 
            "alice", 
            userKeys
        );

        // Store a secret
        String sensitiveData = "My secret cryptocurrency private key: 0x123abc...";
        String password = "strongPassword123!";
        
        Block secretBlock = api.storeSecret(sensitiveData, password);
        System.out.println("Secret stored in block #" + secretBlock.getBlockNumber());
        
        // Later retrieve the secret
        Long blockNumber = secretBlock.getBlockNumber();
        String retrievedSecret = api.retrieveSecret(blockNumber, password);
        
        if (retrievedSecret != null) {
            System.out.println("Retrieved secret: " + retrievedSecret);
        } else {
            System.out.println("Failed to decrypt - wrong password or block not found");
        }
    }
}
```

### Example 2: Medical Records Management

```java
import java.util.List;
import java.util.ArrayList;

public class MedicalRecordsExample {
    private final UserFriendlyEncryptionAPI api;
    private final String MEDICAL_PASSWORD = "MedicalSecure2025!";
    
    public MedicalRecordsExample(Blockchain blockchain, KeyPair doctorKeys) {
        this.api = new UserFriendlyEncryptionAPI(blockchain, "Dr.Smith", doctorKeys);
    }
    
    // Store patient record with searchable identifier
    public Block storePatientRecord(String patientId, String diagnosis, String treatment) {
        String medicalRecord = String.format(
            "Patient ID: %s\nDiagnosis: %s\nTreatment: %s\nDate: %s",
            patientId, diagnosis, treatment, LocalDateTime.now()
        );
        
        // Store with searchable patient identifier
        Block medicalBlock = api.storeDataWithIdentifier(
            medicalRecord,
            MEDICAL_PASSWORD,
            "patient:" + patientId
        );
        
        System.out.println("Medical record for patient " + patientId + 
                          " stored in block #" + medicalBlock.getBlockNumber());
        return medicalBlock;
    }
    
    // Find and decrypt all records for a specific patient
    public List<String> getPatientRecords(String patientId) {
        List<String> patientRecords = new ArrayList<>();
        
        // Find all blocks for this patient
        List<Block> patientBlocks = api.findRecordsByIdentifier("patient:" + patientId);
        
        for (Block block : patientBlocks) {
            String record = api.retrieveSecret(block.getBlockNumber(), MEDICAL_PASSWORD);
            if (record != null) {
                patientRecords.add(record);
                System.out.println("Retrieved record from block #" + block.getBlockNumber());
            }
        }
        
        return patientRecords;
    }
    
    // Search for records by medical condition
    public List<String> findRecordsByCondition(String condition) {
        List<String> matchingRecords = new ArrayList<>();
        
        // Search for encrypted data containing the condition
        List<Block> results = api.findAndDecryptData(condition, MEDICAL_PASSWORD);
        
        for (Block block : results) {
            // Block data is already decrypted by findAndDecryptData
            String record = block.getData();
            if (record != null && record.toLowerCase().contains(condition.toLowerCase())) {
                matchingRecords.add(record);
                System.out.println("Found " + condition + " in block #" + block.getBlockNumber());
            }
        }
        
        return matchingRecords;
    }
    
    // Usage example
    public static void main(String[] args) throws Exception {
        Blockchain blockchain = new Blockchain();
        KeyPair doctorKeys = generateDoctorKeys();
        MedicalRecordsExample medical = new MedicalRecordsExample(blockchain, doctorKeys);
        
        // Store multiple patient records
        medical.storePatientRecord("P-001", "Type 2 Diabetes", "Metformin 500mg daily");
        medical.storePatientRecord("P-001", "Hypertension", "Lisinopril 10mg daily");
        medical.storePatientRecord("P-002", "Diabetes Type 1", "Insulin therapy");
        
        // Retrieve all records for patient P-001
        List<String> patient001Records = medical.getPatientRecords("P-001");
        System.out.println("Patient P-001 has " + patient001Records.size() + " records");
        
        // Find all diabetes-related records
        List<String> diabetesRecords = medical.findRecordsByCondition("diabetes");
        System.out.println("Found " + diabetesRecords.size() + " diabetes-related records");
    }
}
```

### Example 3: Financial Transaction Processing

```java
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FinancialTransactionsExample {
    private final UserFriendlyEncryptionAPI api;
    private final String FINANCE_PASSWORD = "FinancialVault2025!";
    
    public FinancialTransactionsExample(Blockchain blockchain, KeyPair financeKeys) {
        this.api = new UserFriendlyEncryptionAPI(blockchain, "FinanceOfficer", financeKeys);
    }
    
    // Store encrypted transaction
    public Block recordTransaction(String fromAccount, String toAccount, 
                                 BigDecimal amount, String reference) {
        String transactionData = String.format(
            "TRANSACTION RECORD\n" +
            "From: %s\n" +
            "To: %s\n" +
            "Amount: $%s\n" +
            "Reference: %s\n" +
            "Timestamp: %s\n" +
            "Status: Completed",
            fromAccount, toAccount, amount, reference, LocalDateTime.now()
        );
        
        // Store with transaction reference as identifier
        Block txBlock = api.storeDataWithIdentifier(
            transactionData,
            FINANCE_PASSWORD,
            "tx:" + reference
        );
        
        System.out.println("Transaction " + reference + 
                          " recorded in block #" + txBlock.getBlockNumber());
        return txBlock;
    }
    
    // Retrieve specific transaction by reference
    public String getTransaction(String reference) {
        List<Block> txBlocks = api.findRecordsByIdentifier("tx:" + reference);
        
        for (Block block : txBlocks) {
            String txData = api.retrieveSecret(block.getBlockNumber(), FINANCE_PASSWORD);
            if (txData != null) {
                System.out.println("Retrieved transaction " + reference + 
                                  " from block #" + block.getBlockNumber());
                return txData;
            }
        }
        
        System.out.println("Transaction " + reference + " not found or could not decrypt");
        return null;
    }
    
    // Find all transactions for an account
    public List<String> getAccountTransactions(String accountNumber) {
        List<String> accountTxs = new ArrayList<>();
        
        // Search for the account number in encrypted transaction data
        List<Block> results = api.findAndDecryptData(accountNumber, FINANCE_PASSWORD);
        
        for (Block block : results) {
            String txData = block.getData();
            if (txData != null && txData.contains(accountNumber)) {
                accountTxs.add(txData);
                System.out.println("Found transaction for account " + accountNumber + 
                                  " in block #" + block.getBlockNumber());
            }
        }
        
        return accountTxs;
    }
    
    // Audit trail - verify transaction integrity
    public boolean verifyTransaction(String reference) {
        String txData = getTransaction(reference);
        if (txData == null) {
            return false;
        }
        
        // Additional verification logic here
        // Check digital signatures, amounts, etc.
        
        System.out.println("Transaction " + reference + " verified successfully");
        return true;
    }
    
    // Usage example
    public static void main(String[] args) throws Exception {
        Blockchain blockchain = new Blockchain();
        KeyPair financeKeys = generateFinanceKeys();
        FinancialTransactionsExample finance = new FinancialTransactionsExample(blockchain, financeKeys);
        
        // Record several transactions
        finance.recordTransaction("ACC-001", "ACC-002", new BigDecimal("1500.00"), "TXN-2025-001");
        finance.recordTransaction("ACC-003", "ACC-001", new BigDecimal("2300.50"), "TXN-2025-002");
        finance.recordTransaction("ACC-001", "ACC-004", new BigDecimal("750.25"), "TXN-2025-003");
        
        // Retrieve specific transaction
        String tx = finance.getTransaction("TXN-2025-001");
        System.out.println("Transaction details:\n" + tx);
        
        // Find all transactions for account ACC-001
        List<String> acc001Txs = finance.getAccountTransactions("ACC-001");
        System.out.println("Account ACC-001 has " + acc001Txs.size() + " transactions");
        
        // Verify transaction integrity
        boolean isValid = finance.verifyTransaction("TXN-2025-002");
        System.out.println("Transaction verification: " + (isValid ? "PASSED" : "FAILED"));
    }
}
```

### Example 4: Legal Document Management

```java
public class LegalDocumentsExample {
    private final UserFriendlyEncryptionAPI api;
    private final String LEGAL_PASSWORD = "LegalVault2025!";
    
    public LegalDocumentsExample(Blockchain blockchain, KeyPair legalKeys) {
        this.api = new UserFriendlyEncryptionAPI(blockchain, "LegalCounsel", legalKeys);
    }
    
    // Store confidential legal document
    public Block storeDocument(String caseNumber, String documentType, 
                             String content, String clientName) {
        String documentRecord = String.format(
            "LEGAL DOCUMENT\n" +
            "Case: %s\n" +
            "Type: %s\n" +
            "Client: %s\n" +
            "Date: %s\n" +
            "Content:\n%s",
            caseNumber, documentType, clientName, LocalDateTime.now(), content
        );
        
        // Store with case number as identifier
        Block docBlock = api.storeDataWithIdentifier(
            documentRecord,
            LEGAL_PASSWORD,
            "case:" + caseNumber
        );
        
        System.out.println("Legal document for case " + caseNumber + 
                          " stored in block #" + docBlock.getBlockNumber());
        return docBlock;
    }
    
    // Retrieve all documents for a case
    public List<String> getCaseDocuments(String caseNumber) {
        List<String> caseDocuments = new ArrayList<>();
        
        List<Block> caseBlocks = api.findRecordsByIdentifier("case:" + caseNumber);
        
        for (Block block : caseBlocks) {
            String document = api.retrieveSecret(block.getBlockNumber(), LEGAL_PASSWORD);
            if (document != null) {
                caseDocuments.add(document);
                System.out.println("Retrieved document from block #" + block.getBlockNumber());
            }
        }
        
        return caseDocuments;
    }
    
    // Search for documents by client name
    public List<String> getClientDocuments(String clientName) {
        List<String> clientDocs = new ArrayList<>();
        
        // Search encrypted content for client name
        List<Block> results = api.findAndDecryptData(clientName, LEGAL_PASSWORD);
        
        for (Block block : results) {
            String document = block.getData();
            if (document != null && document.contains(clientName)) {
                clientDocs.add(document);
                System.out.println("Found document for client " + clientName + 
                                  " in block #" + block.getBlockNumber());
            }
        }
        
        return clientDocs;
    }
    
    // Usage example
    public static void main(String[] args) throws Exception {
        Blockchain blockchain = new Blockchain();
        KeyPair legalKeys = generateLegalKeys();
        LegalDocumentsExample legal = new LegalDocumentsExample(blockchain, legalKeys);
        
        // Store legal documents
        legal.storeDocument("CASE-2025-001", "Contract", 
            "Service Agreement between CompanyA and CompanyB...", "CompanyA");
        legal.storeDocument("CASE-2025-001", "Amendment", 
            "Amendment #1 to Service Agreement...", "CompanyA");
        legal.storeDocument("CASE-2025-002", "Will", 
            "Last Will and Testament of John Doe...", "John Doe");
        
        // Retrieve case documents
        List<String> case001Docs = legal.getCaseDocuments("CASE-2025-001");
        System.out.println("Case CASE-2025-001 has " + case001Docs.size() + " documents");
        
        // Find client documents
        List<String> companyADocs = legal.getClientDocuments("CompanyA");
        System.out.println("Found " + companyADocs.size() + " documents for CompanyA");
    }
}
```

## Error Handling Best Practices

### Example 5: Robust Error Handling

```java
public class RobustDecryptionExample {
    private final UserFriendlyEncryptionAPI api;
    
    public RobustDecryptionExample(UserFriendlyEncryptionAPI api) {
        this.api = api;
    }
    
    // Safe decryption with comprehensive error handling
    public DecryptionResult safeDecrypt(Long blockNumber, String password) {
        try {
            // Validate inputs
            if (blockNumber == null) {
                return new DecryptionResult(false, "Block number cannot be null", null);
            }
            
            if (password == null || password.trim().isEmpty()) {
                return new DecryptionResult(false, "Password cannot be null or empty", null);
            }
            
            // Check if block exists and is encrypted
            if (!api.isBlockEncrypted(blockNumber)) {
                return new DecryptionResult(false, "Block is not encrypted or does not exist", null);
            }
            
            // Attempt decryption
            String decryptedData = api.retrieveSecret(blockNumber, password);
            
            if (decryptedData != null) {
                return new DecryptionResult(true, "Decryption successful", decryptedData);
            } else {
                return new DecryptionResult(false, "Decryption failed - wrong password or corrupted data", null);
            }
            
        } catch (IllegalArgumentException e) {
            return new DecryptionResult(false, "Invalid argument: " + e.getMessage(), null);
        } catch (Exception e) {
            return new DecryptionResult(false, "Unexpected error: " + e.getMessage(), null);
        }
    }
    
    // Batch decryption with error tracking
    public List<DecryptionResult> batchDecrypt(List<Long> blockNumbers, String password) {
        List<DecryptionResult> results = new ArrayList<>();
        
        for (Long blockNumber : blockNumbers) {
            DecryptionResult result = safeDecrypt(blockNumber, password);
            results.add(result);
            
            if (result.isSuccess()) {
                System.out.println("✅ Block #" + blockNumber + " decrypted successfully");
            } else {
                System.err.println("❌ Block #" + blockNumber + " failed: " + result.getErrorMessage());
            }
        }
        
        return results;
    }
    
    // Result class for safe operations
    public static class DecryptionResult {
        private final boolean success;
        private final String errorMessage;
        private final String data;
        
        public DecryptionResult(boolean success, String errorMessage, String data) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.data = data;
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public String getData() { return data; }
    }
    
    // Usage example with error handling
    public static void main(String[] args) throws Exception {
        Blockchain blockchain = new Blockchain();
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, "user", generateKeys());
        RobustDecryptionExample robust = new RobustDecryptionExample(api);
        
        // Store some test data
        Block block1 = api.storeSecret("Test data 1", "password123");
        Block block2 = api.storeSecret("Test data 2", "password456");
        
        List<Long> blocksToDecrypt = Arrays.asList(
            block1.getBlockNumber(),
            block2.getBlockNumber(),
            999L  // Non-existent block
        );
        
        // Batch decrypt with different passwords
        List<DecryptionResult> results1 = robust.batchDecrypt(blocksToDecrypt, "password123");
        List<DecryptionResult> results2 = robust.batchDecrypt(blocksToDecrypt, "wrongPassword");
        
        // Process results
        long successCount = results1.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        System.out.println("Successfully decrypted " + successCount + " out of " + blocksToDecrypt.size() + " blocks");
    }
}
```

## Performance Optimization Examples

### Example 6: Batch Operations for Better Performance

```java
public class OptimizedDecryptionExample {
    private final UserFriendlyEncryptionAPI api;
    
    public OptimizedDecryptionExample(UserFriendlyEncryptionAPI api) {
        this.api = api;
    }
    
    // Optimized search and decrypt for large datasets
    public Map<Long, String> searchAndDecryptOptimized(String searchTerm, String password, int maxResults) {
        Map<Long, String> results = new HashMap<>();
        
        // Use the optimized findEncryptedData method (uses batch operations internally)
        List<Block> encryptedBlocks = api.findEncryptedData(searchTerm);
        
        // Limit results to avoid memory issues
        List<Block> limitedBlocks = encryptedBlocks.stream()
            .limit(maxResults)
            .collect(Collectors.toList());
        
        System.out.println("Processing " + limitedBlocks.size() + " encrypted blocks...");
        
        // Decrypt in batches to manage memory
        int batchSize = 10;
        for (int i = 0; i < limitedBlocks.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, limitedBlocks.size());
            List<Block> batch = limitedBlocks.subList(i, endIndex);
            
            System.out.println("Processing batch " + (i/batchSize + 1) + 
                              " (" + batch.size() + " blocks)");
            
            for (Block block : batch) {
                String decryptedData = api.retrieveSecret(block.getBlockNumber(), password);
                if (decryptedData != null) {
                    results.put(block.getBlockNumber(), decryptedData);
                }
            }
            
            // Small delay between batches to avoid overwhelming the system
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("Successfully decrypted " + results.size() + " blocks");
        return results;
    }
    
    // Cache frequently accessed blocks
    private final Map<Long, String> decryptionCache = new ConcurrentHashMap<>();
    
    public String getCachedDecryption(Long blockNumber, String password) {
        String cacheKey = blockNumber + ":" + password.hashCode();
        
        // Check cache first
        if (decryptionCache.containsKey(blockNumber)) {
            System.out.println("Cache hit for block #" + blockNumber);
            return decryptionCache.get(blockNumber);
        }
        
        // Decrypt and cache
        String decryptedData = api.retrieveSecret(blockNumber, password);
        if (decryptedData != null) {
            decryptionCache.put(blockNumber, decryptedData);
            System.out.println("Cached decryption for block #" + blockNumber);
        }
        
        return decryptedData;
    }
    
    // Usage example
    public static void main(String[] args) throws Exception {
        Blockchain blockchain = new Blockchain();
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, "user", generateKeys());
        OptimizedDecryptionExample optimized = new OptimizedDecryptionExample(api);
        
        // Create test data
        String password = "testPassword123";
        for (int i = 0; i < 100; i++) {
            api.storeSecret("Test data " + i + " medical research", password);
        }
        
        // Optimized search and decrypt
        long startTime = System.currentTimeMillis();
        Map<Long, String> results = optimized.searchAndDecryptOptimized("medical", password, 50);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Found and decrypted " + results.size() + " blocks in " + 
                          (endTime - startTime) + "ms");
        
        // Test caching performance
        if (!results.isEmpty()) {
            Long firstBlock = results.keySet().iterator().next();
            
            // First access (cache miss)
            startTime = System.nanoTime();
            optimized.getCachedDecryption(firstBlock, password);
            long firstAccess = System.nanoTime() - startTime;
            
            // Second access (cache hit)
            startTime = System.nanoTime();
            optimized.getCachedDecryption(firstBlock, password);
            long secondAccess = System.nanoTime() - startTime;
            
            System.out.println("Cache performance: " + 
                              "First access: " + (firstAccess / 1_000_000) + "ms, " +
                              "Second access: " + (secondAccess / 1_000_000) + "ms");
        }
    }
}
```

## Integration Testing Examples

### Example 7: Complete Integration Test

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class BlockNumberDecryptionIntegrationTest {
    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private KeyPair testKeys;
    
    @BeforeEach
    public void setUp() throws Exception {
        blockchain = new Blockchain();
        testKeys = generateTestKeyPair();
        api = new UserFriendlyEncryptionAPI(blockchain, "testUser", testKeys);
    }
    
    @Test
    public void testCompleteWorkflow() {
        String originalData = "Test medical data for patient P-001";
        String password = "testPassword123";
        
        // Store encrypted data
        Block storedBlock = api.storeSecret(originalData, password);
        assertNotNull(storedBlock);
        assertNotNull(storedBlock.getBlockNumber());
        
        // Verify block is encrypted
        assertTrue(api.isBlockEncrypted(storedBlock.getBlockNumber()));
        
        // Retrieve and decrypt data using block number
        String retrievedData = api.retrieveSecret(storedBlock.getBlockNumber(), password);
        assertNotNull(retrievedData);
        assertEquals(originalData, retrievedData);
        
        // Test wrong password
        String wrongPasswordResult = api.retrieveSecret(storedBlock.getBlockNumber(), "wrongPassword");
        assertNull(wrongPasswordResult);
        
        // Test non-existent block
        String nonExistentResult = api.retrieveSecret(99999L, password);
        assertNull(nonExistentResult);
    }
    
    @Test
    public void testSearchAndDecrypt() {
        String password = "searchTestPassword";
        
        // Store multiple records with identifiers
        Block medicalBlock1 = api.storeDataWithIdentifier(
            "Patient P-001: Diabetes diagnosis", password, "patient:P-001");
        Block medicalBlock2 = api.storeDataWithIdentifier(
            "Patient P-002: Hypertension diagnosis", password, "patient:P-002");
        Block financialBlock = api.storeDataWithIdentifier(
            "Transaction TX-001: Payment processed", password, "tx:TX-001");
        
        // Search for medical records
        List<Block> patientBlocks = api.findRecordsByIdentifier("patient:P-001");
        assertEquals(1, patientBlocks.size());
        
        // Decrypt found blocks
        String decryptedMedical = api.retrieveSecret(patientBlocks.get(0).getBlockNumber(), password);
        assertNotNull(decryptedMedical);
        assertTrue(decryptedMedical.contains("P-001"));
        assertTrue(decryptedMedical.contains("Diabetes"));
        
        // Search and decrypt in one operation
        List<Block> diabetesBlocks = api.findAndDecryptData("diabetes", password);
        assertEquals(1, diabetesBlocks.size());
        
        // Verify decrypted content is available
        String decryptedContent = diabetesBlocks.get(0).getData();
        assertNotNull(decryptedContent);
        assertTrue(decryptedContent.contains("Diabetes"));
    }
    
    @Test
    public void testBlockNumberConsistency() {
        String password = "consistencyTest";
        List<Block> storedBlocks = new ArrayList<>();
        
        // Store multiple blocks
        for (int i = 0; i < 5; i++) {
            Block block = api.storeSecret("Test data " + i, password);
            storedBlocks.add(block);
        }
        
        // Verify all blocks can be decrypted using their block numbers
        for (Block block : storedBlocks) {
            String decrypted = api.retrieveSecret(block.getBlockNumber(), password);
            assertNotNull(decrypted, "Failed to decrypt block #" + block.getBlockNumber());
            assertTrue(decrypted.contains("Test data"));
        }
        
        // Verify block numbers are sequential and consistent
        for (int i = 1; i < storedBlocks.size(); i++) {
            Long prevBlockNumber = storedBlocks.get(i-1).getBlockNumber();
            Long currentBlockNumber = storedBlocks.get(i).getBlockNumber();
            assertEquals(prevBlockNumber + 1, currentBlockNumber.longValue(),
                "Block numbers should be sequential");
        }
    }
}
```

These practical examples demonstrate how to effectively use the new block number-based decryption methods in real-world scenarios, providing robust error handling, performance optimization, and comprehensive testing patterns.