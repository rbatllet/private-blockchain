package com.rbatllet.blockchain.config.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for SensitiveDataMasker.
 *
 * Tests all masking capabilities including:
 * - Password masking
 * - Connection string masking (JDBC URLs)
 * - Properties masking
 * - Edge cases and boundary conditions
 * - Security requirements (no sensitive data leakage)
 */
@DisplayName("SensitiveDataMasker Tests")
class SensitiveDataMaskerTest {

    private static final String REDACTED = SensitiveDataMasker.REDACTION_MARKER;
    private static final String REDACTED_ENV = SensitiveDataMasker.REDACTION_MARKER_ENV;

    // ============================================================================
    // Password Masking Tests
    // ============================================================================

    @Test
    @DisplayName("maskPassword() should redact non-null passwords")
    void testMaskPassword_NonNull() {
        assertEquals(REDACTED, SensitiveDataMasker.maskPassword("secret123"));
        assertEquals(REDACTED, SensitiveDataMasker.maskPassword("p@ssw0rd!"));
        assertEquals(REDACTED, SensitiveDataMasker.maskPassword("a"));
        assertEquals(REDACTED, SensitiveDataMasker.maskPassword("very_long_password_with_special_chars_!@#$%^&*()"));
    }

    @Test
    @DisplayName("maskPassword() should handle null input")
    void testMaskPassword_Null() {
        assertNull(SensitiveDataMasker.maskPassword(null));
    }

    @Test
    @DisplayName("maskPassword() should mask empty string")
    void testMaskPassword_Empty() {
        assertEquals(REDACTED, SensitiveDataMasker.maskPassword(""));
    }

    // ============================================================================
    // Connection String Masking Tests - PostgreSQL
    // ============================================================================

    @Test
    @DisplayName("maskConnectionString() should mask PostgreSQL URL with password parameter")
    void testMaskConnectionString_PostgreSQL_PasswordParam() {
        String original = "jdbc:postgresql://localhost:5432/blockchain?user=admin&password=secret123";
        String masked = SensitiveDataMasker.maskConnectionString(original);

        assertFalse(masked.contains("secret123"), "Password should be masked");
        assertFalse(masked.contains("admin"), "Username should be masked");
        assertTrue(masked.contains("postgresql"), "Database type should remain");
        assertTrue(masked.contains("localhost"), "Host should remain");
        assertTrue(masked.contains("5432"), "Port should remain");
        assertTrue(masked.contains("blockchain"), "Database name should remain");
        assertTrue(masked.contains(REDACTED), "Should contain redaction marker");
    }

    @Test
    @DisplayName("maskConnectionString() should mask PostgreSQL URL with credentials in URL")
    void testMaskConnectionString_PostgreSQL_CredentialsInURL() {
        String original = "jdbc:postgresql://admin:secret123@localhost:5432/blockchain";
        String masked = SensitiveDataMasker.maskConnectionString(original);

        assertFalse(masked.contains("secret123"), "Password should be masked");
        assertFalse(masked.contains("admin"), "Username should be masked");
        assertTrue(masked.contains("postgresql"), "Database type should remain");
        assertTrue(masked.contains("localhost"), "Host should remain");
        assertTrue(masked.contains(REDACTED), "Should contain redaction marker");
    }

    @Test
    @DisplayName("maskConnectionString() should mask PostgreSQL URL with both URL credentials and query params")
    void testMaskConnectionString_PostgreSQL_Mixed() {
        String original = "jdbc:postgresql://admin:secret@localhost:5432/db?password=pwd123&sslmode=require";
        String masked = SensitiveDataMasker.maskConnectionString(original);

        assertFalse(masked.contains("admin"), "Username in URL should be masked");
        assertFalse(masked.contains("secret"), "Password in URL should be masked");
        assertFalse(masked.contains("pwd123"), "Password in query should be masked");
        assertTrue(masked.contains("sslmode=require"), "Non-sensitive params should remain");
    }

    // ============================================================================
    // Connection String Masking Tests - MySQL
    // ============================================================================

