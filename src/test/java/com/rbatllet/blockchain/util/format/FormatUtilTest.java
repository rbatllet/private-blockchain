package com.rbatllet.blockchain.util.format;

import com.rbatllet.blockchain.entity.Block;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

// Using test doubles instead of Mockito to avoid dependency issues

/**
 * Test class for FormatUtil
 */
public class FormatUtilTest {
    
    @Test
    public void testFormatTimestamp_Default() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 6, 16, 19, 30, 0);
        String expected = "2025-06-16 19:30:00";
        
        assertEquals(expected, FormatUtil.formatTimestamp(timestamp),
                "Default timestamp format should match expected pattern");
    }
    
    @Test
    public void testFormatTimestamp_NullTimestamp() {
        assertEquals("null", FormatUtil.formatTimestamp(null),
                "Null timestamp should return 'null' string");
    }
    
    @Test
    public void testFormatTimestamp_CustomPattern() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 6, 16, 19, 30, 0);
        String pattern = "dd/MM/yyyy HH:mm";
        String expected = "16/06/2025 19:30";
        
        assertEquals(expected, FormatUtil.formatTimestamp(timestamp, pattern),
                "Custom timestamp format should match expected pattern");
    }
    
    @Test
    public void testTruncateHash_NullHash() {
        assertEquals("null", FormatUtil.truncateHash(null),
                "Should return 'null' for null hash");
    }
    
    @Test
    public void testTruncateHash_ShortHash() {
        String shortHash = "abcdef";
        assertEquals(shortHash, FormatUtil.truncateHash(shortHash),
                "Short hash should not be truncated");
    }
    
    @Test
    public void testTruncateHash_LongHash() {
        String longHash = "abcdef1234567890abcdef1234567890abcdef1234567890";
        String result = FormatUtil.truncateHash(longHash);
        
        // Verify general behavior rather than exact output
        assertTrue(result.length() < longHash.length(), 
                "Result should be shorter than original hash");
        assertTrue(result.contains("..."), 
                "Result should contain ellipsis");
        assertTrue(result.startsWith(longHash.substring(0, 16)), 
                "Result should start with beginning of original hash");
        assertTrue(result.endsWith(longHash.substring(longHash.length() - 20)), 
                "Result should end with end of original hash");
    }
    
    // Test implementation of Block
    private static class TestBlock extends Block {
        
        public TestBlock(Long blockNumber, LocalDateTime timestamp, String hash, String previousHash, String data) {
            // Set all required fields directly
            setBlockNumber(blockNumber);
            setTimestamp(timestamp);
            setHash(hash);
            setPreviousHash(previousHash);
            setData(data);
        }
    }
    
    @Test
    public void testFormatBlockInfo() {
        // Create test block
        LocalDateTime timestamp = LocalDateTime.of(2025, 6, 16, 19, 30, 0);
        String originalHash = "abcdef1234567890abcdef1234567890";
        String originalPrevHash = "0123456789abcdef0123456789abcdef";
        String blockData = "Test block data";
        
        Block testBlock = new TestBlock(
            42L,
            timestamp,
            originalHash,
            originalPrevHash,
            blockData
        );
        
        String result = FormatUtil.formatBlockInfo(testBlock);
        
        // Test general behavior instead of specific formatting
        assertTrue(result.contains("Block #42"), "Block info should contain block number");
        assertTrue(result.contains("2025-06-16 19:30:00"), "Block info should contain formatted timestamp");
        
        // Verify hash truncation behavior instead of exact output
        String truncatedHash = FormatUtil.truncateHash(originalHash);
        String truncatedPrevHash = FormatUtil.truncateHash(originalPrevHash);
        assertTrue(result.contains(truncatedHash), "Block info should contain properly truncated hash");
        assertTrue(result.contains(truncatedPrevHash), "Block info should contain properly truncated previous hash");
        
        assertTrue(result.contains("Data Length: " + blockData.length() + " chars"), "Block info should contain correct data length");
    }
    
    @Test
    public void testFormatBlockInfo_NullBlock() {
        assertEquals("null", FormatUtil.formatBlockInfo(null),
                "Should return 'null' for null block");
    }
    
    @Test
    public void testFixedWidth_NormalString() {
        String input = "test";
        int width = 10;
        String expected = "test      "; // 4 chars + 6 spaces
        
        assertEquals(expected, FormatUtil.fixedWidth(input, width),
                "String shorter than width should be padded with spaces");
    }
    
    @Test
    public void testFixedWidth_LongString() {
        String input = "this is a very long string";
        int width = 10;
        String result = FormatUtil.fixedWidth(input, width);
        
        // Verify general behavior rather than exact output
        assertTrue(result.length() <= width, 
                "Result should not exceed specified width");
        assertTrue(result.endsWith("..."), 
                "Result should end with ellipsis");
        assertTrue(result.startsWith(input.substring(0, result.length() - 3)), 
                "Result should start with beginning of original string");
    }
    
    @Test
    public void testFixedWidth_NullString() {
        int width = 10;
        String expected = "          "; // 10 spaces
        
        assertEquals(expected, FormatUtil.fixedWidth(null, width),
                "Null string should be replaced with spaces");
    }
}
