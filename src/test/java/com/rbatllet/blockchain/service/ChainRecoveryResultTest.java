package com.rbatllet.blockchain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

@DisplayName("ChainRecoveryResult Tests")
class ChainRecoveryResultTest {

    private ChainRecoveryResult result;
    private LocalDateTime fixedStartTime;
    private LocalDateTime fixedEndTime;
    private List<ChainRecoveryResult.RecoveryAction> testActions;
    private ChainRecoveryResult.RecoveryStatistics testStats;

    @BeforeEach
    void setUp() {
        fixedStartTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        fixedEndTime = LocalDateTime.of(2024, 1, 1, 10, 30, 0);

        // Create test actions
        testActions = Arrays.asList(
            new ChainRecoveryResult.RecoveryAction("REPAIR", 1L, "Repaired block 1", true, Duration.ofSeconds(5)),
            new ChainRecoveryResult.RecoveryAction("REPAIR", 2L, "Repaired block 2", false, Duration.ofSeconds(3)),
            new ChainRecoveryResult.RecoveryAction("ROLLBACK", 3L, "Rolled back block 3", true, Duration.ofSeconds(2))
        );

        testStats = new ChainRecoveryResult.RecoveryStatistics()
            .withBlocksAnalyzed(10)
            .withCorruptedBlocks(3)
            .withBlocksRepaired(2)
            .withBlocksRolledBack(1);

        // Create result with complete constructor
        result = new ChainRecoveryResult("test-recovery-001",
            ChainRecoveryResult.RecoveryStatus.PARTIAL_SUCCESS,
            "Test recovery completed with some warnings",
            fixedStartTime, fixedEndTime, testActions, testStats);
    }

    @Nested
    @DisplayName("Basic Getters Tests")
    class BasicGettersTests {

        @Test
        @DisplayName("getStartTime() should return the correct start time")
        void shouldReturnCorrectStartTime() {
            assertEquals(fixedStartTime, result.getStartTime());
        }

        @Test
        @DisplayName("getEndTime() should return the correct end time")
        void shouldReturnCorrectEndTime() {
            assertEquals(fixedEndTime, result.getEndTime());
        }

        @Test
        @DisplayName("getStartTime() and getEndTime() should be consistent for simple constructor")
        void shouldHaveConsistentTimesForSimpleConstructor() {
            ChainRecoveryResult simpleResult = new ChainRecoveryResult("simple",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, "Simple test");

            assertNotNull(simpleResult.getStartTime());
            assertNotNull(simpleResult.getEndTime());
            // For simple constructor, both times should be very close (same or within milliseconds)
            assertTrue(Duration.between(simpleResult.getStartTime(), simpleResult.getEndTime()).toMillis() < 1000);
        }
    }

    @Nested
    @DisplayName("Metadata Management Tests")
    class MetadataTests {

        @Test
        @DisplayName("getMetadata() should return empty map initially")
        void shouldReturnEmptyMetadataInitially() {
            Map<String, Object> metadata = result.getMetadata();
            assertNotNull(metadata);
            assertTrue(metadata.isEmpty());
        }

        @Test
        @DisplayName("addMetadata() should add key-value pairs correctly")
        void shouldAddMetadataCorrectly() {
            // When
            result.addMetadata("database", "postgresql");
            result.addMetadata("version", "1.2.3");
            result.addMetadata("nodeCount", 5);

            // Then
            Map<String, Object> metadata = result.getMetadata();
            assertEquals(3, metadata.size());
            assertEquals("postgresql", metadata.get("database"));
            assertEquals("1.2.3", metadata.get("version"));
            assertEquals(5, metadata.get("nodeCount"));
        }

        @Test
        @DisplayName("addMetadata() should handle null values")
        void shouldHandleNullMetadataValues() {
            // When
            result.addMetadata("nullValue", null);
            result.addMetadata("normalValue", "test");

            // Then
            Map<String, Object> metadata = result.getMetadata();
            assertEquals(2, metadata.size());
            assertNull(metadata.get("nullValue"));
            assertEquals("test", metadata.get("normalValue"));
        }

