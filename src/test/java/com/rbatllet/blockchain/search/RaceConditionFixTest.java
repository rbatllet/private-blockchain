package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.search.metadata.BlockMetadataLayers;
import com.rbatllet.blockchain.search.metadata.MetadataLayerManager;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.entity.Block;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.PrivateKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test to verify race condition fix in SearchFrameworkEngine
 * 
 * This test ensures that when multiple threads try to index the same block
 * simultaneously, only one thread actually processes it and generates metadata,
 * preventing the massive MetadataLayerManager overprocessing issue.
 */
public class RaceConditionFixTest {

    @Mock
    private MetadataLayerManager metadataManager;
    
    @Mock
    private Block mockBlock;
    
    @Mock
    private PrivateKey privateKey;
    
    @Mock
    private EncryptionConfig config;
    
    private SearchFrameworkEngine searchEngine;
    private AtomicInteger metadataGenerationCalls;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        metadataGenerationCalls = new AtomicInteger(0);
        
        // Mock block setup
        when(mockBlock.getHash()).thenReturn("test-block-hash-12345");
        when(mockBlock.isDataEncrypted()).thenReturn(false);
        
        // Mock metadata generation with counter
        when(metadataManager.generateMetadataLayers(any(), any(), any(), any()))
            .thenAnswer(invocation -> {
                metadataGenerationCalls.incrementAndGet();
                Thread.sleep(10); // Simulate work to increase race condition window
                return mock(BlockMetadataLayers.class);
            });
        
        // Create search engine and replace metadataManager via reflection
        searchEngine = new SearchFrameworkEngine();
        try {
            java.lang.reflect.Field field = SearchFrameworkEngine.class.getDeclaredField("metadataManager");
            field.setAccessible(true);
            field.set(searchEngine, metadataManager);
        } catch (Exception e) {
            fail("Failed to inject mock metadata manager: " + e.getMessage());
        }
    }    
    @Test
    public void testConcurrentIndexing_PreventsDuplicateProcessing() throws InterruptedException {
        final int NUM_THREADS = 10;
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("TestWorker-", 0).factory()); // Java 25 Virtual Threads;
        
        // Submit multiple concurrent indexing tasks for the same block
        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    searchEngine.indexBlock(mockBlock, "testPassword", privateKey, config);
                } catch (Exception e) {
                    fail("Indexing should not throw exceptions: " + e.getMessage());
                }
            });
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), 
                  "All indexing tasks should complete within 5 seconds");
        
        // CRITICAL ASSERTION: Metadata should only be generated ONCE despite 10 concurrent attempts
        assertEquals(1, metadataGenerationCalls.get(),
                    "Metadata generation should be called exactly once, not " + 
                    metadataGenerationCalls.get() + " times");
    }
    
    @Test
    public void testProcessingPlaceholder_Functionality() {
        // Verify the placeholder works correctly
        BlockMetadataLayers placeholder = BlockMetadataLayers.PROCESSING_PLACEHOLDER;
        
        assertNotNull(placeholder);
        assertTrue(placeholder.isProcessingPlaceholder());
        
        // Normal metadata should not be a placeholder
        BlockMetadataLayers normalMetadata = mock(BlockMetadataLayers.class);
        when(normalMetadata.isProcessingPlaceholder()).thenReturn(false);
        assertFalse(normalMetadata.isProcessingPlaceholder());
    }
    
    @Test
    public void testAtomicCheckAndReserve_MapBehavior() {
        ConcurrentHashMap<String, BlockMetadataLayers> testMap = new ConcurrentHashMap<>();
        String key = "test-key";
        
        // First putIfAbsent should return null (key was absent)
        BlockMetadataLayers result1 = testMap.putIfAbsent(key, BlockMetadataLayers.PROCESSING_PLACEHOLDER);
        assertNull(result1, "First putIfAbsent should return null");
        
        // Second putIfAbsent should return the existing placeholder
        BlockMetadataLayers result2 = testMap.putIfAbsent(key, BlockMetadataLayers.PROCESSING_PLACEHOLDER);
        assertNotNull(result2, "Second putIfAbsent should return existing value");
        assertTrue(result2.isProcessingPlaceholder(), "Should return the processing placeholder");
    }
}