    @Test
    @DisplayName("maskConnectionString() should mask MySQL URL")
    void testMaskConnectionString_MySQL() {
        String original = "jdbc:mysql://localhost:3306/blockchain?user=root&password=mysql_pass";
        String masked = SensitiveDataMasker.maskConnectionString(original);

        assertFalse(masked.contains("root"), "Username should be masked");
        assertFalse(masked.contains("mysql_pass"), "Password should be masked");
        assertTrue(masked.contains("mysql"), "Database type should remain");
        assertTrue(masked.contains("3306"), "Port should remain");
    }

    @Test
    @DisplayName("maskConnectionString() should mask MySQL URL with credentials in URL")
    void testMaskConnectionString_MySQL_CredentialsInURL() {
        String original = "jdbc:mysql://root:mysql_pass@localhost:3306/blockchain";
        String masked = SensitiveDataMasker.maskConnectionString(original);

        assertFalse(masked.contains("root"), "Username should be masked");
        assertFalse(masked.contains("mysql_pass"), "Password should be masked");
        assertTrue(masked.contains(REDACTED), "Should contain redaction marker");
    }

    // ============================================================================
    // Connection String Masking Tests - SQLite (no credentials)
    // ============================================================================

    @Test
    @DisplayName("maskConnectionString() should not modify SQLite URLs (no credentials)")
    void testMaskConnectionString_SQLite() {
        String original = "jdbc:sqlite:blockchain.db?journal_mode=WAL";
        String masked = SensitiveDataMasker.maskConnectionString(original);

        assertEquals(original, masked, "SQLite URL without credentials should remain unchanged");
    }

    // ============================================================================
    // Connection String Masking Tests - Edge Cases
    // ============================================================================

    @Test
    @DisplayName("maskConnectionString() should handle null input")
    void testMaskConnectionString_Null() {
        assertNull(SensitiveDataMasker.maskConnectionString(null));
    }

    @Test
    @DisplayName("maskConnectionString() should handle empty string")
    void testMaskConnectionString_Empty() {
        assertEquals("", SensitiveDataMasker.maskConnectionString(""));
    }

    @Test
    @DisplayName("maskConnectionString() should mask case-insensitive password variants")
    void testMaskConnectionString_CaseInsensitive() {
        String url1 = "jdbc:postgresql://localhost/db?PASSWORD=secret";
        String url2 = "jdbc:postgresql://localhost/db?Password=secret";
        String url3 = "jdbc:postgresql://localhost/db?pwd=secret";
        String url4 = "jdbc:postgresql://localhost/db?PWD=secret";
        String url5 = "jdbc:postgresql://localhost/db?passwd=secret";

        assertFalse(SensitiveDataMasker.maskConnectionString(url1).contains("secret"));
        assertFalse(SensitiveDataMasker.maskConnectionString(url2).contains("secret"));
        assertFalse(SensitiveDataMasker.maskConnectionString(url3).contains("secret"));
        assertFalse(SensitiveDataMasker.maskConnectionString(url4).contains("secret"));
        assertFalse(SensitiveDataMasker.maskConnectionString(url5).contains("secret"));
    }

    @Test
    @DisplayName("maskConnectionString() should mask case-insensitive username variants")
    void testMaskConnectionString_CaseInsensitive_User() {
        String url1 = "jdbc:postgresql://localhost/db?USER=admin";
        String url2 = "jdbc:postgresql://localhost/db?User=admin";
        String url3 = "jdbc:postgresql://localhost/db?username=admin";
        String url4 = "jdbc:postgresql://localhost/db?USERNAME=admin";

        assertFalse(SensitiveDataMasker.maskConnectionString(url1).contains("admin"));
        assertFalse(SensitiveDataMasker.maskConnectionString(url2).contains("admin"));
        assertFalse(SensitiveDataMasker.maskConnectionString(url3).contains("admin"));
        assertFalse(SensitiveDataMasker.maskConnectionString(url4).contains("admin"));
    }

    @Test
    @DisplayName("maskConnectionString() should handle special characters in passwords")
    void testMaskConnectionString_SpecialChars() {
        String original = "jdbc:postgresql://localhost/db?password=p@ss!w0rd%20&user=admin";
        String masked = SensitiveDataMasker.maskConnectionString(original);

        assertFalse(masked.contains("p@ss!w0rd%20"), "Complex password should be masked");
        assertFalse(masked.contains("admin"), "Username should be masked");
    }

