package com.rbatllet.blockchain.util.format;

import com.rbatllet.blockchain.entity.Block;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        String expected = "abcdef1234567890...7890abcdef1234567890";
        
        assertEquals(expected, FormatUtil.truncateHash(longHash),
                "Long hash should be truncated with ellipsis in the middle");
    }
    
    @Test
    public void testFormatBlockInfo() {
        // Create mock block
        Block mockBlock = Mockito.mock(Block.class);
        LocalDateTime timestamp = LocalDateTime.of(2025, 6, 16, 19, 30, 0);
        
        when(mockBlock.getBlockNumber()).thenReturn(42L);
        when(mockBlock.getTimestamp()).thenReturn(timestamp);
        when(mockBlock.getHash()).thenReturn("abcdef1234567890abcdef1234567890");
        when(mockBlock.getPreviousHash()).thenReturn("0123456789abcdef0123456789abcdef");
        when(mockBlock.getData()).thenReturn("Test block data");
        
        String result = FormatUtil.formatBlockInfo(mockBlock);
        
        assertTrue(result.contains("Block #42"), "Block info should contain block number");
        assertTrue(result.contains("2025-06-16 19:30:00"), "Block info should contain formatted timestamp");
        assertTrue(result.contains("abcdef1234567890...7890abcdef1234567890"), "Block info should contain truncated hash");
        assertTrue(result.contains("0123456789abcdef...cdef0123456789abcdef"), "Block info should contain truncated previous hash");
        assertTrue(result.contains("Data Length: 15 chars"), "Block info should contain data length");
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
        String expected = "this is a...";
        
        assertEquals(expected, FormatUtil.fixedWidth(input, width),
                "String longer than width should be truncated with ellipsis");
    }
    
    @Test
    public void testFixedWidth_NullString() {
        int width = 10;
        String expected = "          "; // 10 spaces
        
        assertEquals(expected, FormatUtil.fixedWidth(null, width),
                "Null string should be replaced with spaces");
    }
}
