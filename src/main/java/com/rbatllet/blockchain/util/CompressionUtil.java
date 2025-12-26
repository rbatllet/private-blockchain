package com.rbatllet.blockchain.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Utility class for data compression and decompression
 * Uses DEFLATE algorithm for efficient metadata compression
 * 
 * Features:
 * - High compression ratio for JSON metadata
 * - Base64 encoding for safe storage
 * - Integrity validation
 * - Performance optimization for small data
 */
public class CompressionUtil {
    
    // Compression settings optimized for metadata
    private static final int COMPRESSION_LEVEL = Deflater.BEST_COMPRESSION;
    private static final int BUFFER_SIZE = 1024;
    private static final int MIN_COMPRESSION_SIZE = 100; // Don't compress data smaller than 100 bytes
    
    // Compression format constants
    private static final String COMPRESSION_HEADER = "DEFLATE_V1:";
    private static final String UNCOMPRESSED_HEADER = "RAW_V1:";
    
    /**
     * Compress string data using DEFLATE algorithm
     * @param data The string to compress
     * @return Base64-encoded compressed data with header
     * @throws IllegalArgumentException if data is null or empty
     */
    public static String compressString(String data) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        
        byte[] inputBytes = data.getBytes(StandardCharsets.UTF_8);
        
        // Skip compression for small data (overhead not worth it)
        if (inputBytes.length < MIN_COMPRESSION_SIZE) {
            return UNCOMPRESSED_HEADER + Base64.getEncoder().encodeToString(inputBytes);
        }
        
