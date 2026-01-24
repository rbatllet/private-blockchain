package com.rbatllet.blockchain.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Comprehensive test suite for PasswordUtil
 * Tests password validation, security features, and edge cases
 */
public class PasswordUtilTest {
    private static final Logger logger = LoggerFactory.getLogger(PasswordUtilTest.class);


    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testValidPasswordMinimumRequirements() {
        // Test minimum valid password
        assertTrue(PasswordUtil.isValidPassword("Test123a"));
        assertTrue(PasswordUtil.isValidPassword("Password1"));
        assertTrue(PasswordUtil.isValidPassword("Abc12345"));
    }

    @Test
    void testValidPasswordWithSpecialCharacters() {
        // Test passwords with special characters (should still be valid)
        assertTrue(PasswordUtil.isValidPassword("Test123!@#"));
        assertTrue(PasswordUtil.isValidPassword("MyPass123$"));
        assertTrue(PasswordUtil.isValidPassword("Secure1_Pass"));
    }

    @Test
    void testInvalidPasswordTooShort() {
        // Clear previous output first
        errContent.reset();
        
        // Test passwords shorter than 8 characters
        assertFalse(PasswordUtil.isValidPassword("Test1"));
        assertFalse(PasswordUtil.isValidPassword("Abc123"));
        assertFalse(PasswordUtil.isValidPassword("1234567"));
        
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("at least 8 characters long"));
    }

    @Test
    void testInvalidPasswordTooLong() {
        // Clear previous output first
        errContent.reset();
        
        // Test password longer than 128 characters
        String longPassword = "VeryLongPassword123".repeat(10); // 190 characters
        assertFalse(PasswordUtil.isValidPassword(longPassword));
        
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("too long"));
    }

    @Test
    void testInvalidPasswordNoLetters() {
        // Clear previous output first
        errContent.reset();
        
        // Test passwords with no letters
        assertFalse(PasswordUtil.isValidPassword("12345678"));
        assertFalse(PasswordUtil.isValidPassword("123456789"));
        assertFalse(PasswordUtil.isValidPassword("!@#$%^&*()123"));
        
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("at least one letter and one number"));
    }

    @Test
    void testInvalidPasswordNoNumbers() {
        // Clear previous output first
        errContent.reset();
        
        // Test passwords with no numbers
        assertFalse(PasswordUtil.isValidPassword("TestPassword"));
        
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("at least one letter and one number"), "Expected 'at least one letter and one number' but got: " + errorOutput);
        
        assertFalse(PasswordUtil.isValidPassword("abcdefghij"));
        assertFalse(PasswordUtil.isValidPassword("Password!@#"));
    }

    @Test
    void testNullPassword() {
        // Clear previous output first
        errContent.reset();
        
        assertFalse(PasswordUtil.isValidPassword(null));
        
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("at least 8 characters long"));
    }

    @Test
    void testEmptyPassword() {
        // Clear previous output first
        errContent.reset();
        
        assertFalse(PasswordUtil.isValidPassword(""));
        
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("at least 8 characters long"));
    }

    @Test
    void testPasswordExactlyAtLimit() {
        // Test password exactly at 128 character limit
        String exactLimitPassword = "A1".repeat(64); // Exactly 128 characters
        assertTrue(PasswordUtil.isValidPassword(exactLimitPassword));
    }

    @Test
    void testPasswordBoundaryConditions() {
        // Test password exactly at 8 characters
        assertTrue(PasswordUtil.isValidPassword("Test123a"));
        
        // Test password just under limit (127 chars)
        String nearLimitPassword = "A1".repeat(63) + "A"; // 127 characters
        assertTrue(PasswordUtil.isValidPassword(nearLimitPassword));
        
        // Test password just over limit (129 chars)
        String overLimitPassword = "A1".repeat(64) + "A"; // 129 characters
        assertFalse(PasswordUtil.isValidPassword(overLimitPassword));
    }

    @Test
    void testReadPasswordWithInput() {
        // Test reading password when input is provided
        String testPassword = "TestPassword123";
        ByteArrayInputStream input = new ByteArrayInputStream((testPassword + "\n").getBytes());
        
        String result = PasswordUtil.readPassword("Enter password: ", input);
        
        // When Console is not available (like in tests), it falls back to Scanner
        // The password will be visible, and a warning should be shown
        assertEquals(testPassword, result);
        String output = outContent.toString();
        assertTrue(output.contains("WARNING: Password will be visible"));
    }

    @Test
    void testReadPasswordWithConfirmationMatch() {
        // Test password confirmation when passwords match
        String testPassword = "TestPassword123";
        String input = testPassword + "\n" + testPassword + "\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        
        String result = PasswordUtil.readPasswordWithConfirmation("Enter password: ", inputStream);
        assertEquals(testPassword, result);
    }

    @Test
    void testReadPasswordWithConfirmationMismatch() {
        // Clear previous output first
        errContent.reset();
        
        // Test password confirmation when passwords don't match
        String password1 = "TestPassword123";
        String password2 = "DifferentPassword456";
        String input = password1 + "\n" + password2 + "\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        
        String result = PasswordUtil.readPasswordWithConfirmation("Enter password: ", inputStream);
        assertNull(result);
        
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("don't match"));
    }

    @Test
    void testReadPasswordEmpty() {
        // Test reading empty password
        ByteArrayInputStream input = new ByteArrayInputStream("\n".getBytes());
        
        String result = PasswordUtil.readPassword("Enter password: ", input);
        assertEquals("", result);
    }

    @Test
    void testPasswordSecurityRequirements() {
        // Test various combinations that should be valid
        String[] validPasswords = {
            "Password1",
            "MySecure123",
            "Test1234",
            "Blockchain2023",
            "User123Pass",
            "SecretKey1",
            "Access123",
            "Login1234"
        };
        
        for (String password : validPasswords) {
            assertTrue(PasswordUtil.isValidPassword(password), 
                      "Password should be valid: " + password);
        }
    }

    @Test
    void testPasswordInsecureExamples() {
        // Test examples that should be invalid
        String[] invalidPasswords = {
            "password",      // no numbers
            "12345678",      // no letters  
            "Pass1",         // too short
            "PASSWORD1",     // valid but could be stronger
            "Password123!",   // valid but all lowercase
            "123ABC",        // too short
            "",              // empty
            "1234567",       // too short and no letters
            "abcdefgh"       // no numbers
        };
        
        for (String password : invalidPasswords) {
            boolean result = PasswordUtil.isValidPassword(password);
            if (password.equals("PASSWORD1") || password.equals("Password123!")) {
                // These are actually valid by current criteria, even if not ideal
                assertTrue(result, "Password should be valid by current criteria: " + password);
            } else {
                assertFalse(result, "Password should be invalid: " + password);
            }
        }
    }

    @Test
    void testUnicodePasswords() {
        // Test passwords with Unicode characters
        assertTrue(PasswordUtil.isValidPassword("Contraseña1")); // Spanish
        assertTrue(PasswordUtil.isValidPassword("Mot2Passe"));   // French accent characters
        assertTrue(PasswordUtil.isValidPassword("Päßwort123"));  // German
        
        // Unicode should count properly for length
        String unicodePassword = "测试密码123好"; // Chinese characters + numbers (8 characters)
        
        // Print each character's Unicode value for debugging
        logger.error("Unicode password: " + unicodePassword);
        logger.error("Unicode password length: " + unicodePassword.length());
        
        // Print Unicode code points for each character
        StringBuilder debug = new StringBuilder("Unicode code points: ");
        for (int i = 0; i < unicodePassword.length(); i++) {
            debug.append(String.format("\\u%04x ", (int)unicodePassword.charAt(i)));
        }
        logger.error(debug.toString());
        
        // This should be valid if it meets the criteria
        boolean result = PasswordUtil.isValidPassword(unicodePassword);
        logger.error("Password validation result: " + result);
        
        // Result depends on whether Unicode letters count as "letters"
        // Current implementation should handle this correctly
        assertTrue(result, "Unicode password should be valid");
    }

    @Test
    void testWhitespaceInPasswords() {
        // Test passwords with whitespace
        assertTrue(PasswordUtil.isValidPassword("My Pass 123"));
        assertTrue(PasswordUtil.isValidPassword("Test 1234"));
        assertFalse(PasswordUtil.isValidPassword("       1")); // Mostly spaces, too short effective content
    }
}
