package com.rbatllet.blockchain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test field validation limits in Block entity to prevent SQL overflow.
 *
 * Tests that:
 * - manualKeywords: max 1024 chars
 * - autoKeywords: max 1024 chars
 * - searchableContent: max 2048 chars
 * - Combined searchableContent validation
 */
@DisplayName("Block Field Validation Tests")
class BlockFieldValidationTest {

    @Test
    @DisplayName("manualKeywords should reject >1024 characters")
    void testManualKeywordsExceedsLimit() {
        Block block = new Block();
        String tooLong = "x".repeat(1025);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> block.setManualKeywords(tooLong),
            "Should throw IllegalArgumentException for manualKeywords >1024 chars"
        );

        assertTrue(exception.getMessage().contains("1024 characters"));
        assertTrue(exception.getMessage().contains("1025"));
    }

    @Test
    @DisplayName("manualKeywords should accept exactly 1024 characters")
    void testManualKeywordsAtLimit() {
        Block block = new Block();
        String atLimit = "x".repeat(1024);

        assertDoesNotThrow(
            () -> block.setManualKeywords(atLimit),
            "Should accept manualKeywords at exactly 1024 chars"
        );

        assertEquals(atLimit, block.getManualKeywords());
    }

    @Test
    @DisplayName("manualKeywords should accept null and empty")
    void testManualKeywordsNullAndEmpty() {
        Block block = new Block();

        assertDoesNotThrow(() -> block.setManualKeywords(null));
        assertNull(block.getManualKeywords());

        assertDoesNotThrow(() -> block.setManualKeywords(""));
        assertEquals("", block.getManualKeywords());
    }

    @Test
    @DisplayName("autoKeywords should reject >1024 characters")
    void testAutoKeywordsExceedsLimit() {
        Block block = new Block();
        String tooLong = "y".repeat(1025);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> block.setAutoKeywords(tooLong),
            "Should throw IllegalArgumentException for autoKeywords >1024 chars"
        );

        assertTrue(exception.getMessage().contains("1024 characters"));
        assertTrue(exception.getMessage().contains("1025"));
    }

    @Test
    @DisplayName("autoKeywords should accept exactly 1024 characters")
    void testAutoKeywordsAtLimit() {
        Block block = new Block();
        String atLimit = "y".repeat(1024);

        assertDoesNotThrow(
            () -> block.setAutoKeywords(atLimit),
            "Should accept autoKeywords at exactly 1024 chars"
        );

        assertEquals(atLimit, block.getAutoKeywords());
    }

    @Test
    @DisplayName("searchableContent should reject >2048 characters")
    void testSearchableContentExceedsLimit() {
        Block block = new Block();
        String tooLong = "z".repeat(2049);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> block.setSearchableContent(tooLong),
            "Should throw IllegalArgumentException for searchableContent >2048 chars"
        );

        assertTrue(exception.getMessage().contains("2048 characters"));
        assertTrue(exception.getMessage().contains("2049"));
    }

    @Test
    @DisplayName("searchableContent should accept exactly 2048 characters")
    void testSearchableContentAtLimit() {
        Block block = new Block();
        String atLimit = "z".repeat(2048);

        assertDoesNotThrow(
            () -> block.setSearchableContent(atLimit),
            "Should accept searchableContent at exactly 2048 chars"
        );

        assertEquals(atLimit, block.getSearchableContent());
    }

    @Test
    @DisplayName("updateSearchableContent should reject combined content >2048 chars")
    void testUpdateSearchableContentExceedsLimit() {
        Block block = new Block();

        // Set keywords that individually are valid but combined exceed limit
        block.setManualKeywords("a".repeat(1024)); // Max manual
        block.setAutoKeywords("b".repeat(1024));   // Max auto
        // Combined: 1024 + 1 (space) + 1024 = 2049 chars

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> block.updateSearchableContent(),
            "Should throw IllegalStateException when combined content >2048 chars"
        );

        assertTrue(exception.getMessage().contains("2048"));
        assertTrue(exception.getMessage().contains("2049"));
    }

    @Test
    @DisplayName("updateSearchableContent should accept combined content at limit")
    void testUpdateSearchableContentAtLimit() {
        Block block = new Block();

        // Set keywords that combined are exactly at limit
        block.setManualKeywords("a".repeat(1023)); // 1023 chars
        block.setAutoKeywords("b".repeat(1024));   // 1024 chars
        // Combined: 1023 + 1 (space) + 1024 = 2048 chars

        assertDoesNotThrow(
            () -> block.updateSearchableContent(),
            "Should accept combined content at exactly 2048 chars"
        );

        assertEquals(2048, block.getSearchableContent().length());
    }

    @Test
    @DisplayName("updateSearchableContent should handle empty keywords correctly")
    void testUpdateSearchableContentWithEmpty() {
        Block block = new Block();

        // Test with only manual keywords
        block.setManualKeywords("manual");
        block.setAutoKeywords(null);
        block.updateSearchableContent();
        assertEquals("manual", block.getSearchableContent());

        // Test with only auto keywords
        block.setManualKeywords(null);
        block.setAutoKeywords("auto");
        block.updateSearchableContent();
        assertEquals("auto", block.getSearchableContent());

        // Test with both empty
        block.setManualKeywords(null);
        block.setAutoKeywords(null);
        block.updateSearchableContent();
        assertEquals("", block.getSearchableContent());
    }

    @Test
    @DisplayName("Error messages should be clear and actionable")
    void testErrorMessageQuality() {
        Block block = new Block();

        try {
            block.setManualKeywords("x".repeat(1500));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("manualKeywords"));
            assertTrue(e.getMessage().contains("1024"));
            assertTrue(e.getMessage().contains("1500"));
            assertTrue(e.getMessage().toLowerCase().contains("keyword"));
        }

        try {
            block.setAutoKeywords("y".repeat(2000));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("autoKeywords"));
            assertTrue(e.getMessage().contains("1024"));
            assertTrue(e.getMessage().contains("2000"));
        }

        try {
            block.setSearchableContent("z".repeat(3000));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("searchableContent"));
            assertTrue(e.getMessage().contains("2048"));
            assertTrue(e.getMessage().contains("3000"));
        }
    }
}
