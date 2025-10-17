package com.rbatllet.blockchain.config.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for SecurityWarning.
 */
@DisplayName("SecurityWarning Tests")
class SecurityWarningTest {

    // ============================================================================
    // Builder Tests
    // ============================================================================

    @Test
    @DisplayName("Builder should create warning with all fields")
    void testBuilder_AllFields() {
        Instant timestamp = Instant.now();
        SecurityWarning warning = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.HIGH)
            .message("Test warning message")
            .code("TEST_CODE")
            .addRemediationStep("Step 1")
            .addRemediationStep("Step 2")
            .timestamp(timestamp)
            .build();

        assertEquals(SecurityWarning.Severity.HIGH, warning.getSeverity());
        assertEquals("Test warning message", warning.getMessage());
        assertEquals("TEST_CODE", warning.getCode());
        assertEquals(2, warning.getRemediationSteps().size());
        assertEquals("Step 1", warning.getRemediationSteps().get(0));
        assertEquals("Step 2", warning.getRemediationSteps().get(1));
        assertEquals(timestamp, warning.getTimestamp());
    }

    @Test
    @DisplayName("Builder should create warning with minimal fields")
    void testBuilder_MinimalFields() {
        SecurityWarning warning = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.LOW)
            .message("Minimal warning")
            .build();

        assertEquals(SecurityWarning.Severity.LOW, warning.getSeverity());
        assertEquals("Minimal warning", warning.getMessage());
        assertNull(warning.getCode());
        assertTrue(warning.getRemediationSteps().isEmpty());
        assertNotNull(warning.getTimestamp()); // Auto-generated
    }

    @Test
    @DisplayName("Builder should throw NPE if severity is null")
    void testBuilder_NullSeverity() {
        assertThrows(NullPointerException.class, () ->
            SecurityWarning.builder()
                .severity(null)
                .message("Test")
                .build()
        );
    }

    @Test
    @DisplayName("Builder should throw NPE if message is null")
    void testBuilder_NullMessage() {
        assertThrows(NullPointerException.class, () ->
            SecurityWarning.builder()
                .severity(SecurityWarning.Severity.HIGH)
                .message(null)
                .build()
        );
    }

    @Test
    @DisplayName("Builder should ignore null remediation steps")
    void testBuilder_NullRemediationStep() {
        SecurityWarning warning = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.MEDIUM)
            .message("Test")
            .addRemediationStep(null)
            .addRemediationStep("")
            .addRemediationStep("Valid step")
            .build();

        assertEquals(1, warning.getRemediationSteps().size());
        assertEquals("Valid step", warning.getRemediationSteps().get(0));
    }

    @Test
    @DisplayName("Builder should add multiple remediation steps from list")
    void testBuilder_AddRemediationSteps() {
        List<String> steps = List.of("Step A", "Step B", "Step C");

        SecurityWarning warning = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.CRITICAL)
            .message("Test")
            .addRemediationSteps(steps)
            .build();

        assertEquals(3, warning.getRemediationSteps().size());
        assertEquals(steps, warning.getRemediationSteps());
    }

    // ============================================================================
    // Factory Method Tests
    // ============================================================================

    @Test
    @DisplayName("of() should create simple warning")
    void testOf() {
        SecurityWarning warning = SecurityWarning.of(SecurityWarning.Severity.MEDIUM, "Test message");

        assertEquals(SecurityWarning.Severity.MEDIUM, warning.getSeverity());
        assertEquals("Test message", warning.getMessage());
        assertNull(warning.getCode());
        assertTrue(warning.getRemediationSteps().isEmpty());
    }

    @Test
    @DisplayName("critical() should create CRITICAL severity warning")
    void testCritical() {
        SecurityWarning warning = SecurityWarning.critical("Critical issue");

        assertEquals(SecurityWarning.Severity.CRITICAL, warning.getSeverity());
        assertEquals("Critical issue", warning.getMessage());
    }

    @Test
    @DisplayName("high() should create HIGH severity warning")
    void testHigh() {
        SecurityWarning warning = SecurityWarning.high("High priority issue");

        assertEquals(SecurityWarning.Severity.HIGH, warning.getSeverity());
        assertEquals("High priority issue", warning.getMessage());
    }

    @Test
    @DisplayName("medium() should create MEDIUM severity warning")
    void testMedium() {
        SecurityWarning warning = SecurityWarning.medium("Medium priority issue");

        assertEquals(SecurityWarning.Severity.MEDIUM, warning.getSeverity());
        assertEquals("Medium priority issue", warning.getMessage());
    }

    @Test
    @DisplayName("low() should create LOW severity warning")
    void testLow() {
        SecurityWarning warning = SecurityWarning.low("Low priority issue");

        assertEquals(SecurityWarning.Severity.LOW, warning.getSeverity());
        assertEquals("Low priority issue", warning.getMessage());
    }

    @Test
    @DisplayName("info() should create INFO severity warning")
    void testInfo() {
        SecurityWarning warning = SecurityWarning.info("Informational message");

        assertEquals(SecurityWarning.Severity.INFO, warning.getSeverity());
        assertEquals("Informational message", warning.getMessage());
    }

    // ============================================================================
    // Immutability Tests
    // ============================================================================

    @Test
    @DisplayName("getRemediationSteps() should return immutable list")
    void testRemediationSteps_Immutable() {
        SecurityWarning warning = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.HIGH)
            .message("Test")
            .addRemediationStep("Step 1")
            .build();

        List<String> steps = warning.getRemediationSteps();

        assertThrows(UnsupportedOperationException.class, () ->
            steps.add("New step")
        );
    }

    // ============================================================================
    // Equals and HashCode Tests
    // ============================================================================

    @Test
    @DisplayName("equals() should compare by severity, message, and code")
    void testEquals() {
        SecurityWarning warning1 = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.HIGH)
            .message("Test message")
            .code("CODE1")
            .addRemediationStep("Step")
            .build();

        SecurityWarning warning2 = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.HIGH)
            .message("Test message")
            .code("CODE1")
            .addRemediationStep("Different step")
            .build();

        SecurityWarning warning3 = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.MEDIUM)
            .message("Test message")
            .code("CODE1")
            .build();

        assertEquals(warning1, warning2, "Should be equal (remediation steps not compared)");
        assertNotEquals(warning1, warning3, "Should not be equal (different severity)");
    }

    @Test
    @DisplayName("equals() should handle null code")
    void testEquals_NullCode() {
        SecurityWarning warning1 = SecurityWarning.of(SecurityWarning.Severity.HIGH, "Test");
        SecurityWarning warning2 = SecurityWarning.of(SecurityWarning.Severity.HIGH, "Test");

        assertEquals(warning1, warning2);
    }

    @Test
    @DisplayName("equals() should return false for null")
    void testEquals_Null() {
        SecurityWarning warning = SecurityWarning.high("Test");
        assertNotEquals(warning, null);
    }

    @Test
    @DisplayName("equals() should return true for same instance")
    void testEquals_SameInstance() {
        SecurityWarning warning = SecurityWarning.high("Test");
        assertEquals(warning, warning);
    }

    @Test
    @DisplayName("hashCode() should be consistent with equals()")
    void testHashCode() {
        SecurityWarning warning1 = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.HIGH)
            .message("Test")
            .code("CODE1")
            .build();

        SecurityWarning warning2 = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.HIGH)
            .message("Test")
            .code("CODE1")
            .build();

        assertEquals(warning1.hashCode(), warning2.hashCode());
    }

    // ============================================================================
    // toString Tests
    // ============================================================================

    @Test
    @DisplayName("toString() should include main fields")
    void testToString() {
        SecurityWarning warning = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.CRITICAL)
            .message("Test warning")
            .code("TEST_CODE")
            .addRemediationStep("Step 1")
            .addRemediationStep("Step 2")
            .build();

        String str = warning.toString();

        assertTrue(str.contains("CRITICAL"));
        assertTrue(str.contains("Test warning"));
        assertTrue(str.contains("TEST_CODE"));
        assertTrue(str.contains("remediationSteps=2"));
        assertTrue(str.contains("timestamp="));
    }

    // ============================================================================
    // Severity Enum Tests
    // ============================================================================

    @Test
    @DisplayName("Severity enum should have all expected values")
    void testSeverity_AllValues() {
        SecurityWarning.Severity[] severities = SecurityWarning.Severity.values();

        assertEquals(5, severities.length);
        assertEquals(SecurityWarning.Severity.CRITICAL, severities[0]);
        assertEquals(SecurityWarning.Severity.HIGH, severities[1]);
        assertEquals(SecurityWarning.Severity.MEDIUM, severities[2]);
        assertEquals(SecurityWarning.Severity.LOW, severities[3]);
        assertEquals(SecurityWarning.Severity.INFO, severities[4]);
    }

    @Test
    @DisplayName("Severity.valueOf() should work correctly")
    void testSeverity_ValueOf() {
        assertEquals(SecurityWarning.Severity.CRITICAL, SecurityWarning.Severity.valueOf("CRITICAL"));
        assertEquals(SecurityWarning.Severity.HIGH, SecurityWarning.Severity.valueOf("HIGH"));
        assertEquals(SecurityWarning.Severity.MEDIUM, SecurityWarning.Severity.valueOf("MEDIUM"));
        assertEquals(SecurityWarning.Severity.LOW, SecurityWarning.Severity.valueOf("LOW"));
        assertEquals(SecurityWarning.Severity.INFO, SecurityWarning.Severity.valueOf("INFO"));
    }

    // ============================================================================
    // Edge Cases
    // ============================================================================

    @Test
    @DisplayName("Warning with empty message should be allowed")
    void testEmptyMessage() {
        SecurityWarning warning = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.LOW)
            .message("")
            .build();

        assertEquals("", warning.getMessage());
    }

    @Test
    @DisplayName("Warning with very long message should work")
    void testLongMessage() {
        String longMessage = "A".repeat(10000);

        SecurityWarning warning = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.MEDIUM)
            .message(longMessage)
            .build();

        assertEquals(longMessage, warning.getMessage());
    }

    @Test
    @DisplayName("Warning with many remediation steps should work")
    void testManyRemediationSteps() {
        SecurityWarning.Builder builder = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.HIGH)
            .message("Test");

        for (int i = 0; i < 100; i++) {
            builder.addRemediationStep("Step " + i);
        }

        SecurityWarning warning = builder.build();

        assertEquals(100, warning.getRemediationSteps().size());
    }

    // ============================================================================
    // Timestamp Tests
    // ============================================================================

    @Test
    @DisplayName("Timestamp should be auto-generated if not provided")
    void testTimestamp_AutoGenerated() {
        Instant before = Instant.now();

        SecurityWarning warning = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.LOW)
            .message("Test")
            .build();

        Instant after = Instant.now();

        assertNotNull(warning.getTimestamp());
        assertFalse(warning.getTimestamp().isBefore(before));
        assertFalse(warning.getTimestamp().isAfter(after));
    }

    @Test
    @DisplayName("Custom timestamp should be preserved")
    void testTimestamp_Custom() {
        Instant customTime = Instant.parse("2025-01-01T00:00:00Z");

        SecurityWarning warning = SecurityWarning.builder()
            .severity(SecurityWarning.Severity.HIGH)
            .message("Test")
            .timestamp(customTime)
            .build();

        assertEquals(customTime, warning.getTimestamp());
    }
}
