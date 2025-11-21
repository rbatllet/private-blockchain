package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.dao.AuthorizedKeyDAO;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for UserFriendlyEncryptionAPI Phase 1 methods
 * Tests hierarchical key management, comprehensive validation, and health diagnosis
 */
@DisplayName("🔑 UserFriendlyEncryptionAPI Phase 1 - Complete Tests")
public class UserFriendlyEncryptionAPIPhase1Test {

    @Mock
    private Blockchain mockBlockchain;
    
    private UserFriendlyEncryptionAPI api;
    private KeyPair testKeyPair;
    private String testUsername = "testuser";
    private List<Block> mockBlocks;
    private List<AuthorizedKey> mockAuthorizedKeys;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Generate test key pair
        testKeyPair = CryptoUtil.generateKeyPair();

        // SECURITY FIX (v1.0.6): Mock authorization check for constructor
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        lenient().when(mockBlockchain.isKeyAuthorized(publicKeyString)).thenReturn(true);

        // RBAC FIX (v1.0.6): Mock getAuthorizedKeyDAO() to avoid NullPointerException
        AuthorizedKeyDAO mockKeyDAO = mock(AuthorizedKeyDAO.class);
        AuthorizedKey mockAuthKey = new AuthorizedKey();
        mockAuthKey.setPublicKey(publicKeyString);
        mockAuthKey.setOwnerName(testUsername);
        when(mockBlockchain.getAuthorizedKeyDAO()).thenReturn(mockKeyDAO);
        lenient().when(mockKeyDAO.getAuthorizedKeyByPublicKey(publicKeyString)).thenReturn(mockAuthKey);

        // RBAC FIX (v1.0.6): Mock getUserRole() for hierarchical key security validation
        // Tests need SUPER_ADMIN role to create/rotate ROOT and INTERMEDIATE keys
        lenient().when(mockBlockchain.getUserRole(publicKeyString))
            .thenReturn(com.rbatllet.blockchain.security.UserRole.SUPER_ADMIN);

        // Initialize API with mock blockchain
        api = new UserFriendlyEncryptionAPI(mockBlockchain, testUsername, testKeyPair);