        @Test
        @DisplayName("addMetadata() should allow method chaining")
        void shouldAllowMetadataMethodChaining() {
            // When & Then
            ChainRecoveryResult chainedResult = result
                .addMetadata("key1", "value1")
                .addMetadata("key2", "value2")
                .addMetadata("key3", "value3");

            assertSame(result, chainedResult);
            assertEquals(3, result.getMetadata().size());
        }

        @Test
        @DisplayName("addMetadata() should overwrite existing keys")
        void shouldOverwriteExistingMetadataKeys() {
            // When
            result.addMetadata("key", "original");
            result.addMetadata("key", "updated");

            // Then
            assertEquals("updated", result.getMetadata().get("key"));
            assertEquals(1, result.getMetadata().size());
        }

        @Test
        @DisplayName("getMetadata() should return unmodifiable map")
        void shouldReturnUnmodifiableMetadata() {
            result.addMetadata("test", "value");
            Map<String, Object> metadata = result.getMetadata();

            assertThrows(UnsupportedOperationException.class, () -> {
                metadata.put("newKey", "newValue");
            });
        }

        @Test
        @DisplayName("addMetadata() should handle null keys gracefully")
        void shouldHandleNullMetadataKeys() {
            assertDoesNotThrow(() -> {
                result.addMetadata(null, "value");
            });

            Map<String, Object> metadata = result.getMetadata();
            assertEquals("value", metadata.get(null));
        }
    }

    @Nested
    @DisplayName("Warnings Management Tests")
    class WarningsTests {

        @Test
        @DisplayName("getWarnings() should return empty list initially")
        void shouldReturnEmptyWarningsInitially() {
            List<String> warnings = result.getWarnings();
            assertNotNull(warnings);
            assertTrue(warnings.isEmpty());
        }

        @Test
        @DisplayName("addWarning() should add warnings correctly")
        void shouldAddWarningsCorrectly() {
            // When
            result.addWarning("First warning");
            result.addWarning("Second warning");
            result.addWarning("Third warning");

            // Then
            List<String> warnings = result.getWarnings();
            assertEquals(3, warnings.size());
            assertEquals("First warning", warnings.get(0));
            assertEquals("Second warning", warnings.get(1));
            assertEquals("Third warning", warnings.get(2));
        }

        @Test
        @DisplayName("addWarning() should handle null values")
        void shouldHandleNullWarnings() {
            // When
            result.addWarning(null);
            result.addWarning("Valid warning");

            // Then
            List<String> warnings = result.getWarnings();
            assertEquals(2, warnings.size());
            assertNull(warnings.get(0));
            assertEquals("Valid warning", warnings.get(1));
        }

        @Test
        @DisplayName("addWarning() should allow method chaining")
        void shouldAllowWarningMethodChaining() {
            // When & Then
            ChainRecoveryResult chainedResult = result
                .addWarning("Warning 1")
                .addWarning("Warning 2")
                .addWarning("Warning 3");

            assertSame(result, chainedResult);
            assertEquals(3, result.getWarnings().size());
        }

        @Test
        @DisplayName("addWarning() should allow duplicate warnings")
        void shouldAllowDuplicateWarnings() {
            // When
            result.addWarning("Duplicate warning");
            result.addWarning("Duplicate warning");
            result.addWarning("Different warning");

            // Then
            List<String> warnings = result.getWarnings();
            assertEquals(3, warnings.size());
            assertEquals("Duplicate warning", warnings.get(0));
            assertEquals("Duplicate warning", warnings.get(1));
            assertEquals("Different warning", warnings.get(2));
        }

        @Test
        @DisplayName("getWarnings() should return unmodifiable list")
        void shouldReturnUnmodifiableWarnings() {
            result.addWarning("Test warning");
            List<String> warnings = result.getWarnings();

            assertThrows(UnsupportedOperationException.class, () -> {
                warnings.add("New warning");
            });
        }

        @Test
        @DisplayName("addWarning() should handle empty strings")
        void shouldHandleEmptyStringWarnings() {
            // When
            result.addWarning("");
            result.addWarning("   ");
            result.addWarning("Valid warning");

            // Then
            List<String> warnings = result.getWarnings();
            assertEquals(3, warnings.size());
            assertEquals("", warnings.get(0));
            assertEquals("   ", warnings.get(1));
            assertEquals("Valid warning", warnings.get(2));
        }
    }

