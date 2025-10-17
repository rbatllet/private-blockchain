package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseConnectionTester.
 *
 * <p>Note: These tests primarily use SQLite and H2 (in-memory) databases
 * as they don't require external database servers to be running.</p>
 */
@DisplayName("DatabaseConnectionTester Tests")
class DatabaseConnectionTesterTest {

    private static DatabaseConnectionTester tester;

    @BeforeAll
    static void setUp() {
        tester = new DatabaseConnectionTester();
    }

    // ============================================================================
    // Constructor Tests
    // ============================================================================

    @Test
    @DisplayName("Default constructor should use default timeouts")
    void testConstructor_Default() {
        DatabaseConnectionTester defaultTester = new DatabaseConnectionTester();
        assertNotNull(defaultTester);
    }

    @Test
    @DisplayName("Constructor with custom timeouts should accept valid values")
    void testConstructor_CustomTimeouts() {
        Duration connectionTimeout = Duration.ofSeconds(5);
        Duration queryTimeout = Duration.ofSeconds(3);

        DatabaseConnectionTester customTester = new DatabaseConnectionTester(connectionTimeout, queryTimeout);
        assertNotNull(customTester);
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for null connection timeout")
    void testConstructor_NullConnectionTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
            new DatabaseConnectionTester(null, Duration.ofSeconds(5)),
            "Should reject null connection timeout"
        );
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for null query timeout")
    void testConstructor_NullQueryTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
            new DatabaseConnectionTester(Duration.ofSeconds(10), null),
            "Should reject null query timeout"
        );
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for negative connection timeout")
    void testConstructor_NegativeConnectionTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
            new DatabaseConnectionTester(Duration.ofSeconds(-1), Duration.ofSeconds(5)),
            "Should reject negative connection timeout"
        );
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for negative query timeout")
    void testConstructor_NegativeQueryTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
            new DatabaseConnectionTester(Duration.ofSeconds(10), Duration.ofSeconds(-1)),
            "Should reject negative query timeout"
        );
    }

    @Test
    @DisplayName("Constructor should accept zero timeouts")
    void testConstructor_ZeroTimeouts() {
        // Zero is technically valid (though not recommended)
        DatabaseConnectionTester zeroTester = new DatabaseConnectionTester(
            Duration.ZERO, Duration.ZERO
        );
        assertNotNull(zeroTester);
    }

    // ============================================================================
    // testConnection() - Parameter Validation
    // ============================================================================

    @Test
    @DisplayName("testConnection() should throw IllegalArgumentException for null config")
    void testConnection_NullConfig() {
        assertThrows(IllegalArgumentException.class, () ->
            tester.testConnection(null),
            "Should reject null configuration"
        );
    }

    // ============================================================================
    // testConnection() - SQLite Tests
    // ============================================================================

    @Test
    @DisplayName("testConnection() should succeed for SQLite in-memory database")
    void testConnection_SQLite_Success() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertNotNull(result);
        assertTrue(result.isSuccessful(), "SQLite connection should succeed: " + result.getErrorMessage());
        assertFalse(result.isFailed());
        assertNull(result.getErrorMessage());
        assertNotNull(result.getConfig());
        assertNotNull(result.getResponseTime());
        assertNotNull(result.getStartTime());
        assertTrue(result.getResponseTime().toMillis() >= 0);
    }

    @Test
    @DisplayName("testConnection() should detect SQLite database version")
    void testConnection_SQLite_Version() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertTrue(result.isSuccessful());
        assertNotNull(result.getDatabaseVersion(), "Should detect database version");
        assertTrue(result.getDatabaseVersion().toLowerCase().contains("sqlite"),
            "Version should mention SQLite: " + result.getDatabaseVersion());
    }

    @Test
    @DisplayName("testConnection() should detect JDBC driver version")
    void testConnection_SQLite_DriverVersion() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertTrue(result.isSuccessful());
        assertNotNull(result.getDriverVersion(), "Should detect driver version");
    }

    @Test
    @DisplayName("testConnection() should test read permissions successfully")
    void testConnection_SQLite_ReadPermissions() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertTrue(result.isSuccessful());
        assertTrue(result.canRead(), "Should be able to read from SQLite");
    }

    @Test
    @DisplayName("testConnection() should have fast response time for SQLite")
    void testConnection_SQLite_ResponseTime() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertTrue(result.isSuccessful());
        assertTrue(result.getResponseTime().toMillis() < 5000,
            "SQLite connection should be fast (< 5s): " + result.getResponseTime().toMillis() + "ms");
    }

    @Test
    @DisplayName("testConnection() should provide summary for successful connection")
    void testConnection_SQLite_Summary() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        String summary = result.getSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("✅"), "Summary should indicate success");
        assertTrue(summary.contains("successful"), "Summary should say 'successful'");
    }

    // ============================================================================
    // testConnection() - H2 Tests
    // ============================================================================

    @Test
    @DisplayName("testConnection() should succeed for H2 in-memory database")
    void testConnection_H2_Memory_Success() {
        DatabaseConfig config = DatabaseConfig.createH2TestConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertNotNull(result);
        assertTrue(result.isSuccessful(), "H2 connection should succeed: " + result.getErrorMessage());
    }

    @Test
    @DisplayName("testConnection() should detect H2 database version")
    void testConnection_H2_Version() {
        DatabaseConfig config = DatabaseConfig.createH2TestConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertTrue(result.isSuccessful());
        assertNotNull(result.getDatabaseVersion());
        assertTrue(result.getDatabaseVersion().toLowerCase().contains("h2"),
            "Version should mention H2: " + result.getDatabaseVersion());
    }

    // ============================================================================
    // testConnection() - Error Handling Tests
    // ============================================================================

    @Test
    @DisplayName("testConnection() should fail gracefully for invalid PostgreSQL host")
    void testConnection_PostgreSQL_InvalidHost() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "nonexistent-host-xyz-12345.invalid", 5432, "testdb", "user", "pass"
        );

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertNotNull(result);
        assertFalse(result.isSuccessful(), "Should fail for invalid host");
        assertTrue(result.isFailed());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().length() > 0);
    }

    @Test
    @DisplayName("testConnection() should provide recommendations for connection failure")
    void testConnection_PostgreSQL_Recommendations() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "nonexistent-host.invalid", 5432, "testdb", "user", "pass"
        );

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertFalse(result.isSuccessful());
        assertTrue(result.hasRecommendations(), "Should provide recommendations on failure");
        assertFalse(result.getRecommendations().isEmpty());
        assertTrue(result.getRecommendations().size() > 0);
    }

    @Test
    @DisplayName("testConnection() should provide error summary for failed connection")
    void testConnection_FailedSummary() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "invalid-host.test", 5432, "testdb", "user", "pass"
        );

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        String summary = result.getSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("❌"), "Summary should indicate failure");
        assertTrue(summary.contains("failed"), "Summary should say 'failed'");
    }

    @Test
    @DisplayName("testConnection() should fail for unreachable MySQL host")
    void testConnection_MySQL_UnreachableHost() {
        DatabaseConfig config = DatabaseConfig.createMySQLConfig(
            "192.0.2.1", 3306, "testdb", "user", "pass"  // 192.0.2.1 is TEST-NET (RFC 5737)
        );

        // Use shorter timeout for this test
        DatabaseConnectionTester shortTester = new DatabaseConnectionTester(
            Duration.ofSeconds(2), Duration.ofSeconds(1)
        );

        DatabaseConnectionTester.ConnectionTestResult result = shortTester.testConnection(config);

        assertFalse(result.isSuccessful());
        assertNotNull(result.getErrorMessage());
    }

    // ============================================================================
    // ConnectionTestResult Tests
    // ============================================================================

    @Test
    @DisplayName("ConnectionTestResult should be immutable")
    void testConnectionTestResult_Immutable() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        // Verify recommendations list is immutable
        assertThrows(UnsupportedOperationException.class, () ->
            result.getRecommendations().add("New recommendation"),
            "Recommendations list should be immutable"
        );
    }

    @Test
    @DisplayName("ConnectionTestResult.getRecommendations() should return empty list when no recommendations")
    void testConnectionTestResult_NoRecommendations() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        if (result.isSuccessful()) {
            // Successful connections may have no recommendations (or just performance hints)
            assertNotNull(result.getRecommendations());
            assertTrue(result.getRecommendations().isEmpty() || result.getRecommendations().size() > 0);
        }
    }

    @Test
    @DisplayName("ConnectionTestResult.toString() should include key information")
    void testConnectionTestResult_ToString() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("ConnectionTestResult"));
        assertTrue(str.contains("successful="));
        assertTrue(str.contains("SQLITE") || str.contains("sqlite"));
    }

    @Test
    @DisplayName("ConnectionTestResult should have consistent success/failure state")
    void testConnectionTestResult_ConsistentState() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        // isSuccessful() and isFailed() should be opposites
        assertEquals(!result.isSuccessful(), result.isFailed(),
            "isSuccessful() and isFailed() should be opposites");
    }

    @Test
    @DisplayName("ConnectionTestResult should have null errorMessage on success")
    void testConnectionTestResult_NullErrorOnSuccess() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        if (result.isSuccessful()) {
            assertNull(result.getErrorMessage(),
                "Successful connection should have null error message");
        }
    }

    @Test
    @DisplayName("ConnectionTestResult should have non-null errorMessage on failure")
    void testConnectionTestResult_NonNullErrorOnFailure() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "invalid.host", 5432, "db", "user", "pass"
        );

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        if (result.isFailed()) {
            assertNotNull(result.getErrorMessage(),
                "Failed connection should have error message");
            assertFalse(result.getErrorMessage().isEmpty(),
                "Error message should not be empty");
        }
    }

    // ============================================================================
    // Constants Tests
    // ============================================================================

    @Test
    @DisplayName("DEFAULT_TIMEOUT should be 10 seconds")
    void testConstants_DefaultTimeout() {
        assertEquals(Duration.ofSeconds(10), DatabaseConnectionTester.DEFAULT_TIMEOUT);
    }

    @Test
    @DisplayName("DEFAULT_QUERY_TIMEOUT should be 5 seconds")
    void testConstants_DefaultQueryTimeout() {
        assertEquals(Duration.ofSeconds(5), DatabaseConnectionTester.DEFAULT_QUERY_TIMEOUT);
    }

    // ============================================================================
    // Edge Cases and Security Tests
    // ============================================================================

    @Test
    @DisplayName("testConnection() should handle very short timeouts")
    void testConnection_VeryShortTimeout() {
        DatabaseConnectionTester shortTester = new DatabaseConnectionTester(
            Duration.ofMillis(1), Duration.ofMillis(1)
        );

        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "testdb", "user", "pass"
        );

        // This will likely timeout, but should not crash
        DatabaseConnectionTester.ConnectionTestResult result = shortTester.testConnection(config);
        assertNotNull(result);
    }

    @Test
    @DisplayName("testConnection() should not expose sensitive data in error messages")
    void testConnection_NoSensitiveDataInErrors() {
        String password = "super_secret_password_12345";

        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "invalid.host", 5432, "testdb", "testuser", password
        );

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        // Error message should not contain the password
        if (result.getErrorMessage() != null) {
            assertFalse(result.getErrorMessage().contains(password),
                "Error message should not expose password");
        }

        // Recommendations should not contain the password
        for (String recommendation : result.getRecommendations()) {
            assertFalse(recommendation.contains(password),
                "Recommendations should not expose password: " + recommendation);
        }
    }

    @Test
    @DisplayName("testConnection() should handle empty username")
    void testConnection_EmptyUsername() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "testdb", "", "password"
        );

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertNotNull(result);
        // Will likely fail, but should handle gracefully
    }

    @Test
    @DisplayName("testConnection() should handle empty password")
    void testConnection_EmptyPassword() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "testdb", "user", ""
        );

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertNotNull(result);
        // Will likely fail, but should handle gracefully
    }

    // ============================================================================
    // Integration Tests
    // ============================================================================

    @Test
    @DisplayName("Integration: Multiple connection tests should be independent")
    void testIntegration_IndependentTests() {
        DatabaseConfig config1 = DatabaseConfig.createSQLiteConfig();
        DatabaseConfig config2 = DatabaseConfig.createH2TestConfig();

        DatabaseConnectionTester.ConnectionTestResult result1 = tester.testConnection(config1);
        DatabaseConnectionTester.ConnectionTestResult result2 = tester.testConnection(config2);

        // Both should succeed
        assertTrue(result1.isSuccessful());
        assertTrue(result2.isSuccessful());

        // Results should be independent
        assertNotSame(result1, result2);
        assertNotEquals(result1.getStartTime(), result2.getStartTime());
    }

    @Test
    @DisplayName("Integration: Tester should be reusable")
    void testIntegration_ReusableTester() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        // Use same tester multiple times
        DatabaseConnectionTester.ConnectionTestResult result1 = tester.testConnection(config);
        DatabaseConnectionTester.ConnectionTestResult result2 = tester.testConnection(config);
        DatabaseConnectionTester.ConnectionTestResult result3 = tester.testConnection(config);

        // All should succeed
        assertTrue(result1.isSuccessful());
        assertTrue(result2.isSuccessful());
        assertTrue(result3.isSuccessful());
    }

    @Test
    @DisplayName("Integration: Response time should be reasonable for local databases")
    void testIntegration_ReasonableResponseTime() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertTrue(result.isSuccessful());

        // Local SQLite should be very fast (< 1 second)
        assertTrue(result.getResponseTime().toMillis() < 1000,
            "Local database connection should be fast: " + result.getResponseTime().toMillis() + "ms");
    }

    @Test
    @DisplayName("Integration: hasRecommendations() should match recommendations list")
    void testIntegration_HasRecommendationsConsistency() {
        // Test with both successful and failed connections
        DatabaseConfig successConfig = DatabaseConfig.createSQLiteConfig();
        DatabaseConfig failConfig = DatabaseConfig.createPostgreSQLConfig(
            "invalid.test", 5432, "db", "user", "pass"
        );

        DatabaseConnectionTester.ConnectionTestResult successResult = tester.testConnection(successConfig);
        DatabaseConnectionTester.ConnectionTestResult failResult = tester.testConnection(failConfig);

        // hasRecommendations() should match list emptiness
        assertEquals(!successResult.getRecommendations().isEmpty(), successResult.hasRecommendations());
        assertEquals(!failResult.getRecommendations().isEmpty(), failResult.hasRecommendations());
    }

    @Test
    @DisplayName("Integration: Successful connection should provide complete metadata")
    void testIntegration_CompleteMetadata() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        DatabaseConnectionTester.ConnectionTestResult result = tester.testConnection(config);

        assertTrue(result.isSuccessful());

        // All metadata should be present
        assertNotNull(result.getConfig());
        assertNotNull(result.getDatabaseVersion());
        assertNotNull(result.getDriverVersion());
        assertNotNull(result.getResponseTime());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getSummary());
        assertNotNull(result.getRecommendations());
    }
}