        try {
            byte[] compressedBytes = compressBytes(inputBytes);
            
            // Check if compression actually helped
            if (compressedBytes.length >= inputBytes.length * 0.9) {
                // Less than 10% compression, store uncompressed
                return UNCOMPRESSED_HEADER + Base64.getEncoder().encodeToString(inputBytes);
            }
            
            return COMPRESSION_HEADER + Base64.getEncoder().encodeToString(compressedBytes);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Decompress string data
     * @param compressedData Base64-encoded compressed data with header
     * @return Original uncompressed string
     * @throws IllegalArgumentException if data is null, empty, or invalid format
     */
    public static String decompressString(String compressedData) {
        if (compressedData == null || compressedData.trim().isEmpty()) {
            throw new IllegalArgumentException("Compressed data cannot be null or empty");
        }
        
        try {
            if (compressedData.startsWith(COMPRESSION_HEADER)) {
                // Data is compressed
                String base64Data = compressedData.substring(COMPRESSION_HEADER.length());
                byte[] compressedBytes = Base64.getDecoder().decode(base64Data);
                byte[] decompressedBytes = decompressBytes(compressedBytes);
                return new String(decompressedBytes, StandardCharsets.UTF_8);
                
            } else if (compressedData.startsWith(UNCOMPRESSED_HEADER)) {
                // Data is uncompressed
                String base64Data = compressedData.substring(UNCOMPRESSED_HEADER.length());
                byte[] dataBytes = Base64.getDecoder().decode(base64Data);
                return new String(dataBytes, StandardCharsets.UTF_8);
                
            } else {
                throw new IllegalArgumentException("Invalid compression format - missing header");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decompress data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Compress byte array using DEFLATE
     * @param data Bytes to compress
     * @return Compressed bytes
     * @throws IOException if compression fails
     */
    private static byte[] compressBytes(byte[] data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Deflater deflater = new Deflater(COMPRESSION_LEVEL);
             DeflaterOutputStream deflaterStream = new DeflaterOutputStream(outputStream, deflater)) {

            deflaterStream.write(data);
            deflaterStream.finish();
        }

        return outputStream.toByteArray();
    }
    
    /**
     * Decompress byte array using INFLATE
     * @param compressedData Compressed bytes
     * @return Decompressed bytes
     * @throws IOException if decompression fails
     */
    private static byte[] decompressBytes(byte[] compressedData) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);

        try (Inflater inflater = new Inflater();
             InflaterInputStream inflaterStream = new InflaterInputStream(inputStream, inflater)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inflaterStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return outputStream.toByteArray();
    }
    
    /**
     * Calculate compression ratio as percentage
     * @param originalSize Original data size in bytes
     * @param compressedSize Compressed data size in bytes
     * @return Compression ratio (e.g., 0.75 means 75% of original size)
     */
    public static double calculateCompressionRatio(int originalSize, int compressedSize) {
        if (originalSize <= 0) {
            return 1.0;
        }
        return (double) compressedSize / originalSize;
    }
    
    /**
     * Calculate space savings as percentage
     * @param originalSize Original data size in bytes
     * @param compressedSize Compressed data size in bytes
     * @return Space savings percentage (e.g., 0.25 means 25% savings)
     */
    public static double calculateSpaceSavings(int originalSize, int compressedSize) {
        return 1.0 - calculateCompressionRatio(originalSize, compressedSize);
    }
    
    /**
     * Check if data appears to be compressed (has compression header)
     * @param data Data to check
     * @return true if data has compression header
     */
    public static boolean isCompressed(String data) {
        if (data == null) {
            return false;
        }
        return data.startsWith(COMPRESSION_HEADER);
    }
    
    /**
     * Check if data is stored uncompressed (has uncompressed header)
     * @param data Data to check
     * @return true if data has uncompressed header
     */
    public static boolean isUncompressed(String data) {
        if (data == null) {
            return false;
        }
        return data.startsWith(UNCOMPRESSED_HEADER);
    }
    
    /**
     * Get compression statistics for given data
     * @param originalData Original string data
     * @return CompressionStats object with detailed information
     */
    public static CompressionStats getCompressionStats(String originalData) {
        if (originalData == null || originalData.trim().isEmpty()) {
            return new CompressionStats(0, 0, 0.0, 0.0, false);
        }
        
        int originalSize = originalData.getBytes(StandardCharsets.UTF_8).length;
        String compressed = compressString(originalData);
        int compressedSize = compressed.getBytes(StandardCharsets.UTF_8).length;
        
        double compressionRatio = calculateCompressionRatio(originalSize, compressedSize);
        double spaceSavings = calculateSpaceSavings(originalSize, compressedSize);
        boolean wasCompressed = isCompressed(compressed);
        
        return new CompressionStats(originalSize, compressedSize, compressionRatio, spaceSavings, wasCompressed);
    }
    
    /**
     * Data class for compression statistics
     */
    public static class CompressionStats {
        private final int originalSize;
        private final int compressedSize;
        private final double compressionRatio;
        private final double spaceSavings;
        private final boolean wasCompressed;
        
        public CompressionStats(int originalSize, int compressedSize, double compressionRatio, 
                              double spaceSavings, boolean wasCompressed) {
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
            this.compressionRatio = compressionRatio;
            this.spaceSavings = spaceSavings;
            this.wasCompressed = wasCompressed;
        }
        
        public int getOriginalSize() { return originalSize; }
        public int getCompressedSize() { return compressedSize; }
        public double getCompressionRatio() { return compressionRatio; }
        public double getSpaceSavings() { return spaceSavings; }
        public boolean wasCompressed() { return wasCompressed; }
        public int getSpaceSaved() { return originalSize - compressedSize; }
        
        @Override
        public String toString() {
            return String.format("CompressionStats{original=%d bytes, compressed=%d bytes, ratio=%.2f%%, savings=%d bytes, compressed=%s}",
                    originalSize, compressedSize, compressionRatio * 100, getSpaceSaved(), wasCompressed);
        }
    }
    
    /**
     * Compress data conditionally based on configuration and data characteristics
     * @param data Data to potentially compress
     * @param enableCompression Whether compression is enabled
     * @return Compressed or uncompressed data as appropriate
     */
    public static String compressConditionally(String data, boolean enableCompression) {
        if (!enableCompression || data == null || data.trim().isEmpty()) {
            // Store as uncompressed with header for consistency
            if (data == null || data.trim().isEmpty()) {
                return UNCOMPRESSED_HEADER + "";
            }
            byte[] inputBytes = data.getBytes(StandardCharsets.UTF_8);
            return UNCOMPRESSED_HEADER + Base64.getEncoder().encodeToString(inputBytes);
        }
        
        return compressString(data);
    }
    
    /**
     * Validate that compressed data can be successfully decompressed
     * @param compressedData Data to validate
     * @return true if data can be decompressed without errors
     */
    public static boolean validateCompressedData(String compressedData) {
        try {
            decompressString(compressedData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}