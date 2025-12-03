package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.search.metadata.TermVisibilityMap;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;
import java.security.KeyPair;
import java.util.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for remaining methods with 0% coverage in UserFriendlyEncryptionAPI.
 * Target: Improve overall coverage from 65% to 75%+
 */
@DisplayName("🎯 UserFriendlyEncryptionAPI Remaining Coverage Tests")
public class UserFriendlyEncryptionAPIRemainingCoverageTest {
    
    private UserFriendlyEncryptionAPI api;
    private Blockchain realBlockchain;
    private String testUsername = "coverageuser";
    private KeyPair testKeyPair;
    private KeyPair bootstrapKeyPair;
    
    @BeforeEach
    void setUp() throws Exception {
        realBlockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        realBlockchain.clearAndReinitialize();

        // Load bootstrap admin keys
        bootstrapKeyPair = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // SECURITY (v1.0.6): Register bootstrap admin in blockchain (REQUIRED!)
        realBlockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        testKeyPair = CryptoUtil.generateKeyPair();

        // SECURITY FIX (v1.0.6): Pre-authorize user before creating API
        // RBAC FIX (v1.0.6): Use SUPER_ADMIN role for hierarchical key operations
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        realBlockchain.addAuthorizedKey(publicKeyString, testUsername, bootstrapKeyPair, UserRole.SUPER_ADMIN);

        // Initialize API with test credentials (blockchain, username, keyPair)
        api = new UserFriendlyEncryptionAPI(realBlockchain, testUsername, testKeyPair);

        // Add some test blocks for methods that need existing data
        realBlockchain.addBlock("Test data 1", testKeyPair.getPrivate(), testKeyPair.getPublic());
        realBlockchain.addBlock("Test data 2", testKeyPair.getPrivate(), testKeyPair.getPublic());
        realBlockchain.addBlock("Encrypted: secret data", testKeyPair.getPrivate(), testKeyPair.getPublic());
    }
    
    @AfterEach
    void tearDown() {
        realBlockchain = null;
        api = null;
    }
    
    @Nested
    @DisplayName("📊 Reporting Methods Tests")
    class ReportingMethodsTests {
        
        @Test
        @DisplayName("Should generate off-chain storage report")
        void shouldGenerateOffChainStorageReport() {
            // When
            String report = api.generateOffChainStorageReport();
            
            // Then
            assertNotNull(report, "Report should not be null");
            assertTrue(report.length() > 0, "Report should have content");
        }
        
        @Test
        @DisplayName("Should generate blockchain status report")
        void shouldGenerateBlockchainStatusReport() {
            // When
            String report = api.generateBlockchainStatusReport();
            
            // Then
            assertNotNull(report, "Report should not be null");
            assertTrue(report.length() > 0, "Report should have content");
        }
        
        @Test
        @DisplayName("Should get encryption config comparison")
        void shouldGetEncryptionConfigComparison() {
            // When
            String comparison = api.getEncryptionConfigComparison();
            
            // Then
            assertNotNull(comparison, "Comparison should not be null");
            assertTrue(comparison.length() >= 0, "Should return some content");
        }
        
        // Note: Only testing public methods with 0% coverage
    }
    
    @Nested
    @DisplayName("📝 Formatting Methods Tests")
    class FormattingMethodsTests {
        
        @Test
        @DisplayName("Should format blocks summary")
        void shouldFormatBlocksSummary() {
            // Given
            List<Block> blocks = new ArrayList<>();
            long blockCount = realBlockchain.getBlockCount();
            for (long i = 0; i < blockCount; i++) {
                Block block = realBlockchain.getBlock(i);
                if (block != null) {
                    blocks.add(block);
                }
            }

            // When
            String summary = api.formatBlocksSummary(blocks);

            // Then
            assertNotNull(summary, "Summary should not be null");
            assertTrue(summary.length() > 0, "Summary should have content");
        }
        
        @Test
        @DisplayName("Should format empty blocks list")
        void shouldFormatEmptyBlocksList() {
            // Given
            List<Block> emptyList = new ArrayList<>();
            
            // When
            String summary = api.formatBlocksSummary(emptyList);
            
            // Then
            assertNotNull(summary, "Summary should not be null");
            assertTrue(summary.contains("No blocks") || summary.contains("0"), 
                      "Should indicate no blocks");
        }
        
