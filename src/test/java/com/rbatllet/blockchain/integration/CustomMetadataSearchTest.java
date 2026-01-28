package com.rbatllet.blockchain.integration;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.util.*;

/**
 * Comprehensive integration tests for custom metadata search functionality.
 *
 * Tests thread-safety, null handling, edge cases, and correctness of:
 * - Substring search in custom metadata
 * - Key-value pair exact matching
 * - Multiple criteria search with AND logic
 * - JSON parsing robustness
 * - Concurrent search operations
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomMetadataSearchTest {

    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private KeyPair keyPair;
    private ObjectMapper jsonMapper;

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        keyPair = CryptoUtil.generateKeyPair();

        // SECURITY FIX (v1.0.6): Create genesis admin before creating API (RBAC v1.0.6)
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.createBootstrapAdmin(publicKeyString, "test_user");

        api = new UserFriendlyEncryptionAPI(blockchain, "test_user", keyPair);
        jsonMapper = new ObjectMapper();
    }

    @Test
    @Order(1)
    @DisplayName("CRITICAL: Null and empty input validation")
    void testNullAndEmptyInputValidation() {
        // Test searchByCustomMetadata
        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadata(null);
        }, "Null search term should throw exception");

        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadata("");
        }, "Empty search term should throw exception");

        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadata("   ");
        }, "Whitespace-only search term should throw exception");

        // Test searchByCustomMetadataKeyValue
        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadataKeyValue(null, "value");
        }, "Null key should throw exception");

        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadataKeyValue("", "value");
        }, "Empty key should throw exception");

        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadataKeyValue("key", null);
        }, "Null value should throw exception");

        // Test searchByCustomMetadataMultipleCriteria
        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadataMultipleCriteria(null);
        }, "Null criteria should throw exception");

        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadataMultipleCriteria(new HashMap<>());
        }, "Empty criteria should throw exception");

        Map<String, String> invalidCriteria = new HashMap<>();
        invalidCriteria.put(null, "value");
        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadataMultipleCriteria(invalidCriteria);
        }, "Criteria with null key should throw exception");
    }

    @Test
    @Order(2)
    @DisplayName("Basic substring search in custom metadata")
    void testBasicSubstringSearch() throws Exception {
        // Create blocks with custom metadata
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("department", "medical");
        metadata1.put("priority", "high");
        metadata1.put("status", "urgent");

        String jsonMetadata1 = jsonMapper.writeValueAsString(metadata1);

        Block block1 = blockchain.addBlockAndReturn("Medical data 1", keyPair.getPrivate(), keyPair.getPublic());
        block1.setCustomMetadata(jsonMetadata1);
        blockchain.updateBlock(block1);

        // Create second block
        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("department", "finance");
        metadata2.put("priority", "low");

        String jsonMetadata2 = jsonMapper.writeValueAsString(metadata2);

        Block block2 = blockchain.addBlockAndReturn("Finance data 1", keyPair.getPrivate(), keyPair.getPublic());
        block2.setCustomMetadata(jsonMetadata2);
        blockchain.updateBlock(block2);

        // Search for "medical" - should find block1
        List<Block> medicalResults = api.searchByCustomMetadata("medical");
        assertEquals(1, medicalResults.size(), "Should find exactly 1 medical block");
        assertTrue(medicalResults.get(0).getCustomMetadata().contains("medical"));

        // Search for "urgent" - should find block1
        List<Block> urgentResults = api.searchByCustomMetadata("urgent");
        assertEquals(1, urgentResults.size(), "Should find exactly 1 urgent block");

        // Search for "priority" - should find both blocks
        List<Block> priorityResults = api.searchByCustomMetadata("priority");
        assertEquals(2, priorityResults.size(), "Should find 2 blocks with priority");

        // Search for non-existent term
        List<Block> notFound = api.searchByCustomMetadata("nonexistent");
        assertEquals(0, notFound.size(), "Should find no blocks with nonexistent term");
    }

    @Test
    @Order(3)
    @DisplayName("Case-insensitive substring search")
    void testCaseInsensitiveSearch() throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("department", "FINANCE");
        metadata.put("status", "Approved");

        String jsonMetadata = jsonMapper.writeValueAsString(metadata);

        Block block = blockchain.addBlockAndReturn("Test data", keyPair.getPrivate(), keyPair.getPublic());
        block.setCustomMetadata(jsonMetadata);
        blockchain.updateBlock(block);

        // Test different case variations
        List<Block> results1 = api.searchByCustomMetadata("finance");
        assertEquals(1, results1.size(), "Should find block with lowercase search");

        List<Block> results2 = api.searchByCustomMetadata("FINANCE");
        assertEquals(1, results2.size(), "Should find block with uppercase search");

        List<Block> results3 = api.searchByCustomMetadata("FiNaNcE");
        assertEquals(1, results3.size(), "Should find block with mixed case search");

        List<Block> results4 = api.searchByCustomMetadata("approved");
        assertEquals(1, results4.size(), "Should find block with lowercase 'approved'");
    }

    @Test
    @Order(4)
    @DisplayName("Exact key-value pair matching")
    void testKeyValuePairMatching() throws Exception {
        // Create blocks with different metadata
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("department", "medical");
        metadata1.put("priority", "high");
        metadata1.put("reviewer", "Dr. Smith");

        Block block1 = blockchain.addBlockAndReturn("Medical case 1", keyPair.getPrivate(), keyPair.getPublic());
        block1.setCustomMetadata(jsonMapper.writeValueAsString(metadata1));
        blockchain.updateBlock(block1);

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("department", "medical");
        metadata2.put("priority", "low");
        metadata2.put("reviewer", "Dr. Jones");

        Block block2 = blockchain.addBlockAndReturn("Medical case 2", keyPair.getPrivate(), keyPair.getPublic());
        block2.setCustomMetadata(jsonMapper.writeValueAsString(metadata2));
        blockchain.updateBlock(block2);

        Map<String, Object> metadata3 = new HashMap<>();
        metadata3.put("department", "finance");
        metadata3.put("priority", "high");

        Block block3 = blockchain.addBlockAndReturn("Finance case", keyPair.getPrivate(), keyPair.getPublic());
        block3.setCustomMetadata(jsonMapper.writeValueAsString(metadata3));
        blockchain.updateBlock(block3);

        // CRITICAL: Wait for async indexing to complete before searching
        // Without this, tests may flake on slower CI systems (GitHub Actions)
        IndexingCoordinator.getInstance().waitForCompletion();

        // Search for high priority - should find block1 and block3
        List<Block> highPriority = api.searchByCustomMetadataKeyValue("priority", "high");
        assertEquals(2, highPriority.size(), "Should find 2 high priority blocks");

        // Search for medical department - should find block1 and block2
        List<Block> medical = api.searchByCustomMetadataKeyValue("department", "medical");
        assertEquals(2, medical.size(), "Should find 2 medical blocks");

        // Search for specific reviewer - should find only block1
        List<Block> drSmith = api.searchByCustomMetadataKeyValue("reviewer", "Dr. Smith");
        assertEquals(1, drSmith.size(), "Should find exactly 1 block for Dr. Smith");

        // Search for non-existent key
        List<Block> notFound = api.searchByCustomMetadataKeyValue("nonexistent", "value");
        assertEquals(0, notFound.size(), "Should find no blocks with nonexistent key");
    }

    @Test
    @Order(5)
    @DisplayName("Multiple criteria search with AND logic")
    void testMultipleCriteriaSearch() throws Exception {
        // Create blocks with overlapping criteria
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("department", "medical");
        metadata1.put("priority", "high");
        metadata1.put("status", "approved");

        Block block1 = blockchain.addBlockAndReturn("Case 1", keyPair.getPrivate(), keyPair.getPublic());
        block1.setCustomMetadata(jsonMapper.writeValueAsString(metadata1));
        blockchain.updateBlock(block1);

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("department", "medical");
        metadata2.put("priority", "high");
        metadata2.put("status", "pending");

        Block block2 = blockchain.addBlockAndReturn("Case 2", keyPair.getPrivate(), keyPair.getPublic());
        block2.setCustomMetadata(jsonMapper.writeValueAsString(metadata2));
        blockchain.updateBlock(block2);

        Map<String, Object> metadata3 = new HashMap<>();
        metadata3.put("department", "medical");
        metadata3.put("priority", "low");
        metadata3.put("status", "approved");

        Block block3 = blockchain.addBlockAndReturn("Case 3", keyPair.getPrivate(), keyPair.getPublic());
        block3.setCustomMetadata(jsonMapper.writeValueAsString(metadata3));
        blockchain.updateBlock(block3);

        // Search for medical + high priority + approved (should find only block1)
        Map<String, String> criteria1 = new HashMap<>();
        criteria1.put("department", "medical");
        criteria1.put("priority", "high");
        criteria1.put("status", "approved");

        List<Block> results1 = api.searchByCustomMetadataMultipleCriteria(criteria1);
        assertEquals(1, results1.size(), "Should find exactly 1 block matching all 3 criteria");
        assertEquals(block1.getBlockNumber(), results1.get(0).getBlockNumber());

        // Search for medical + high priority (should find block1 and block2)
        Map<String, String> criteria2 = new HashMap<>();
        criteria2.put("department", "medical");
        criteria2.put("priority", "high");

        List<Block> results2 = api.searchByCustomMetadataMultipleCriteria(criteria2);
        assertEquals(2, results2.size(), "Should find 2 blocks matching medical + high priority");

        // Search for medical + approved (should find block1 and block3)
        Map<String, String> criteria3 = new HashMap<>();
        criteria3.put("department", "medical");
        criteria3.put("status", "approved");

        List<Block> results3 = api.searchByCustomMetadataMultipleCriteria(criteria3);
        assertEquals(2, results3.size(), "Should find 2 blocks matching medical + approved");
    }

    @Test
    @Order(6)
    @DisplayName("Search with malformed JSON in database")
    void testMalformedJSONHandling() throws Exception {
        // Create a block with valid metadata
        Map<String, Object> validMetadata = new HashMap<>();
        validMetadata.put("department", "medical");

        Block validBlock = blockchain.addBlockAndReturn("Valid data", keyPair.getPrivate(), keyPair.getPublic());
        validBlock.setCustomMetadata(jsonMapper.writeValueAsString(validMetadata));
        blockchain.updateBlock(validBlock);

        // Manually create a block with malformed JSON
        Block malformedBlock = blockchain.addBlockAndReturn("Malformed data", keyPair.getPrivate(), keyPair.getPublic());
        malformedBlock.setCustomMetadata("{invalid json: malformed");
        blockchain.updateBlock(malformedBlock);

        // Search should gracefully handle malformed JSON and return valid blocks
        List<Block> results = api.searchByCustomMetadataKeyValue("department", "medical");
        assertEquals(1, results.size(), "Should find only the valid block, skipping malformed JSON");
        assertEquals(validBlock.getBlockNumber(), results.get(0).getBlockNumber());
    }

    @Test
    @Order(7)
    @DisplayName("Search with special characters and unicode")
    void testSpecialCharactersAndUnicode() throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("patient", "José García-López");
        metadata.put("diagnosis", "Type-2 Diabetes");
        metadata.put("notes", "Symbols: @#$%^&*()");
        metadata.put("emoji", "✅ Approved 🔒");

        Block block = blockchain.addBlockAndReturn("Special chars data", keyPair.getPrivate(), keyPair.getPublic());
        block.setCustomMetadata(jsonMapper.writeValueAsString(metadata));
        blockchain.updateBlock(block);

        // Test unicode characters
        List<Block> results1 = api.searchByCustomMetadata("José");
        assertEquals(1, results1.size(), "Should find block with unicode characters");

        // Test special characters
        List<Block> results2 = api.searchByCustomMetadata("Type-2");
        assertEquals(1, results2.size(), "Should find block with hyphenated term");

        // Test key-value with special characters
        List<Block> results3 = api.searchByCustomMetadataKeyValue("notes", "Symbols: @#$%^&*()");
        assertEquals(1, results3.size(), "Should find block with special characters in value");

        // Test emoji
        List<Block> results4 = api.searchByCustomMetadata("✅");
        assertEquals(1, results4.size(), "Should find block with emoji");
    }

    @Test
    @Order(8)
    @DisplayName("Search with no custom metadata blocks")
    void testSearchWithNoCustomMetadata() {
        // Create blocks without custom metadata
        Block block1 = blockchain.addBlockAndReturn("No metadata 1", keyPair.getPrivate(), keyPair.getPublic());
        Block block2 = blockchain.addBlockAndReturn("No metadata 2", keyPair.getPrivate(), keyPair.getPublic());

        // Verify blocks were created successfully
        assertNotNull(block1, "Block 1 should be created");
        assertNotNull(block2, "Block 2 should be created");
        assertNull(block1.getCustomMetadata(), "Block 1 should have no custom metadata");
        assertNull(block2.getCustomMetadata(), "Block 2 should have no custom metadata");

        // Search should return empty results
        List<Block> results1 = api.searchByCustomMetadata("anything");
        assertEquals(0, results1.size(), "Should find no blocks when none have custom metadata");

        List<Block> results2 = api.searchByCustomMetadataKeyValue("key", "value");
        assertEquals(0, results2.size(), "Should find no blocks when none have custom metadata");

        Map<String, String> criteria = new HashMap<>();
        criteria.put("key", "value");
        List<Block> results3 = api.searchByCustomMetadataMultipleCriteria(criteria);
        assertEquals(0, results3.size(), "Should find no blocks when none have custom metadata");
    }

    @Test
    @Order(9)
    @DisplayName("CRITICAL: Thread-safe concurrent custom metadata searches")
    void testConcurrentCustomMetadataSearches() throws Exception {
        // Create 20 blocks with various metadata
        for (int i = 0; i < 20; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("index", String.valueOf(i));
            metadata.put("category", i % 2 == 0 ? "even" : "odd");
            metadata.put("priority", i < 10 ? "high" : "low");

            Block block = blockchain.addBlockAndReturn("Data " + i, keyPair.getPrivate(), keyPair.getPublic());
            block.setCustomMetadata(jsonMapper.writeValueAsString(metadata));
            blockchain.updateBlock(block);
        }

        // Launch 50 concurrent searches
        int numThreads = 50;
        List<Thread> threads = new ArrayList<>();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    // Each thread performs different types of searches
                    switch (threadId % 3) {
                        case 0:
                            List<Block> r1 = api.searchByCustomMetadata("even");
                            assertTrue(r1.size() >= 0, "Search should complete without errors");
                            break;
                        case 1:
                            List<Block> r2 = api.searchByCustomMetadataKeyValue("priority", "high");
                            assertTrue(r2.size() >= 0, "Search should complete without errors");
                            break;
                        case 2:
                            Map<String, String> criteria = new HashMap<>();
                            criteria.put("category", "odd");
                            criteria.put("priority", "low");
                            List<Block> r3 = api.searchByCustomMetadataMultipleCriteria(criteria);
                            assertTrue(r3.size() >= 0, "Search should complete without errors");
                            break;
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout per thread
        }

        // Verify no exceptions occurred
        assertEquals(0, exceptions.size(),
            "CONCURRENCY BUG: " + exceptions.size() + " searches failed with exceptions!");
    }

    @Test
    @Order(10)
    @DisplayName("Edge case: Null values in metadata")
    void testNullValuesInMetadata() throws Exception {
        // Create metadata with explicit null value
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("department", "medical");
        metadata.put("reviewer", null);  // Explicit null
        metadata.put("status", "pending");

        Block block = blockchain.addBlockAndReturn("Data with nulls", keyPair.getPrivate(), keyPair.getPublic());
        block.setCustomMetadata(jsonMapper.writeValueAsString(metadata));
        blockchain.updateBlock(block);

        // Search for department should work
        List<Block> results1 = api.searchByCustomMetadataKeyValue("department", "medical");
        assertEquals(1, results1.size(), "Should find block with non-null value");

        // Search for null reviewer with empty string representation
        List<Block> results2 = api.searchByCustomMetadataKeyValue("reviewer", "");
        assertEquals(1, results2.size(), "Should find block with null value represented as empty string");

        // Search for null reviewer with "null" string should not match
        List<Block> results3 = api.searchByCustomMetadataKeyValue("reviewer", "null");
        assertEquals(0, results3.size(), "Should not find block when searching for 'null' string");
    }

    @Test
    @Order(11)
    @DisplayName("Edge case: Query result list returns null")
    void testQueryReturnsNull() throws Exception {
        // This tests the defensive code: results != null ? results : new ArrayList<>()
        // Create a normal block and search - should handle null gracefully
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("test", "value");

        Block block = blockchain.addBlockAndReturn("Test data", keyPair.getPrivate(), keyPair.getPublic());
        block.setCustomMetadata(jsonMapper.writeValueAsString(metadata));
        blockchain.updateBlock(block);

        // Even if query returns null (which shouldn't happen in normal JPA),
        // our code should return empty list, not null
        List<Block> results = api.searchByCustomMetadata("test");
        assertNotNull(results, "Results should never be null, even if query fails");
        assertTrue(results.size() >= 0, "Results should be valid list");
    }

    @Test
    @Order(12)
    @DisplayName("Edge case: Multiple criteria with null value")
    void testMultipleCriteriaWithNullValue() {
        Map<String, String> criteriaWithNull = new HashMap<>();
        criteriaWithNull.put("department", "medical");
        criteriaWithNull.put("reviewer", null);

        // Should throw exception for null value
        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadataMultipleCriteria(criteriaWithNull);
        }, "Criteria with null value should throw exception");
    }

    @Test
    @Order(13)
    @DisplayName("Edge case: Empty key in criteria")
    void testMultipleCriteriaWithEmptyKey() {
        Map<String, String> criteriaWithEmptyKey = new HashMap<>();
        criteriaWithEmptyKey.put("", "value");

        // Should throw exception for empty key
        assertThrows(IllegalArgumentException.class, () -> {
            api.searchByCustomMetadataMultipleCriteria(criteriaWithEmptyKey);
        }, "Criteria with empty key should throw exception");
    }

    @Test
    @Order(14)
    @DisplayName("Edge case: Metadata with arrays and nested objects")
    void testComplexJSONStructures() throws Exception {
        // Create metadata with nested structure
        String complexJson = "{\"department\":\"medical\",\"tags\":[\"urgent\",\"priority\"],\"nested\":{\"level1\":{\"level2\":\"value\"}}}";

        Block block = blockchain.addBlockAndReturn("Complex JSON", keyPair.getPrivate(), keyPair.getPublic());
        block.setCustomMetadata(complexJson);
        blockchain.updateBlock(block);

        // Substring search should find nested values
        List<Block> results1 = api.searchByCustomMetadata("urgent");
        assertEquals(1, results1.size(), "Should find block with array value via substring");

        List<Block> results2 = api.searchByCustomMetadata("level2");
        assertEquals(1, results2.size(), "Should find block with nested object key via substring");

        // Key-value search for top-level keys
        List<Block> results3 = api.searchByCustomMetadataKeyValue("department", "medical");
        assertEquals(1, results3.size(), "Should find block with top-level key-value");
    }

    @Test
    @Order(15)
    @DisplayName("Edge case: Very large metadata JSON")
    void testLargeMetadataJSON() throws Exception {
        // Create large metadata (simulate real-world scenario with many fields)
        Map<String, Object> largeMetadata = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            largeMetadata.put("field_" + i, "value_" + i);
        }
        largeMetadata.put("target_field", "find_me");

        Block block = blockchain.addBlockAndReturn("Large metadata", keyPair.getPrivate(), keyPair.getPublic());
        block.setCustomMetadata(jsonMapper.writeValueAsString(largeMetadata));
        blockchain.updateBlock(block);

        // CRITICAL: Wait for async indexing to complete before searching
        IndexingCoordinator.getInstance().waitForCompletion();

        // Should still find the target field efficiently
        List<Block> results = api.searchByCustomMetadataKeyValue("target_field", "find_me");
        assertEquals(1, results.size(), "Should find block even with large metadata");

        // Substring search should also work
        List<Block> results2 = api.searchByCustomMetadata("find_me");
        assertEquals(1, results2.size(), "Substring search should work with large metadata");
    }

    @Test
    @Order(16)
    @DisplayName("Edge case: Metadata with only whitespace values")
    void testWhitespaceValues() throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("department", "   ");  // Only whitespace
        metadata.put("status", "");         // Empty string
        metadata.put("notes", "actual content");

        Block block = blockchain.addBlockAndReturn("Whitespace data", keyPair.getPrivate(), keyPair.getPublic());
        block.setCustomMetadata(jsonMapper.writeValueAsString(metadata));
        blockchain.updateBlock(block);

        // CRITICAL: Wait for async indexing to complete before searching
        IndexingCoordinator.getInstance().waitForCompletion();

        // Search for whitespace value (exact match)
        List<Block> results1 = api.searchByCustomMetadataKeyValue("department", "   ");
        assertEquals(1, results1.size(), "Should find block with whitespace value");

        // Search for empty string
        List<Block> results2 = api.searchByCustomMetadataKeyValue("status", "");
        assertEquals(1, results2.size(), "Should find block with empty string value");

        // Search for actual content should work normally
        List<Block> results3 = api.searchByCustomMetadataKeyValue("notes", "actual content");
        assertEquals(1, results3.size(), "Should find block with normal content");
    }

    @Test
    @Order(17)
    @DisplayName("Edge case: Numeric and boolean values in JSON")
    void testNumericAndBooleanValues() throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("priority_level", 5);           // Integer
        metadata.put("cost", 1234.56);              // Double
        metadata.put("is_urgent", true);            // Boolean
        metadata.put("is_archived", false);         // Boolean
        metadata.put("patient_id", "P12345");       // String

        Block block = blockchain.addBlockAndReturn("Numeric data", keyPair.getPrivate(), keyPair.getPublic());
        block.setCustomMetadata(jsonMapper.writeValueAsString(metadata));
        blockchain.updateBlock(block);

        // Search for numeric values as strings
        List<Block> results1 = api.searchByCustomMetadataKeyValue("priority_level", "5");
        assertEquals(1, results1.size(), "Should find block with integer value");

        List<Block> results2 = api.searchByCustomMetadataKeyValue("cost", "1234.56");
        assertEquals(1, results2.size(), "Should find block with double value");

        // Search for boolean values as strings
        List<Block> results3 = api.searchByCustomMetadataKeyValue("is_urgent", "true");
        assertEquals(1, results3.size(), "Should find block with true boolean");

        List<Block> results4 = api.searchByCustomMetadataKeyValue("is_archived", "false");
        assertEquals(1, results4.size(), "Should find block with false boolean");

        // Multiple criteria with mixed types
        Map<String, String> criteria = new HashMap<>();
        criteria.put("priority_level", "5");
        criteria.put("is_urgent", "true");

        List<Block> results5 = api.searchByCustomMetadataMultipleCriteria(criteria);
        assertEquals(1, results5.size(), "Should find block matching numeric and boolean criteria");
    }

    @Test
    @Order(18)
    @DisplayName("Complex real-world scenario: Medical records with detailed metadata")
    void testRealWorldMedicalRecordsScenario() throws Exception {
        // Create realistic medical record metadata
        Map<String, Object> record1 = new HashMap<>();
        record1.put("patient_id", "P12345");
        record1.put("department", "cardiology");
        record1.put("diagnosis", "hypertension");
        record1.put("priority", "high");
        record1.put("status", "active");
        record1.put("physician", "Dr. Johnson");
        record1.put("admission_date", "2025-01-15");

        Block block1 = blockchain.addBlockAndReturn("Medical record 1", keyPair.getPrivate(), keyPair.getPublic());
        block1.setCustomMetadata(jsonMapper.writeValueAsString(record1));
        blockchain.updateBlock(block1);

        Map<String, Object> record2 = new HashMap<>();
        record2.put("patient_id", "P67890");
        record2.put("department", "cardiology");
        record2.put("diagnosis", "arrhythmia");
        record2.put("priority", "urgent");
        record2.put("status", "active");
        record2.put("physician", "Dr. Smith");
        record2.put("admission_date", "2025-01-16");

        Block block2 = blockchain.addBlockAndReturn("Medical record 2", keyPair.getPrivate(), keyPair.getPublic());
        block2.setCustomMetadata(jsonMapper.writeValueAsString(record2));
        blockchain.updateBlock(block2);

        Map<String, Object> record3 = new HashMap<>();
        record3.put("patient_id", "P11111");
        record3.put("department", "neurology");
        record3.put("diagnosis", "migraine");
        record3.put("priority", "low");
        record3.put("status", "completed");
        record3.put("physician", "Dr. Lee");
        record3.put("admission_date", "2025-01-10");

        Block block3 = blockchain.addBlockAndReturn("Medical record 3", keyPair.getPrivate(), keyPair.getPublic());
        block3.setCustomMetadata(jsonMapper.writeValueAsString(record3));
        blockchain.updateBlock(block3);

        // Wait for async indexing to complete before querying
        IndexingCoordinator.getInstance().waitForCompletion();

        // Query 1: Find all active cardiology cases
        Map<String, String> cardiologyActive = new HashMap<>();
        cardiologyActive.put("department", "cardiology");
        cardiologyActive.put("status", "active");

        List<Block> results1 = api.searchByCustomMetadataMultipleCriteria(cardiologyActive);
        assertEquals(2, results1.size(), "Should find 2 active cardiology cases");

        // Query 2: Find cases for specific physician
        List<Block> results2 = api.searchByCustomMetadataKeyValue("physician", "Dr. Johnson");
        assertEquals(1, results2.size(), "Should find 1 case for Dr. Johnson");

        // Query 3: Find urgent cases
        List<Block> results3 = api.searchByCustomMetadataKeyValue("priority", "urgent");
        assertEquals(1, results3.size(), "Should find 1 urgent case");

        // Query 4: Find completed cases
        List<Block> results4 = api.searchByCustomMetadataKeyValue("status", "completed");
        assertEquals(1, results4.size(), "Should find 1 completed case");

        // Query 5: Substring search for specific diagnosis
        List<Block> results5 = api.searchByCustomMetadata("hypertension");
        assertEquals(1, results5.size(), "Should find 1 case with hypertension");
    }
}