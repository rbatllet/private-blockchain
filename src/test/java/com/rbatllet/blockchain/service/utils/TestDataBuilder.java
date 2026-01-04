package com.rbatllet.blockchain.service.utils;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class for creating test data consistently across all test files.
 * 
 * Eliminates duplication in test data creation and provides standard
 * test objects with realistic values.
 */
public class TestDataBuilder {
    
    // Standard test identifiers
    private static final List<String> MEDICAL_IDENTIFIERS = Arrays.asList(
        "PATIENT-001", "PATIENT-002", "MEDICAL-RECORD-123", "DIAGNOSIS-456"
    );
    
    private static final List<String> FINANCIAL_IDENTIFIERS = Arrays.asList(
        "TXN-2024-001", "ACCOUNT-789", "FINANCIAL-REPORT-2024", "AUDIT-001"
    );
    
    private static final List<String> LEGAL_IDENTIFIERS = Arrays.asList(
        "CONTRACT-ABC-2024", "LEGAL-DOC-001", "AGREEMENT-789", "COMPLIANCE-REP"
    );
    
    /**
     * Create a mock block with realistic test data
     * @param blockNumber Block number
     * @param data Block data content
     * @return Mock block
     */
    public static Block createMockBlock(Long blockNumber, String data) {
        Block mockBlock = Mockito.mock(Block.class);

        Mockito.when(mockBlock.getBlockNumber()).thenReturn(blockNumber);
        Mockito.when(mockBlock.getData()).thenReturn(data);
        Mockito.when(mockBlock.getHash()).thenReturn("hash_" + blockNumber);
        Mockito.when(mockBlock.getPreviousHash()).thenReturn(blockNumber > 1 ? "hash_" + (blockNumber - 1) : "genesis");
        Mockito.when(mockBlock.getTimestamp()).thenReturn(LocalDateTime.now().minusMinutes(blockNumber));
        Mockito.when(mockBlock.isDataEncrypted()).thenReturn(true);
        Mockito.when(mockBlock.getSignerPublicKey()).thenReturn("test_public_key");
        Mockito.when(mockBlock.getContentCategory()).thenReturn("general");
        
        return mockBlock;
    }
    
    /**
     * Create multiple mock blocks with incrementing IDs
     * @param count Number of blocks to create
     * @return List of mock blocks
     */
    public static List<Block> createMockBlocks(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> createMockBlock((long) i, "Test data " + i))
                .collect(Collectors.toList());
    }
    
    /**
     * Create mock blocks with specific categories
     * @param count Number of blocks per category
     * @param categories Block categories to create
     * @return List of categorized mock blocks
     */
    public static List<Block> createCategorizedMockBlocks(int count, String... categories) {
        List<Block> blocks = new ArrayList<>();
        
        for (String category : categories) {
            for (int i = 1; i <= count; i++) {
                Block block = createMockBlock((long) (blocks.size() + 1), 
                                           category + " test data " + i);
                Mockito.when(block.getContentCategory()).thenReturn(category);
                blocks.add(block);
            }
        }
        
        return blocks;
    }
    
    /**
     * Create test block with medical data identifiers
     * @param index Index for unique identifiers
     * @return Mock medical block
     */
    public static Block createMedicalBlock(int index) {
        String identifier = MEDICAL_IDENTIFIERS.get(index % MEDICAL_IDENTIFIERS.size()) + "_" + index;
        Block block = createMockBlock((long) index, "Medical data for " + identifier);
        Mockito.when(block.getContentCategory()).thenReturn("medical");
        Mockito.when(block.getManualKeywords()).thenReturn(identifier + " medical healthcare");
        return block;
    }
    
    /**
     * Create test block with financial data identifiers
     * @param index Index for unique identifiers
     * @return Mock financial block
     */
    public static Block createFinancialBlock(int index) {
        String identifier = FINANCIAL_IDENTIFIERS.get(index % FINANCIAL_IDENTIFIERS.size()) + "_" + index;
        Block block = createMockBlock((long) index, "Financial data for " + identifier);
        Mockito.when(block.getContentCategory()).thenReturn("financial");
        Mockito.when(block.getManualKeywords()).thenReturn(identifier + " financial transaction");
        return block;
    }
    
    /**
     * Create test block with legal data identifiers
     * @param index Index for unique identifiers
     * @return Mock legal block
     */
    public static Block createLegalBlock(int index) {
        String identifier = LEGAL_IDENTIFIERS.get(index % LEGAL_IDENTIFIERS.size()) + "_" + index;
        Block block = createMockBlock((long) index, "Legal data for " + identifier);
        Mockito.when(block.getContentCategory()).thenReturn("legal");
        Mockito.when(block.getManualKeywords()).thenReturn(identifier + " legal contract");
        return block;
    }
    
    /**
     * Create mock off-chain data for testing
     * @param filePath File path
     * @param size File size in bytes
     * @return Mock OffChainData
     */
    public static OffChainData createMockOffChainData(String filePath, long size) {
        OffChainData mockData = Mockito.mock(OffChainData.class);
        
        Mockito.when(mockData.getFilePath()).thenReturn(filePath);
        Mockito.when(mockData.getFileSize()).thenReturn(size);
        // Add only methods that exist in OffChainData
        
        return mockData;
    }
    
    /**
     * Create standard test options map
     * @return Default options for testing
     */
    public static Map<String, Object> createDefaultOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("deepScan", false);
        options.put("checkIntegrity", true);
        options.put("validateKeys", false);
        options.put("checkConsistency", false);
        options.put("detailedReport", true);
        options.put("efficientMode", false);
        return options;
    }
    
    /**
     * Create performance test options
     * @return Options optimized for performance testing
     */
    public static Map<String, Object> createPerformanceOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("deepScan", false);
        options.put("checkIntegrity", false);
        options.put("validateKeys", false);
        options.put("checkConsistency", false);
        options.put("detailedReport", false);
        options.put("efficientMode", true);
        return options;
    }
    
    /**
     * Create comprehensive test options for thorough testing
     * @return Options for comprehensive testing
     */
    public static Map<String, Object> createComprehensiveOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("deepScan", true);
        options.put("checkIntegrity", true);
        options.put("validateKeys", true);
        options.put("checkConsistency", true);
        options.put("detailedReport", true);
        options.put("efficientMode", false);
        return options;
    }
    
    /**
     * Create search criteria for testing advanced search
     * @param keywords Search keywords
     * @param categories Content categories
     * @return Search criteria map
     */
    public static Map<String, Object> createSearchCriteria(String keywords, String... categories) {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("keywords", keywords);
        criteria.put("categories", Set.of(categories));
        criteria.put("includeEncrypted", true);
        criteria.put("startDate", LocalDateTime.now().minusDays(30));
        criteria.put("endDate", LocalDateTime.now());
        return criteria;
    }
    
    /**
     * Generate test password with specified characteristics
     * @param length Password length
     * @param includeSpecialChars Whether to include special characters
     * @return Generated test password
     */
    public static String generateTestPassword(int length, boolean includeSpecialChars) {
        String baseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String specialChars = "!@#$%^&*";
        String charset = includeSpecialChars ? baseChars + specialChars : baseChars;
        
        Random random = new Random();
        return random.ints(length, 0, charset.length())
                     .mapToObj(charset::charAt)
                     .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                     .toString();
    }
}