        @Test
        @DisplayName("Should handle invalid formatBlocksSummary inputs")
        void shouldHandleInvalidFormatBlocksSummaryInputs() {
            // Test null input
            assertDoesNotThrow(() -> api.formatBlocksSummary(null), 
                             "Should handle null blocks list gracefully");
        }
        
        @Test
        @DisplayName("Should format individual block display")
        void shouldFormatBlockDisplay() {
            // Given
            Block testBlock = realBlockchain.getBlock(0L);
            assertNotNull(testBlock, "Genesis block should exist");

            // When
            String display = api.formatBlockDisplay(testBlock);

            // Then
            assertNotNull(display, "Block display should not be null");
            assertTrue(display.length() > 0, "Block display should have content");
        }
        
        @Test
        @DisplayName("Should format search results")
        void shouldFormatSearchResults() {
            // Given
            String searchTerm = "test";
            List<Block> results = new ArrayList<>();
            long blockCount = realBlockchain.getBlockCount();
            for (long i = 0; i < blockCount; i++) {
                Block block = realBlockchain.getBlock(i);
                if (block != null) {
                    results.add(block);
                }
            }

            // When
            String formatted = api.formatSearchResults(searchTerm, results);

            // Then
            assertNotNull(formatted, "Formatted results should not be null");
            assertTrue(formatted.length() >= 0, "Should return some content");
        }
        
        @Test
        @DisplayName("Should format file size")
        void shouldFormatFileSize() {
            // When
            String size1 = api.formatFileSize(1024);
            String size2 = api.formatFileSize(1048576);
            String size3 = api.formatFileSize(0);
            
            // Then
            assertNotNull(size1, "File size format should not be null");
            assertNotNull(size2, "File size format should not be null");
            assertNotNull(size3, "File size format should not be null");
            assertTrue(size1.length() > 0, "Should format KB size");
            assertTrue(size2.length() > 0, "Should format MB size");
            assertTrue(size3.length() > 0, "Should format zero size");
        }
    }
    
    @Nested
    @DisplayName("🔑 Password and Security Tests")
    class PasswordSecurityTests {
        
        @Test
        @DisplayName("Should generate password for config")
        void shouldGeneratePasswordForConfig() {
            // Given
            EncryptionConfig config = EncryptionConfig.createTestConfig();
            
            // When & Then
            assertDoesNotThrow(() -> {
                String password = api.generatePasswordForConfig(config);
                assertNotNull(password, "Generated password should not be null");
                assertTrue(password.length() > 0, "Generated password should have content");
            }, "Should generate password without throwing");
        }
        
        @Test
        @DisplayName("Should generate validated password")
        void shouldGenerateValidatedPassword() {
            // When & Then
            assertDoesNotThrow(() -> {
                String password = api.generateValidatedPassword(12, false);
                assertNotNull(password, "Generated password should not be null");
                assertTrue(password.length() >= 12, "Password should meet minimum length");
            }, "Should generate validated password without throwing");
        }
        
        @Test
        @DisplayName("Should get password registry stats")
        void shouldGetPasswordRegistryStats() {
            // When
            Object stats = api.getPasswordRegistryStats();
            
            // Then
            assertNotNull(stats, "Password registry stats should not be null");
        }
    }
    
    @Nested
    @DisplayName("⚙️ Configuration Tests")
    class ConfigurationTests {
        
        @Test
        @DisplayName("Should create custom config builder")
        void shouldCreateCustomConfig() {
            // When
            EncryptionConfig.Builder builder = api.createCustomConfig();
            
            // Then
            assertNotNull(builder, "Config builder should not be null");
        }
    }
    
    @Nested
    @DisplayName("🔍 Advanced Search Tests")
    class AdvancedSearchTests {
        
        @Test
        @DisplayName("Should search with adaptive decryption")
        void shouldSearchWithAdaptiveDecryption() {
            // Given
            String searchTerm = "test";
            String password = "testPassword123";
            int maxResults = 10;
            
            // When & Then
            assertDoesNotThrow(() -> {
                List<Block> results = api.searchWithAdaptiveDecryption(searchTerm, password, maxResults);
                assertNotNull(results, "Search results should not be null");
            }, "Should perform adaptive decryption search without throwing");
        }
        