    @Nested
    @DisplayName("Actions Management Tests")
    class ActionsTests {

        @Test
        @DisplayName("addAction() should add recovery actions correctly")
        void shouldAddActionsCorrectly() {
            // Create a fresh result for this test
            ChainRecoveryResult freshResult = new ChainRecoveryResult("fresh",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, "Fresh test");

            // When
            ChainRecoveryResult.RecoveryAction action1 = new ChainRecoveryResult.RecoveryAction(
                "VALIDATE", 1L, "Validated block 1", true, Duration.ofSeconds(1));
            ChainRecoveryResult.RecoveryAction action2 = new ChainRecoveryResult.RecoveryAction(
                "REPAIR", 2L, "Failed to repair block 2", false, Duration.ofSeconds(2));

            freshResult.addAction(action1);
            freshResult.addAction(action2);

            // Then
            List<ChainRecoveryResult.RecoveryAction> actions = freshResult.getActions();
            assertEquals(2, actions.size());
            assertEquals("VALIDATE", actions.get(0).getActionType());
            assertEquals("REPAIR", actions.get(1).getActionType());
            assertTrue(actions.get(0).isSuccess());
            assertFalse(actions.get(1).isSuccess());
        }

        @Test
        @DisplayName("addAction() should allow method chaining")
        void shouldAllowActionMethodChaining() {
            ChainRecoveryResult freshResult = new ChainRecoveryResult("fresh",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, "Fresh test");

            ChainRecoveryResult.RecoveryAction action1 = new ChainRecoveryResult.RecoveryAction(
                "TYPE1", 1L, "Action 1", true, Duration.ofSeconds(1));
            ChainRecoveryResult.RecoveryAction action2 = new ChainRecoveryResult.RecoveryAction(
                "TYPE2", 2L, "Action 2", false, Duration.ofSeconds(2));

            // When & Then
            ChainRecoveryResult chainedResult = freshResult
                .addAction(action1)
                .addAction(action2);

            assertSame(freshResult, chainedResult);
            assertEquals(2, freshResult.getActions().size());
        }

        @Test
        @DisplayName("addAction() should handle null actions gracefully")
        void shouldHandleNullActions() {
            ChainRecoveryResult freshResult = new ChainRecoveryResult("fresh",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, "Fresh test");

            assertDoesNotThrow(() -> {
                freshResult.addAction(null);
            });

            List<ChainRecoveryResult.RecoveryAction> actions = freshResult.getActions();
            assertEquals(1, actions.size());
            assertNull(actions.get(0));
        }
    }

    @Nested
    @DisplayName("Failed Actions Analysis Tests")
    class FailedActionsTests {

        @Test
        @DisplayName("getFailedActions() should return only failed actions")
        void shouldReturnOnlyFailedActions() {
            List<ChainRecoveryResult.RecoveryAction> failedActions = result.getFailedActions();

            assertEquals(1, failedActions.size());
            assertEquals("REPAIR", failedActions.get(0).getActionType());
            assertEquals(2L, failedActions.get(0).getBlockNumber());
            assertFalse(failedActions.get(0).isSuccess());
        }

        @Test
        @DisplayName("getFailedActions() should return empty list when all actions succeed")
        void shouldReturnEmptyListWhenAllActionsSucceed() {
            // Create result with only successful actions
            List<ChainRecoveryResult.RecoveryAction> successfulActions = Arrays.asList(
                new ChainRecoveryResult.RecoveryAction("REPAIR", 1L, "Success 1", true, Duration.ofSeconds(1)),
                new ChainRecoveryResult.RecoveryAction("VALIDATE", 2L, "Success 2", true, Duration.ofSeconds(2))
            );

            ChainRecoveryResult successResult = new ChainRecoveryResult("success-test",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, "All successful",
                fixedStartTime, fixedEndTime, successfulActions, testStats);

            List<ChainRecoveryResult.RecoveryAction> failedActions = successResult.getFailedActions();
            assertTrue(failedActions.isEmpty());
        }

