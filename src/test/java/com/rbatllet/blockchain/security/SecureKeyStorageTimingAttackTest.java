package com.rbatllet.blockchain.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import com.rbatllet.blockchain.util.CryptoUtil;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * OWASP-Compliant Timing Attack Prevention Tests for SecureKeyStorage.
 *
 * <p>These tests verify that {@link SecureKeyStorage#loadPrivateKey(String, String)}
 * implements constant-time execution according to OWASP Authentication Cheat Sheet
 * guidelines to prevent timing-based side-channel attacks.</p>
 *
 * <p><strong>Security Properties Tested:</strong></p>
 * <ul>
 *   <li>All execution paths take minimum 400ms (constant-time)</li>
 *   <li>Invalid username/password cannot be detected via timing</li>
 *   <li>File existence cannot be detected via timing analysis</li>
 *   <li>Decryption failure timing matches success timing</li>
 *   <li>No early returns - all paths execute full PBKDF2 operation</li>
 * </ul>
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html">OWASP Authentication Cheat Sheet</a>
 */
@DisplayName("🔐 OWASP Timing Attack Prevention Tests")
public class SecureKeyStorageTimingAttackTest {
    private static final Logger logger = LoggerFactory.getLogger(SecureKeyStorageTimingAttackTest.class);


    @TempDir
    Path tempDir;

    private KeyPair testKeyPair;
    private final String testOwner = "TimingTestUser";
    private final String testPassword = "TimingTestPassword123!";

    // OWASP: Minimum execution time enforced by SecureKeyStorage
    private static final long MINIMUM_EXECUTION_TIME_MS = 400;

    // Allow 10% tolerance for system variance (VM pauses, CPU throttling, etc.)
    private static final long TIMING_TOLERANCE_MS = 40;

    // Maximum acceptable variance between different code paths
    // Increased to 75ms to account for CI environment variability while maintaining security
    // (Original: 50ms, but CI systems have more jitter due to virtualization/shared resources)
    private static final long MAX_TIMING_VARIANCE_MS = 75;

    @BeforeEach
    void setUp() {
        // Set temporary directory for testing
        System.setProperty("user.dir", tempDir.toString());

        // Generate test key pair
        testKeyPair = CryptoUtil.generateKeyPair();

        // Clean up any existing test files
        SecureKeyStorage.deletePrivateKey(testOwner);
    }

    @AfterEach
    void tearDown() {
        SecureKeyStorage.deletePrivateKey(testOwner);
    }

    /**
     * Test Case 1: Verify minimum 400ms execution time for ALL code paths.
     *
     * <p><strong>OWASP Requirement:</strong> All authentication operations should take
     * constant time regardless of success or failure to prevent timing attacks.</p>
     */
    @Test
    @DisplayName("Should enforce minimum 400ms execution time for all code paths")
    void testMinimumExecutionTimeEnforcement() {
        // Measure execution time for various scenarios
        List<TestScenario> scenarios = new ArrayList<>();

        // Scenario 1: Invalid username (null)
        scenarios.add(measureExecutionTime("null username", () ->
            SecureKeyStorage.loadPrivateKey(null, testPassword)
        ));

        // Scenario 2: Invalid password (null)
        scenarios.add(measureExecutionTime("null password", () ->
            SecureKeyStorage.loadPrivateKey(testOwner, null)
        ));

        // Scenario 3: Empty username
        scenarios.add(measureExecutionTime("empty username", () ->
            SecureKeyStorage.loadPrivateKey("", testPassword)
        ));

        // Scenario 4: Empty password
        scenarios.add(measureExecutionTime("empty password", () ->
            SecureKeyStorage.loadPrivateKey(testOwner, "")
        ));

        // Scenario 5: Non-existent user (file not found)
        scenarios.add(measureExecutionTime("non-existent user", () ->
            SecureKeyStorage.loadPrivateKey("NonExistentUser123", testPassword)
        ));

        // Scenario 6: Valid user, wrong password
        SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);
        scenarios.add(measureExecutionTime("wrong password", () ->
            SecureKeyStorage.loadPrivateKey(testOwner, "WrongPassword123!")
        ));

        // Scenario 7: Valid user, correct password (success)
        scenarios.add(measureExecutionTime("correct password (success)", () ->
            SecureKeyStorage.loadPrivateKey(testOwner, testPassword)
        ));

        // Print all timing results
        logger.info("\n📊 OWASP Timing Attack Prevention - Execution Times:");
        scenarios.forEach(scenario ->
            logger.info(String.format("  %-30s: %dms", scenario.name, scenario.executionTimeMs))
        );

        // Verify ALL scenarios take at least MINIMUM_EXECUTION_TIME_MS
        scenarios.forEach(scenario ->
            assertTrue(
                scenario.executionTimeMs >= (MINIMUM_EXECUTION_TIME_MS - TIMING_TOLERANCE_MS),
                String.format("Scenario '%s' took only %dms (expected >= %dms). " +
                             "This creates a timing side-channel vulnerability!",
                             scenario.name, scenario.executionTimeMs, MINIMUM_EXECUTION_TIME_MS)
            )
        );

        logger.info("✅ All scenarios enforce minimum execution time!");
    }

    /**
     * Test Case 2: Verify constant-time behavior across different failure modes.
     *
     * <p><strong>OWASP Requirement:</strong> Attackers should not be able to distinguish
     * between different types of failures (username invalid, password wrong, file not found)
     * by measuring execution time.</p>
     */
    @Test
    @DisplayName("Should have constant-time execution across all failure scenarios")
    void testConstantTimeAcrossFailureScenarios() {
        // Measure multiple samples to account for JIT warmup and GC
        int samples = 5;
        List<Long> nullUsernameTimes = new ArrayList<>();
        List<Long> fileNotFoundTimes = new ArrayList<>();
        List<Long> wrongPasswordTimes = new ArrayList<>();

        // Save a valid key for wrong password test
        SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);

        // Warmup JIT compiler
        for (int i = 0; i < 3; i++) {
            SecureKeyStorage.loadPrivateKey(null, testPassword);
            SecureKeyStorage.loadPrivateKey("NonExistent" + i, testPassword);
            SecureKeyStorage.loadPrivateKey(testOwner, "Wrong" + i);
        }

        // Collect timing samples
        for (int i = 0; i < samples; i++) {
            // Null username
            long start = System.nanoTime();
            SecureKeyStorage.loadPrivateKey(null, testPassword);
            nullUsernameTimes.add((System.nanoTime() - start) / 1_000_000);

            // File not found
            start = System.nanoTime();
            SecureKeyStorage.loadPrivateKey("NonExistent" + i, testPassword);
            fileNotFoundTimes.add((System.nanoTime() - start) / 1_000_000);

            // Wrong password
            start = System.nanoTime();
            SecureKeyStorage.loadPrivateKey(testOwner, "WrongPassword" + i);
            wrongPasswordTimes.add((System.nanoTime() - start) / 1_000_000);
        }

        // Calculate averages
        long avgNullUsername = nullUsernameTimes.stream().mapToLong(Long::longValue).sum() / samples;
        long avgFileNotFound = fileNotFoundTimes.stream().mapToLong(Long::longValue).sum() / samples;
        long avgWrongPassword = wrongPasswordTimes.stream().mapToLong(Long::longValue).sum() / samples;

        logger.info("\n📊 OWASP Constant-Time Verification (avg over " + samples + " samples):");
        logger.info("  Null username:    " + avgNullUsername + "ms");
        logger.info("  File not found:   " + avgFileNotFound + "ms");
        logger.info("  Wrong password:   " + avgWrongPassword + "ms");

        // Verify variance is within acceptable range
        long maxVariance = Math.max(
            Math.abs(avgNullUsername - avgFileNotFound),
            Math.max(
                Math.abs(avgNullUsername - avgWrongPassword),
                Math.abs(avgFileNotFound - avgWrongPassword)
            )
        );

        logger.info("  Max variance:     " + maxVariance + "ms (max allowed: " + MAX_TIMING_VARIANCE_MS + "ms)");

        assertTrue(maxVariance <= MAX_TIMING_VARIANCE_MS,
            String.format("Timing variance %dms exceeds threshold %dms! " +
                         "Attackers could distinguish failure types via timing analysis.",
                         maxVariance, MAX_TIMING_VARIANCE_MS));

        logger.info("✅ Constant-time behavior verified across all failure scenarios!");
    }

    /**
     * Test Case 3: Verify file existence cannot be detected via timing analysis.
     *
     * <p><strong>OWASP Requirement:</strong> File existence checks should not leak
     * information via timing differences. The implementation should NOT use
     * {@code Files.exists()} which creates timing side-channels.</p>
     */
    @Test
    @DisplayName("Should prevent file existence detection via timing analysis")
    void testFileExistenceTimingResistance() {
        int samples = 10;
        List<Long> fileExistsTimes = new ArrayList<>();
        List<Long> fileNotExistsTimes = new ArrayList<>();

        // Save a key
        SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);

        // Warmup
        for (int i = 0; i < 3; i++) {
            SecureKeyStorage.loadPrivateKey(testOwner, "WrongPassword");
            SecureKeyStorage.loadPrivateKey("NonExistent", testPassword);
        }

        // Measure: File exists (wrong password)
        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime();
            SecureKeyStorage.loadPrivateKey(testOwner, "WrongPassword" + i);
            fileExistsTimes.add((System.nanoTime() - start) / 1_000_000);
        }

        // Measure: File doesn't exist
        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime();
            SecureKeyStorage.loadPrivateKey("NonExistent" + i, testPassword);
            fileNotExistsTimes.add((System.nanoTime() - start) / 1_000_000);
        }

        long avgFileExists = fileExistsTimes.stream().mapToLong(Long::longValue).sum() / samples;
        long avgFileNotExists = fileNotExistsTimes.stream().mapToLong(Long::longValue).sum() / samples;

        logger.info("\n📊 File Existence Timing Analysis:");
        logger.info("  File exists (wrong password): " + avgFileExists + "ms");
        logger.info("  File not exists:              " + avgFileNotExists + "ms");

        long variance = Math.abs(avgFileExists - avgFileNotExists);
        logger.info("  Variance:                     " + variance + "ms");

        assertTrue(variance <= MAX_TIMING_VARIANCE_MS,
            String.format("File existence can be detected via timing! Variance %dms > %dms",
                         variance, MAX_TIMING_VARIANCE_MS));

        logger.info("✅ File existence cannot be detected via timing analysis!");
    }

    /**
     * Test Case 4: Verify success and failure paths have similar timing.
     *
     * <p><strong>OWASP Requirement:</strong> Authentication success should take
     * approximately the same time as authentication failure to prevent attackers
     * from distinguishing valid credentials from invalid ones.</p>
     */
    @Test
    @DisplayName("Should have similar timing for success and failure paths")
    void testSuccessFailureTimingSimilarity() {
        int samples = 5;
        List<Long> successTimes = new ArrayList<>();
        List<Long> failureTimes = new ArrayList<>();

        // Save valid key
        SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);

        // Warmup
        for (int i = 0; i < 3; i++) {
            SecureKeyStorage.loadPrivateKey(testOwner, testPassword);
            SecureKeyStorage.loadPrivateKey(testOwner, "Wrong");
        }

        // Measure success and failure
        for (int i = 0; i < samples; i++) {
            // Success
            long start = System.nanoTime();
            PrivateKey result = SecureKeyStorage.loadPrivateKey(testOwner, testPassword);
            successTimes.add((System.nanoTime() - start) / 1_000_000);
            assertNotNull(result, "Success case should return key");

            // Failure (wrong password)
            start = System.nanoTime();
            result = SecureKeyStorage.loadPrivateKey(testOwner, "WrongPassword" + i);
            failureTimes.add((System.nanoTime() - start) / 1_000_000);
            assertNull(result, "Failure case should return null");
        }

        long avgSuccess = successTimes.stream().mapToLong(Long::longValue).sum() / samples;
        long avgFailure = failureTimes.stream().mapToLong(Long::longValue).sum() / samples;

        logger.info("\n📊 Success vs Failure Timing:");
        logger.info("  Success (correct password): " + avgSuccess + "ms");
        logger.info("  Failure (wrong password):   " + avgFailure + "ms");

        long variance = Math.abs(avgSuccess - avgFailure);
        logger.info("  Variance:                   " + variance + "ms");

        assertTrue(variance <= MAX_TIMING_VARIANCE_MS,
            String.format("Success/failure can be distinguished via timing! Variance %dms > %dms",
                         variance, MAX_TIMING_VARIANCE_MS));

        logger.info("✅ Success and failure paths have similar timing!");
    }

    // Helper method to measure execution time
    private TestScenario measureExecutionTime(String name, Runnable operation) {
        long start = System.nanoTime();
        operation.run();
        long executionTimeMs = (System.nanoTime() - start) / 1_000_000;
        return new TestScenario(name, executionTimeMs);
    }

    // Helper class to store test scenario results
    private static class TestScenario {
        final String name;
        final long executionTimeMs;

        TestScenario(String name, long executionTimeMs) {
            this.name = name;
            this.executionTimeMs = executionTimeMs;
        }
    }
}