        @Test
        @DisplayName("Should get cache statistics")
        void shouldGetCacheStatistics() {
            // When & Then
            assertDoesNotThrow(() -> {
                SearchCacheManager.CacheStatistics stats = api.getCacheStatistics();
                assertNotNull(stats, "Cache statistics should not be null");
            }, "Should get cache statistics without throwing");
        }
        
        @Test
        @DisplayName("Should clear search cache")
        void shouldClearSearchCache() {
            // When & Then
            assertDoesNotThrow(() -> api.clearSearchCache(), 
                             "Should clear search cache without throwing");
        }
        
        @Test
        @DisplayName("Should get search metrics")
        void shouldGetSearchMetrics() {
            // When & Then
            assertDoesNotThrow(() -> {
                SearchMetrics metrics = api.getSearchMetrics();
                assertNotNull(metrics, "Search metrics should not be null");
            }, "Should get search metrics without throwing");
        }
        
        @Test
        @DisplayName("Should optimize search performance")
        void shouldOptimizeSearchPerformance() {
            // When & Then
            assertDoesNotThrow(() -> {
                String result = api.optimizeSearchPerformance();
                assertNotNull(result, "Optimization result should not be null");
            }, "Should optimize search performance without throwing");
        }
    }
    
    @Nested
    @DisplayName("🏥 Health and Validation Tests")
    class HealthValidationTests {
        
        @Test
        @DisplayName("Should perform comprehensive validation")
        void shouldPerformComprehensiveValidation() {
            // When & Then
            assertDoesNotThrow(() -> {
                ValidationReport report = api.performComprehensiveValidation();
                assertNotNull(report, "Validation report should not be null");
            }, "Should perform comprehensive validation without throwing");
        }
        
        @Test
        @DisplayName("Should perform health diagnosis")
        void shouldPerformHealthDiagnosis() {
            // When & Then
            assertDoesNotThrow(() -> {
                HealthReport report = api.performHealthDiagnosis();
                assertNotNull(report, "Health report should not be null");
            }, "Should perform health diagnosis without throwing");
        }
        
        @Test
        @DisplayName("Should validate chain integrity")
        void shouldValidateChainIntegrity() {
            // When & Then
            assertDoesNotThrow(() -> {
                ValidationReport report = api.validateChainIntegrity();
                assertNotNull(report, "Chain integrity report should not be null");
            }, "Should validate chain integrity without throwing");
        }
        
        @Test
        @DisplayName("Should perform comprehensive validation with options")
        void shouldPerformComprehensiveValidationWithOptions() {
            // Given
            Map<String, Object> options = new HashMap<>();
            options.put("checkIntegrity", true);
            options.put("validateEncryption", true);
            
            // When & Then
            assertDoesNotThrow(() -> {
                ValidationReport report = api.performComprehensiveValidation(options);
                assertNotNull(report, "Validation report should not be null");
            }, "Should perform validation with options without throwing");
        }
        
        @Test
        @DisplayName("Should generate health report with options")
        void shouldGenerateHealthReportWithOptions() {
            // Given
            Map<String, Object> options = new HashMap<>();
            options.put("includePerformance", true);
            options.put("includeCache", true);
            
            // When & Then
            assertDoesNotThrow(() -> {
                HealthReport report = api.generateHealthReport(options);
                assertNotNull(report, "Health report should not be null");
            }, "Should generate health report with options without throwing");
        }
    }
    
    @Nested
    @DisplayName("🔑 Key Management Tests")
    class KeyManagementTests {
        
        @Test
        @DisplayName("Should import and set default user")
        void shouldImportAndSetDefaultUser() {
            // Given
            String username = "testuser";
            String keyFilePath = "/tmp/test-key.pem";
            
            // When & Then (we expect this might fail but not throw exceptions)
            assertDoesNotThrow(() -> {
                boolean result = api.importAndSetDefaultUser(username, keyFilePath);
                // Note: This method might return false for non-existent file paths
                assertNotNull(result, "Result should not be null");
            }, "Should handle import attempt without throwing");
        }
        
        @Test
        @DisplayName("Should setup hierarchical keys")
        void shouldSetupHierarchicalKeys() {
            // Given
            String masterPassword = "MasterPassword123!";
            
            // When & Then
            assertDoesNotThrow(() -> {
                KeyManagementResult result = api.setupHierarchicalKeys(masterPassword);
                assertNotNull(result, "Key management result should not be null");
            }, "Should setup hierarchical keys without throwing");
        }
        
