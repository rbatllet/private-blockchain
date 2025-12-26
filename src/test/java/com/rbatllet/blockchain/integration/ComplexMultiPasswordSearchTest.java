package com.rbatllet.blockchain.integration;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Rigorous integration tests for complex multi-password search patterns.
 *
 * This test suite is designed to find implementation defects in:
 * - Multi-tenant password isolation
 * - Cross-department search security
 * - Password-based encryption/decryption correctness
 * - Concurrent multi-password operations
 * - Search result accuracy with multiple encryption contexts
 *
 * Each test is designed to potentially expose real bugs, not just verify happy paths.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ComplexMultiPasswordSearchTest {

    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI medicalAPI;
    private UserFriendlyEncryptionAPI financeAPI;
    private UserFriendlyEncryptionAPI legalAPI;
    private UserFriendlyEncryptionAPI hrAPI;

    private String medicalPassword;
    private String financePassword;
    private String legalPassword;
    private String hrPassword;

    private KeyPair corporateAdminKeys;
    private KeyPair medicalKeys;
    private KeyPair financeKeys;
    private KeyPair legalKeys;
    private KeyPair hrKeys;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize fresh blockchain
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize(); // CRITICAL: Clear all data from previous tests
        // BlockRepository now package-private - use clearAndReinitialize();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();

        // Department passwords - intentionally similar to test edge cases
        medicalPassword = "MedicalSecure2025!";
        financePassword = "FinanceSecure2025!";
        legalPassword = "LegalSecure2025!";
        hrPassword = "HRSecure2025!";

        // Generate separate keys for corporate admin and each department
        corporateAdminKeys = CryptoUtil.generateKeyPair();
        medicalKeys = CryptoUtil.generateKeyPair();
        financeKeys = CryptoUtil.generateKeyPair();
        legalKeys = CryptoUtil.generateKeyPair();
        hrKeys = CryptoUtil.generateKeyPair();

        // Authorize all departments (RBAC v1.0.6)
        // Corporate Admin is the bootstrap admin that creates all departments
        blockchain.createBootstrapAdmin(CryptoUtil.publicKeyToString(corporateAdminKeys.getPublic()), "Corporate_Admin");
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(medicalKeys.getPublic()), "Medical_Dept", corporateAdminKeys, UserRole.USER);
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(financeKeys.getPublic()), "Finance_Dept", corporateAdminKeys, UserRole.USER);
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(legalKeys.getPublic()), "Legal_Dept", corporateAdminKeys, UserRole.USER);
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(hrKeys.getPublic()), "HR_Dept", corporateAdminKeys, UserRole.USER);

        // RBAC FIX (v1.0.6): Usernames must match key owner names registered with addAuthorizedKey()
        medicalAPI = new UserFriendlyEncryptionAPI(blockchain, "Medical_Dept", medicalKeys);
        financeAPI = new UserFriendlyEncryptionAPI(blockchain, "Finance_Dept", financeKeys);
        legalAPI = new UserFriendlyEncryptionAPI(blockchain, "Legal_Dept", legalKeys);
        hrAPI = new UserFriendlyEncryptionAPI(blockchain, "HR_Dept", hrKeys);

        // Initialize search with all passwords upfront for optimal performance
        blockchain.initializeAdvancedSearchWithMultiplePasswords(new String[]{
            medicalPassword, financePassword, legalPassword, hrPassword
        });
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Phase 5.4 FIX: Wait for async indexing to complete before cleanup
        IndexingCoordinator.getInstance().waitForCompletion();

        // CRITICAL: Clear database + search indexes to prevent state contamination
        if (blockchain != null) {
            blockchain.clearAndReinitialize();  // Also calls clearIndexes() + clearCache()
        }

        // Reset IndexingCoordinator singleton state
        IndexingCoordinator.getInstance().forceShutdown();
        IndexingCoordinator.getInstance().clearShutdownFlag();
        IndexingCoordinator.getInstance().disableTestMode();

        if (blockchain != null) {
            blockchain.shutdown();
        }
    }

    @Test
    @Order(1)
    @DisplayName("CRITICAL: Password isolation - wrong password must never decrypt data")
    void testPasswordIsolationIsAbsolute() throws Exception {
        // Store highly sensitive data with medical password
        String sensitiveData = "Patient John Doe SSN:123-45-6789 HIV+ status confirmed";
        String[] privateTerms = {"john", "doe", "ssn", "hiv", "123-45-6789"};

        Block medicalBlock = medicalAPI.storeSearchableData(
            sensitiveData,
            medicalPassword,
            privateTerms
        );
        assertNotNull(medicalBlock, "Medical block should be stored");

        IndexingCoordinator.getInstance().waitForCompletion();

        // CRITICAL TEST: Finance department tries to access medical data with their password
        List<Block> financeAttempt = financeAPI.searchAndDecryptByTerms(
            new String[]{"hiv"},
            financePassword,  // WRONG PASSWORD
            10
        );

        // Should find no results OR find encrypted blocks that cannot be decrypted
        if (financeAttempt.size() > 0) {
            for (Block block : financeAttempt) {
                // If block is found, verify data is NOT decrypted
                String data = block.getData();
                assertFalse(
                    data.contains("HIV+") || data.contains("John Doe"),
                    "SECURITY VIOLATION: Wrong password decrypted sensitive data!"
                );
            }
        }

        // CRITICAL TEST: Legal department tries with their password
        List<Block> legalAttempt = legalAPI.searchAndDecryptByTerms(
            new String[]{"ssn"},
            legalPassword,  // WRONG PASSWORD
            10
        );

        if (legalAttempt.size() > 0) {
            for (Block block : legalAttempt) {
                String data = block.getData();
                assertFalse(
                    data.contains("123-45-6789") || data.contains("John Doe"),
                    "SECURITY VIOLATION: Wrong password decrypted SSN!"
                );
            }
        }

        // Verify CORRECT password works
        List<Block> medicalCorrect = medicalAPI.searchAndDecryptByTerms(
            new String[]{"hiv"},
            medicalPassword,  // CORRECT PASSWORD
            10
        );

        assertTrue(medicalCorrect.size() > 0, "Correct password should find data");
        boolean foundDecrypted = false;
        for (Block block : medicalCorrect) {
            if (block.getData().contains("HIV+")) {
                foundDecrypted = true;
                break;
            }
        }
        assertTrue(foundDecrypted, "Correct password should decrypt data");
    }

    @Test
    @Order(2)
    @DisplayName("CRITICAL: Cross-department search must not leak information")
    void testCrossDepartmentSearchLeakage() throws Exception {
        // Medical stores patient financial info
        String medicalWithFinancial = "Patient treatment cost: $50,000 insurance claim";
        medicalAPI.storeSearchableData(medicalWithFinancial, medicalPassword,
            new String[]{"patient", "treatment", "50000", "insurance"});

        // Finance stores unrelated financial data
        String financeData = "Company revenue: $50,000 quarterly bonus";
        financeAPI.storeSearchableData(financeData, financePassword,
            new String[]{"revenue", "50000", "bonus", "quarterly"});

        IndexingCoordinator.getInstance().waitForCompletion();

        // Finance searches for "50000" with their password
        List<Block> financeResults = financeAPI.searchAndDecryptByTerms(
            new String[]{"50000"},
            financePassword,
            10
        );

        // CRITICAL: Verify finance CANNOT see medical data
        for (Block block : financeResults) {
            String data = block.getData();
            assertFalse(
                data.contains("Patient") || data.contains("treatment") || data.contains("insurance"),
                "LEAK DETECTED: Finance dept accessed medical data!"
            );
        }

        // Medical searches for "50000" with their password
        List<Block> medicalResults = medicalAPI.searchAndDecryptByTerms(
            new String[]{"50000"},
            medicalPassword,
            10
        );

        // CRITICAL: Verify medical CANNOT see finance data
        for (Block block : medicalResults) {
            String data = block.getData();
            assertFalse(
                data.contains("revenue") || data.contains("bonus") || data.contains("quarterly"),
                "LEAK DETECTED: Medical dept accessed finance data!"
            );
        }
    }

    @Test
    @Order(3)
    @DisplayName("CRITICAL: Public terms must be accessible without password")
    void testPublicTermsAccessibility() throws Exception {
        // Store data with clear public/private separation
        String legalContract = "Employment contract for Software Engineer position, salary confidential";
        String[] publicTerms = {"employment", "contract", "engineer"};
        String[] privateTerms = {"salary", "confidential", "compensation"};

        Block legalBlock = legalAPI.storeSearchableDataWithLayers(
            legalContract,
            legalPassword,
            publicTerms,
            privateTerms
        );

        assertNotNull(legalBlock);
        IndexingCoordinator.getInstance().waitForCompletion();

        // CRITICAL TEST: Public terms should be searchable WITHOUT any password
        List<Block> publicSearch = legalAPI.searchByTerms(
            new String[]{"employment"},
            null,  // NO PASSWORD
            10
        );

        assertTrue(publicSearch.size() > 0,
            "BUG: Public terms are not searchable without password!");

        // Verify other public terms
        publicSearch = legalAPI.searchByTerms(new String[]{"engineer"}, null, 10);
        assertTrue(publicSearch.size() > 0, "Public term 'engineer' should be findable");

        // CRITICAL TEST: Private terms should NOT be searchable without password
        List<Block> privateSearchNoPassword = legalAPI.searchByTerms(
            new String[]{"salary"},
            null,  // NO PASSWORD
            10
        );

        assertEquals(0, privateSearchNoPassword.size(),
            "SECURITY BUG: Private term 'salary' found without password!");

        // CRITICAL TEST: Private terms SHOULD be searchable with correct password
        List<Block> privateSearchWithPassword = legalAPI.searchAndDecryptByTerms(
            new String[]{"salary"},
            legalPassword,  // CORRECT PASSWORD
            10
        );

        assertTrue(privateSearchWithPassword.size() > 0,
            "BUG: Private terms not searchable even with correct password!");
    }

    @Test
    @Order(4)
    @DisplayName("CRITICAL: Multiple passwords in same search context")
    void testMultiplePasswordsSimultaneously() throws Exception {
        // Store 4 different encrypted blocks with 4 different passwords
        Block medBlock = medicalAPI.storeSearchableData(
            "Medical record alpha", medicalPassword, new String[]{"alpha", "medical"});
        Block finBlock = financeAPI.storeSearchableData(
            "Financial record beta", financePassword, new String[]{"beta", "financial"});
        Block legBlock = legalAPI.storeSearchableData(
            "Legal document gamma", legalPassword, new String[]{"gamma", "legal"});
        Block hrBlock = hrAPI.storeSearchableData(
            "HR file delta", hrPassword, new String[]{"delta", "hr"});

        assertNotNull(medBlock);
        assertNotNull(finBlock);
        assertNotNull(legBlock);
        assertNotNull(hrBlock);

        IndexingCoordinator.getInstance().waitForCompletion();

        // Each department searches for their own data - all should succeed
        List<Block> medResults = medicalAPI.searchAndDecryptByTerms(
            new String[]{"alpha"}, medicalPassword, 10);
        List<Block> finResults = financeAPI.searchAndDecryptByTerms(
            new String[]{"beta"}, financePassword, 10);
        List<Block> legResults = legalAPI.searchAndDecryptByTerms(
            new String[]{"gamma"}, legalPassword, 10);
        List<Block> hrResults = hrAPI.searchAndDecryptByTerms(
            new String[]{"delta"}, hrPassword, 10);

        assertTrue(medResults.size() > 0, "Medical should find 'alpha'");
        assertTrue(finResults.size() > 0, "Finance should find 'beta'");
        assertTrue(legResults.size() > 0, "Legal should find 'gamma'");
        assertTrue(hrResults.size() > 0, "HR should find 'delta'");

        // CRITICAL: Verify no cross-contamination
        List<Block> medWrongSearch = medicalAPI.searchAndDecryptByTerms(
            new String[]{"beta"}, medicalPassword, 10);

        if (medWrongSearch.size() > 0) {
            for (Block b : medWrongSearch) {
                assertFalse(b.getData().contains("Financial record beta"),
                    "CONTAMINATION: Medical decrypted finance data!");
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("CRITICAL: Concurrent multi-password searches must not interfere")
    void testConcurrentMultiPasswordSearches() throws Exception {
        // Store 20 blocks across 4 departments
        List<Block> allBlocks = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            allBlocks.add(medicalAPI.storeSearchableData(
                "Medical record " + i, medicalPassword, new String[]{"medical", "record" + i}));
            allBlocks.add(financeAPI.storeSearchableData(
                "Finance doc " + i, financePassword, new String[]{"finance", "doc" + i}));
            allBlocks.add(legalAPI.storeSearchableData(
                "Legal file " + i, legalPassword, new String[]{"legal", "file" + i}));
            allBlocks.add(hrAPI.storeSearchableData(
                "HR report " + i, hrPassword, new String[]{"hr", "report" + i}));
        }

        // Phase 5.4 FIX: Wait for async indexing to complete BEFORE searching
        IndexingCoordinator.getInstance().waitForCompletion();

        // Launch 40 concurrent searches (10 per department)
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 25 Virtual Threads;
        List<Future<SearchResult>> futures = new ArrayList<>();

        // Medical searches
        for (int i = 0; i < 10; i++) {
            int index = i;
            futures.add(executor.submit(() -> {
                try {
                    List<Block> results = medicalAPI.searchAndDecryptByTerms(
                        new String[]{"medical"}, medicalPassword, 20);
                    return new SearchResult("medical", index, results.size(),
                        results.stream().anyMatch(b -> b.getData().contains("Medical")));
                } catch (Exception e) {
                    return new SearchResult("medical", index, -1, false, e);
                }
            }));
        }

        // Finance searches
        for (int i = 0; i < 10; i++) {
            int index = i;
            futures.add(executor.submit(() -> {
                try {
                    List<Block> results = financeAPI.searchAndDecryptByTerms(
                        new String[]{"finance"}, financePassword, 20);
                    return new SearchResult("finance", index, results.size(),
                        results.stream().anyMatch(b -> b.getData().contains("Finance")));
                } catch (Exception e) {
                    return new SearchResult("finance", index, -1, false, e);
                }
            }));
        }

        // Legal searches
        for (int i = 0; i < 10; i++) {
            int index = i;
            futures.add(executor.submit(() -> {
                try {
                    List<Block> results = legalAPI.searchAndDecryptByTerms(
                        new String[]{"legal"}, legalPassword, 20);
                    return new SearchResult("legal", index, results.size(),
                        results.stream().anyMatch(b -> b.getData().contains("Legal")));
                } catch (Exception e) {
                    return new SearchResult("legal", index, -1, false, e);
                }
            }));
        }

        // HR searches
        for (int i = 0; i < 10; i++) {
            int index = i;
            futures.add(executor.submit(() -> {
                try {
                    List<Block> results = hrAPI.searchAndDecryptByTerms(
                        new String[]{"hr"}, hrPassword, 20);
                    return new SearchResult("hr", index, results.size(),
                        results.stream().anyMatch(b -> b.getData().contains("HR")));
                } catch (Exception e) {
                    return new SearchResult("hr", index, -1, false, e);
                }
            }));
        }

        // Wait for all searches to complete
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS),
            "Searches took too long - possible deadlock!");

        // Analyze results
        List<SearchResult> results = new ArrayList<>();
        for (Future<SearchResult> future : futures) {
            results.add(future.get());
        }

        // CRITICAL CHECKS
        long failedSearches = results.stream().filter(r -> r.exception != null).count();
        assertEquals(0, failedSearches,
            "CONCURRENCY BUG: " + failedSearches + " searches failed with exceptions!");

        long emptyResults = results.stream().filter(r -> r.resultCount == 0).count();
        assertEquals(0, emptyResults,
            "CONCURRENCY BUG: " + emptyResults + " searches returned no results!");

        long wrongContent = results.stream().filter(r -> !r.foundExpectedContent).count();
        assertEquals(0, wrongContent,
            "CONCURRENCY BUG: " + wrongContent + " searches returned wrong department data!");

        // Verify each department got their expected count (5 blocks each)
        List<SearchResult> medicalResults = results.stream()
            .filter(r -> r.department.equals("medical"))
            .collect(Collectors.toList());

        for (SearchResult sr : medicalResults) {
            assertEquals(5, sr.resultCount,
                "Medical search " + sr.searchIndex + " should find exactly 5 blocks, found: " + sr.resultCount);
        }
    }

    @Test
    @Order(6)
    @DisplayName("CRITICAL: Wrong password must not partially decrypt data")
    void testPartialDecryptionImpossible() throws Exception {
        // Store complex data with multiple sensitive fields
        String complexData = "CONFIDENTIAL: Employee Jane Smith SSN:987-65-4321 Salary:$150,000 " +
                           "Performance:Excellent Department:Engineering StartDate:2023-01-15 " +
                           "Clearance:Level-5 Project:SecretProjectX";

        Block hrBlock = hrAPI.storeSearchableData(
            complexData,
            hrPassword,
            new String[]{"jane", "smith", "987-65-4321", "150000", "engineering", "secretprojectx"}
        );

        assertNotNull(hrBlock);
        IndexingCoordinator.getInstance().waitForCompletion();

        // Try to decrypt with slightly wrong password (typo simulation)
        String wrongPassword1 = "HRSecure2025";  // Missing !
        String wrongPassword2 = "HRSecure2026!";  // Wrong year
        String wrongPassword3 = "hrSecure2025!";  // Wrong case

        List<Block> attempt1 = hrAPI.searchAndDecryptByTerms(
            new String[]{"jane"}, wrongPassword1, 10);
        List<Block> attempt2 = hrAPI.searchAndDecryptByTerms(
            new String[]{"smith"}, wrongPassword2, 10);
        List<Block> attempt3 = hrAPI.searchAndDecryptByTerms(
            new String[]{"engineering"}, wrongPassword3, 10);

        // CRITICAL: No partial data should ever be visible
        for (List<Block> attempt : List.of(attempt1, attempt2, attempt3)) {
            for (Block block : attempt) {
                String data = block.getData();

                // Check that NO sensitive information leaked
                assertFalse(data.contains("Jane"), "Partial leak: name visible!");
                assertFalse(data.contains("Smith"), "Partial leak: surname visible!");
                assertFalse(data.contains("987-65-4321"), "Partial leak: SSN visible!");
                assertFalse(data.contains("150,000"), "Partial leak: salary visible!");
                assertFalse(data.contains("SecretProjectX"), "Partial leak: project visible!");
                assertFalse(data.contains("Level-5"), "Partial leak: clearance visible!");

                // Should be either fully encrypted or null
                assertTrue(
                    data.startsWith("ENC:") || data.isEmpty() || data.equals("null"),
                    "Data should be fully encrypted or empty, not partially decrypted!"
                );
            }
        }

        // Verify correct password decrypts everything
        List<Block> correctAttempt = hrAPI.searchAndDecryptByTerms(
            new String[]{"jane"}, hrPassword, 10);

        assertTrue(correctAttempt.size() > 0, "Correct password should find data");
        boolean fullyDecrypted = correctAttempt.stream()
            .anyMatch(b -> b.getData().contains("Jane Smith") &&
                          b.getData().contains("987-65-4321") &&
                          b.getData().contains("150,000"));

        assertTrue(fullyDecrypted, "Correct password should fully decrypt all fields");
    }

    @Test
    @Order(7)
    @DisplayName("CRITICAL: Empty and null password edge cases")
    void testPasswordEdgeCases() throws Exception {
        // Test with empty password
        assertThrows(Exception.class, () -> {
            medicalAPI.storeSearchableData("Test data", "", new String[]{"test"});
        }, "Empty password should throw exception");

        // Test with null password for encrypted storage
        assertThrows(Exception.class, () -> {
            medicalAPI.storeSearchableData("Test data", null, new String[]{"test"});
        }, "Null password should throw exception");

        // Test searching with null password on encrypted data with PRIVATE keywords
        Block encryptedBlock = medicalAPI.storeSearchableDataWithLayers(
            "Encrypted content", 
            medicalPassword, 
            null,  // NO public keywords
            new String[]{"private:encrypted", "private:secret"}  // Only PRIVATE keywords
        );

        assertNotNull(encryptedBlock);
        IndexingCoordinator.getInstance().waitForCompletion();

        // Search with null password should NOT find blocks with only private keywords
        List<Block> nullPasswordSearch = medicalAPI.searchAndDecryptByTerms(
            new String[]{"encrypted"}, null, 10);

        // Should return empty list - no blocks with private-only keywords
        assertTrue(nullPasswordSearch.isEmpty(),
            "NULL PASSWORD VULNERABILITY: Found block with private keywords using null password!");
    }

    @Test
    @Order(8)
    @DisplayName("CRITICAL: Large-scale multi-tenant search accuracy")
    void testLargeScaleMultiTenantAccuracy() throws Exception {
        // Store 100 blocks across 4 departments (25 each)
        List<Block> medicalBlocks = new ArrayList<>();
        List<Block> financeBlocks = new ArrayList<>();
        List<Block> legalBlocks = new ArrayList<>();
        List<Block> hrBlocks = new ArrayList<>();

        for (int i = 0; i < 25; i++) {
            medicalBlocks.add(medicalAPI.storeSearchableData(
                "Medical patient record " + i + " diagnosis treatment",
                medicalPassword,
                new String[]{"patient", "record" + i, "diagnosis", "treatment"}
            ));

            financeBlocks.add(financeAPI.storeSearchableData(
                "Finance transaction " + i + " payment invoice",
                financePassword,
                new String[]{"transaction", "trans" + i, "payment", "invoice"}
            ));

            legalBlocks.add(legalAPI.storeSearchableData(
                "Legal contract " + i + " agreement terms",
                legalPassword,
                new String[]{"contract", "cont" + i, "agreement", "terms"}
            ));

            hrBlocks.add(hrAPI.storeSearchableData(
                "HR employee " + i + " performance review",
                hrPassword,
                new String[]{"employee", "emp" + i, "performance", "review"}
            ));
        }

        // Phase 5.4 FIX: Wait for async indexing to complete BEFORE searching
        IndexingCoordinator.getInstance().waitForCompletion();

        // CRITICAL TEST 1: Each department finds exactly their 25 blocks
        List<Block> medSearch = medicalAPI.searchAndDecryptByTerms(
            new String[]{"patient"}, medicalPassword, 100);
        assertEquals(25, medSearch.size(),
            "Medical should find exactly 25 blocks, found: " + medSearch.size());

        List<Block> finSearch = financeAPI.searchAndDecryptByTerms(
            new String[]{"transaction"}, financePassword, 100);
        assertEquals(25, finSearch.size(),
            "Finance should find exactly 25 blocks, found: " + finSearch.size());

        List<Block> legSearch = legalAPI.searchAndDecryptByTerms(
            new String[]{"contract"}, legalPassword, 100);
        assertEquals(25, legSearch.size(),
            "Legal should find exactly 25 blocks, found: " + legSearch.size());

        List<Block> hrSearch = hrAPI.searchAndDecryptByTerms(
            new String[]{"employee"}, hrPassword, 100);
        assertEquals(25, hrSearch.size(),
            "HR should find exactly 25 blocks, found: " + hrSearch.size());

        // CRITICAL TEST 2: Verify no cross-contamination in results
        for (Block block : medSearch) {
            assertTrue(block.getData().contains("Medical"),
                "Medical results contain non-medical data!");
            assertFalse(block.getData().contains("Finance") ||
                       block.getData().contains("Legal") ||
                       block.getData().contains("HR"),
                "CONTAMINATION: Medical results contain other department data!");
        }

        for (Block block : finSearch) {
            assertTrue(block.getData().contains("Finance"),
                "Finance results contain non-finance data!");
        }

        for (Block block : legSearch) {
            assertTrue(block.getData().contains("Legal"),
                "Legal results contain non-legal data!");
        }

        for (Block block : hrSearch) {
            assertTrue(block.getData().contains("HR"),
                "HR results contain non-HR data!");
        }

        // CRITICAL TEST 3: Cross-department search returns zero results
        List<Block> medWrongPassword = medicalAPI.searchAndDecryptByTerms(
            new String[]{"transaction"}, medicalPassword, 100);

        // Should either find nothing or find blocks that are still encrypted
        for (Block block : medWrongPassword) {
            assertFalse(block.getData().contains("Finance transaction"),
                "SECURITY BREACH: Medical password decrypted finance data!");
        }
    }

    /**
     * Helper class to capture search results from concurrent operations
     */
    private static class SearchResult {
        String department;
        int searchIndex;
        int resultCount;
        boolean foundExpectedContent;
        Exception exception;

        SearchResult(String dept, int index, int count, boolean foundContent) {
            this.department = dept;
            this.searchIndex = index;
            this.resultCount = count;
            this.foundExpectedContent = foundContent;
            this.exception = null;
        }

        SearchResult(String dept, int index, int count, boolean foundContent, Exception e) {
            this(dept, index, count, foundContent);
            this.exception = e;
        }
    }
}