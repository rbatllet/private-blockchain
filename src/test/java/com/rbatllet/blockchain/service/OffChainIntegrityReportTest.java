package com.rbatllet.blockchain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for OffChainIntegrityReport
 * Validates integrity reporting functionality for off-chain data verification
 */
public class OffChainIntegrityReportTest {

    private static final Logger logger = LoggerFactory.getLogger(
        OffChainIntegrityReportTest.class
    );
    private OffChainIntegrityReport report;
    private static final String TEST_REPORT_ID = "TEST_REPORT_001";

    @BeforeEach
    void setUp() {
        report = new OffChainIntegrityReport(TEST_REPORT_ID);
        logger.info("✅ OffChainIntegrityReport initialized for test");
    }

    @Test
    @DisplayName("Test getReportTimestamp() returns valid timestamp")
    void testGetReportTimestamp() {
        LocalDateTime timestamp = report.getReportTimestamp();

        assertNotNull(timestamp, "Report timestamp should not be null");
        assertTrue(
            timestamp.isBefore(LocalDateTime.now().plusSeconds(1)),
            "Timestamp should be recent"
        );
        assertTrue(
            timestamp.isAfter(LocalDateTime.now().minusMinutes(1)),
            "Timestamp should not be too old"
        );

        logger.info("✅ Report timestamp test completed: {}", timestamp);
    }

    @Test
    @DisplayName("Test getReportId() returns correct ID")
    void testGetReportId() {
        String reportId = report.getReportId();

        assertNotNull(reportId, "Report ID should not be null");
        assertEquals(
            TEST_REPORT_ID,
            reportId,
            "Report ID should match the provided ID"
        );
        assertFalse(reportId.trim().isEmpty(), "Report ID should not be empty");

        logger.info("✅ Report ID test completed: {}", reportId);
    }

    @Test
    @DisplayName("Test getRecommendations() returns valid recommendations")
    void testGetRecommendations() {
        // Initially should be empty
        List<String> initialRecommendations = report.getRecommendations();
        assertTrue(
            initialRecommendations.isEmpty(),
            "Initial recommendations should be empty"
        );

        // Add some test data to trigger recommendations
        addTestCheckResults();
        report.generateRecommendations();

        final List<String> recommendations = report.getRecommendations();
        assertNotNull(recommendations, "Recommendations should not be null");
        assertFalse(
            recommendations.isEmpty(),
            "Recommendations should be generated after adding data"
        );

        // Verify recommendations are immutable
        assertThrows(
            UnsupportedOperationException.class,
            () -> recommendations.add("New recommendation"),
            "Recommendations list should be immutable"
        );

        logger.info(
            "✅ Recommendations test completed. Found {} recommendations",
            recommendations.size()
        );
        recommendations.forEach(rec -> logger.debug("   - {}", rec));
    }

    @Test
    @DisplayName("Test getIssuesByCategory() categorizes issues correctly")
    void testGetIssuesByCategory() {
        // Initially should be empty
        Map<String, List<String>> initialIssues = report.getIssuesByCategory();
        assertTrue(initialIssues.isEmpty(), "Initial issues should be empty");

        // Add test data with different issue types
        addDiverseTestCheckResults();

        final Map<String, List<String>> issues = report.getIssuesByCategory();
        assertNotNull(issues, "Issues by category should not be null");
        assertFalse(
            issues.isEmpty(),
            "Issues should be categorized after adding failed checks"
        );

        // Verify categories exist for failed checks
        assertTrue(
            issues.containsKey("HASH_VERIFICATION"),
            "Should have HASH_VERIFICATION category"
        );
        assertTrue(
            issues.containsKey("FILE_EXISTENCE"),
            "Should have FILE_EXISTENCE category"
        );

        // Verify issues are properly formatted
        List<String> hashIssues = issues.get("HASH_VERIFICATION");
        assertNotNull(
            hashIssues,
            "Hash verification issues should not be null"
        );
        assertFalse(
            hashIssues.isEmpty(),
            "Hash verification should have issues"
        );

        // Verify immutability
        assertThrows(
            UnsupportedOperationException.class,
            () -> issues.put("NEW_CATEGORY", Arrays.asList("test")),
            "Issues map should be immutable"
        );

        logger.info(
            "✅ Issues by category test completed. Found {} categories",
            issues.size()
        );
        issues.forEach((category, categoryIssues) ->
            logger.debug(
                "   Category '{}': {} issues",
                category,
                categoryIssues.size()
            )
        );
    }