        @Test
        @DisplayName("getFailedActions() should return empty list when no actions exist")
        void shouldReturnEmptyListWhenNoActions() {
            ChainRecoveryResult emptyResult = new ChainRecoveryResult("empty",
                ChainRecoveryResult.RecoveryStatus.NOT_NEEDED, "No actions needed");

            List<ChainRecoveryResult.RecoveryAction> failedActions = emptyResult.getFailedActions();
            assertNotNull(failedActions);
            assertTrue(failedActions.isEmpty());
        }

        @Test
        @DisplayName("getFailedActions() should handle null actions in list")
        void shouldHandleNullActionsInList() {
            ChainRecoveryResult resultWithNull = new ChainRecoveryResult("test-null",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, "Test with null");

            resultWithNull.addAction(null);
            resultWithNull.addAction(new ChainRecoveryResult.RecoveryAction(
                "REPAIR", 1L, "Failed action", false, Duration.ofSeconds(1)));

            assertDoesNotThrow(() -> {
                List<ChainRecoveryResult.RecoveryAction> failedActions = resultWithNull.getFailedActions();
                // Should filter out null and include the failed action
                assertEquals(1, failedActions.size());
                assertFalse(failedActions.get(0).isSuccess());
            });
        }
    }

    @Nested
    @DisplayName("Actions Grouping Tests")
    class ActionsGroupingTests {

        @Test
        @DisplayName("groupActionsByType() should group actions by their type correctly")
        void shouldGroupActionsByTypeCorrectly() {
            Map<String, List<ChainRecoveryResult.RecoveryAction>> grouped = result.groupActionsByType();

            assertEquals(2, grouped.size());
            assertTrue(grouped.containsKey("REPAIR"));
            assertTrue(grouped.containsKey("ROLLBACK"));

            assertEquals(2, grouped.get("REPAIR").size());
            assertEquals(1, grouped.get("ROLLBACK").size());

            // Verify REPAIR actions
            List<ChainRecoveryResult.RecoveryAction> repairActions = grouped.get("REPAIR");
            assertEquals(1L, repairActions.get(0).getBlockNumber());
            assertEquals(2L, repairActions.get(1).getBlockNumber());

            // Verify ROLLBACK actions
            List<ChainRecoveryResult.RecoveryAction> rollbackActions = grouped.get("ROLLBACK");
            assertEquals(3L, rollbackActions.get(0).getBlockNumber());
        }

        @Test
        @DisplayName("groupActionsByType() should return empty map when no actions exist")
        void shouldReturnEmptyMapWhenNoActions() {
            ChainRecoveryResult emptyResult = new ChainRecoveryResult("empty",
                ChainRecoveryResult.RecoveryStatus.NOT_NEEDED, "No actions");

            Map<String, List<ChainRecoveryResult.RecoveryAction>> grouped = emptyResult.groupActionsByType();
            assertNotNull(grouped);
            assertTrue(grouped.isEmpty());
        }

        @Test
        @DisplayName("groupActionsByType() should handle single action type")
        void shouldHandleSingleActionType() {
            List<ChainRecoveryResult.RecoveryAction> singleTypeActions = Arrays.asList(
                new ChainRecoveryResult.RecoveryAction("VALIDATE", 1L, "Validate 1", true, Duration.ofSeconds(1)),
                new ChainRecoveryResult.RecoveryAction("VALIDATE", 2L, "Validate 2", false, Duration.ofSeconds(2)),
                new ChainRecoveryResult.RecoveryAction("VALIDATE", 3L, "Validate 3", true, Duration.ofSeconds(3))
            );

            ChainRecoveryResult singleTypeResult = new ChainRecoveryResult("single-type",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, "Single type test",
                fixedStartTime, fixedEndTime, singleTypeActions, testStats);

            Map<String, List<ChainRecoveryResult.RecoveryAction>> grouped = singleTypeResult.groupActionsByType();

            assertEquals(1, grouped.size());
            assertTrue(grouped.containsKey("VALIDATE"));
            assertEquals(3, grouped.get("VALIDATE").size());
        }