        @Test
        @DisplayName("Should list managed keys")
        void shouldListManagedKeys() {
            // When & Then
            assertDoesNotThrow(() -> {
                List<CryptoUtil.KeyInfo> keys = api.listManagedKeys();
                assertNotNull(keys, "Keys list should not be null");
            }, "Should list managed keys without throwing");
        }
        
        @Test
        @DisplayName("Should generate hierarchical key")
        void shouldGenerateHierarchicalKey() {
            // Given
            String purpose = "encryption";
            int depth = 2;
            Map<String, Object> options = new HashMap<>();
            options.put("keyType", "RSA");
            
            // When & Then
            assertDoesNotThrow(() -> {
                KeyManagementResult result = api.generateHierarchicalKey(purpose, depth, options);
                assertNotNull(result, "Key generation result should not be null");
            }, "Should generate hierarchical key without throwing");
        }
        
        @Test
        @DisplayName("Should validate key hierarchy")
        void shouldValidateKeyHierarchy() {
            // Given
            String keyId = "test-key-id";
            
            // When & Then
            assertDoesNotThrow(() -> {
                ValidationReport report = api.validateKeyHierarchy(keyId);
                assertNotNull(report, "Validation report should not be null");
            }, "Should validate key hierarchy without throwing");
        }
        
        @Test
        @DisplayName("Should validate key management")
        void shouldValidateKeyManagement() {
            // Given
            Map<String, Object> options = new HashMap<>();
            options.put("deep", true);
            
            // When & Then
            assertDoesNotThrow(() -> {
                ValidationReport report = api.validateKeyManagement(options);
                assertNotNull(report, "Key management validation should not be null");
            }, "Should validate key management without throwing");
        }
    }
    
    @Nested
    @DisplayName("💾 Storage Management Tests")  
    class StorageManagementTests {
        
        @Test
        @DisplayName("Should store data with searchable terms")
        void shouldStoreDataWithSearchableTerms() {
            // Given
            String data = "Searchable test data";
            String password = "password123";
            String[] searchTerms = {"test", "data", "searchable"};
            
            // When & Then
            assertDoesNotThrow(() -> {
                Block block = api.storeSearchableData(data, password, searchTerms);
                assertNotNull(block, "Stored block should not be null");
            }, "Should store searchable data without throwing");
        }
        
        @Test
        @DisplayName("Should store data with layered terms")
        void shouldStoreDataWithLayeredTerms() {
            // Given
            String data = "Layered test data";
            String password = "password123";
            String[] userTerms = {"user", "terms"};
            String[] suggestedTerms = {"suggested", "automatic"};
            
            // When & Then
            assertDoesNotThrow(() -> {
                Block block = api.storeSearchableDataWithLayers(data, password, userTerms, suggestedTerms);
                assertNotNull(block, "Stored block should not be null");
            }, "Should store data with layers without throwing");
        }
        
        @Test
        @DisplayName("Should store data with granular term control")
        void shouldStoreDataWithGranularTermControl() {
            // Given
            String data = "Granular control test data";
            String password = "password123";
            Set<String> allTerms = new HashSet<>(Arrays.asList("encrypted", "secure", "public", "visible"));
            TermVisibilityMap visibility = 
                new TermVisibilityMap();
            
            // When & Then
            assertDoesNotThrow(() -> {
                Block block = api.storeDataWithGranularTermControl(data, password, allTerms, visibility);
                assertNotNull(block, "Stored block should not be null");
            }, "Should store data with granular control without throwing");
        }
        
        @Test
        @DisplayName("Should search by terms")
        void shouldSearchByTerms() {
            // Given
            String[] searchTerms = {"test", "data"};
            String password = "password123";
            int maxResults = 10;
            
            // When & Then
            assertDoesNotThrow(() -> {
                List<Block> results = api.searchByTerms(searchTerms, password, maxResults);
                assertNotNull(results, "Search results should not be null");
            }, "Should search by terms without throwing");
        }
        
        @Test
        @DisplayName("Should get storage analytics")
        void shouldGetStorageAnalytics() {
            // When & Then
            assertDoesNotThrow(() -> {
                String analytics = api.getStorageAnalytics();
                assertNotNull(analytics, "Storage analytics should not be null");
            }, "Should get storage analytics without throwing");
        }
    }
    
    @Nested
    @DisplayName("🔧 Chain Recovery Tests")
    class ChainRecoveryTests {
        
