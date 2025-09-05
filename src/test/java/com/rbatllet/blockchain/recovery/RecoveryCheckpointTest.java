package com.rbatllet.blockchain.recovery;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.service.RecoveryCheckpoint;
import com.rbatllet.blockchain.service.RecoveryCheckpoint.CheckpointStatus;
import com.rbatllet.blockchain.service.RecoveryCheckpoint.CheckpointType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("RecoveryCheckpoint Test Suite")
class RecoveryCheckpointTest {

    private RecoveryCheckpoint checkpoint;
    private static final String TEST_ID = "test-checkpoint-001";
    private static final String TEST_DESCRIPTION = "Test checkpoint";
    private static final Long TEST_BLOCK_NUMBER = 100L;
    private static final String TEST_BLOCK_HASH =
        "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567";
    private static final long TEST_TOTAL_BLOCKS = 150L;

    @BeforeEach
    void setUp() {
        checkpoint = new RecoveryCheckpoint(
            TEST_ID,
            CheckpointType.MANUAL,
            TEST_DESCRIPTION,
            TEST_BLOCK_NUMBER,
            TEST_BLOCK_HASH,
            TEST_TOTAL_BLOCKS,
            1024L // dataSize
        );
    }

    @Nested
    @DisplayName("Constructor and Basic Getters")
    class ConstructorAndBasicGetters {

        @Test
        @DisplayName("Should create checkpoint with all required fields")
        void shouldCreateCheckpointWithAllFields() {
            assertNotNull(checkpoint);
            assertEquals(TEST_ID, checkpoint.getCheckpointId());
            assertEquals(CheckpointType.MANUAL, checkpoint.getType());
            assertEquals(TEST_DESCRIPTION, checkpoint.getDescription());
            assertEquals(TEST_BLOCK_NUMBER, checkpoint.getLastBlockNumber());
            assertEquals(TEST_BLOCK_HASH, checkpoint.getLastBlockHash());
            assertEquals(TEST_TOTAL_BLOCKS, checkpoint.getTotalBlocks());
            assertNotNull(checkpoint.getCreatedAt());
            assertEquals(CheckpointStatus.ACTIVE, checkpoint.getStatus());
        }

        @Test
        @DisplayName("Should handle null block hash")
        void shouldHandleNullBlockHash() {
            RecoveryCheckpoint checkpointWithNullHash = new RecoveryCheckpoint(
                "test-null-hash",
                CheckpointType.AUTOMATIC,
                "Test with null hash",
                50L,
                null,
                75L,
                512L
            );

            assertNull(checkpointWithNullHash.getLastBlockHash());
            assertNotNull(checkpointWithNullHash.toString());
            assertNotNull(checkpointWithNullHash.getFormattedInfo());
        }

        @Test
        @DisplayName("Should set default expiration based on checkpoint type")
        void shouldSetDefaultExpirationBasedOnType() {
            RecoveryCheckpoint manualCheckpoint = new RecoveryCheckpoint(
                "manual",
                CheckpointType.MANUAL,
                "Manual",
                1L,
                "hash",
                1L,
                100L
            );
            RecoveryCheckpoint automaticCheckpoint = new RecoveryCheckpoint(
                "auto",
                CheckpointType.AUTOMATIC,
                "Auto",
                1L,
                "hash",
                1L,
                100L
            );

            LocalDateTime manualExpiry = manualCheckpoint.getExpiresAt();
            LocalDateTime autoExpiry = automaticCheckpoint.getExpiresAt();

            assertNotNull(manualExpiry);
            assertNotNull(autoExpiry);
            assertTrue(manualExpiry.isAfter(autoExpiry));
        }
    }

    @Nested
    @DisplayName("Status Management")
    class StatusManagement {

        @Test
        @DisplayName("getStatus() should return current status")
        void getStatusShouldReturnCurrentStatus() {
            assertEquals(CheckpointStatus.ACTIVE, checkpoint.getStatus());
        }

