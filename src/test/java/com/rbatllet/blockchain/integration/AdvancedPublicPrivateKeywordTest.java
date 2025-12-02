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

/**
 * Advanced tests for public/private keyword combinations and edge cases.
 * 
 * This test suite covers:
 * - Public vs private keyword visibility
 * - Cross-department keyword sharing
 * - Multi-term searches with mixed visibility
 * - Edge cases: null/empty passwords, case sensitivity, duplicates
 * - Performance: large keyword sets
 * - Security: wrong password isolation
 * - Concurrency: simultaneous public/private searches
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdvancedPublicPrivateKeywordTest {

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
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();

        medicalPassword = "MedicalSecure2025!";
        financePassword = "FinanceSecure2025!";
        legalPassword = "LegalSecure2025!";
        hrPassword = "HRSecure2025!";

        corporateAdminKeys = CryptoUtil.generateKeyPair();
        medicalKeys = CryptoUtil.generateKeyPair();
        financeKeys = CryptoUtil.generateKeyPair();
        legalKeys = CryptoUtil.generateKeyPair();
        hrKeys = CryptoUtil.generateKeyPair();

        blockchain.createBootstrapAdmin(CryptoUtil.publicKeyToString(corporateAdminKeys.getPublic()), "Corporate_Admin");
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(medicalKeys.getPublic()), "Medical_Dept", corporateAdminKeys, UserRole.USER);
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(financeKeys.getPublic()), "Finance_Dept", corporateAdminKeys, UserRole.USER);
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(legalKeys.getPublic()), "Legal_Dept", corporateAdminKeys, UserRole.USER);
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(hrKeys.getPublic()), "HR_Dept", corporateAdminKeys, UserRole.USER);

        medicalAPI = new UserFriendlyEncryptionAPI(blockchain, "Medical_Dept", medicalKeys);
        financeAPI = new UserFriendlyEncryptionAPI(blockchain, "Finance_Dept", financeKeys);
        legalAPI = new UserFriendlyEncryptionAPI(blockchain, "Legal_Dept", legalKeys);
        hrAPI = new UserFriendlyEncryptionAPI(blockchain, "HR_Dept", hrKeys);

        blockchain.initializeAdvancedSearchWithMultiplePasswords(new String[]{
            medicalPassword, financePassword, legalPassword, hrPassword
        });
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        IndexingCoordinator.getInstance().waitForCompletion();
        blockchain.clearAndReinitialize();
        IndexingCoordinator.getInstance().forceShutdown();
        IndexingCoordinator.getInstance().clearShutdownFlag();
        IndexingCoordinator.getInstance().disableTestMode();
        blockchain.shutdown();
    }

    @Test
    @Order(1)
    @DisplayName("ADVANCED: Public + Private keyword combinations")
    void testPublicPrivateKeywordCombinations() throws Exception {
        // Test 1: Block with ONLY public keywords
        medicalAPI.storeSearchableData(
            "Public medical research data",
            medicalPassword,
            new String[]{"research", "medical"}
        );

        // Test 2: Block with ONLY private keywords
        medicalAPI.storeSearchableDataWithLayers(
            "Confidential patient diagnosis",
            medicalPassword,
            null,
            new String[]{"patient", "diagnosis"}
        );

        // Test 3: Block with BOTH public and private keywords
        medicalAPI.storeSearchableDataWithLayers(
            "Mixed visibility medical record",
            medicalPassword,
            new String[]{"general", "health"},
            new String[]{"ssn", "confidential"}
        );

        IndexingCoordinator.getInstance().waitForCompletion();

        // Search WITHOUT password - should find public-only and mixed (public part)
        List<Block> noPasswordPublic = medicalAPI.searchAndDecryptByTerms(
            new String[]{"research"}, null, 10);
        assertEquals(1, noPasswordPublic.size(), "Should find block with public:research");

        List<Block> noPasswordGeneral = medicalAPI.searchAndDecryptByTerms(
            new String[]{"general"}, null, 10);
        assertEquals(1, noPasswordGeneral.size(), "Should find mixed block via public keyword");

        // Search for private keyword WITHOUT password - should find NOTHING
        List<Block> noPasswordPrivate = medicalAPI.searchAndDecryptByTerms(
            new String[]{"patient"}, null, 10);
        assertEquals(0, noPasswordPrivate.size(), "Should NOT find private keywords without password");

        // Search WITH password - should find all relevant blocks
        List<Block> withPasswordPublic = medicalAPI.searchAndDecryptByTerms(
            new String[]{"research"}, medicalPassword, 10);
        assertEquals(1, withPasswordPublic.size(), "Should find public block with password");

        List<Block> withPasswordPrivate = medicalAPI.searchAndDecryptByTerms(
            new String[]{"patient"}, medicalPassword, 10);
        assertEquals(1, withPasswordPrivate.size(), "Should find private block with password");

        List<Block> withPasswordMixed = medicalAPI.searchAndDecryptByTerms(
            new String[]{"ssn"}, medicalPassword, 10);
        assertEquals(1, withPasswordMixed.size(), "Should find mixed block via private keyword");
    }

    @Test
    @Order(2)
    @DisplayName("ADVANCED: Multi-term search with public/private mix")
    void testMultiTermPublicPrivateMix() throws Exception {
        medicalAPI.storeSearchableDataWithLayers(
            "Emergency patient records",
            medicalPassword,
            new String[]{"emergency"},
            new String[]{"critical", "patient"}
        );

        medicalAPI.storeSearchableDataWithLayers(
            "Routine checkup data",
            medicalPassword,
            new String[]{"routine", "checkup"},
            null
        );

        medicalAPI.storeSearchableDataWithLayers(
            "Lab results confidential",
            medicalPassword,
            null,
            new String[]{"lab", "results", "confidential"}
        );

        IndexingCoordinator.getInstance().waitForCompletion();

        // Multi-term search with public terms only, no password
        List<Block> publicMulti = medicalAPI.searchAndDecryptByTerms(
            new String[]{"emergency", "routine"}, null, 10);
        assertEquals(2, publicMulti.size(), "Should find both blocks with public keywords");

        // Multi-term search mixing public and private terms, no password
        List<Block> mixedNoPassword = medicalAPI.searchAndDecryptByTerms(
            new String[]{"emergency", "lab"}, null, 10);
        assertEquals(1, mixedNoPassword.size(), "Should only find block with public:emergency");

        // Multi-term search with password
        List<Block> mixedWithPassword = medicalAPI.searchAndDecryptByTerms(
            new String[]{"emergency", "lab"}, medicalPassword, 10);
        assertEquals(2, mixedWithPassword.size(), "Should find both blocks with password");

        // Search for only private terms with password
        List<Block> privateOnly = medicalAPI.searchAndDecryptByTerms(
            new String[]{"confidential", "critical"}, medicalPassword, 10);
        assertEquals(2, privateOnly.size(), "Should find 2 blocks with private keywords");
    }

    @Test
    @Order(3)
    @DisplayName("SECURITY: Wrong password cannot decrypt private keywords")
    void testWrongPasswordPrivateKeywordSecurity() throws Exception {
        medicalAPI.storeSearchableDataWithLayers(
            "Medical confidential diagnosis: cancer",
            medicalPassword,
            null,
            new String[]{"diagnosis", "cancer"}
        );

        IndexingCoordinator.getInstance().waitForCompletion();

        // Try to search with WRONG department password
        List<Block> wrongPasswordSearch = financeAPI.searchAndDecryptByTerms(
            new String[]{"diagnosis"}, financePassword, 10);

        assertEquals(0, wrongPasswordSearch.size(),
            "SECURITY BREACH: Wrong password found private keywords!");

        // Verify correct password works
        List<Block> correctPasswordSearch = medicalAPI.searchAndDecryptByTerms(
            new String[]{"diagnosis"}, medicalPassword, 10);
        assertEquals(1, correctPasswordSearch.size(), "Correct password should find block");
    }

    @Test
    @Order(4)
    @DisplayName("ADVANCED: Cross-department public keyword sharing")
    void testCrossDepartmentPublicSharing() throws Exception {
        medicalAPI.storeSearchableData(
            "Medical COVID-19 protocols",
            medicalPassword,
            new String[]{"covid19", "protocols"}
        );

        hrAPI.storeSearchableData(
            "HR COVID-19 work-from-home policy",
            hrPassword,
            new String[]{"covid19", "policy"}
        );

        legalAPI.storeSearchableData(
            "Legal COVID-19 compliance requirements",
            legalPassword,
            new String[]{"covid19", "compliance"}
        );

        IndexingCoordinator.getInstance().waitForCompletion();

        // Search for public keyword WITHOUT password - should find ALL departments
        List<Block> publicCovid = medicalAPI.searchAndDecryptByTerms(
            new String[]{"covid19"}, null, 10);
        assertEquals(3, publicCovid.size(), "Should find all 3 departments with public:covid19");

        // Verify each department can decrypt their own
        List<Block> medicalCovid = medicalAPI.searchAndDecryptByTerms(
            new String[]{"covid19"}, medicalPassword, 10);
        assertTrue(medicalCovid.stream().anyMatch(b -> b.getData().contains("Medical COVID-19")));

        List<Block> hrCovid = hrAPI.searchAndDecryptByTerms(
            new String[]{"covid19"}, hrPassword, 10);
        assertTrue(hrCovid.stream().anyMatch(b -> b.getData().contains("HR COVID-19")));

        List<Block> legalCovid = legalAPI.searchAndDecryptByTerms(
            new String[]{"covid19"}, legalPassword, 10);
        assertTrue(legalCovid.stream().anyMatch(b -> b.getData().contains("Legal COVID-19")));
    }

    @Test
    @Order(5)
    @DisplayName("EDGE CASE: Empty password vs null password")
    void testEmptyVsNullPassword() throws Exception {
        // Both empty and null should fail during STORAGE
        assertThrows(Exception.class, () -> {
            medicalAPI.storeSearchableData("Test", "", new String[]{"test"});
        }, "Empty password should throw exception during storage");

        assertThrows(Exception.class, () -> {
            medicalAPI.storeSearchableData("Test", null, new String[]{"test"});
        }, "Null password should throw exception during storage");

        // Store valid block with public keyword
        medicalAPI.storeSearchableData(
            "Valid data", medicalPassword, new String[]{"valid"});
        IndexingCoordinator.getInstance().waitForCompletion();

        // Search with null password should work for public keywords
        List<Block> nullSearch = medicalAPI.searchAndDecryptByTerms(
            new String[]{"valid"}, null, 10);
        assertEquals(1, nullSearch.size(), "Null password should find public keywords");

        // Search with empty string password should work for public keywords
        List<Block> emptySearch = medicalAPI.searchAndDecryptByTerms(
            new String[]{"valid"}, "", 10);
        assertEquals(1, emptySearch.size(), "Empty password should find public keywords");
    }

    @Test
    @Order(6)
    @DisplayName("EDGE CASE: Case sensitivity in public/private keywords")
    void testKeywordCaseSensitivity() throws Exception {
        medicalAPI.storeSearchableDataWithLayers(
            "Case test data",
            medicalPassword,
            new String[]{"Medical", "URGENT"},
            new String[]{"Patient", "CONFIDENTIAL"}
        );

        IndexingCoordinator.getInstance().waitForCompletion();

        // Search should be case-insensitive
        List<Block> lowerPublic = medicalAPI.searchAndDecryptByTerms(
            new String[]{"medical"}, null, 10);
        assertEquals(1, lowerPublic.size(), "Should find 'Medical' with lowercase search");

        List<Block> upperPublic = medicalAPI.searchAndDecryptByTerms(
            new String[]{"URGENT"}, null, 10);
        assertEquals(1, upperPublic.size(), "Should find 'URGENT' with uppercase search");

        List<Block> lowerPrivate = medicalAPI.searchAndDecryptByTerms(
            new String[]{"patient"}, medicalPassword, 10);
        assertEquals(1, lowerPrivate.size(), "Should find 'Patient' with lowercase search");

        List<Block> upperPrivate = medicalAPI.searchAndDecryptByTerms(
            new String[]{"CONFIDENTIAL"}, medicalPassword, 10);
        assertEquals(1, upperPrivate.size(), "Should find 'CONFIDENTIAL' with uppercase search");
    }

    @Test
    @Order(7)
    @DisplayName("EDGE CASE: Duplicate keywords in same block")
    void testDuplicateKeywordsInBlock() throws Exception {
        medicalAPI.storeSearchableDataWithLayers(
            "Duplicate keyword test",
            medicalPassword,
            new String[]{"test", "test", "duplicate"},
            new String[]{"secret", "secret", "hidden"}
        );

        IndexingCoordinator.getInstance().waitForCompletion();

        // Search should return block only ONCE
        List<Block> publicSearch = medicalAPI.searchAndDecryptByTerms(
            new String[]{"test"}, null, 10);
        assertEquals(1, publicSearch.size(), "Should find block only once despite duplicate keywords");

        List<Block> privateSearch = medicalAPI.searchAndDecryptByTerms(
            new String[]{"secret"}, medicalPassword, 10);
        assertEquals(1, privateSearch.size(), "Should find block only once despite duplicate keywords");

        // Multi-term search with duplicates
        List<Block> multiSearch = medicalAPI.searchAndDecryptByTerms(
            new String[]{"test", "duplicate", "test"}, null, 10);
        assertEquals(1, multiSearch.size(), "Should find block only once despite duplicate search terms");
    }

    @Test
    @Order(8)
    @DisplayName("PERFORMANCE: Large number of keywords per block")
    void testLargeNumberOfKeywords() throws Exception {
        String[] publicKeywords = new String[50];
        String[] privateKeywords = new String[50];

        for (int i = 0; i < 50; i++) {
            publicKeywords[i] = "pub" + i;
            privateKeywords[i] = "priv" + i;
        }

        medicalAPI.storeSearchableDataWithLayers(
            "Large keyword set test",
            medicalPassword,
            publicKeywords,
            privateKeywords
        );

        IndexingCoordinator.getInstance().waitForCompletion();

        // Search for first, middle, and last keywords
        List<Block> first = medicalAPI.searchAndDecryptByTerms(
            new String[]{"pub0"}, null, 10);
        assertEquals(1, first.size(), "Should find block by first public keyword");

        List<Block> middle = medicalAPI.searchAndDecryptByTerms(
            new String[]{"pub25"}, null, 10);
        assertEquals(1, middle.size(), "Should find block by middle public keyword");

        List<Block> last = medicalAPI.searchAndDecryptByTerms(
            new String[]{"pub49"}, null, 10);
        assertEquals(1, last.size(), "Should find block by last public keyword");

        List<Block> privateFirst = medicalAPI.searchAndDecryptByTerms(
            new String[]{"priv0"}, medicalPassword, 10);
        assertEquals(1, privateFirst.size(), "Should find block by first private keyword");

        List<Block> privateLast = medicalAPI.searchAndDecryptByTerms(
            new String[]{"priv49"}, medicalPassword, 10);
        assertEquals(1, privateLast.size(), "Should find block by last private keyword");
    }

    @Test
    @Order(9)
    @DisplayName("SECURITY: Same keyword as public in one dept, private in another")
    void testSameKeywordDifferentVisibility() throws Exception {
        // Medical: "diagnosis" is PUBLIC
        medicalAPI.storeSearchableData(
            "Public medical research on common diagnoses",
            medicalPassword,
            new String[]{"diagnosis", "research"}
        );

        // Finance: "diagnosis" is PRIVATE
        financeAPI.storeSearchableDataWithLayers(
            "Financial health diagnosis - confidential",
            financePassword,
            null,
            new String[]{"diagnosis", "financial"}
        );

        IndexingCoordinator.getInstance().waitForCompletion();

        // Search WITHOUT password - should only find medical (public)
        List<Block> noPassword = medicalAPI.searchAndDecryptByTerms(
            new String[]{"diagnosis"}, null, 10);
        assertEquals(1, noPassword.size(), "Should only find medical block with public keyword");
        assertTrue(noPassword.get(0).getData().contains("medical research"));

        // Search with MEDICAL password - should find medical
        List<Block> medPassword = medicalAPI.searchAndDecryptByTerms(
            new String[]{"diagnosis"}, medicalPassword, 10);
        assertEquals(1, medPassword.size(), "Medical password should only find medical block");

        // Search with FINANCE password - should find finance block
        List<Block> finPassword = financeAPI.searchAndDecryptByTerms(
            new String[]{"diagnosis"}, financePassword, 10);
        assertTrue(finPassword.size() >= 1, "Finance password should find at least finance block");
        assertTrue(finPassword.stream().anyMatch(b -> b.getData().contains("Financial health")));
    }

    @Test
    @Order(10)
    @DisplayName("CONCURRENCY: Simultaneous public/private keyword searches")
    void testConcurrentPublicPrivateSearches() throws Exception {
        // Create mix of public and private blocks
        for (int i = 0; i < 10; i++) {
            medicalAPI.storeSearchableData(
                "Public medical record " + i,
                medicalPassword,
                new String[]{"medical", "record" + i}
            );

            medicalAPI.storeSearchableDataWithLayers(
                "Private patient file " + i,
                medicalPassword,
                null,
                new String[]{"patient", "file" + i}
            );
        }

        IndexingCoordinator.getInstance().waitForCompletion();

        // Launch concurrent searches
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<Future<SearchResult>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            // Public search (no password)
            futures.add(executor.submit(() -> {
                try {
                    List<Block> results = medicalAPI.searchAndDecryptByTerms(
                        new String[]{"medical"}, null, 20);
                    return new SearchResult("public", results.size(), 
                        results.stream().allMatch(b -> b.getData().contains("Public")));
                } catch (Exception e) {
                    return new SearchResult("public", 0, false, e);
                }
            }));

            // Private search (with password)
            futures.add(executor.submit(() -> {
                try {
                    List<Block> results = medicalAPI.searchAndDecryptByTerms(
                        new String[]{"patient"}, medicalPassword, 20);
                    return new SearchResult("private", results.size(),
                        results.stream().allMatch(b -> b.getData().contains("Private")));
                } catch (Exception e) {
                    return new SearchResult("private", 0, false, e);
                }
            }));
        }

        // Collect results
        List<SearchResult> publicResults = new ArrayList<>();
        List<SearchResult> privateResults = new ArrayList<>();

        for (Future<SearchResult> future : futures) {
            SearchResult result = future.get(10, TimeUnit.SECONDS);
            if (result.department.equals("public")) {
                publicResults.add(result);
            } else {
                privateResults.add(result);
            }
        }

        executor.shutdown();

        // Verify all public searches found 10 public blocks
        for (SearchResult result : publicResults) {
            assertNull(result.exception, "Public search failed with exception");
            assertEquals(10, result.resultCount, "Public search should find exactly 10 blocks");
            assertTrue(result.foundExpectedContent, "Public search found non-public content!");
        }

        // Verify all private searches found 10 private blocks
        for (SearchResult result : privateResults) {
            assertNull(result.exception, "Private search failed with exception");
            assertEquals(10, result.resultCount, "Private search should find exactly 10 blocks");
            assertTrue(result.foundExpectedContent, "Private search found non-private content!");
        }
    }

    private static class SearchResult {
        String department;
        int resultCount;
        boolean foundExpectedContent;
        Exception exception;

        SearchResult(String dept, int count, boolean foundContent) {
            this.department = dept;
            this.resultCount = count;
            this.foundExpectedContent = foundContent;
            this.exception = null;
        }

        SearchResult(String dept, int count, boolean foundContent, Exception e) {
            this(dept, count, foundContent);
            this.exception = e;
        }
    }
}
