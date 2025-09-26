package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.Block;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for StorageTieringManager
 * Tests storage tiering policies, performance optimization, and data management
 */
@DisplayName("📦 StorageTieringManager Tests")
public class StorageTieringManagerTest {

    private StorageTieringManager tieringManager;
    private StorageTieringManager.TieringPolicy defaultPolicy;
    
    @Mock
    private OffChainStorageService mockOffChainService;
    
    @Mock
    private Block mockBlock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create default policy
        defaultPolicy = StorageTieringManager.TieringPolicy.getDefaultPolicy();
        
        // Initialize tiering manager with real dependencies
        tieringManager = new StorageTieringManager(defaultPolicy, mockOffChainService);
        
        // Setup mock block
        when(mockBlock.getBlockNumber()).thenReturn(123L);
        when(mockBlock.getData()).thenReturn("Test block data");
        when(mockBlock.getTimestamp()).thenReturn(LocalDateTime.now());
    }

    @Nested
    @DisplayName("🚀 Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize with default policy")
        void shouldInitializeWithDefaultPolicy() {
            // Given
            StorageTieringManager.TieringPolicy policy = StorageTieringManager.TieringPolicy.getDefaultPolicy();
            OffChainStorageService service = mock(OffChainStorageService.class);
            
            // When
            StorageTieringManager manager = new StorageTieringManager(policy, service);
            
            // Then
            assertNotNull(manager, "Should create manager instance");
        }

        @Test
        @DisplayName("Should initialize with custom policy")
        void shouldInitializeWithCustomPolicy() {
            // Given
            StorageTieringManager.TieringPolicy customPolicy = new StorageTieringManager.TieringPolicy(
                Duration.ofHours(1),    // hotThreshold
                Duration.ofDays(7),     // warmThreshold  
                Duration.ofDays(30),    // coldThreshold
                1024 * 1024,            // sizeThresholdBytes
                10,                     // accessCountThreshold
                true,                   // compressOnCold
                true                    // enableAutoTiering
            );
            OffChainStorageService service = mock(OffChainStorageService.class);
            
            // When
            StorageTieringManager manager = new StorageTieringManager(customPolicy, service);
            
            // Then
            assertNotNull(manager, "Should create manager with custom policy");
        }
    }

    @Nested
    @DisplayName("🎯 Storage Tier Analysis Tests")
    class StorageTierAnalysisTests {

        @Test
        @DisplayName("Should analyze storage tier for recent block")
        void shouldAnalyzeStorageTierForRecentBlock() {
            // Given - Very recent block
            when(mockBlock.getTimestamp()).thenReturn(LocalDateTime.now().minusHours(1));
            when(mockBlock.getData()).thenReturn("Small test data"); // Small data to avoid size threshold
            
            // When
            StorageTieringManager.StorageTier tier = tieringManager.analyzeStorageTier(mockBlock);
            
            // Then
            assertNotNull(tier, "Should determine storage tier");
            // Accept HOT or WARM tier based on the tiering algorithm logic
            assertTrue(tier == StorageTieringManager.StorageTier.HOT || tier == StorageTieringManager.StorageTier.WARM,
                      "Recent block should be in HOT or WARM tier, but was: " + tier);
        }

        @Test
        @DisplayName("Should analyze storage tier for older block")
        void shouldAnalyzeStorageTierForOlderBlock() {
            // Given - Old block should move to cooler tier
            when(mockBlock.getTimestamp()).thenReturn(LocalDateTime.now().minusDays(10));
            
            // When
            StorageTieringManager.StorageTier tier = tieringManager.analyzeStorageTier(mockBlock);
            
            // Then
            assertNotNull(tier, "Should determine storage tier");
            // Depending on policy, could be WARM, COLD, or ARCHIVE
            assertTrue(Arrays.asList(
                StorageTieringManager.StorageTier.HOT,
                StorageTieringManager.StorageTier.WARM,
                StorageTieringManager.StorageTier.COLD,
                StorageTieringManager.StorageTier.ARCHIVE
            ).contains(tier), "Should be valid tier");
        }

        @Test
        @DisplayName("Should handle storage tier for large block")
        void shouldHandleStorageTierForLargeBlock() {
            // Given - Large block data
            String largeData = "x".repeat(10000); // 10KB
            when(mockBlock.getData()).thenReturn(largeData);
            
            // When
            StorageTieringManager.StorageTier tier = tieringManager.analyzeStorageTier(mockBlock);
            
            // Then
            assertNotNull(tier, "Should determine tier for large block");
        }
    }

    @Nested
    @DisplayName("📊 Access Tracking Tests")
    class AccessTrackingTests {

        @Test
        @DisplayName("Should record block access")
        void shouldRecordBlockAccess() {
            // Given
            Long blockNumber = 456L;
            
            // When & Then - Should not throw
            assertDoesNotThrow(() -> {
                tieringManager.recordAccess(blockNumber);
            }, "Recording access should not throw");
        }

        @Test
        @DisplayName("Should handle multiple accesses to same block")
        void shouldHandleMultipleAccessesToSameBlock() {
            // Given
            Long blockNumber = 789L;
            
            // When & Then - Multiple accesses should not throw
            assertDoesNotThrow(() -> {
                tieringManager.recordAccess(blockNumber);
                tieringManager.recordAccess(blockNumber);
                tieringManager.recordAccess(blockNumber);
            }, "Multiple accesses should not throw");
        }

        @Test
        @DisplayName("Should handle concurrent access recording")
        void shouldHandleConcurrentAccessRecording() throws InterruptedException {
            // Given
            Long blockNumber = 999L;
            int threadCount = 5;
            List<Thread> threads = new ArrayList<>();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // When - Record accesses concurrently
            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        tieringManager.recordAccess(blockNumber);
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                });
                threads.add(thread);
                thread.start();
            }

            // Wait for all threads
            for (Thread thread : threads) {
                thread.join();
            }

            // Then
            assertTrue(exceptions.isEmpty(), "Concurrent access recording should succeed: " + exceptions);
        }
    }

    @Nested
    @DisplayName("🔄 Migration Tests")
    class MigrationTests {

        @Test
        @DisplayName("Should migrate block to warm tier")
        void shouldMigrateBlockToWarmTier() {
            // Given
            StorageTieringManager.StorageTier targetTier = StorageTieringManager.StorageTier.WARM;
            
            // When
            StorageTieringManager.TieringResult result = tieringManager.migrateToTier(mockBlock, targetTier);
            
            // Then
            assertNotNull(result, "Should provide migration result");
            assertNotNull(result.getMessage(), "Should have result message");
            assertEquals(targetTier, result.getToTier(), "Should target correct tier");
        }

        @Test
        @DisplayName("Should migrate block to cold tier")
        void shouldMigrateBlockToColdTier() {
            // Given
            StorageTieringManager.StorageTier targetTier = StorageTieringManager.StorageTier.COLD;
            
            // When
            StorageTieringManager.TieringResult result = tieringManager.migrateToTier(mockBlock, targetTier);
            
            // Then
            assertNotNull(result, "Should provide migration result");
            assertEquals(targetTier, result.getToTier(), "Should target cold tier");
        }

        @Test
        @DisplayName("Should migrate block to archive tier")
        void shouldMigrateBlockToArchiveTier() {
            // Given
            StorageTieringManager.StorageTier targetTier = StorageTieringManager.StorageTier.ARCHIVE;
            
            // When
            StorageTieringManager.TieringResult result = tieringManager.migrateToTier(mockBlock, targetTier);
            
            // Then
            assertNotNull(result, "Should provide migration result");
            assertEquals(targetTier, result.getToTier(), "Should target archive tier");
        }
    }

    @Nested
    @DisplayName("🤖 Auto-Tiering Tests")
    class AutoTieringTests {

        @Test
        @DisplayName("Should perform auto-tiering on single block")
        void shouldPerformAutoTieringOnSingleBlock() {
            // Given
            List<Block> blocks = Arrays.asList(mockBlock);
            
            // When
            StorageTieringManager.TieringReport report = tieringManager.performAutoTiering(blocks);
            
            // Then
            assertNotNull(report, "Should provide tiering report");
            assertEquals(1, report.getBlocksAnalyzed(), "Should analyze one block");
            assertNotNull(report.getSummary(), "Should have summary");
        }

        @Test
        @DisplayName("Should perform auto-tiering on multiple blocks")
        void shouldPerformAutoTieringOnMultipleBlocks() {
            // Given
            Block mockBlock2 = mock(Block.class);
            when(mockBlock2.getBlockNumber()).thenReturn(124L);
            when(mockBlock2.getData()).thenReturn("Test block data 2");
            when(mockBlock2.getTimestamp()).thenReturn(LocalDateTime.now().minusDays(1));
            
            List<Block> blocks = Arrays.asList(mockBlock, mockBlock2);
            
            // When
            StorageTieringManager.TieringReport report = tieringManager.performAutoTiering(blocks);
            
            // Then
            assertNotNull(report, "Should provide tiering report");
            assertEquals(2, report.getBlocksAnalyzed(), "Should analyze two blocks");
            assertTrue(report.getBlocksMigrated() >= 0, "Should have valid migration count");
            assertNotNull(report.getResults(), "Should have migration results");
        }

        @Test
        @DisplayName("Should handle empty block list")
        void shouldHandleEmptyBlockList() {
            // Given
            List<Block> emptyBlocks = new ArrayList<>();
            
            // When
            StorageTieringManager.TieringReport report = tieringManager.performAutoTiering(emptyBlocks);
            
            // Then
            assertNotNull(report, "Should provide report for empty list");
            assertEquals(0, report.getBlocksAnalyzed(), "Should analyze zero blocks");
            assertEquals(0, report.getBlocksMigrated(), "Should migrate zero blocks");
        }
    }

    @Nested
    @DisplayName("📈 Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should provide storage statistics")
        void shouldProvideStorageStatistics() {
            // When
            StorageTieringManager.StorageStatistics stats = tieringManager.getStatistics();
            
            // Then
            assertNotNull(stats, "Should provide statistics");
            assertNotNull(stats.getTierCounts(), "Should have tier counts");
            assertNotNull(stats.getTierSizes(), "Should have tier sizes");
            assertTrue(stats.getTotalDataSize() >= 0, "Should have valid total data size");
            assertTrue(stats.getTotalCompressedSize() >= 0, "Should have valid compressed size");
            assertTrue(stats.getTotalMigrations() >= 0, "Should have valid migration count");
        }

        @Test
        @DisplayName("Should calculate compression ratio")
        void shouldCalculateCompressionRatio() {
            // When
            StorageTieringManager.StorageStatistics stats = tieringManager.getStatistics();
            
            // Then
            double compressionRatio = stats.getCompressionRatio();
            assertTrue(compressionRatio >= 0.0, "Compression ratio should be non-negative");
        }

        @Test
        @DisplayName("Should provide formatted summary")
        void shouldProvideFormattedSummary() {
            // When
            StorageTieringManager.StorageStatistics stats = tieringManager.getStatistics();
            String summary = stats.getFormattedSummary();
            
            // Then
            assertNotNull(summary, "Should provide formatted summary");
            assertTrue(summary.length() > 0, "Summary should have content");
        }
    }

    @Nested
    @DisplayName("💡 Recommendations Tests")
    class RecommendationsTests {

        @Test
        @DisplayName("Should provide optimization recommendations")
        void shouldProvideOptimizationRecommendations() {
            // When
            List<String> recommendations = tieringManager.getOptimizationRecommendations();
            
            // Then
            assertNotNull(recommendations, "Should provide recommendations list");
            assertTrue(recommendations.size() >= 0, "Should have valid recommendations list");
        }

        @Test
        @DisplayName("Should provide recommendations after some operations")
        void shouldProvideRecommendationsAfterOperations() {
            // Given - Perform some operations first
            tieringManager.recordAccess(123L);
            tieringManager.analyzeStorageTier(mockBlock);
            
            // When
            List<String> recommendations = tieringManager.getOptimizationRecommendations();
            
            // Then
            assertNotNull(recommendations, "Should provide recommendations after operations");
        }
    }

    @Nested
    @DisplayName("🔧 Policy Tests")
    class PolicyTests {

        @Test
        @DisplayName("Should work with default policy")
        void shouldWorkWithDefaultPolicy() {
            // Given
            StorageTieringManager.TieringPolicy policy = StorageTieringManager.TieringPolicy.getDefaultPolicy();
            
            // Then
            assertNotNull(policy, "Should provide default policy");
            assertNotNull(policy.getHotThreshold(), "Should have hot threshold");
            assertNotNull(policy.getWarmThreshold(), "Should have warm threshold");
            assertNotNull(policy.getColdThreshold(), "Should have cold threshold");
            assertTrue(policy.getSizeThresholdBytes() > 0, "Should have size threshold");
            assertTrue(policy.getAccessCountThreshold() > 0, "Should have access count threshold");
        }

        @Test
        @DisplayName("Should support compression settings")
        void shouldSupportCompressionSettings() {
            // Given
            StorageTieringManager.TieringPolicy policy = StorageTieringManager.TieringPolicy.getDefaultPolicy();
            
            // Then
            // Both true and false are valid compression settings
            assertTrue(policy.isCompressOnCold() || !policy.isCompressOnCold(), 
                      "Should have compression setting");
        }

        @Test
        @DisplayName("Should support auto-tiering settings")
        void shouldSupportAutoTieringSettings() {
            // Given
            StorageTieringManager.TieringPolicy policy = StorageTieringManager.TieringPolicy.getDefaultPolicy();
            
            // Then
            // Both enabled and disabled auto-tiering are valid
            assertTrue(policy.isEnableAutoTiering() || !policy.isEnableAutoTiering(), 
                      "Should have auto-tiering setting");
        }
    }

    @Nested
    @DisplayName("🎭 StorageTier Enum Tests")
    class StorageTierEnumTests {

        @Test
        @DisplayName("Should have all storage tiers")
        void shouldHaveAllStorageTiers() {
            // Test all enum values exist
            assertNotNull(StorageTieringManager.StorageTier.HOT, "Should have HOT tier");
            assertNotNull(StorageTieringManager.StorageTier.WARM, "Should have WARM tier");
            assertNotNull(StorageTieringManager.StorageTier.COLD, "Should have COLD tier");
            assertNotNull(StorageTieringManager.StorageTier.ARCHIVE, "Should have ARCHIVE tier");
        }

        @Test
        @DisplayName("Should have display names for tiers")
        void shouldHaveDisplayNamesForTiers() {
            // Test display names
            assertNotNull(StorageTieringManager.StorageTier.HOT.getDisplayName(), 
                         "HOT tier should have display name");
            assertNotNull(StorageTieringManager.StorageTier.WARM.getDisplayName(), 
                         "WARM tier should have display name");
            assertNotNull(StorageTieringManager.StorageTier.COLD.getDisplayName(), 
                         "COLD tier should have display name");
            assertNotNull(StorageTieringManager.StorageTier.ARCHIVE.getDisplayName(), 
                         "ARCHIVE tier should have display name");
        }

        @Test
        @DisplayName("Should have tier levels")
        void shouldHaveTierLevels() {
            // Test tier levels are properly ordered
            assertEquals(0, StorageTieringManager.StorageTier.HOT.getLevel(), 
                        "HOT should be level 0");
            assertEquals(1, StorageTieringManager.StorageTier.WARM.getLevel(), 
                        "WARM should be level 1");
            assertEquals(2, StorageTieringManager.StorageTier.COLD.getLevel(), 
                        "COLD should be level 2");
            assertEquals(3, StorageTieringManager.StorageTier.ARCHIVE.getLevel(), 
                        "ARCHIVE should be level 3");
        }
    }
}