        @Test
        @DisplayName("updateStatus() should update status correctly")
        void updateStatusShouldUpdateCorrectly() {
            RecoveryCheckpoint result = checkpoint.updateStatus(
                CheckpointStatus.EXPIRED
            );

            assertSame(checkpoint, result); // Should return self for chaining
            assertEquals(CheckpointStatus.EXPIRED, checkpoint.getStatus());
        }

        @ParameterizedTest
        @EnumSource(CheckpointStatus.class)
        @DisplayName("updateStatus() should handle all status types")
        void updateStatusShouldHandleAllTypes(CheckpointStatus status) {
            checkpoint.updateStatus(status);
            assertEquals(status, checkpoint.getStatus());
        }

        @Test
        @DisplayName("updateStatus() should throw exception for null")
        void updateStatusShouldThrowForNull() {
            // Now the robust implementation throws exception for null
            assertThrows(NullPointerException.class, () -> {
                checkpoint.updateStatus(null);
            });
        }
    }

    @Nested
    @DisplayName("Expiration Management")
    class ExpirationManagement {

        @Test
        @DisplayName("getExpiresAt() should return expiration date")
        void getExpiresAtShouldReturnExpirationDate() {
            LocalDateTime expiresAt = checkpoint.getExpiresAt();
            assertNotNull(expiresAt);
            assertTrue(expiresAt.isAfter(checkpoint.getCreatedAt()));
        }

        @Test
        @DisplayName("setExpirationDate() should update expiration")
        void setExpirationDateShouldUpdate() {
            LocalDateTime newExpiry = LocalDateTime.now().plusDays(10);
            RecoveryCheckpoint result = checkpoint.setExpirationDate(newExpiry);

            assertSame(checkpoint, result); // Should return self for chaining
            assertEquals(newExpiry, checkpoint.getExpiresAt());
        }

        @Test
        @DisplayName("setExpirationDate() should handle null")
        void setExpirationDateShouldHandleNull() {
            checkpoint.setExpirationDate(null);
            assertNull(checkpoint.getExpiresAt());
        }

        @Test
        @DisplayName("isExpired() should return false for future expiration")
        void isExpiredShouldReturnFalseForFuture() {
            checkpoint.setExpirationDate(LocalDateTime.now().plusDays(1));
            assertFalse(checkpoint.isExpired());
        }

        @Test
        @DisplayName("isExpired() should return true for past expiration")
        void isExpiredShouldReturnTrueForPast() {
            checkpoint.setExpirationDate(LocalDateTime.now().minusDays(1));
            assertTrue(checkpoint.isExpired());
        }

        @Test
        @DisplayName("isExpired() should return false for null expiration")
        void isExpiredShouldReturnFalseForNull() {
            checkpoint.setExpirationDate(null);
            assertFalse(checkpoint.isExpired());
        }
    }

    @Nested
    @DisplayName("Data Size Management")
    class DataSizeManagement {

        @Test
        @DisplayName("getDataSize() should return data size")
        void getDataSizeShouldReturnSize() {
            // Now dataSize is properly set from constructor
            long dataSize = checkpoint.getDataSize();
            assertEquals(1024L, dataSize);
        }

        @Test
        @DisplayName("getDataSizeMB() should return size in MB")
        void getDataSizeMBShouldReturnSizeInMB() {
            // Now returns correct MB calculation (1024 bytes = 0.0009765625 MB)
            double dataSizeMB = checkpoint.getDataSizeMB();
            assertEquals(1024.0 / (1024.0 * 1024.0), dataSizeMB, 0.001);
        }

        @Test
        @DisplayName("getDataSizeMB() calculation should be correct for 1MB")
        void getDataSizeMBCalculationShouldBeCorrect() {
            // Test with exactly 1MB of data
            RecoveryCheckpoint largeCp = new RecoveryCheckpoint(
                "large",
                CheckpointType.MANUAL,
                "Large",
                1L,
                "hash",
                1L,
                1048576L
            );
            double result = largeCp.getDataSizeMB();
            assertEquals(1.0, result, 0.001);
        }
    }

    @Nested
    @DisplayName("Age Calculation")
    class AgeCalculation {

        @Test
        @DisplayName("getAgeInHours() should return non-negative age")
        void getAgeInHoursShouldReturnNonNegativeAge() {
            long age = checkpoint.getAgeInHours();
            assertTrue(age >= 0, "Age should not be negative");
        }