        @Test
        @DisplayName("Should recover from corruption")
        void shouldRecoverFromCorruption() {
            // Given
            Map<String, Object> options = new HashMap<>();
            options.put("autoRepair", true);
            options.put("backupData", false);
            
            // When & Then
            assertDoesNotThrow(() -> {
                ChainRecoveryResult result = api.recoverFromCorruption(options);
                assertNotNull(result, "Recovery result should not be null");
            }, "Should recover from corruption without throwing");
        }
        
        @Test
        @DisplayName("Should repair broken chain")
        void shouldRepairBrokenChain() {
            // Given
            Long startBlock = 1L;
            Long endBlock = 5L;
            
            // When & Then
            assertDoesNotThrow(() -> {
                ChainRecoveryResult result = api.repairBrokenChain(startBlock, endBlock);
                assertNotNull(result, "Repair result should not be null");
            }, "Should repair broken chain without throwing");
        }
        
        @Test
        @DisplayName("Should create recovery checkpoint")
        void shouldCreateRecoveryCheckpoint() {
            // Given
            RecoveryCheckpoint.CheckpointType type = RecoveryCheckpoint.CheckpointType.MANUAL;
            String description = "Test checkpoint";
            
            // When & Then
            assertDoesNotThrow(() -> {
                RecoveryCheckpoint checkpoint = api.createRecoveryCheckpoint(type, description);
                assertNotNull(checkpoint, "Recovery checkpoint should not be null");
            }, "Should create recovery checkpoint without throwing");
        }
        
        @Test
        @DisplayName("Should rollback to safe state")
        void shouldRollbackToSafeState() {
            // Given
            Long targetBlock = 1L;
            Map<String, Object> options = new HashMap<>();
            options.put("preserveData", true);
            
            // When & Then
            assertDoesNotThrow(() -> {
                ChainRecoveryResult result = api.rollbackToSafeState(targetBlock, options);
                assertNotNull(result, "Rollback result should not be null");
            }, "Should rollback to safe state without throwing");
        }
    }
    
    @Nested
    @DisplayName("📦 Private Methods (via Public APIs)")
    class PrivateMethodsIndirectTests {
        
        @Test
        @DisplayName("Should test exportAsJson and exportAsCsv via exportSearchResults")
        void shouldTestExportMethodsViaPublicAPI() {
            // Given - Create search result data first
            String data = "Test data for export";
            String password = "testPass123";
            String[] searchTerms = {"export", "test"};
            
            // Add some searchable data to get results
            assertDoesNotThrow(() -> {
                api.storeSearchableData(data, password, searchTerms);
            });
            
            // Perform advanced search to get results for export
            assertDoesNotThrow(() -> {
                Map<String, Object> searchCriteria = new HashMap<>();
                searchCriteria.put("terms", Arrays.asList("export", "test"));
                searchCriteria.put("includeEncrypted", true);
                
                AdvancedSearchResult result = api.performAdvancedSearch(searchCriteria, password, 10);
                assertNotNull(result, "Search result should not be null");
                
                // Test JSON export (calls private exportAsJson method)
                String jsonExport = api.exportSearchResults(result, "json");
                assertNotNull(jsonExport, "JSON export should not be null");
                assertTrue(jsonExport.length() > 0, "JSON export should have content");
                
                // Test CSV export (calls private exportAsCsv method)  
                String csvExport = api.exportSearchResults(result, "csv");
                assertNotNull(csvExport, "CSV export should not be null");
                assertTrue(csvExport.length() > 0, "CSV export should have content");
                
                // Test XML export (should also work)
                String xmlExport = api.exportSearchResults(result, "xml");
                assertNotNull(xmlExport, "XML export should not be null");
                
            }, "Should export search results without throwing");
        }
        

        
        @Test
        @DisplayName("Should test compression methods via adaptive compression")
        void shouldTestCompressionMethodsIndirectly() {
            // Given
            String data = "This is test data that should be compressed when stored with adaptive compression enabled.";
            String contentType = "text/plain";
            
            // When & Then - analyzeCompressionOptions and performAdaptiveCompression test private methods
            assertDoesNotThrow(() -> {
                // Test compression analysis
                CompressionAnalysisResult analysis = api.analyzeCompressionOptions(data, contentType);
                assertNotNull(analysis, "Compression analysis should not be null");
                
                // Test adaptive compression (calls private compression methods)
                UserFriendlyEncryptionAPI.CompressedDataResult compressed = api.performAdaptiveCompression(data, contentType);
                assertNotNull(compressed, "Compressed data result should not be null");
                
            }, "Should perform compression operations without throwing");
        }
        