        @Test
        @DisplayName("groupActionsByType() should handle null actions gracefully")
        void shouldHandleNullActionsInGrouping() {
            ChainRecoveryResult resultWithNull = new ChainRecoveryResult("test-null",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, "Test with null");

            resultWithNull.addAction(null);
            resultWithNull.addAction(new ChainRecoveryResult.RecoveryAction(
                "REPAIR", 1L, "Normal action", true, Duration.ofSeconds(1)));

            assertDoesNotThrow(() -> {
                Map<String, List<ChainRecoveryResult.RecoveryAction>> grouped = resultWithNull.groupActionsByType();
                // Should handle null gracefully and group the non-null action
                assertTrue(grouped.containsKey("REPAIR"));
                assertEquals(1, grouped.get("REPAIR").size());
            });
        }
    }

    @Nested
    @DisplayName("Formatted Summary Tests")
    class FormattedSummaryTests {

        @Test
        @DisplayName("getFormattedSummary() should include all major sections")
        void shouldIncludeAllMajorSections() {
            // Add some warnings and recommendations for complete test
            result.addWarning("Test warning message");
            result.addRecommendation("Test recommendation");

            String summary = result.getFormattedSummary();

            assertNotNull(summary);
            assertFalse(summary.isEmpty());

            // Check for major sections
            assertTrue(summary.contains("Chain Recovery Report"));
            assertTrue(summary.contains("Recovery ID:"));
            assertTrue(summary.contains("Status:"));
            assertTrue(summary.contains("Message:"));
            assertTrue(summary.contains("Duration:"));
            assertTrue(summary.contains("Recovery Statistics:"));
            assertTrue(summary.contains("Recovery Actions:"));
            assertTrue(summary.contains("Warnings:"));
            assertTrue(summary.contains("Recommendations:"));
        }

        @Test
        @DisplayName("getFormattedSummary() should handle empty warnings and recommendations")
        void shouldHandleEmptyWarningsAndRecommendations() {
            String summary = result.getFormattedSummary();

            assertNotNull(summary);
            // Should not contain warning or recommendation sections when empty
            assertFalse(summary.contains("⚠️ Warnings:"));
            assertFalse(summary.contains("💡 Recommendations:"));
        }

        @Test
        @DisplayName("getFormattedSummary() should handle empty actions")
        void shouldHandleEmptyActions() {
            ChainRecoveryResult emptyResult = new ChainRecoveryResult("empty",
                ChainRecoveryResult.RecoveryStatus.NOT_NEEDED, "No actions");

            String summary = emptyResult.getFormattedSummary();

            assertNotNull(summary);
            assertFalse(summary.contains("🔄 Recovery Actions:"));
        }

        @Test
        @DisplayName("getFormattedSummary() should format statistics correctly")
        void shouldFormatStatisticsCorrectly() {
            String summary = result.getFormattedSummary();

            assertTrue(summary.contains("Blocks Analyzed: 10"));
            assertTrue(summary.contains("Corrupted Found: 3"));
            assertTrue(summary.contains("Blocks Repaired: 2"));
            assertTrue(summary.contains("Blocks Rolled Back: 1"));
            assertTrue(summary.contains("Success Rate:"));
        }

        @Test
        @DisplayName("getFormattedSummary() should format actions summary correctly")
        void shouldFormatActionsSummaryCorrectly() {
            String summary = result.getFormattedSummary();

            assertTrue(summary.contains("REPAIR: 1/2 successful"));
            assertTrue(summary.contains("ROLLBACK: 1/1 successful"));
        }

        @Test
        @DisplayName("getFormattedSummary() should handle very long messages")
        void shouldHandleLongMessages() {
            String longMessage = "A".repeat(1000);
            ChainRecoveryResult longMessageResult = new ChainRecoveryResult("long",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, longMessage);

            assertDoesNotThrow(() -> {
                String summary = longMessageResult.getFormattedSummary();
                assertNotNull(summary);
                assertTrue(summary.contains(longMessage));
            });
        }