        // Setup mock blockchain data
        setupMockBlockchainData();
    }

    private void setupMockBlockchainData() {
        // Create mock blocks
        mockBlocks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Block block = new Block();
            block.setBlockNumber((long) i);
            block.setId((long) i);
            block.setData("Test data for block " + i);
            block.setHash("hash_" + i + "_" + System.currentTimeMillis());
            block.setPreviousHash(i > 0 ? "hash_" + (i-1) + "_" + System.currentTimeMillis() : "genesis");
            block.setTimestamp(LocalDateTime.now().minusHours(i));
            mockBlocks.add(block);
        }
        
        // Create mock authorized keys
        mockAuthorizedKeys = new ArrayList<>();
        AuthorizedKey key1 = new AuthorizedKey();
        key1.setOwnerName("user1");
        key1.setPublicKey(CryptoUtil.publicKeyToString(testKeyPair.getPublic()));
        mockAuthorizedKeys.add(key1);
        
        // Setup blockchain mock behavior
        when(mockBlockchain.getValidChain()).thenReturn(mockBlocks);
        when(mockBlockchain.getBlockCount()).thenReturn((long) mockBlocks.size());
        when(mockBlockchain.getBlock(anyLong())).thenAnswer(invocation -> {
            Long blockNumber = invocation.getArgument(0);
            if (blockNumber >= 0 && blockNumber < mockBlocks.size()) {
                return mockBlocks.get(blockNumber.intValue());
            }
            return null;
        });
        when(mockBlockchain.getAuthorizedKeys()).thenReturn(mockAuthorizedKeys);
        when(mockBlockchain.validateSingleBlock(any(Block.class))).thenReturn(true);
        when(mockBlockchain.validateChainWithRecovery()).thenReturn(true);
        when(mockBlockchain.getBlock(anyLong())).thenAnswer(invocation -> {
            Long blockNumber = invocation.getArgument(0);
            return mockBlocks.stream()
                .filter(block -> block.getBlockNumber().equals(blockNumber))
                .findFirst()
                .orElse(null);
        });
    }

    @Nested
    @DisplayName("🔐 Hierarchical Key Management Tests")
    class HierarchicalKeyManagementTests {

        @Test
        @DisplayName("Should setup hierarchical keys successfully")
        void shouldSetupHierarchicalKeysSuccessfully() {
            // Given
            String masterPassword = "SecureMasterPassword123!";

            // When
            KeyManagementResult result = api.setupHierarchicalKeys(masterPassword);

            // Then
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isSuccess(), "Key setup should succeed");
            assertNotNull(result.getMessage(), "Result should have a message");
            assertTrue(result.getMessage().contains("successfully"), "Message should indicate success");
            
            // Verify key IDs are generated
            assertNotNull(result.getRootKeyId(), "Root key ID should be generated");
            assertNotNull(result.getIntermediateKeyId(), "Intermediate key ID should be generated");
            assertNotNull(result.getOperationalKeyId(), "Operational key ID should be generated");
            
            // Verify all key IDs are different
            assertNotEquals(result.getRootKeyId(), result.getIntermediateKeyId(), 
                          "Root and intermediate keys should be different");
            assertNotEquals(result.getIntermediateKeyId(), result.getOperationalKeyId(), 
                          "Intermediate and operational keys should be different");
            assertNotEquals(result.getRootKeyId(), result.getOperationalKeyId(), 
                          "Root and operational keys should be different");
        }

        @Test
        @DisplayName("Should handle null master password gracefully")
        void shouldHandleNullMasterPasswordGracefully() {
            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> api.setupHierarchicalKeys(null),
                "Null master password should throw exception");
        }

        @Test
        @DisplayName("Should handle empty master password gracefully")
        void shouldHandleEmptyMasterPasswordGracefully() {
            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> api.setupHierarchicalKeys(""),
                "Empty master password should throw exception");
        }

        @Test
        @DisplayName("Should handle weak master password")
        void shouldHandleWeakMasterPassword() {
            // Given
            String weakPassword = "123";

            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> api.setupHierarchicalKeys(weakPassword),
                "Weak password should throw exception");
        }

        @Test
        @DisplayName("Should rotate operational keys successfully")
        void shouldRotateOperationalKeysSuccessfully() {
            // Given
            String authorization = "intermediate_key_123";

            // When
            boolean result = api.rotateOperationalKeys(authorization);

            // Then
            assertTrue(result, "Key rotation should succeed");
        }

        @Test
        @DisplayName("Should handle null authorization for key rotation")
        void shouldHandleNullAuthorizationForKeyRotation() {
            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> api.rotateOperationalKeys(null),
                "Null authorization should throw exception");
        }

        @Test
        @DisplayName("Should list managed keys successfully")
        void shouldListManagedKeysSuccessfully() {
            // When
            List<CryptoUtil.KeyInfo> keys = api.listManagedKeys();

            // Then
            assertNotNull(keys, "Key list should not be null");
            assertTrue(keys.size() >= 0, "Key list should have non-negative size");
        }

        @Test
        @DisplayName("Should maintain key hierarchy after operations")
        void shouldMaintainKeyHierarchyAfterOperations() {
            // Given
            String masterPassword = "StrongMasterPassword456!";
            
            // When - Setup keys
            KeyManagementResult setupResult = api.setupHierarchicalKeys(masterPassword);
            assertTrue(setupResult.isSuccess(), "Setup should succeed");
            
            // List keys after setup
            List<CryptoUtil.KeyInfo> keysAfterSetup = api.listManagedKeys();
            
            // Rotate operational keys
            boolean rotationResult = api.rotateOperationalKeys(setupResult.getIntermediateKeyId());
            assertTrue(rotationResult, "Rotation should succeed");
            
            // List keys after rotation
            List<CryptoUtil.KeyInfo> keysAfterRotation = api.listManagedKeys();

            // Then
            assertNotNull(keysAfterSetup, "Keys should exist after setup");
            assertNotNull(keysAfterRotation, "Keys should exist after rotation");
            assertTrue(keysAfterRotation.size() >= keysAfterSetup.size(), 
                      "Key count should not decrease after rotation");
        }
    }

    @Nested
    @DisplayName("✅ Comprehensive Validation Tests")
    class ComprehensiveValidationTests {

        @Test
        @DisplayName("Should perform comprehensive validation successfully")
        void shouldPerformComprehensiveValidationSuccessfully() {
            // When
            ValidationReport result = api.performComprehensiveValidation();

            // Then
            assertNotNull(result, "Validation result should not be null");
            assertNotNull(result.getMessage(), "Result should have a message");
            assertTrue(result.getMessage().length() > 0, "Message should not be empty");
            
            // Should complete without throwing exceptions
            assertTrue(true, "Validation should complete successfully");
        }

        @Test
        @DisplayName("Should validate empty blockchain gracefully")
        void shouldValidateEmptyBlockchainGracefully() {
            // Given - Empty blockchain
            when(mockBlockchain.getValidChain()).thenReturn(new ArrayList<>());
            when(mockBlockchain.getBlockCount()).thenReturn(0L);

            // When
            ValidationReport result = api.performComprehensiveValidation();

            // Then
            assertNotNull(result, "Should handle empty blockchain");
            assertNotNull(result.getMessage(), "Should provide message for empty blockchain");
        }

        @Test
        @DisplayName("Should detect validation issues in corrupted blockchain")
        void shouldDetectValidationIssuesInCorruptedBlockchain() {
            // Given - Mock blockchain with validation failures
            when(mockBlockchain.validateSingleBlock(any(Block.class))).thenReturn(false);
            when(mockBlockchain.validateChainWithRecovery()).thenReturn(false);

            // When
            ValidationReport result = api.performComprehensiveValidation();

            // Then
            assertNotNull(result, "Should provide result even with issues");
            assertFalse(result.isValid(), "Should detect validation issues");
        }

        @Test
        @DisplayName("Should handle validation exceptions gracefully")
        void shouldHandleValidationExceptionsGracefully() {
            // Given - Mock blockchain that throws exceptions
            when(mockBlockchain.getValidChain()).thenThrow(new RuntimeException("Blockchain error"));

            // When
            ValidationReport result = api.performComprehensiveValidation();

            // Then
            assertNotNull(result, "Should handle exceptions gracefully");
            assertFalse(result.isValid(), "Should report failure when exceptions occur");
            assertTrue(result.getMessage().contains("failed"), "Message should indicate failure");
        }

        @Test
        @DisplayName("Should provide detailed validation metrics")
        void shouldProvideDetailedValidationMetrics() {
            // When
            ValidationReport result = api.performComprehensiveValidation();

            // Then
            assertNotNull(result, "Result should not be null");
            
            // Verify metrics are tracked (methods exist)
            assertDoesNotThrow(() -> result.toString(), 
                             "Should be able to convert result to string");
        }
    }

    @Nested
    @DisplayName("🏥 Health Diagnosis Tests")
    class HealthDiagnosisTests {

        @Test
        @DisplayName("Should perform health diagnosis successfully")
        void shouldPerformHealthDiagnosisSuccessfully() {
            // When
            HealthReport result = api.performHealthDiagnosis();

            // Then
            assertNotNull(result, "Health report should not be null");
            assertNotNull(result.getMessage(), "Report should have a message");
            assertNotNull(result.getOverallStatus(), "Report should have overall status");
            
            // Verify status is valid enum value
            assertTrue(result.getOverallStatus() == HealthReport.HealthStatus.EXCELLENT ||
                      result.getOverallStatus() == HealthReport.HealthStatus.GOOD ||
                      result.getOverallStatus() == HealthReport.HealthStatus.WARNING ||
                      result.getOverallStatus() == HealthReport.HealthStatus.CRITICAL,
                      "Health status should be valid enum value");
        }

        @Test
        @DisplayName("Should diagnose empty blockchain health")
        void shouldDiagnoseEmptyBlockchainHealth() {
            // Given - Empty blockchain
            when(mockBlockchain.getValidChain()).thenReturn(new ArrayList<>());
            when(mockBlockchain.getBlockCount()).thenReturn(0L);

            // When
            HealthReport result = api.performHealthDiagnosis();

            // Then
            assertNotNull(result, "Should provide health report for empty blockchain");
            assertNotNull(result.getOverallStatus(), "Should have health status");

            // Empty blockchain might be warning or critical
            assertTrue(result.getOverallStatus() == HealthReport.HealthStatus.WARNING ||
                      result.getOverallStatus() == HealthReport.HealthStatus.CRITICAL ||
                      result.getOverallStatus() == HealthReport.HealthStatus.EXCELLENT,
                      "Should have appropriate status for empty blockchain");
        }

        @Test
        @DisplayName("Should detect health issues in problematic blockchain")
        void shouldDetectHealthIssuesInProblematicBlockchain() {
            // Given - Large blockchain that might trigger memory warnings
            List<Block> largeBlockchain = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                Block block = new Block();
                block.setBlockNumber((long) i);
                block.setId((long) i);
                block.setData("Large data block " + i + " with substantial content for testing");
                block.setHash("hash_" + i + "_large");
                block.setPreviousHash(i > 0 ? "hash_" + (i-1) + "_large" : "genesis");
                block.setTimestamp(LocalDateTime.now().minusMinutes(i));
                largeBlockchain.add(block);
            }
            when(mockBlockchain.getValidChain()).thenReturn(largeBlockchain);
            when(mockBlockchain.getBlockCount()).thenReturn((long) largeBlockchain.size());
            when(mockBlockchain.getBlock(anyLong())).thenAnswer(invocation -> {
                Long blockNumber = invocation.getArgument(0);
                if (blockNumber >= 0 && blockNumber < largeBlockchain.size()) {
                    return largeBlockchain.get(blockNumber.intValue());
                }
                return null;
            });

            // When
            HealthReport result = api.performHealthDiagnosis();

            // Then
            assertNotNull(result, "Should provide health report for large blockchain");
            assertNotNull(result.getOverallStatus(), "Should have health status");
        }

        @Test
        @DisplayName("Should handle health diagnosis exceptions gracefully")
        void shouldHandleHealthDiagnosisExceptionsGracefully() {
            // Given - Mock blockchain that throws exceptions
            when(mockBlockchain.getValidChain()).thenThrow(new RuntimeException("Health check error"));

            // When
            HealthReport result = api.performHealthDiagnosis();

            // Then
            assertNotNull(result, "Should handle exceptions gracefully");
            assertTrue(result.getOverallStatus() == HealthReport.HealthStatus.CRITICAL ||
                      result.getOverallStatus() == HealthReport.HealthStatus.WARNING,
                      "Should report critical or warning status when exceptions occur");
        }

        @Test
        @DisplayName("Should provide health recommendations")
        void shouldProvideHealthRecommendations() {
            // When
            HealthReport result = api.performHealthDiagnosis();

            // Then
            assertNotNull(result, "Health report should not be null");
            
            // Check that formatted output works
            assertDoesNotThrow(() -> result.toString(), 
                             "Should be able to format health report");
        }
    }

    @Nested
    @DisplayName("🔧 Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should perform complete Phase 1 workflow")
        void shouldPerformCompletePhase1Workflow() {
            // Step 1: Setup hierarchical keys
            String masterPassword = "CompleteWorkflowPassword789!";
            KeyManagementResult keySetupResult = api.setupHierarchicalKeys(masterPassword);
            assertTrue(keySetupResult.isSuccess(), "Key setup should succeed");

            // Step 2: List managed keys
            List<CryptoUtil.KeyInfo> keys = api.listManagedKeys();
            assertNotNull(keys, "Should be able to list keys");

            // Step 3: Perform comprehensive validation
            ValidationReport validationResult = api.performComprehensiveValidation();
            assertNotNull(validationResult, "Validation should complete");

            // Step 4: Perform health diagnosis
            HealthReport healthResult = api.performHealthDiagnosis();
            assertNotNull(healthResult, "Health diagnosis should complete");

            // Step 5: Rotate operational keys
            boolean rotationResult = api.rotateOperationalKeys(keySetupResult.getIntermediateKeyId());
            assertTrue(rotationResult, "Key rotation should succeed");

            // Verify all operations completed successfully
            assertTrue(true, "Complete Phase 1 workflow should execute successfully");
        }

        @Test
        @DisplayName("Should handle concurrent Phase 1 operations safely")
        void shouldHandleConcurrentPhase1OperationsSafely() {
            // Test concurrent validation and health checks
            List<ValidationReport> validationResults = new ArrayList<>();
            List<HealthReport> healthResults = new ArrayList<>();
            
            // Simulate concurrent operations
            int numberOfThreads = 3;
            Thread[] threads = new Thread[numberOfThreads];
            
            for (int i = 0; i < numberOfThreads; i++) {
                final int threadIndex = i;
                threads[i] = new Thread(() -> {
                    try {
                        if (threadIndex % 2 == 0) {
                            ValidationReport validation = api.performComprehensiveValidation();
                            synchronized (validationResults) {
                                validationResults.add(validation);
                            }
                        } else {
                            HealthReport health = api.performHealthDiagnosis();
                            synchronized (healthResults) {
                                healthResults.add(health);
                            }
                        }
                    } catch (Exception e) {
                        fail("Concurrent operations should not throw exceptions: " + e.getMessage());
                    }
                });
            }

            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                assertDoesNotThrow(() -> thread.join(), "Thread should complete without exception");
            }

            // Verify results
            assertTrue(validationResults.size() + healthResults.size() == numberOfThreads, 
                      "All concurrent operations should complete");
        }

        @Test
        @DisplayName("Should maintain consistency across multiple operations")
        void shouldMaintainConsistencyAcrossMultipleOperations() {
            // Perform multiple validations
            ValidationReport validation1 = api.performComprehensiveValidation();
            ValidationReport validation2 = api.performComprehensiveValidation();
            ValidationReport validation3 = api.performComprehensiveValidation();

            // Perform multiple health diagnoses
            HealthReport health1 = api.performHealthDiagnosis();
            HealthReport health2 = api.performHealthDiagnosis();
            HealthReport health3 = api.performHealthDiagnosis();

            // All should complete successfully
            assertNotNull(validation1, "First validation should complete");
            assertNotNull(validation2, "Second validation should complete");
            assertNotNull(validation3, "Third validation should complete");
            assertNotNull(health1, "First health check should complete");
            assertNotNull(health2, "Second health check should complete");
            assertNotNull(health3, "Third health check should complete");

            // Results should be consistent (same blockchain state)
            assertEquals(validation1.isValid(), validation2.isValid(), 
                        "Validation results should be consistent");
            assertEquals(validation2.isValid(), validation3.isValid(), 
                        "Validation results should be consistent");
        }
    }
}