    @Test
    @DisplayName("Test groupByStatus() groups results correctly")
    void testGroupByStatus() {
        // Initially should be empty
        Map<
            OffChainIntegrityReport.IntegrityStatus,
            List<OffChainIntegrityReport.IntegrityCheckResult>
        > grouped = report.groupByStatus();
        assertTrue(grouped.isEmpty(), "Initial grouping should be empty");

        // Add diverse test data
        addDiverseTestCheckResults();

        grouped = report.groupByStatus();
        assertNotNull(grouped, "Grouped results should not be null");

        // Verify all status types are represented
        assertTrue(
            grouped.containsKey(
                OffChainIntegrityReport.IntegrityStatus.HEALTHY
            ),
            "Should have HEALTHY group"
        );
        assertTrue(
            grouped.containsKey(
                OffChainIntegrityReport.IntegrityStatus.WARNING
            ),
            "Should have WARNING group"
        );
        assertTrue(
            grouped.containsKey(
                OffChainIntegrityReport.IntegrityStatus.CORRUPTED
            ),
            "Should have CORRUPTED group"
        );
        assertTrue(
            grouped.containsKey(
                OffChainIntegrityReport.IntegrityStatus.MISSING
            ),
            "Should have MISSING group"
        );

        // Verify counts
        List<OffChainIntegrityReport.IntegrityCheckResult> healthyResults =
            grouped.get(OffChainIntegrityReport.IntegrityStatus.HEALTHY);
        List<OffChainIntegrityReport.IntegrityCheckResult> warningResults =
            grouped.get(OffChainIntegrityReport.IntegrityStatus.WARNING);
        List<OffChainIntegrityReport.IntegrityCheckResult> corruptedResults =
            grouped.get(OffChainIntegrityReport.IntegrityStatus.CORRUPTED);
        List<OffChainIntegrityReport.IntegrityCheckResult> missingResults =
            grouped.get(OffChainIntegrityReport.IntegrityStatus.MISSING);

        assertNotNull(healthyResults, "Healthy results should not be null");
        assertNotNull(warningResults, "Warning results should not be null");
        assertNotNull(corruptedResults, "Corrupted results should not be null");
        assertNotNull(missingResults, "Missing results should not be null");

        assertEquals(2, healthyResults.size(), "Should have 2 healthy results");
        assertEquals(1, warningResults.size(), "Should have 1 warning result");
        assertEquals(
            1,
            corruptedResults.size(),
            "Should have 1 corrupted result"
        );
        assertEquals(1, missingResults.size(), "Should have 1 missing result");

        logger.info("✅ Group by status test completed");
        grouped.forEach((status, results) ->
            logger.debug(
                "   Status '{}': {} results",
                status.getDisplayName(),
                results.size()
            )
        );
    }

