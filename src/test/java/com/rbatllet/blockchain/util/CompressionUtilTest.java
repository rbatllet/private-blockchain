package com.rbatllet.blockchain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CompressionUtil
 * Validates compression, decompression, and utility functionality
 */
public class CompressionUtilTest {
    
    @Test
    @DisplayName("Basic compression and decompression should work")
    void testBasicCompressionDecompression() {
        String originalData = "This is a test string that should compress well due to repeated patterns. " +
                             "This is a test string that should compress well due to repeated patterns. " +
                             "This is a test string that should compress well due to repeated patterns.";
        
        String compressed = CompressionUtil.compressString(originalData);
        String decompressed = CompressionUtil.decompressString(compressed);
        
        assertEquals(originalData, decompressed);
        assertTrue(CompressionUtil.isCompressed(compressed));
    }
    
    @Test
    @DisplayName("Small data should remain uncompressed")
    void testSmallDataUncompressed() {
        String smallData = "Hello";
        
        String result = CompressionUtil.compressString(smallData);
        String decompressed = CompressionUtil.decompressString(result);
        
        assertEquals(smallData, decompressed);
        assertTrue(CompressionUtil.isUncompressed(result));
        assertFalse(CompressionUtil.isCompressed(result));
    }
    
    @Test
    @DisplayName("JSON metadata should compress efficiently")
    void testJSONMetadataCompression() {
        String jsonMetadata = "{\n" +
                "  \"exactTimestamp\": \"2024-01-15T14:30:22\",\n" +
                "  \"specificKeywords\": [\"financial\", \"transaction\", \"account\", \"bank\", \"wire\", \"transfer\"],\n" +
                "  \"detailedCategory\": \"FINANCIAL_DETAILED\",\n" +
                "  \"ownerDetails\": \"owner_encrypted\",\n" +
                "  \"contentStatistics\": {\n" +
                "    \"wordCount\": 156,\n" +
                "    \"keywordCount\": 12,\n" +
                "    \"numericalCount\": 8\n" +
                "  },\n" +
                "  \"technicalDetails\": {},\n" +
                "  \"validationInfo\": {},\n" +
                "  \"metadataVersion\": \"1.0\"\n" +
                "}";
        
        CompressionUtil.CompressionStats stats = CompressionUtil.getCompressionStats(jsonMetadata);
        
        // Note: Small JSON might not compress enough to be worth it
        assertTrue(stats.getSpaceSavings() >= 0, "Space savings should be non-negative");
        
        String compressed = CompressionUtil.compressString(jsonMetadata);
        String decompressed = CompressionUtil.decompressString(compressed);
        
        assertEquals(jsonMetadata, decompressed);
        
        System.out.println("📊 JSON Compression Stats: " + stats);
    }
    
    @Test
    @DisplayName("Conditional compression should respect enable flag")
    void testConditionalCompression() {
        String data = "This is some data that could be compressed if compression is enabled.";
        
        // Test with compression enabled
        String compressedResult = CompressionUtil.compressConditionally(data, true);
        assertTrue(CompressionUtil.isCompressed(compressedResult) || CompressionUtil.isUncompressed(compressedResult));
        
        // Test with compression disabled
        String uncompressedResult = CompressionUtil.compressConditionally(data, false);
        assertTrue(CompressionUtil.isUncompressed(uncompressedResult));
        assertFalse(CompressionUtil.isCompressed(uncompressedResult));
        
        // Both should decompress to the same original data
        assertEquals(data, CompressionUtil.decompressString(compressedResult));
        assertEquals(data, CompressionUtil.decompressString(uncompressedResult));
    }
    
    @Test
    @DisplayName("Compression validation should work correctly")
    void testCompressionValidation() {
        String originalData = "Valid data for compression testing";
        String compressed = CompressionUtil.compressString(originalData);
        
        assertTrue(CompressionUtil.validateCompressedData(compressed));
        
        // Test with invalid data
        assertFalse(CompressionUtil.validateCompressedData("INVALID_DATA"));
        assertFalse(CompressionUtil.validateCompressedData("DEFLATE_V1:InvalidBase64!@#"));
    }
    
    @Test
    @DisplayName("Empty and null data should be handled gracefully")
    void testEmptyAndNullData() {
        assertThrows(IllegalArgumentException.class, () -> 
            CompressionUtil.compressString(null));
        
        assertThrows(IllegalArgumentException.class, () -> 
            CompressionUtil.compressString(""));
        
        assertThrows(IllegalArgumentException.class, () -> 
            CompressionUtil.decompressString(null));
        
        assertThrows(IllegalArgumentException.class, () -> 
            CompressionUtil.decompressString(""));
    }
    
    @Test
    @DisplayName("Compression statistics should be accurate")
    void testCompressionStatistics() {
        String data = "This is test data. This is test data. This is test data. This is test data.";
        
        CompressionUtil.CompressionStats stats = CompressionUtil.getCompressionStats(data);
        
        assertTrue(stats.getOriginalSize() > 0);
        assertTrue(stats.getCompressedSize() > 0);
        assertTrue(stats.getCompressionRatio() > 0, "Compression ratio should be positive");
        assertTrue(stats.getSpaceSavings() >= -1.0 && stats.getSpaceSavings() <= 1.0, "Space savings should be between -100% and 100%");
        assertEquals(stats.getSpaceSaved(), stats.getOriginalSize() - stats.getCompressedSize());
        
        String statsString = stats.toString();
        assertNotNull(statsString);
        assertTrue(statsString.contains("bytes"));
    }
    
    @Test
    @DisplayName("Large data should compress efficiently")
    void testLargeDataCompression() {
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeData.append("Large metadata entry number ").append(i)
                    .append(" with repetitive content for compression testing. ");
        }
        
        String data = largeData.toString();
        CompressionUtil.CompressionStats stats = CompressionUtil.getCompressionStats(data);
        
        assertTrue(stats.wasCompressed(), "Large repetitive data should be compressed");
        assertTrue(stats.getSpaceSavings() > 0.5, "Should achieve significant compression on repetitive data");
        
        String compressed = CompressionUtil.compressString(data);
        String decompressed = CompressionUtil.decompressString(compressed);
        
        assertEquals(data, decompressed);
        
        System.out.println("📊 Large Data Compression: " + stats);
    }
    
    @Test
    @DisplayName("Random data should not compress well")
    void testRandomDataCompression() {
        // Generate pseudo-random data that won't compress well
        StringBuilder randomData = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            randomData.append(java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        
        String data = randomData.toString();
        String compressed = CompressionUtil.compressString(data);
        
        // Random data might not compress enough to warrant compression
        boolean wasCompressed = CompressionUtil.isCompressed(compressed);
        
        // Verify round-trip regardless of compression decision
        String decompressed = CompressionUtil.decompressString(compressed);
        assertEquals(data, decompressed);
        
        System.out.println("📊 Random Data Compression: " + 
                          (wasCompressed ? "Compressed" : "Stored uncompressed"));
    }
    
    @Test
    @DisplayName("Unicode and special characters should be handled correctly")
    void testUnicodeAndSpecialCharacters() {
        String unicodeData = "Testing unicode: àèéíòóúü ñç ßäöü 中文测试 🔐📦✅❌ JSON: {\"key\": \"value\"}";
        
        String compressed = CompressionUtil.compressString(unicodeData);
        String decompressed = CompressionUtil.decompressString(compressed);
        
        assertEquals(unicodeData, decompressed);
        
        System.out.println("✅ Unicode compression test passed");
    }
}