    @Test
    @DisplayName("maskConnectionString() should mask multiple parameters")
    void testMaskConnectionString_MultipleParams() {
        String original = "jdbc:postgresql://localhost/db?user=admin&password=secret&ssl=true&user=backup";
        String masked = SensitiveDataMasker.maskConnectionString(original);

        assertFalse(masked.contains("admin"), "First username should be masked");
        assertFalse(masked.contains("backup"), "Second username should be masked");
        assertFalse(masked.contains("secret"), "Password should be masked");
        assertTrue(masked.contains("ssl=true"), "Non-sensitive param should remain");
    }

    // ============================================================================
    // Properties Masking Tests
    // ============================================================================

    @Test
    @DisplayName("maskProperties() should mask password properties")
    void testMaskProperties_Passwords() {
        Properties props = new Properties();
        props.setProperty("db.password", "secret123");
        props.setProperty("db.postgresql.password", "pg_secret");
        props.setProperty("db.mysql.passwd", "mysql_secret");
        props.setProperty("app.pwd", "app_secret");

        Properties masked = SensitiveDataMasker.maskProperties(props);

        assertEquals(REDACTED, masked.getProperty("db.password"));
        assertEquals(REDACTED, masked.getProperty("db.postgresql.password"));
        assertEquals(REDACTED, masked.getProperty("db.mysql.passwd"));
        assertEquals(REDACTED, masked.getProperty("app.pwd"));
    }

    @Test
    @DisplayName("maskProperties() should mask username properties")
    void testMaskProperties_Usernames() {
        Properties props = new Properties();
        props.setProperty("db.user", "admin");
        props.setProperty("db.username", "dbuser");
        props.setProperty("app.usr", "appuser");

        Properties masked = SensitiveDataMasker.maskProperties(props);

        assertEquals(REDACTED, masked.getProperty("db.user"));
        assertEquals(REDACTED, masked.getProperty("db.username"));
        assertEquals(REDACTED, masked.getProperty("app.usr"));
    }

    @Test
    @DisplayName("maskProperties() should mask other sensitive keys")
    void testMaskProperties_OtherSensitive() {
        Properties props = new Properties();
        props.setProperty("api.secret", "api_secret_key");
        props.setProperty("oauth.token", "bearer_token");
        props.setProperty("encryption.key", "enc_key");
        props.setProperty("auth.credential", "credentials");

        Properties masked = SensitiveDataMasker.maskProperties(props);

        assertEquals(REDACTED, masked.getProperty("api.secret"));
        assertEquals(REDACTED, masked.getProperty("oauth.token"));
        assertEquals(REDACTED, masked.getProperty("encryption.key"));
        assertEquals(REDACTED, masked.getProperty("auth.credential"));
    }

    @Test
    @DisplayName("maskProperties() should not mask non-sensitive properties")
    void testMaskProperties_NonSensitive() {
        Properties props = new Properties();
        props.setProperty("db.host", "localhost");
        props.setProperty("db.port", "5432");
        props.setProperty("db.name", "blockchain");
        props.setProperty("app.timeout", "30000");
        props.setProperty("db.type", "postgresql");

        Properties masked = SensitiveDataMasker.maskProperties(props);

        assertEquals("localhost", masked.getProperty("db.host"));
        assertEquals("5432", masked.getProperty("db.port"));
        assertEquals("blockchain", masked.getProperty("db.name"));
        assertEquals("30000", masked.getProperty("app.timeout"));
        assertEquals("postgresql", masked.getProperty("db.type"));
    }

    @Test
    @DisplayName("maskProperties() should handle null input")
    void testMaskProperties_Null() {
        assertNull(SensitiveDataMasker.maskProperties(null));
    }

    @Test
    @DisplayName("maskProperties() should handle empty properties")
    void testMaskProperties_Empty() {
        Properties props = new Properties();
        Properties masked = SensitiveDataMasker.maskProperties(props);

        assertNotNull(masked);
        assertTrue(masked.isEmpty());
    }

    @Test
    @DisplayName("maskProperties() should not modify original properties")
    void testMaskProperties_Immutable() {
        Properties original = new Properties();
        original.setProperty("db.password", "secret");
        original.setProperty("db.host", "localhost");

        Properties masked = SensitiveDataMasker.maskProperties(original);

        // Original should remain unchanged
        assertEquals("secret", original.getProperty("db.password"));
        assertEquals("localhost", original.getProperty("db.host"));

        // Masked should have redacted password
        assertEquals(REDACTED, masked.getProperty("db.password"));
        assertEquals("localhost", masked.getProperty("db.host"));
    }