    @Test
    @DisplayName(
        "Test generateRecommendations() creates appropriate recommendations"
    )
    void testGenerateRecommendations() {
        // Test with no data - should have basic recommendations about no checks
        report.generateRecommendations();
        List<String> recommendations = report.getRecommendations();

        assertNotNull(recommendations, "Recommendations should not be null");
        assertFalse(
            recommendations.isEmpty(),
            "Should have basic recommendations even with no data"
        );

        // Debug: log actual recommendations to see what's generated
        logger.info("Generated recommendations with no data:");
        recommendations.forEach(rec -> logger.info("   - {}", rec));

        // Verify basic recommendations exist - check for "no checks" pattern
        assertTrue(
            recommendations
                .stream()
                .anyMatch(
                    r ->
                        r.contains("No integrity checks") ||
                        r.contains("run initial")
                ),
            "Should recommend running initial verification when no data. Actual recommendations: " +
            recommendations
        );

        // Test with problematic data
        addProblematicTestData();
        report.generateRecommendations();
        recommendations = report.getRecommendations();

        // Should have more recommendations now
        assertTrue(
            recommendations.size() > 3,
            "Should have more recommendations with problematic data"
        );

        // Verify specific recommendations for issues
        assertTrue(
            recommendations
                .stream()
                .anyMatch(
                    r ->
                        r.contains("immediate action required") ||
                        r.contains("CRITICAL")
                ),
            "Should recommend immediate action for low integrity"
        );
        assertTrue(
            recommendations
                .stream()
                .anyMatch(r -> r.contains("Corrupted data detected")),
            "Should recommend backup restoration for corrupted data"
        );
        assertTrue(
            recommendations
                .stream()
                .anyMatch(r -> r.contains("Missing data detected")),
            "Should recommend restoring missing files"
        );

        logger.info(
            "✅ Generate recommendations test completed. Total recommendations: {}",
            recommendations.size()
        );
        recommendations.forEach(rec -> logger.debug("   - {}", rec));
    }

    @Test
    @DisplayName("Test getFormattedSummary() produces comprehensive report")
    void testGetFormattedSummary() {
        // Add test data
        addDiverseTestCheckResults();

        String summary = report.getFormattedSummary();

        assertNotNull(summary, "Summary should not be null");
        assertFalse(summary.trim().isEmpty(), "Summary should not be empty");

        // Verify key sections are present
        assertTrue(
            summary.contains("Off-Chain Integrity Report"),
            "Should have report title"
        );
        assertTrue(summary.contains("Report ID"), "Should include report ID");
        assertTrue(
            summary.contains("Generated"),
            "Should include generation timestamp"
        );
        assertTrue(
            summary.contains("Overall Status"),
            "Should include overall status"
        );
        assertTrue(
            summary.contains("Verification Statistics"),
            "Should include statistics section"
        );
        assertTrue(
            summary.contains("Issues by Category"),
            "Should include issues section"
        );
        assertTrue(
            summary.contains("Recommendations"),
            "Should include recommendations section"
        );

        // Verify statistics are included
        assertTrue(
            summary.contains("Total Checks"),
            "Should show total checks"
        );
        assertTrue(summary.contains("Healthy"), "Should show healthy count");
        assertTrue(summary.contains("Warnings"), "Should show warning count");
        assertTrue(
            summary.contains("Corrupted"),
            "Should show corrupted count"
        );
        assertTrue(summary.contains("Missing"), "Should show missing count");
        assertTrue(
            summary.contains("Data Processed"),
            "Should show data processed"
        );
        assertTrue(
            summary.contains("Average Speed"),
            "Should show average speed"
        );

        // Verify formatting includes emojis and structure
        assertTrue(summary.contains("🔐"), "Should have security emoji");
        assertTrue(summary.contains("📋"), "Should have clipboard emoji");
        assertTrue(summary.contains("📊"), "Should have chart emoji");
        assertTrue(summary.contains("⚠️"), "Should have warning emoji");
        assertTrue(summary.contains("💡"), "Should have lightbulb emoji");

        logger.info(
            "✅ Formatted summary test completed. Summary length: {} characters",
            summary.length()
        );
        logger.debug("Summary content:\n{}", summary);
    }

    @Test
    @DisplayName("Test edge cases and robust validation")
    void testEdgeCases() {
        // Test with null report ID - should throw exception
        assertThrows(
            IllegalArgumentException.class,
            () -> new OffChainIntegrityReport(null),
            "Should throw exception for null report ID"
        );

        // Test with empty report ID - should throw exception
        assertThrows(
            IllegalArgumentException.class,
            () -> new OffChainIntegrityReport(""),
            "Should throw exception for empty report ID"
        );

        // Test with whitespace-only report ID - should throw exception
        assertThrows(
            IllegalArgumentException.class,
            () -> new OffChainIntegrityReport("   "),
            "Should throw exception for whitespace-only report ID"
        );

        // Test with valid report ID
        OffChainIntegrityReport validReport = new OffChainIntegrityReport(
            "VALID_ID"
        );
        assertEquals(
            "VALID_ID",
            validReport.getReportId(),
            "Should accept valid report ID"
        );

        // Test formatted summary with no data
        String emptySummary = validReport.getFormattedSummary();
        assertNotNull(
            emptySummary,
            "Summary should not be null even with no data"
        );
        assertTrue(
            emptySummary.contains("No verification statistics"),
            "Should indicate no statistics available"
        );

        // Test invalid check result creation
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new OffChainIntegrityReport.IntegrityCheckResult(
                    null,
                    "TEST",
                    OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                    "Test details",
                    Duration.ofMillis(100)
                ),
            "Should throw exception for null dataId"
        );

