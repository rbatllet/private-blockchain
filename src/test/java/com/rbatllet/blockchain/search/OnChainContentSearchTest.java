package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.search.OnChainContentSearch.OnChainMatch;
import com.rbatllet.blockchain.search.OnChainContentSearch.OnChainSearchResult;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService.SecureEncryptedData;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;

/**
 * Comprehensive test suite for OnChainContentSearch
 *
 * Tests all scenarios:
 * - Plain text block search
 * - Encrypted block search with correct password
 * - Encrypted block search with wrong password
 * - Mixed content search
 * - Thread safety
 * - Performance under load
 * - Edge cases
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OnChainContentSearchTest {

    private OnChainContentSearch onChainSearch;
    private String testPassword;
    private PrivateKey testPrivateKey;

    @BeforeEach
    void setUp() throws Exception {
        onChainSearch = new OnChainContentSearch();
        testPassword = "TestPassword123!";

        KeyPair keyPair = CryptoUtil.generateKeyPair();
        testPrivateKey = keyPair.getPrivate();
    }

    @Test
    @Order(1)
    @DisplayName("🔍 Search in Plain Text Blocks")
    void testPlainTextBlockSearch() {
        System.out.println("🧪 Testing plain text block search...");

        List<Block> blocks = new ArrayList<>();

        // Create plain text blocks
        Block block1 = createPlainTextBlock(
            1L,
            "Medical record with patient diabetes information"
        );
        Block block2 = createPlainTextBlock(
            2L,
            "Financial transaction for medical supplies"
        );
        Block block3 = createPlainTextBlock(
            3L,
            "General hospital announcement"
        );

        blocks.add(block1);
        blocks.add(block2);
        blocks.add(block3);

        // Search for "medical"
        OnChainSearchResult result = onChainSearch.searchOnChainContent(
            blocks,
            "medical",
            testPassword,
            testPrivateKey,
            10
        );

        assertNotNull(result);
        assertEquals("medical", result.getSearchTerm());
        assertEquals(
            2,
            result.getMatchCount(),
            "Should find 'medical' in 2 blocks"
        );
        assertEquals(3, result.getTotalBlocksSearched());
        assertEquals(0, result.getEncryptedBlocksDecrypted());

        // Verify match details
        for (OnChainMatch match : result.getMatches()) {
            assertEquals("plain", match.getContentType());
            assertTrue(match.getMatchCount() > 0);
            assertFalse(match.getMatchingSnippets().isEmpty());

            // Check snippet contains highlighted match
            String snippet = match.getPreviewSnippet();
            assertTrue(
                snippet.contains("**medical**") ||
                snippet.contains("**Medical**")
            );
        }

        System.out.println(
            "✅ Plain text search: " + result.getSearchSummary()
        );
    }

    @Test
    @Order(2)
    @DisplayName("🔐 Search in Encrypted Blocks with Correct Password")
    void testEncryptedBlockSearchWithPassword() throws Exception {
        System.out.println(
            "🧪 Testing encrypted block search with correct password..."
        );

        List<Block> blocks = new ArrayList<>();

        // Create encrypted blocks
        Block block1 = createEncryptedBlock(
            1L,
            "Confidential patient diabetes diagnosis",
            testPassword
        );
        Block block2 = createEncryptedBlock(
            2L,
            "Private medical insurance claim",
            testPassword
        );
        Block block3 = createEncryptedBlock(
            3L,
            "Secret research data about cancer",
            testPassword
        );

        blocks.add(block1);
        blocks.add(block2);
        blocks.add(block3);

        // Search for "medical" with correct password
        OnChainSearchResult result = onChainSearch.searchOnChainContent(
            blocks,
            "medical",
            testPassword,
            testPrivateKey,
            10
        );

        assertNotNull(result);
        assertEquals(
            1,
            result.getMatchCount(),
            "Should find 'medical' in 1 encrypted block"
        );
        assertEquals(3, result.getTotalBlocksSearched());
        assertEquals(
            3,
            result.getEncryptedBlocksDecrypted(),
            "Should decrypt all 3 blocks"
        );

        // Verify encrypted match
        OnChainMatch match = result.getMatches().get(0);
        assertEquals("encrypted", match.getContentType());
        assertTrue(
            match.getRelevanceScore() > 10,
            "Encrypted matches get bonus score"
        );

        System.out.println(
            "✅ Encrypted search with password: " + result.getSearchSummary()
        );
    }

    @Test
    @Order(3)
    @DisplayName("❌ Search in Encrypted Blocks with Wrong Password")
    void testEncryptedBlockSearchWithWrongPassword() throws Exception {
        System.out.println(
            "🧪 Testing encrypted block search with wrong password..."
        );

        List<Block> blocks = new ArrayList<>();

        // Create encrypted blocks
        Block block1 = createEncryptedBlock(
            1L,
            "Confidential medical data",
            testPassword
        );
        Block block2 = createEncryptedBlock(
            2L,
            "Private patient records",
            testPassword
        );

        blocks.add(block1);
        blocks.add(block2);

        // Search with wrong password
        OnChainSearchResult result = onChainSearch.searchOnChainContent(
            blocks,
            "medical",
            "WrongPassword123",
            testPrivateKey,
            10
        );

        assertNotNull(result);
        assertEquals(
            0,
            result.getMatchCount(),
            "Should not find matches with wrong password"
        );
        assertEquals(2, result.getTotalBlocksSearched());
        assertEquals(
            0,
            result.getEncryptedBlocksDecrypted(),
            "Should not decrypt any blocks"
        );

        System.out.println(
            "✅ Wrong password test: " + result.getSearchSummary()
        );
    }

    @Test
    @Order(4)
    @DisplayName("🎯 Mixed Content Search (Plain + Encrypted)")
    void testMixedContentSearch() throws Exception {
        System.out.println("🧪 Testing mixed content search...");

        List<Block> blocks = new ArrayList<>();

        // Mix of plain and encrypted blocks
        blocks.add(createPlainTextBlock(1L, "Public medical announcement"));
        blocks.add(
            createEncryptedBlock(
                2L,
                "Confidential medical diagnosis",
                testPassword
            )
        );
        blocks.add(createPlainTextBlock(3L, "General hospital news"));
        blocks.add(
            createEncryptedBlock(
                4L,
                "Private medical insurance data",
                testPassword
            )
        );
        blocks.add(createPlainTextBlock(5L, "Public health medical advisory"));

        // Search for "medical"
        OnChainSearchResult result = onChainSearch.searchOnChainContent(
            blocks,
            "medical",
            testPassword,
            testPrivateKey,
            10
        );

        assertNotNull(result);
        assertEquals(
            4,
            result.getMatchCount(),
            "Should find in 2 plain + 2 encrypted"
        );
        assertEquals(5, result.getTotalBlocksSearched());
        assertEquals(2, result.getEncryptedBlocksDecrypted());

        // Verify mix of content types
        int plainCount = 0;
        int encryptedCount = 0;
        for (OnChainMatch match : result.getMatches()) {
            if ("plain".equals(match.getContentType())) plainCount++;
            if ("encrypted".equals(match.getContentType())) encryptedCount++;
        }

        assertEquals(2, plainCount, "Should have 2 plain text matches");
        assertEquals(2, encryptedCount, "Should have 2 encrypted matches");

        System.out.println(
            "✅ Mixed content search: " + result.getSearchSummary()
        );
    }

    @Test
    @Order(5)
    @DisplayName("🧵 Thread Safety Test")
    void testThreadSafety() throws Exception {
        System.out.println("🧪 Testing thread safety of on-chain search...");

        // Create test blocks
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                blocks.add(createPlainTextBlock(i, "Medical data block " + i));
            } else {
                blocks.add(
                    createEncryptedBlock(
                        i,
                        "Confidential medical record " + i,
                        testPassword
                    )
                );
            }
        }

        // Concurrent search test
        int threadCount = 10;
        int searchesPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int s = 0; s < searchesPerThread; s++) {
                        String searchTerm = (s % 2 == 0)
                            ? "medical"
                            : "confidential";

                        OnChainSearchResult result =
                            onChainSearch.searchOnChainContent(
                                blocks,
                                searchTerm,
                                testPassword,
                                testPrivateKey,
                                10
                            );

                        if (result != null && result.getMatchCount() > 0) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println(
                        "❌ Thread " + threadId + " error: " + e.getMessage()
                    );
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion - increase timeout for CI environments
        assertTrue(
            latch.await(60, TimeUnit.SECONDS),
            "All threads should complete"
        );
        executor.shutdown();

        // Validate results
        int totalSearches = threadCount * searchesPerThread;
        assertEquals(
            totalSearches,
            successCount.get(),
            "All searches should succeed"
        );
        assertEquals(0, errorCount.get(), "No errors should occur");

        System.out.println("✅ Thread safety test passed:");
        System.out.println("   🔍 Total searches: " + totalSearches);
        System.out.println("   ✅ Successful: " + successCount.get());
        System.out.println("   ❌ Errors: " + errorCount.get());
    }

    @Test
    @Order(6)
    @DisplayName("⚡ Performance Test")
    void testPerformance() throws Exception {
        System.out.println("🧪 Testing search performance...");

        // Create larger dataset
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            if (i % 3 == 0) {
                blocks.add(
                    createPlainTextBlock(
                        i,
                        "Medical record " + i + " with patient data"
                    )
                );
            } else if (i % 3 == 1) {
                blocks.add(
                    createEncryptedBlock(
                        i,
                        "Confidential medical diagnosis " + i,
                        testPassword
                    )
                );
            } else {
                blocks.add(
                    createPlainTextBlock(i, "General hospital information " + i)
                );
            }
        }

        // Measure search time
        long startTime = System.nanoTime();
        OnChainSearchResult result = onChainSearch.searchOnChainContent(
            blocks,
            "medical",
            testPassword,
            testPrivateKey,
            50
        );
        long endTime = System.nanoTime();

        double searchTimeMs = (endTime - startTime) / 1_000_000.0;

        assertNotNull(result);
        assertTrue(result.getMatchCount() > 0);
        assertTrue(
            result.getTotalBlocksSearched() >= 74,
            "Should search at least 74 blocks, found: " +
            result.getTotalBlocksSearched()
        );

        System.out.println("⚡ Performance results:");
        System.out.println(
            "   📊 Blocks searched: " + result.getTotalBlocksSearched()
        );
        System.out.println(
            "   🔐 Encrypted blocks: " + result.getEncryptedBlocksDecrypted()
        );
        System.out.println("   🎯 Matches found: " + result.getMatchCount());
        System.out.println(
            "   ⏱️ Search time: " + String.format("%.2f", searchTimeMs) + "ms"
        );
        System.out.println(
            "   📈 Per block: " +
            String.format("%.2f", searchTimeMs / 100) +
            "ms"
        );

        // Performance assertion - allow more time for CI environments
        assertTrue(
            searchTimeMs < 3000,
            "Search should complete within 3 seconds for 100 blocks"
        );
    }

    @Test
    @Order(7)
    @DisplayName("🔍 Case Sensitivity and Special Characters")
    void testCaseSensitivityAndSpecialChars() {
        System.out.println(
            "🧪 Testing case sensitivity and special characters..."
        );

        List<Block> blocks = new ArrayList<>();
        blocks.add(createPlainTextBlock(1L, "MEDICAL emergency"));
        blocks.add(createPlainTextBlock(2L, "Medical treatment"));
        blocks.add(createPlainTextBlock(3L, "medical diagnosis"));
        blocks.add(createPlainTextBlock(4L, "MeDiCaL record"));

        // Search should be case-insensitive
        OnChainSearchResult result = onChainSearch.searchOnChainContent(
            blocks,
            "medical",
            testPassword,
            testPrivateKey,
            10
        );

        assertEquals(
            4,
            result.getMatchCount(),
            "Should find all case variations"
        );

        // Test with special characters
        Block specialBlock = createPlainTextBlock(
            5L,
            "medical@hospital.com $medical-cost"
        );
        blocks.add(specialBlock);

        result = onChainSearch.searchOnChainContent(
            blocks,
            "medical",
            testPassword,
            testPrivateKey,
            10
        );

        assertEquals(
            5,
            result.getMatchCount(),
            "Should find medical in special contexts"
        );

        System.out.println("✅ Case sensitivity test passed");
    }

    @Test
    @Order(8)
    @DisplayName("🛡️ Edge Cases")
    void testEdgeCases() {
        System.out.println("🧪 Testing edge cases...");

        // Empty block list
        OnChainSearchResult result = onChainSearch.searchOnChainContent(
            new ArrayList<>(),
            "test",
            testPassword,
            testPrivateKey,
            10
        );
        assertNotNull(result);
        assertEquals(0, result.getMatchCount());

        // Null search term
        result = onChainSearch.searchOnChainContent(
            List.of(createPlainTextBlock(1L, "test")),
            null,
            testPassword,
            testPrivateKey,
            10
        );
        assertNotNull(result);
        assertEquals(0, result.getMatchCount());

        // Empty search term
        result = onChainSearch.searchOnChainContent(
            List.of(createPlainTextBlock(1L, "test")),
            "",
            testPassword,
            testPrivateKey,
            10
        );
        assertNotNull(result);
        assertEquals(0, result.getMatchCount());

        // Block with null data
        Block nullDataBlock = new Block();
        nullDataBlock.setBlockNumber(1L);
        nullDataBlock.setHash("null-data-hash");
        nullDataBlock.setData(null);

        result = onChainSearch.searchOnChainContent(
            List.of(nullDataBlock),
            "test",
            testPassword,
            testPrivateKey,
            10
        );
        assertNotNull(result);
        assertEquals(0, result.getMatchCount());

        System.out.println("✅ Edge cases handled correctly");
    }

    // Helper methods

    private Block createPlainTextBlock(long blockNumber, String data) {
        Block block = new Block();
        block.setBlockNumber(blockNumber);
        block.setHash("plain-hash-" + blockNumber);
        block.setData(data);
        block.setIsEncrypted(false);
        return block;
    }

    private Block createEncryptedBlock(
        long blockNumber,
        String data,
        String password
    ) throws Exception {
        Block block = new Block();
        block.setBlockNumber(blockNumber);
        block.setHash("encrypted-hash-" + blockNumber);
        block.setIsEncrypted(true);

        // Encrypt data using SecureBlockEncryptionService
        SecureEncryptedData encrypted =
            SecureBlockEncryptionService.encryptWithPassword(data, password);

        // Store in encryption metadata format
        String metadata = String.format(
            "%d|%s|%s|%s|%s",
            encrypted.getTimestamp(),
            encrypted.getSalt(),
            encrypted.getIv(),
            encrypted.getEncryptedData(),
            encrypted.getDataHash()
        );

        block.setEncryptionMetadata(metadata);
        block.setData("ENCRYPTED"); // Placeholder

        return block;
    }
}