    @Test
    @DisplayName("maskProperties() should preserve empty sensitive values")
    void testMaskProperties_EmptyValues() {
        Properties props = new Properties();
        props.setProperty("db.password", "");
        props.setProperty("db.user", "");

        Properties masked = SensitiveDataMasker.maskProperties(props);

        assertEquals("", masked.getProperty("db.password"), "Empty password should remain empty");
        assertEquals("", masked.getProperty("db.user"), "Empty user should remain empty");
    }

    @Test
    @DisplayName("maskProperties() with useEnvMarker should use environment variable hint")
    void testMaskProperties_EnvMarker() {
        Properties props = new Properties();
        props.setProperty("db.password", "secret");

        Properties masked = SensitiveDataMasker.maskProperties(props, true);

        assertEquals(REDACTED_ENV, masked.getProperty("db.password"));
    }

    @Test
    @DisplayName("maskProperties() should be case-insensitive for property keys")
    void testMaskProperties_CaseInsensitive() {
        Properties props = new Properties();
        props.setProperty("DB.PASSWORD", "secret");
        props.setProperty("App.User", "admin");
        props.setProperty("api.SECRET", "key");

        Properties masked = SensitiveDataMasker.maskProperties(props);

        assertEquals(REDACTED, masked.getProperty("DB.PASSWORD"));
        assertEquals(REDACTED, masked.getProperty("App.User"));
        assertEquals(REDACTED, masked.getProperty("api.SECRET"));
    }

    // ============================================================================
    // Generic maskSensitiveData Tests
    // ============================================================================

    @Test
    @DisplayName("maskSensitiveData() should mask connection strings")
    void testMaskSensitiveData() {
        String text = "Connecting to jdbc:postgresql://admin:secret@localhost/db";
        String masked = SensitiveDataMasker.maskSensitiveData(text);

        assertFalse(masked.contains("admin"));
        assertFalse(masked.contains("secret"));
        assertTrue(masked.contains(REDACTED));
    }

    @Test
    @DisplayName("maskSensitiveData() should handle null")
    void testMaskSensitiveData_Null() {
        assertNull(SensitiveDataMasker.maskSensitiveData(null));
    }

    // ============================================================================
    // isSensitiveKey Tests
    // ============================================================================