        @Test
        @DisplayName("getAgeInHours() should be consistent with creation time")
        void getAgeInHoursShouldBeConsistent() {
            // Create checkpoint and immediately check age
            LocalDateTime beforeCreation = LocalDateTime.now();
            RecoveryCheckpoint newCheckpoint = new RecoveryCheckpoint(
                "age-test",
                CheckpointType.MANUAL,
                "Age test",
                1L,
                "hash",
                1L,
                100L
            );
            LocalDateTime afterCreation = LocalDateTime.now();

            long age = newCheckpoint.getAgeInHours();

            // Age should be 0 or very small for a just-created checkpoint
            assertTrue(
                age <= 1,
                "Newly created checkpoint should have age <= 1 hour"
            );
        }
    }

    @Nested
    @DisplayName("Validation Methods")
    class ValidationMethods {

        @Test
        @DisplayName(
            "isValid() should return true for active non-expired checkpoint"
        )
        void isValidShouldReturnTrueForActiveNonExpired() {
            checkpoint.updateStatus(CheckpointStatus.ACTIVE);
            checkpoint.setExpirationDate(LocalDateTime.now().plusDays(1));

            assertTrue(checkpoint.isValid());
        }

        @Test
        @DisplayName("isValid() should return false for non-active checkpoint")
        void isValidShouldReturnFalseForNonActive() {
            checkpoint.updateStatus(CheckpointStatus.CORRUPTED);
            checkpoint.setExpirationDate(LocalDateTime.now().plusDays(1));

            assertFalse(checkpoint.isValid());
        }

        @Test
        @DisplayName("isValid() should return false for expired checkpoint")
        void isValidShouldReturnFalseForExpired() {
            checkpoint.updateStatus(CheckpointStatus.ACTIVE);
            checkpoint.setExpirationDate(LocalDateTime.now().minusDays(1));

            assertFalse(checkpoint.isValid());
        }