        @Test
        @DisplayName("getFormattedSummary() should format duration correctly")
        void shouldFormatDurationCorrectly() {
            String summary = result.getFormattedSummary();

            // Should contain duration information (30 minutes in our test case)
            assertTrue(summary.contains("Duration:"));
            assertTrue(summary.contains("PT30M") || summary.contains("30M") || summary.contains("minutes"));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Robustness Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very large number of actions")
        void shouldHandleLargeNumberOfActions() {
            ChainRecoveryResult largeResult = new ChainRecoveryResult("large",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, "Large test");

            // Add 1000 actions
            for (int i = 0; i < 1000; i++) {
                largeResult.addAction(new ChainRecoveryResult.RecoveryAction(
                    "TYPE_" + (i % 5), (long) i, "Action " + i, i % 3 == 0, Duration.ofMillis(i)));
            }

            assertDoesNotThrow(() -> {
                assertEquals(1000, largeResult.getActions().size());
                Map<String, List<ChainRecoveryResult.RecoveryAction>> grouped = largeResult.groupActionsByType();
                assertEquals(5, grouped.size()); // TYPE_0 to TYPE_4

                List<ChainRecoveryResult.RecoveryAction> failed = largeResult.getFailedActions();
                assertTrue(failed.size() > 0);

                String summary = largeResult.getFormattedSummary();
                assertNotNull(summary);
            });
        }

        @Test
        @DisplayName("Should handle very large number of warnings")
        void shouldHandleLargeNumberOfWarnings() {
            ChainRecoveryResult largeWarningsResult = new ChainRecoveryResult("warnings",
                ChainRecoveryResult.RecoveryStatus.PARTIAL_SUCCESS, "Many warnings");

            // Add 1000 warnings
            for (int i = 0; i < 1000; i++) {
                largeWarningsResult.addWarning("Warning number " + i);
            }

            assertDoesNotThrow(() -> {
                assertEquals(1000, largeWarningsResult.getWarnings().size());
                String summary = largeWarningsResult.getFormattedSummary();
                assertNotNull(summary);
                assertTrue(summary.contains("⚠️ Warnings:"));
            });
        }

        @Test
        @DisplayName("Should handle concurrent modifications gracefully")
        void shouldHandleConcurrentModifications() {
            // This test verifies that the collections are properly protected
            ChainRecoveryResult concurrentResult = new ChainRecoveryResult("concurrent",
                ChainRecoveryResult.RecoveryStatus.SUCCESS, "Concurrent test");

            // Add some initial data
            concurrentResult.addWarning("Initial warning");
            concurrentResult.addMetadata("initial", "value");

            // Get references to the collections
            List<String> warnings = concurrentResult.getWarnings();
            Map<String, Object> metadata = concurrentResult.getMetadata();

            // Verify they are unmodifiable
            assertThrows(UnsupportedOperationException.class, () -> warnings.add("New warning"));
            assertThrows(UnsupportedOperationException.class, () -> metadata.put("new", "value"));

            // Verify internal state can still be modified through proper methods
            concurrentResult.addWarning("New warning through method");
            concurrentResult.addMetadata("new", "value through method");

            assertEquals(2, concurrentResult.getWarnings().size());
            assertEquals(2, concurrentResult.getMetadata().size());
        }

        @Test
        @DisplayName("Should handle extreme dates gracefully")
        void shouldHandleExtremeDates() {
            LocalDateTime veryOldDate = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
            LocalDateTime veryFutureDate = LocalDateTime.of(2200, 12, 31, 23, 59, 59);

            assertDoesNotThrow(() -> {
                ChainRecoveryResult extremeDateResult = new ChainRecoveryResult("extreme-dates",
                    ChainRecoveryResult.RecoveryStatus.SUCCESS, "Extreme dates test",
                    veryOldDate, veryFutureDate, Collections.emptyList(), new ChainRecoveryResult.RecoveryStatistics());

                assertEquals(veryOldDate, extremeDateResult.getStartTime());
                assertEquals(veryFutureDate, extremeDateResult.getEndTime());

                Duration extremeDuration = extremeDateResult.getTotalDuration();
                assertTrue(extremeDuration.toDays() > 100000); // Very long duration

                String summary = extremeDateResult.getFormattedSummary();
                assertNotNull(summary);
            });
        }
    }
}