    @Test
    @DisplayName("isSensitiveKey() should identify password keys")
    void testIsSensitiveKey_Password() {
        assertTrue(SensitiveDataMasker.isSensitiveKey("password"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("db.password"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("PASSWORD"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("passwd"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("pwd"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("myPasswordField"));
    }

    @Test
    @DisplayName("isSensitiveKey() should identify username keys")
    void testIsSensitiveKey_Username() {
        assertTrue(SensitiveDataMasker.isSensitiveKey("user"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("username"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("db.user"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("usr"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("USER"));
    }

    @Test
    @DisplayName("isSensitiveKey() should identify other sensitive keys")
    void testIsSensitiveKey_Other() {
        assertTrue(SensitiveDataMasker.isSensitiveKey("secret"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("token"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("api.key"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("credential"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("auth"));
    }

    @Test
    @DisplayName("isSensitiveKey() should reject non-sensitive keys")
    void testIsSensitiveKey_NonSensitive() {
        assertFalse(SensitiveDataMasker.isSensitiveKey("host"));
        assertFalse(SensitiveDataMasker.isSensitiveKey("port"));
        assertFalse(SensitiveDataMasker.isSensitiveKey("database"));
        assertFalse(SensitiveDataMasker.isSensitiveKey("timeout"));
        assertFalse(SensitiveDataMasker.isSensitiveKey("db.type"));
    }

    @Test
    @DisplayName("isSensitiveKey() should handle null and empty")
    void testIsSensitiveKey_NullEmpty() {
        assertFalse(SensitiveDataMasker.isSensitiveKey(null));
        assertFalse(SensitiveDataMasker.isSensitiveKey(""));
    }

    // ============================================================================
    // containsSensitiveData Tests
    // ============================================================================

    @Test
    @DisplayName("containsSensitiveData() should detect passwords in URLs")
    void testContainsSensitiveData_Password() {
        assertTrue(SensitiveDataMasker.containsSensitiveData("jdbc:postgresql://localhost/db?password=secret"));
        assertTrue(SensitiveDataMasker.containsSensitiveData("jdbc:mysql://host/db?pwd=pass"));
        assertTrue(SensitiveDataMasker.containsSensitiveData("url?passwd=secret"));
    }

    @Test
    @DisplayName("containsSensitiveData() should detect usernames in URLs")
    void testContainsSensitiveData_Username() {
        assertTrue(SensitiveDataMasker.containsSensitiveData("jdbc:postgresql://localhost/db?user=admin"));
        assertTrue(SensitiveDataMasker.containsSensitiveData("jdbc:mysql://host/db?username=root"));
    }

    @Test
    @DisplayName("containsSensitiveData() should detect credentials in URL format")
    void testContainsSensitiveData_URLFormat() {
        assertTrue(SensitiveDataMasker.containsSensitiveData("jdbc:postgresql://admin:secret@localhost/db"));
        assertTrue(SensitiveDataMasker.containsSensitiveData("http://user@example.com"));
    }

    @Test
    @DisplayName("containsSensitiveData() should not detect in safe strings")
    void testContainsSensitiveData_Safe() {
        assertFalse(SensitiveDataMasker.containsSensitiveData("jdbc:sqlite:blockchain.db"));
        assertFalse(SensitiveDataMasker.containsSensitiveData("localhost:5432"));
        assertFalse(SensitiveDataMasker.containsSensitiveData("database configuration"));
    }

    @Test
    @DisplayName("containsSensitiveData() should handle null and empty")
    void testContainsSensitiveData_NullEmpty() {
        assertFalse(SensitiveDataMasker.containsSensitiveData(null));
        assertFalse(SensitiveDataMasker.containsSensitiveData(""));
    }

    // ============================================================================
    // Security Tests - Ensure NO leakage
    // ============================================================================

    @Test
    @DisplayName("SECURITY: No password should ever appear in masked output")
    void testSecurity_NoPasswordLeakage() {
        String password = "super_secret_password_123!@#";
        String jdbcUrl = "jdbc:postgresql://admin:" + password + "@localhost/db?password=" + password;
        Properties props = new Properties();
        props.setProperty("db.password", password);

        String maskedUrl = SensitiveDataMasker.maskConnectionString(jdbcUrl);
        String maskedPassword = SensitiveDataMasker.maskPassword(password);
        Properties maskedProps = SensitiveDataMasker.maskProperties(props);

        assertFalse(maskedUrl.contains(password), "CRITICAL: Password leaked in masked URL");
        assertFalse(maskedPassword.contains(password), "CRITICAL: Password leaked in maskPassword");
        assertFalse(maskedProps.getProperty("db.password").contains(password), "CRITICAL: Password leaked in masked properties");
    }

    @Test
    @DisplayName("SECURITY: No username should ever appear in masked output")
    void testSecurity_NoUsernameLeakage() {
        String username = "admin_user_123";
        String jdbcUrl = "jdbc:postgresql://" + username + ":secret@localhost/db?user=" + username;
        Properties props = new Properties();
        props.setProperty("db.user", username);

        String maskedUrl = SensitiveDataMasker.maskConnectionString(jdbcUrl);
        Properties maskedProps = SensitiveDataMasker.maskProperties(props);

        assertFalse(maskedUrl.contains(username), "CRITICAL: Username leaked in masked URL");
        assertFalse(maskedProps.getProperty("db.user").contains(username), "CRITICAL: Username leaked in masked properties");
    }

    @Test
    @DisplayName("SECURITY: Masking should be idempotent (safe to mask twice)")
    void testSecurity_Idempotent() {
        String url = "jdbc:postgresql://admin:secret@localhost/db";

        String masked1 = SensitiveDataMasker.maskConnectionString(url);
        String masked2 = SensitiveDataMasker.maskConnectionString(masked1);

        assertEquals(masked1, masked2, "Masking should be idempotent");
        assertFalse(masked2.contains("secret"), "Password should remain masked after double masking");
    }
}