        @Test
        @DisplayName(
            "isValid() should return true for active checkpoint with null expiration"
        )
        void isValidShouldReturnTrueForNullExpiration() {
            checkpoint.updateStatus(CheckpointStatus.ACTIVE);
            checkpoint.setExpirationDate(null);

            assertTrue(checkpoint.isValid());
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {

        @Test
        @DisplayName("toString() should return formatted string")
        void toStringShouldReturnFormattedString() {
            String result = checkpoint.toString();

            assertNotNull(result);
            assertTrue(result.contains(TEST_ID));
            assertTrue(result.contains("MANUAL"));
            assertTrue(result.contains(TEST_BLOCK_NUMBER.toString()));
            assertTrue(result.contains("ACTIVE"));
        }

        @Test
        @DisplayName("toString() should handle null block hash")
        void toStringShouldHandleNullBlockHash() {
            RecoveryCheckpoint checkpointWithNull = new RecoveryCheckpoint(
                "null-test",
                CheckpointType.AUTOMATIC,
                "Null test",
                null,
                null,
                0L,
                0L
            );

            String result = checkpointWithNull.toString();
            assertNotNull(result);
            assertTrue(result.contains("null-test"));
        }

        @Test
        @DisplayName("getFormattedInfo() should return detailed information")
        void getFormattedInfoShouldReturnDetailedInfo() {
            String info = checkpoint.getFormattedInfo();

            assertNotNull(info);
            assertTrue(info.contains("Recovery Checkpoint"));
            assertTrue(info.contains(TEST_ID));
            assertTrue(info.contains(TEST_DESCRIPTION));
            assertTrue(info.contains("Manual"));
            assertTrue(info.contains("Active"));
            assertTrue(info.contains(TEST_BLOCK_NUMBER.toString()));
            assertTrue(info.contains(String.valueOf(TEST_TOTAL_BLOCKS)));
        }

        @Test
        @DisplayName(
            "getFormattedInfo() should handle null block hash gracefully"
        )
        void getFormattedInfoShouldHandleNullBlockHash() {
            RecoveryCheckpoint checkpointWithNull = new RecoveryCheckpoint(
                "null-test",
                CheckpointType.EMERGENCY,
                "Emergency test",
                50L,
                null,
                100L,
                200L
            );

            String info = checkpointWithNull.getFormattedInfo();
            assertNotNull(info);
            assertTrue(info.contains("null"));
        }

        @Test
        @DisplayName("getFormattedInfo() should truncate long block hashes")
        void getFormattedInfoShouldTruncateLongHashes() {
            String longHash = "a".repeat(100);
            RecoveryCheckpoint checkpointWithLongHash = new RecoveryCheckpoint(
                "long-hash",
                CheckpointType.SCHEDULED,
                "Long hash test",
                25L,
                longHash,
                50L,
                300L
            );

            String info = checkpointWithLongHash.getFormattedInfo();
            assertNotNull(info);
            // Should contain truncated hash with "..."
            assertTrue(info.contains("..."));
        }
    }

    @Nested
    @DisplayName("Chain State Management")
    class ChainStateManagement {

        @Test
        @DisplayName("addChainState() should return self for chaining")
        void addChainStateShouldReturnSelf() {
            RecoveryCheckpoint result = checkpoint.addChainState(
                "key1",
                "value1"
            );
            assertSame(checkpoint, result);
        }

        @Test
        @DisplayName("getChainState() should return unmodifiable map")
        void getChainStateShouldReturnUnmodifiableMap() {
            checkpoint.addChainState("key1", "value1");
            var chainState = checkpoint.getChainState();

            assertThrows(UnsupportedOperationException.class, () -> {
                chainState.put("key2", "value2");
            });
        }

        @Test
        @DisplayName("addCriticalHash() should return self for chaining")
        void addCriticalHashShouldReturnSelf() {
            RecoveryCheckpoint result = checkpoint.addCriticalHash("hash123");
            assertSame(checkpoint, result);
        }

        @Test
        @DisplayName("addCriticalHash() should throw exception for null")
        void addCriticalHashShouldThrowForNull() {
            assertThrows(NullPointerException.class, () -> {
                checkpoint.addCriticalHash(null);
            });
        }

        @Test
        @DisplayName(
            "addCriticalHash() should throw exception for empty string"
        )
        void addCriticalHashShouldThrowForEmpty() {
            assertThrows(IllegalArgumentException.class, () -> {
                checkpoint.addCriticalHash("");
            });
            assertThrows(IllegalArgumentException.class, () -> {
                checkpoint.addCriticalHash("   ");
            });
        }

        @Test
        @DisplayName("getCriticalHashes() should return unmodifiable list")
        void getCriticalHashesShouldReturnUnmodifiableList() {
            checkpoint.addCriticalHash("hash123");
            var hashes = checkpoint.getCriticalHashes();

            assertThrows(UnsupportedOperationException.class, () -> {
                hashes.add("hash456");
            });
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Conditions")
    class EdgeCasesAndErrorConditions {

        @Test
        @DisplayName("Should handle negative total blocks")
        void shouldHandleNegativeTotalBlocks() {
            // Now throws exception for negative values
            assertThrows(IllegalArgumentException.class, () -> {
                new RecoveryCheckpoint(
                    "negative-test",
                    CheckpointType.MANUAL,
                    "Negative test",
                    10L,
                    "hash",
                    -5L,
                    100L
                );
            });
        }

        @Test
        @DisplayName("Should handle zero total blocks")
        void shouldHandleZeroTotalBlocks() {
            RecoveryCheckpoint checkpointWithZero = new RecoveryCheckpoint(
                "zero-test",
                CheckpointType.MANUAL,
                "Zero test",
                0L,
                "hash",
                0L,
                0L
            );

            assertEquals(0L, checkpointWithZero.getTotalBlocks());
        }

        @Test
        @DisplayName("Should throw exception for empty checkpoint ID")
        void shouldThrowForEmptyCheckpointId() {
            assertThrows(IllegalArgumentException.class, () -> {
                new RecoveryCheckpoint(
                    "",
                    CheckpointType.MANUAL,
                    "Valid description",
                    1L,
                    "hash",
                    1L,
                    100L
                );
            });
            assertThrows(IllegalArgumentException.class, () -> {
                new RecoveryCheckpoint(
                    "   ",
                    CheckpointType.MANUAL,
                    "Valid description",
                    1L,
                    "hash",
                    1L,
                    100L
                );
            });
        }

        @Test
        @DisplayName("Should handle empty hash and trim description")
        void shouldHandleEmptyHashAndTrimDescription() {
            RecoveryCheckpoint checkpointWithEmpty = new RecoveryCheckpoint(
                "valid-id",
                CheckpointType.MANUAL,
                "  Description with spaces  ",
                1L,
                "",
                1L,
                100L
            );

            assertEquals("valid-id", checkpointWithEmpty.getCheckpointId());
            assertEquals(
                "Description with spaces",
                checkpointWithEmpty.getDescription()
            );
            assertEquals("", checkpointWithEmpty.getLastBlockHash());
        }

        @ParameterizedTest
        @EnumSource(CheckpointType.class)
        @DisplayName("Should work with all checkpoint types")
        void shouldWorkWithAllCheckpointTypes(CheckpointType type) {
            RecoveryCheckpoint testCheckpoint = new RecoveryCheckpoint(
                "type-test",
                type,
                "Type test",
                1L,
                "hash",
                1L,
                100L
            );

            assertEquals(type, testCheckpoint.getType());
            assertNotNull(testCheckpoint.toString());
            assertNotNull(testCheckpoint.getFormattedInfo());
        }

        @Test
        @DisplayName("Should throw for null required parameters")
        void shouldThrowForNullRequiredParameters() {
            assertThrows(NullPointerException.class, () -> {
                new RecoveryCheckpoint(
                    null,
                    CheckpointType.MANUAL,
                    "desc",
                    1L,
                    "hash",
                    1L,
                    100L
                );
            });
            assertThrows(NullPointerException.class, () -> {
                new RecoveryCheckpoint(
                    "id",
                    null,
                    "desc",
                    1L,
                    "hash",
                    1L,
                    100L
                );
            });
            assertThrows(NullPointerException.class, () -> {
                new RecoveryCheckpoint(
                    "id",
                    CheckpointType.MANUAL,
                    null,
                    1L,
                    "hash",
                    1L,
                    100L
                );
            });
        }

        @Test
        @DisplayName("Should throw for negative dataSize")
        void shouldThrowForNegativeDataSize() {
            assertThrows(IllegalArgumentException.class, () -> {
                new RecoveryCheckpoint(
                    "id",
                    CheckpointType.MANUAL,
                    "desc",
                    1L,
                    "hash",
                    1L,
                    -100L
                );
            });
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("isReadyForUse() should return same as isValid()")
        void isReadyForUseShouldReturnSameAsIsValid() {
            // Test when valid
            checkpoint.updateStatus(CheckpointStatus.ACTIVE);
            checkpoint.setExpirationDate(LocalDateTime.now().plusDays(1));

            assertEquals(checkpoint.isValid(), checkpoint.isReadyForUse());
            assertTrue(checkpoint.isReadyForUse());

            // Test when invalid (expired)
            checkpoint.setExpirationDate(LocalDateTime.now().minusDays(1));

            assertEquals(checkpoint.isValid(), checkpoint.isReadyForUse());
            assertFalse(checkpoint.isReadyForUse());

            // Test when invalid (not active)
            checkpoint.updateStatus(CheckpointStatus.CORRUPTED);
            checkpoint.setExpirationDate(LocalDateTime.now().plusDays(1));

            assertEquals(checkpoint.isValid(), checkpoint.isReadyForUse());
            assertFalse(checkpoint.isReadyForUse());
        }

        @Test
        @DisplayName(
            "needsAttention() should return true for expired checkpoints"
        )
        void needsAttentionShouldReturnTrueForExpired() {
            checkpoint.updateStatus(CheckpointStatus.EXPIRED);
            assertTrue(checkpoint.needsAttention());
        }

        @Test
        @DisplayName(
            "needsAttention() should return true for corrupted checkpoints"
        )
        void needsAttentionShouldReturnTrueForCorrupted() {
            checkpoint.updateStatus(CheckpointStatus.CORRUPTED);
            assertTrue(checkpoint.needsAttention());
        }

        @Test
        @DisplayName(
            "needsAttention() should return true for past expiration date"
        )
        void needsAttentionShouldReturnTrueForPastExpiration() {
            checkpoint.updateStatus(CheckpointStatus.ACTIVE);
            checkpoint.setExpirationDate(LocalDateTime.now().minusHours(1));
            assertTrue(checkpoint.needsAttention());
        }

        @Test
        @DisplayName(
            "needsAttention() should return false for healthy active checkpoint"
        )
        void needsAttentionShouldReturnFalseForHealthy() {
            checkpoint.updateStatus(CheckpointStatus.ACTIVE);
            checkpoint.setExpirationDate(LocalDateTime.now().plusDays(1));
            assertFalse(checkpoint.needsAttention());
        }

        @Test
        @DisplayName(
            "needsAttention() should return false for archived checkpoint"
        )
        void needsAttentionShouldReturnFalseForArchived() {
            checkpoint.updateStatus(CheckpointStatus.ARCHIVED);
            assertFalse(checkpoint.needsAttention());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("equals() should return true for same object")
        void equalsShouldReturnTrueForSameObject() {
            assertTrue(checkpoint.equals(checkpoint));
        }

        @Test
        @DisplayName("equals() should return false for null")
        void equalsShouldReturnFalseForNull() {
            assertFalse(checkpoint.equals(null));
        }

        @Test
        @DisplayName("equals() should return false for different class")
        void equalsShouldReturnFalseForDifferentClass() {
            assertFalse(checkpoint.equals("not a checkpoint"));
            assertFalse(checkpoint.equals(42));
        }

        @Test
        @DisplayName("equals() should return true for same checkpointId")
        void equalsShouldReturnTrueForSameId() {
            RecoveryCheckpoint other = new RecoveryCheckpoint(
                TEST_ID, // Same ID
                CheckpointType.EMERGENCY, // Different type
                "Different description",
                999L, // Different block number
                "different-hash",
                500L, // Different total blocks
                2048L // Different data size
            );

            assertTrue(checkpoint.equals(other));
            assertTrue(other.equals(checkpoint));
        }

        @Test
        @DisplayName("equals() should return false for different checkpointId")
        void equalsShouldReturnFalseForDifferentId() {
            RecoveryCheckpoint other = new RecoveryCheckpoint(
                "different-id",
                CheckpointType.MANUAL,
                TEST_DESCRIPTION,
                TEST_BLOCK_NUMBER,
                TEST_BLOCK_HASH,
                TEST_TOTAL_BLOCKS,
                1024L
            );

            assertFalse(checkpoint.equals(other));
            assertFalse(other.equals(checkpoint));
        }

        @Test
        @DisplayName("hashCode() should be same for equal objects")
        void hashCodeShouldBeSameForEqualObjects() {
            RecoveryCheckpoint other = new RecoveryCheckpoint(
                TEST_ID, // Same ID
                CheckpointType.EMERGENCY,
                "Different description",
                999L,
                "different-hash",
                500L,
                2048L
            );

            assertEquals(checkpoint.hashCode(), other.hashCode());
        }

        @Test
        @DisplayName(
            "hashCode() should be different for different checkpointIds"
        )
        void hashCodeShouldBeDifferentForDifferentIds() {
            RecoveryCheckpoint other = new RecoveryCheckpoint(
                "different-id",
                CheckpointType.MANUAL,
                TEST_DESCRIPTION,
                TEST_BLOCK_NUMBER,
                TEST_BLOCK_HASH,
                TEST_TOTAL_BLOCKS,
                1024L
            );

            assertNotEquals(checkpoint.hashCode(), other.hashCode());
        }

        @Test
        @DisplayName("hashCode() should be consistent across multiple calls")
        void hashCodeShouldBeConsistent() {
            int hash1 = checkpoint.hashCode();
            int hash2 = checkpoint.hashCode();
            int hash3 = checkpoint.hashCode();

            assertEquals(hash1, hash2);
            assertEquals(hash2, hash3);
        }

        @Test
        @DisplayName("equals() should be symmetric")
        void equalsShouldBeSymmetric() {
            RecoveryCheckpoint other = new RecoveryCheckpoint(
                TEST_ID,
                CheckpointType.SCHEDULED,
                "Different description",
                777L,
                "another-hash",
                300L,
                512L
            );

            boolean result1 = checkpoint.equals(other);
            boolean result2 = other.equals(checkpoint);

            assertEquals(result1, result2);
            assertTrue(result1); // Should be true since same ID
        }

        @Test
        @DisplayName("equals() should be transitive")
        void equalsShouldBeTransitive() {
            RecoveryCheckpoint other1 = new RecoveryCheckpoint(
                TEST_ID,
                CheckpointType.EMERGENCY,
                "Description 1",
                100L,
                "hash1",
                200L,
                256L
            );

            RecoveryCheckpoint other2 = new RecoveryCheckpoint(
                TEST_ID,
                CheckpointType.PRE_OPERATION,
                "Description 2",
                200L,
                "hash2",
                400L,
                128L
            );

            assertTrue(checkpoint.equals(other1));
            assertTrue(other1.equals(other2));
            assertTrue(checkpoint.equals(other2));
        }

        @Test
        @DisplayName("equals() and hashCode() should handle edge cases")
        void equalsAndHashCodeShouldHandleEdgeCases() {
            // Test with empty string ID (should be caught by validation in constructor)
            // But if somehow created, should still work properly for equals/hashCode

            // Test with very long IDs
            String longId = "a".repeat(1000);
            RecoveryCheckpoint longIdCheckpoint = new RecoveryCheckpoint(
                longId,
                CheckpointType.MANUAL,
                "Long ID test",
                1L,
                "hash",
                1L,
                100L
            );

            RecoveryCheckpoint anotherLongIdCheckpoint = new RecoveryCheckpoint(
                longId,
                CheckpointType.AUTOMATIC,
                "Another long ID test",
                2L,
                "different-hash",
                2L,
                200L
            );

            assertTrue(longIdCheckpoint.equals(anotherLongIdCheckpoint));
            assertEquals(
                longIdCheckpoint.hashCode(),
                anotherLongIdCheckpoint.hashCode()
            );
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should maintain consistency through multiple operations")
        void shouldMaintainConsistencyThroughOperations() {
            // Perform multiple operations
            checkpoint
                .addChainState("blocks", 150)
                .addChainState("difficulty", 12345)
                .addCriticalHash("hash1")
                .addCriticalHash("hash2")
                .setExpirationDate(LocalDateTime.now().plusDays(15))
                .updateStatus(CheckpointStatus.ARCHIVED);

            // Verify state
            assertEquals(CheckpointStatus.ARCHIVED, checkpoint.getStatus());
            assertEquals(2, checkpoint.getChainState().size());
            assertEquals(2, checkpoint.getCriticalHashes().size());
            assertFalse(checkpoint.isValid()); // ARCHIVED is not ACTIVE

            // Verify string representations work
            assertNotNull(checkpoint.toString());
            assertNotNull(checkpoint.getFormattedInfo());
        }

        @Test
        @DisplayName("Should handle checkpoint lifecycle")
        void shouldHandleCheckpointLifecycle() {
            // Start as ACTIVE
            assertTrue(checkpoint.isValid());
            assertEquals(CheckpointStatus.ACTIVE, checkpoint.getStatus());

            // Expire it
            checkpoint.setExpirationDate(LocalDateTime.now().minusMinutes(1));
            assertTrue(checkpoint.isExpired());
            assertFalse(checkpoint.isValid());

            // Mark as expired
            checkpoint.updateStatus(CheckpointStatus.EXPIRED);
            assertEquals(CheckpointStatus.EXPIRED, checkpoint.getStatus());
            assertFalse(checkpoint.isValid());

            // Archive it
            checkpoint.updateStatus(CheckpointStatus.ARCHIVED);
            assertEquals(CheckpointStatus.ARCHIVED, checkpoint.getStatus());
            assertFalse(checkpoint.isValid());
        }
    }
}