        assertThrows(
            IllegalArgumentException.class,
            () ->
                new OffChainIntegrityReport.IntegrityCheckResult(
                    "data001",
                    "TEST",
                    OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                    "Test details",
                    null
                ),
            "Should throw exception for null duration"
        );

        logger.info("✅ Edge cases test completed");
    }

    @Test
    @DisplayName("Test thread safety and concurrent access")
    void testThreadSafety() {
        OffChainIntegrityReport report = new OffChainIntegrityReport(
            "THREAD_TEST"
        );

        // Test that multiple threads can safely add check results
        List<Thread> threads = new ArrayList<>();
        int numThreads = 5;
        int checksPerThread = 10;

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < checksPerThread; i++) {
                    OffChainIntegrityReport.IntegrityCheckResult result =
                        new OffChainIntegrityReport.IntegrityCheckResult(
                            "thread_" + threadId + "_data_" + i,
                            "CONCURRENT_TEST",
                            OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                            "Concurrent test check",
                            Duration.ofMillis(50)
                        ).addMetadata("bytesChecked", 1024L);

                    report.addCheckResult(result);
                }
            });
            threads.add(thread);
        }

        // Start all threads
        threads.forEach(Thread::start);

        // Wait for all threads to complete
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Thread interrupted during test");
            }
        });

        // Verify all check results were added
        assertEquals(
            numThreads * checksPerThread,
            report.getCheckResultsCount(),
            "All check results should have been added safely"
        );

        // Test that recommendations can be generated safely
        report.generateRecommendations();
        List<String> recommendations = report.getRecommendations();
        assertNotNull(
            recommendations,
            "Recommendations should be generated safely"
        );

        logger.info("✅ Thread safety test completed");
    }

    @Test
    @DisplayName("Test validation and limits")
    void testValidationAndLimits() {
        OffChainIntegrityReport report = new OffChainIntegrityReport(
            "VALIDATION_TEST"
        );

        // Test very long strings
        String longString = "x".repeat(3000);
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new OffChainIntegrityReport.IntegrityCheckResult(
                    longString,
                    "TEST",
                    OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                    "Test",
                    Duration.ofMillis(100)
                ),
            "Should reject very long dataId"
        );

        // Test negative duration
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new OffChainIntegrityReport.IntegrityCheckResult(
                    "data001",
                    "TEST",
                    OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                    "Test",
                    Duration.ofMillis(-100)
                ),
            "Should reject negative duration"
        );

        // Test metadata limits
        OffChainIntegrityReport.IntegrityCheckResult result =
            new OffChainIntegrityReport.IntegrityCheckResult(
                "data001",
                "TEST",
                OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                "Test",
                Duration.ofMillis(100)
            );

        // Add valid metadata
        result.addMetadata("key1", "value1");
        result.addMetadata("key2", 42);

        // Test null metadata
        assertThrows(
            IllegalArgumentException.class,
            () -> result.addMetadata("key3", null),
            "Should reject null metadata value"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> result.addMetadata(null, "value"),
            "Should reject null metadata key"
        );

        // Now test the report with valid and invalid data
        report.addCheckResult(result);
        assertEquals(
            1,
            report.getCheckResultsCount(),
            "Report should have 1 check result"
        );

        // Test adding results with invalid data to the report
        assertThrows(
            NullPointerException.class,
            () -> report.addCheckResult(null),
            "Should reject null check result"
        );

        // Verify report state is still valid
        assertTrue(
            report.validateInternalState(),
            "Report should maintain valid state"
        );
        assertNotNull(
            report.getFormattedSummary(),
            "Report should generate formatted output"
        );

        logger.info("✅ Validation and limits test completed");
    }

    @Test
    @DisplayName("Test equals() and hashCode() methods")
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();

        // Create two reports with same ID and timestamp
        OffChainIntegrityReport report1 = new OffChainIntegrityReport(
            "EQUALS_TEST"
        );
        OffChainIntegrityReport report2 = new OffChainIntegrityReport(
            "EQUALS_TEST"
        );

        // Validate that report timestamps are within expected range
        LocalDateTime after = LocalDateTime.now();
        assertTrue(
            !report1.getReportTimestamp().isBefore(now),
            "Report1 timestamp should not be before capture time"
        );
        assertTrue(
            !report1.getReportTimestamp().isAfter(after),
            "Report1 timestamp should not be after creation time"
        );
        assertTrue(
            !report2.getReportTimestamp().isBefore(now),
            "Report2 timestamp should not be before capture time"
        );
        assertTrue(
            !report2.getReportTimestamp().isAfter(after),
            "Report2 timestamp should not be after creation time"
        );

        // Test reflexivity
        assertEquals(report1, report1, "Report should equal itself");
        assertEquals(
            report1.hashCode(),
            report1.hashCode(),
            "HashCode should be consistent"
        );

        // Test that reports with same ID but different timestamps are NOT equal
        assertNotEquals(
            report1,
            report2,
            "Reports with same ID but different timestamps should not be equal"
        );
        assertNotEquals(report2, report1, "Inequality should be symmetric");

        // Test that hashCodes are different for different timestamps
        assertNotEquals(
            report1.hashCode(),
            report2.hashCode(),
            "Reports with different timestamps should have different hashCodes"
        );

        // Verify report2 has valid properties
        assertEquals(
            "EQUALS_TEST",
            report2.getReportId(),
            "Report2 should have correct ID"
        );
        assertNotNull(
            report2.getReportTimestamp(),
            "Report2 should have valid timestamp"
        );

        // Test with different IDs
        OffChainIntegrityReport report3 = new OffChainIntegrityReport(
            "DIFFERENT_ID"
        );
        assertNotEquals(
            report1,
            report3,
            "Reports with different IDs should not be equal"
        );

        // Test with null
        assertNotEquals(report1, null, "Report should not equal null");

        // Test with different type
        assertNotEquals(
            report1,
            "not a report",
            "Report should not equal different type"
        );

        // Test hashCode consistency
        int hash1 = report1.hashCode();
        int hash2 = report1.hashCode();
        assertEquals(
            hash1,
            hash2,
            "HashCode should be consistent across calls"
        );

        logger.info("✅ Equals and hashCode test completed");
    }

    @Test
    @DisplayName("Test toString() method")
    void testToString() {
        // Get initial toString with no data
        String initialToString = report.toString();

        assertNotNull(initialToString, "toString should not return null");
        assertFalse(initialToString.isEmpty(), "toString should not be empty");

        // Verify essential information is present
        assertTrue(
            initialToString.contains("TEST_REPORT_001"),
            "Should contain report ID"
        );
        assertTrue(
            initialToString.contains("OffChainIntegrityReport"),
            "Should contain class name"
        );
        assertTrue(
            initialToString.contains("checks="),
            "Should contain check count"
        );
        assertTrue(
            initialToString.contains("status="),
            "Should contain status"
        );

        // Add some test data to the same report instance
        addTestCheckResults();
        String toStringWithData = report.toString();

        // Should show increased check count
        assertTrue(
            toStringWithData.contains("checks=2"),
            "Should show 2 checks after adding test data"
        );

        // Test with more diverse data
        addDiverseTestCheckResults();
        String toStringWithMoreData = report.toString();

        // Should show total check count (2 + 5 = 7)
        assertTrue(
            toStringWithMoreData.contains("checks=7"),
            "Should show 7 checks after adding more test data. Actual: " +
            toStringWithMoreData
        );

        // Should reflect status change from HEALTHY to something else
        assertFalse(
            toStringWithMoreData.contains("status=HEALTHY"),
            "Should not show HEALTHY status after adding problematic data. Actual: " +
            toStringWithMoreData
        );

        logger.info("✅ toString test completed: {}", toStringWithMoreData);
    }

    @Test
    @DisplayName("Test validateInternalState() method")
    void testValidateInternalState() {
        OffChainIntegrityReport report = new OffChainIntegrityReport(
            "VALIDATION_TEST"
        );

        // Initially should be valid
        assertTrue(
            report.validateInternalState(),
            "Empty report should have valid internal state"
        );

        // Add some check results
        addDiverseTestCheckResults();

        // Should still be valid
        assertTrue(
            report.validateInternalState(),
            "Report with data should have valid internal state"
        );

        // Test validation with large dataset
        for (int i = 0; i < 100; i++) {
            OffChainIntegrityReport.IntegrityCheckResult result =
                new OffChainIntegrityReport.IntegrityCheckResult(
                    "bulk_data_" + i,
                    "BULK_CHECK",
                    i % 2 == 0
                        ? OffChainIntegrityReport.IntegrityStatus.HEALTHY
                        : OffChainIntegrityReport.IntegrityStatus.WARNING,
                    "Bulk test check " + i,
                    Duration.ofMillis(10 + i)
                ).addMetadata("bytesChecked", 512L);

            report.addCheckResult(result);
        }

        // Should still be valid with more data
        assertTrue(
            report.validateInternalState(),
            "Report with bulk data should have valid internal state"
        );

        // Test validation consistency
        boolean validation1 = report.validateInternalState();
        boolean validation2 = report.validateInternalState();
        assertEquals(
            validation1,
            validation2,
            "Validation should be consistent"
        );

        // Test that statistics match actual data
        long actualCount = report.getCheckResultsCount();
        long statisticsCount = report.getStatistics().getTotalChecks();
        assertEquals(
            statisticsCount,
            actualCount,
            "Statistics should match actual data count"
        );

        logger.info(
            "✅ validateInternalState test completed. Total checks: {}",
            actualCount
        );
    }

    @Test
    @DisplayName(
        "Test IntegrityCheckResult equals, hashCode, and toString methods"
    )
    void testIntegrityCheckResultMethods() {
        // Create identical check results
        OffChainIntegrityReport.IntegrityCheckResult result1 =
            new OffChainIntegrityReport.IntegrityCheckResult(
                "test_data",
                "TEST_CHECK",
                OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                "Test check result",
                Duration.ofMillis(100)
            );

        OffChainIntegrityReport.IntegrityCheckResult result2 =
            new OffChainIntegrityReport.IntegrityCheckResult(
                "different_data",
                "TEST_CHECK",
                OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                "Test check result",
                Duration.ofMillis(100)
            );

        // Test equals
        assertEquals(result1, result1, "Result should equal itself");
        assertNotEquals(result1, null, "Result should not equal null");
        assertNotEquals(
            result1,
            "not a result",
            "Result should not equal different type"
        );

        // Results with different data should not be equal
        assertNotEquals(
            result1,
            result2,
            "Results with different data should not be equal"
        );

        // Test that equals considers checkTime (based on actual implementation)
        // Since equals() includes checkTime in comparison, objects created at different times won't be equal
        assertNotEquals(
            result1,
            result2,
            "Results created at different times should not be equal (checkTime is compared)"
        );

        // Test equals with same reference
        assertEquals(result1, result1, "Same reference should be equal");

        // Test hashCode
        int hash1 = result1.hashCode();
        int hash2 = result1.hashCode();
        assertEquals(hash1, hash2, "HashCode should be consistent");

        // Test toString
        String toString = result1.toString();
        assertNotNull(toString, "toString should not be null");
        assertTrue(toString.contains("test_data"), "Should contain dataId");
        assertTrue(toString.contains("TEST_CHECK"), "Should contain checkType");
        assertTrue(toString.contains("HEALTHY"), "Should contain status");
        assertTrue(
            toString.contains("IntegrityCheckResult"),
            "Should contain class name"
        );

        logger.info("✅ IntegrityCheckResult methods test completed");
    }

    // Helper methods to create test data

    private void addTestCheckResults() {
        // Add a healthy check
        OffChainIntegrityReport.IntegrityCheckResult healthyResult =
            new OffChainIntegrityReport.IntegrityCheckResult(
                "data_001",
                "HASH_VERIFICATION",
                OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                "Hash verification successful",
                Duration.ofMillis(100)
            ).addMetadata("bytesChecked", 1024L);

        report.addCheckResult(healthyResult);

        // Add a warning check
        OffChainIntegrityReport.IntegrityCheckResult warningResult =
            new OffChainIntegrityReport.IntegrityCheckResult(
                "data_002",
                "TIMESTAMP_CHECK",
                OffChainIntegrityReport.IntegrityStatus.WARNING,
                "Timestamp slightly outdated",
                Duration.ofMillis(150)
            ).addMetadata("bytesChecked", 2048L);

        report.addCheckResult(warningResult);
    }

    private void addDiverseTestCheckResults() {
        // Healthy results
        report.addCheckResult(
            new OffChainIntegrityReport.IntegrityCheckResult(
                "data_001",
                "HASH_VERIFICATION",
                OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                "Hash verification successful",
                Duration.ofMillis(100)
            ).addMetadata("bytesChecked", 1024L)
        );

        report.addCheckResult(
            new OffChainIntegrityReport.IntegrityCheckResult(
                "data_002",
                "SIZE_CHECK",
                OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                "Size validation passed",
                Duration.ofMillis(50)
            ).addMetadata("bytesChecked", 512L)
        );

        // Warning result
        report.addCheckResult(
            new OffChainIntegrityReport.IntegrityCheckResult(
                "data_003",
                "TIMESTAMP_CHECK",
                OffChainIntegrityReport.IntegrityStatus.WARNING,
                "Timestamp slightly outdated",
                Duration.ofMillis(75)
            ).addMetadata("bytesChecked", 256L)
        );

        // Corrupted result
        report.addCheckResult(
            new OffChainIntegrityReport.IntegrityCheckResult(
                "data_004",
                "HASH_VERIFICATION",
                OffChainIntegrityReport.IntegrityStatus.CORRUPTED,
                "Hash mismatch detected",
                Duration.ofMillis(200)
            ).addMetadata("bytesChecked", 2048L)
        );

        // Missing result
        report.addCheckResult(
            new OffChainIntegrityReport.IntegrityCheckResult(
                "data_005",
                "FILE_EXISTENCE",
                OffChainIntegrityReport.IntegrityStatus.MISSING,
                "Referenced file not found",
                Duration.ofMillis(25)
            ).addMetadata("bytesChecked", 0L)
        );
    }

    private void addProblematicTestData() {
        // Add mostly corrupted/missing data to trigger low integrity warnings
        for (int i = 0; i < 10; i++) {
            OffChainIntegrityReport.IntegrityStatus status = (i % 2 == 0)
                ? OffChainIntegrityReport.IntegrityStatus.CORRUPTED
                : OffChainIntegrityReport.IntegrityStatus.MISSING;

            String checkType = (i % 3 == 0)
                ? "HASH_VERIFICATION"
                : "FILE_EXISTENCE";

            report.addCheckResult(
                new OffChainIntegrityReport.IntegrityCheckResult(
                    "problematic_data_" + i,
                    checkType,
                    status,
                    "Severe integrity issue",
                    Duration.ofMillis(50 + i * 10)
                ).addMetadata("bytesChecked", 100L * i)
            );
        }

        // Add a few healthy ones to avoid 0% healthy
        report.addCheckResult(
            new OffChainIntegrityReport.IntegrityCheckResult(
                "healthy_data_1",
                "HASH_VERIFICATION",
                OffChainIntegrityReport.IntegrityStatus.HEALTHY,
                "Good data",
                Duration.ofMillis(25)
            ).addMetadata("bytesChecked", 500L)
        );
    }
}
