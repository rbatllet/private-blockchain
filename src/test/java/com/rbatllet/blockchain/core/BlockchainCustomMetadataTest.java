package com.rbatllet.blockchain.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.CustomMetadataUtil;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

/**
 * Test suite for addBlockWithKeywordsAndMetadata method
 */
class BlockchainCustomMetadataTest {

    private Blockchain blockchain;
    private KeyPair testKeyPair;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        testKeyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.createBootstrapAdmin(publicKey, "TEST_ADMIN");
    }

    @AfterEach
    void tearDown() {
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
        }
    }

    @Test
    void testAddBlockWithKeywordsAndMetadata_AllParameters() {
        // Arrange
        String[] keywords = {"test", "metadata", "blockchain"};
        String category = "TEST_CATEGORY";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("author", "Test User");
        metadata.put("version", "1.0");
        metadata.put("priority", "high");

        // Act
        Block block = blockchain.addBlockWithKeywordsAndMetadata(
            "Test block content",
            keywords,
            category,
            metadata,
            testKeyPair.getPrivate(),
            testKeyPair.getPublic()
        );

        // Assert
        assertNotNull(block, "Block should be created");
        assertEquals("Test block content", block.getData());
        assertEquals(category, block.getContentCategory());
        assertNotNull(block.getCustomMetadata(), "Custom metadata should be set");

        // Verify metadata content
        Map<String, String> deserializedMetadata = CustomMetadataUtil.deserializeMetadata(block.getCustomMetadata());
        assertEquals(3, deserializedMetadata.size());
        assertEquals("Test User", deserializedMetadata.get("author"));
        assertEquals("1.0", deserializedMetadata.get("version"));
        assertEquals("high", deserializedMetadata.get("priority"));
    }

    @Test
    void testAddBlockWithKeywordsAndMetadata_NullMetadata() {
        // Arrange
        String[] keywords = {"test"};
        String category = "TEST";

        // Act
        Block block = blockchain.addBlockWithKeywordsAndMetadata(
            "Test content",
            keywords,
            category,
            null,  // null metadata
            testKeyPair.getPrivate(),
            testKeyPair.getPublic()
        );

        // Assert
        assertNotNull(block);
        assertNull(block.getCustomMetadata(), "Metadata should be null when not provided");
    }

    @Test
    void testAddBlockWithKeywordsAndMetadata_EmptyMetadata() {
        // Arrange
        Map<String, String> emptyMetadata = new HashMap<>();

        // Act
        Block block = blockchain.addBlockWithKeywordsAndMetadata(
            "Test content",
            null,
            null,
            emptyMetadata,  // empty metadata
            testKeyPair.getPrivate(),
            testKeyPair.getPublic()
        );

        // Assert
        assertNotNull(block);
        assertNull(block.getCustomMetadata(), "Metadata should be null when empty map provided");
    }

    @Test
    void testAddBlockWithKeywordsAndMetadata_OnlyMetadata() {
        // Arrange
        Map<String, String> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");

        // Act
        Block block = blockchain.addBlockWithKeywordsAndMetadata(
            "Test content",
            null,  // no keywords
            null,  // no category
            metadata,
            testKeyPair.getPrivate(),
            testKeyPair.getPublic()
        );

        // Assert
        assertNotNull(block);
        assertNull(block.getContentCategory());
        assertNotNull(block.getCustomMetadata());

        Map<String, String> deserializedMetadata = CustomMetadataUtil.deserializeMetadata(block.getCustomMetadata());
        assertEquals(2, deserializedMetadata.size());
        assertEquals("value1", deserializedMetadata.get("key1"));
        assertEquals("value2", deserializedMetadata.get("key2"));
    }

    @Test
    void testAddBlockWithKeywordsAndMetadata_WithSkipAutoIndexing() {
        // Arrange
        Map<String, String> metadata = new HashMap<>();
        metadata.put("test", "value");

        // Act
        Block block = blockchain.addBlockWithKeywordsAndMetadata(
            "Test content",
            new String[]{"keyword1"},
            "CATEGORY",
            metadata,
            testKeyPair.getPrivate(),
            testKeyPair.getPublic(),
            true  // skip auto-indexing
        );

        // Assert
        assertNotNull(block);
        assertNotNull(block.getCustomMetadata());
        
        Map<String, String> deserializedMetadata = CustomMetadataUtil.deserializeMetadata(block.getCustomMetadata());
        assertEquals("value", deserializedMetadata.get("test"));
    }
}