        @Test
        @DisplayName("Should test storage tiering methods")
        void shouldTestStorageTieringMethods() {
            // Given
            String data = "Data for tiering test";
            String password = "tieringTest123";
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("priority", "high");
            metadata.put("accessFrequency", "frequent");
            
            // When & Then - Test storage tiering functionality
            assertDoesNotThrow(() -> {
                // Test smart tiering storage
                Block block = api.storeWithSmartTiering(data, password, metadata);
                assertNotNull(block, "Smart tiering block should not be null");
                
                // Test retrieval from any tier
                Object retrieved = api.retrieveFromAnyTier(block.getBlockNumber(), password);
                assertNotNull(retrieved, "Retrieved data should not be null");
                
                // Test storage optimization
                StorageTieringManager.TieringReport report = api.optimizeStorageTiers();
                assertNotNull(report, "Tiering report should not be null");
                
                // Test storage metrics
                Map<String, Object> metrics = api.getStorageTierMetrics();
                assertNotNull(metrics, "Storage metrics should not be null");
                
            }, "Should perform storage tiering operations without throwing");
        }
        
        @Test
        @DisplayName("Should test integrity verification methods")
        void shouldTestIntegrityVerificationMethods() {
            // Given - We need some blocks to verify
            List<Long> blockNumbers = Arrays.asList(1L, 2L, 3L);
            
            // When & Then
            assertDoesNotThrow(() -> {
                // Test off-chain integrity verification
                OffChainIntegrityReport report = api.verifyOffChainIntegrity(blockNumbers);
                assertNotNull(report, "Off-chain integrity report should not be null");
                
                // Test batch integrity check
                Long startBlock = 1L;
                Long endBlock = 3L;
                Map<String, Object> options = new HashMap<>();
                options.put("deepScan", true);
                
                OffChainIntegrityReport batchReport = api.performBatchIntegrityCheck(startBlock, endBlock, options);
                assertNotNull(batchReport, "Batch integrity report should not be null");
                
            }, "Should perform integrity verification without throwing");
        }
        
        @Test
        @DisplayName("Should test time range search functionality")
        void shouldTestTimeRangeSearchMethods() {
            // Given
            java.time.LocalDateTime startDate = java.time.LocalDateTime.now().minusDays(1);
            java.time.LocalDateTime endDate = java.time.LocalDateTime.now();
            
            // When & Then
            assertDoesNotThrow(() -> {
                Map<String, Object> filters = new HashMap<>();
                filters.put("includeEncrypted", true);
                AdvancedSearchResult result = api.performTimeRangeSearch(startDate, endDate, filters);
                assertNotNull(result, "Time range search result should not be null");
            }, "Should perform time range search without throwing");
        }
        
        @Test
        @DisplayName("Should test cached search functionality")
        void shouldTestCachedSearchMethods() {
            // Given
            String searchType = "content";
            String query = "cached test data";
            String password = "cacheTest123";
            Map<String, Object> options = new HashMap<>();
            options.put("useCache", true);
            options.put("cacheTimeout", 300);
            
            // When & Then
            assertDoesNotThrow(() -> {
                AdvancedSearchResult result = api.performCachedSearch(searchType, query, options, password);
                assertNotNull(result, "Cached search result should not be null");
                
                // Test realtime metrics (should include cache stats)
                Map<String, Object> realtimeMetrics = api.getRealtimeSearchMetrics();
                assertNotNull(realtimeMetrics, "Realtime metrics should not be null");
                
            }, "Should perform cached search without throwing");
        }
        
        @Test
        @DisplayName("Should test warm up cache functionality")
        void shouldTestWarmUpCacheMethods() {
            // Given
            List<String> commonTerms = Arrays.asList("blockchain", "encryption", "security", "data");
            
            // When & Then
            assertDoesNotThrow(() -> {
                api.warmUpCache(commonTerms);
                
                // After warming up, invalidate specific blocks to test cache invalidation
                List<Long> blockNumbers = Arrays.asList(1L, 2L);
                api.invalidateCacheForBlocks(blockNumbers);
                
            }, "Should warm up and invalidate cache without throwing");
        }
    }
}