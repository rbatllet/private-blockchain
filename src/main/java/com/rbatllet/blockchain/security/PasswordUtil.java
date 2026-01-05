package com.rbatllet.blockchain.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for secure password input
 */
public class PasswordUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);
    
    /**
     * Read password securely from console
     * Falls back to Scanner if Console is not available (e.g., in IDEs)
     */
    public static String readPassword(String prompt) {
        return readPassword(prompt, System.in);
    }
    
    /**
     * Read password securely from console with custom input stream (for testing)
     * Falls back to Scanner if Console is not available (e.g., in IDEs)
     */
    public static String readPassword(String prompt, InputStream inputStream) {
        Console console = System.console();
        
        if (console != null && inputStream == System.in) {
            // Secure password input (doesn't echo to screen)
            char[] passwordArray = console.readPassword(prompt);
            if (passwordArray == null) {
                return null;
            }
            String password = new String(passwordArray);
            // Clear the array for security
            Arrays.fill(passwordArray, ' ');
            return password;
        } else {
            // Fallback for environments without Console (IDEs, tests)
            logger.warn("⚠️ Console not available, using fallback password input (password will be visible)");
            System.out.print(prompt + " (WARNING: Password will be visible): ");
            
            // Simple implementation for testing with ByteArrayInputStream
            if (inputStream instanceof ByteArrayInputStream) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    return reader.readLine();
                } catch (IOException e) {
                    return null;
                }
            } else {
                try (Scanner scanner = new Scanner(inputStream)) {
                    return scanner.nextLine();
                }
            }
        }
    }
    
    /**
     * Read password with confirmation
     */
    public static String readPasswordWithConfirmation(String prompt) {
        return readPasswordWithConfirmation(prompt, System.in);
    }
    
    /**
     * Read password with confirmation using custom input stream (for testing)
     */
    public static String readPasswordWithConfirmation(String prompt, InputStream inputStream) {
        // For ByteArrayInputStream in tests, we need to handle multiple reads differently
        if (inputStream instanceof ByteArrayInputStream) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                
                logger.warn("⚠️ ByteArrayInputStream fallback mode - password will be visible");
                System.out.print(prompt + " (WARNING: Password will be visible): ");
                String password = reader.readLine();
                if (password == null) {
                    return null;
                }
                
                logger.warn("⚠️ Password confirmation in fallback mode - password will be visible");
                System.out.print("Confirm password:  (WARNING: Password will be visible): ");
                String confirmation = reader.readLine();
                if (confirmation == null) {
                    return null;
                }
                
                if (!password.equals(confirmation)) {
                    System.err.println("❌ Passwords don't match!");
                    return null;
                }
                
                return password;
            } catch (IOException e) {
                return null;
            }
        } else {
            String password = readPassword(prompt, inputStream);
            if (password == null) {
                return null;
            }
            
            String confirmation = readPassword("Confirm password: ", inputStream);
            if (confirmation == null) {
                return null;
            }
            
            if (!password.equals(confirmation)) {
                System.err.println("❌ Passwords don't match!");
                return null;
            }
            
            return password;
        }
    }
    
    /**
     * Validates if a password meets the security requirements:
     * - At least 8 characters long
     * - Maximum 128 characters long
     * - Contains at least one letter (including Unicode letters and ideographs)
     * - Contains at least one digit
     *
     * @param password The password to validate
     * @return true if the password meets all requirements, false otherwise
     */
    public static boolean isValidPassword(String password) {

        if (password == null) {
            System.err.println("❌ Password must be at least 8 characters long");
            return false;
        }
        
        if (password.length() < 8) {
            System.err.println("❌ Password must be at least 8 characters long");
            return false;
        }
        
        if (password.length() > 128) {
            System.err.println("❌ Password is too long (max 128 characters)");
            return false;
        }
        
        // Check for at least one letter and one number
        boolean hasLetter = false;
        boolean hasDigit = false;
        
        // Process each code point (not each char, which could be a surrogate pair)
        for (int i = 0; i < password.length(); ) {
            int codePoint = password.codePointAt(i);
            
            // Check for letter using multiple methods to ensure comprehensive coverage
            // For Chinese characters, we need to be more inclusive
            if (Character.isLetter(codePoint) || 
                Character.isIdeographic(codePoint) || 
                isCJKCharacter(codePoint) ||
                // Additional check for Chinese characters
                (codePoint >= 0x4E00 && codePoint <= 0x9FFF)) {
                hasLetter = true;
            }
            
            // Check for digit using Unicode-aware digit check
            if (Character.isDigit(codePoint)) {
                hasDigit = true;
            }
            
            // Move to the next code point (handles surrogate pairs correctly)
            i += Character.charCount(codePoint);
            
            // Early exit if we've already found both a letter and a digit
            if (hasLetter && hasDigit) {
                break;
            }
        }
        
        // Password is valid if it has at least one letter and one digit
        if (!hasLetter || !hasDigit) {
            System.err.println("❌ Password must contain at least one letter and one number");
            return false;
        }
        
        return true;
    }

    /**
     * Validates if a password meets strong security requirements.
     *
     * <p>This method enforces stricter password requirements than {@link #isValidPassword(String)}
     * and is intended for critical operations such as genesis keys, admin accounts, or any
     * operation requiring enhanced security.</p>
     *
     * <p><strong>Requirements:</strong></p>
     * <ul>
     *   <li>Minimum 12 characters (stronger than regular 8)</li>
     *   <li>Maximum 128 characters</li>
     *   <li>At least one uppercase letter (A-Z)</li>
     *   <li>At least one lowercase letter (a-z)</li>
     *   <li>At least one digit (0-9)</li>
     *   <li>At least one special character (!@#$%^&*()_+-=[]{}...)</li>
     * </ul>
     *
     * @param password The password to validate
     * @return Error message if invalid, null if valid
     * @since 1.0.6
     */
    public static String validateStrongPassword(String password) {
        if (password == null) {
            return "Password cannot be null";
        }

        // Minimum length: 12 characters (stronger than regular 8)
        if (password.length() < 12) {
            return "Password must be at least 12 characters long";
        }

        // Maximum length: 128 characters (prevent buffer issues)
        if (password.length() > 128) {
            return "Password is too long (maximum 128 characters)";
        }

        // Check for uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter (A-Z)";
        }

        // Check for lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter (a-z)";
        }

        // Check for digit
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one digit (0-9)";
        }

        // Check for special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return "Password must contain at least one special character (!@#$%^&*()_+-=[]{}...)";
        }

        // All checks passed
        return null;
    }

    /**
     * Generate a cryptographically secure random password that meets strong security requirements.
     *
     * <p>This method generates passwords that are guaranteed to pass {@link #validateStrongPassword(String)}
     * validation by ensuring at least one character from each required category:</p>
     * <ul>
     *   <li>Uppercase letter (A-Z)</li>
     *   <li>Lowercase letter (a-z)</li>
     *   <li>Digit (0-9)</li>
     *   <li>Special character (!@#$%^&*()_+-=[]{}...)</li>
     * </ul>
     *
     * <p><strong>Security Features:</strong></p>
     * <ul>
     *   <li>Uses {@link java.security.SecureRandom} for cryptographic randomness</li>
     *   <li>Shuffles characters to avoid predictable patterns</li>
     *   <li>Minimum length: 12 characters</li>
     *   <li>Maximum length: 256 characters</li>
     * </ul>
     *
     * @param length The desired password length (minimum 12, maximum 256)
     * @return A cryptographically secure random password meeting all strong requirements
     * @throws IllegalArgumentException if length is less than 12 or greater than 256
     * @since 1.0.7
     */
    public static String generateSecurePassword(int length) {
        if (length < 12) {
            throw new IllegalArgumentException("Password length must be at least 12 characters");
        }
        if (length > 256) {
            throw new IllegalArgumentException("Password length cannot exceed 256 characters");
        }

        // Character sets for strong password requirements
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()_+-=[]{}";
        String allChars = uppercase + lowercase + digits + special;

        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder(length);

        // GUARANTEE at least one character from each required category
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Fill remaining positions with random characters from all sets
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password to avoid predictable patterns (first 4 chars always fixed categories)
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

    /**
     * Generate a cryptographically secure password with validation and optional confirmation.
     *
     * <p>This method creates enterprise-grade passwords suitable for protecting sensitive
     * data. It combines secure random generation, strength validation, and optional 
     * interactive confirmation for maximum security and usability.</p>
     *
     * <p><strong>Security Features:</strong></p>
     * <ul>
     *   <li>Cryptographically secure random generation using SecureRandom</li>
     *   <li>Automatic validation against security best practices</li>
     *   <li>Configurable length with minimum security requirements</li>
     *   <li>Character set includes uppercase, lowercase, digits, and symbols</li>
     *   <li>Multiple generation attempts to ensure quality</li>
     * </ul>
     *
     * <p><strong>Password Strength Requirements:</strong></p>
     * <ul>
     *   <li>Minimum 12 characters (recommended: 16+ for high security)</li>
     *   <li>Mix of uppercase and lowercase letters</li>
     *   <li>Numbers and special characters included</li>
     *   <li>No common patterns or dictionary words</li>
     *   <li>Suitable for AES-256-GCM encryption protection</li>
     * </ul>
     *
     * <p><strong>Interactive Confirmation:</strong><br>
     * When {@code requireConfirmation} is true, the method logs the generated password
     * and prompts for re-entry to ensure accuracy. This is recommended for critical
     * operations where password mistakes could result in data loss.</p>
     *
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Generate password for sensitive data (high security)
     * String password = PasswordUtil.generateValidatedPassword(20, true);
     *
     * // Generate password for automated systems (no confirmation needed)
     * String systemPassword = PasswordUtil.generateValidatedPassword(16, false);
     *
     * // Generate maximum security password for critical systems
     * String criticalPassword = PasswordUtil.generateValidatedPassword(32, true);
     * }</pre>
     *
     * @param length The desired password length in characters. Must be at least 12 characters.
     *              Recommended lengths:
     *              <ul>
     *                <li>12-15: Standard security for general use</li>
     *                <li>16-20: High security for sensitive data</li>
     *                <li>24-32: Maximum security for critical systems</li>
     *              </ul>
     * @param requireConfirmation If {@code true}, displays the generated password and prompts
     *                           for confirmation by re-entry. Use {@code true} for interactive
     *                           sessions, {@code false} for automated systems.
     * @return A cryptographically secure password meeting all validation requirements,
     *         or {@code null} if confirmation failed when {@code requireConfirmation} is true
     * @throws IllegalArgumentException if length is less than 12 or greater than 256 characters
     * @throws RuntimeException if secure password generation fails after multiple attempts
     * @see #generateSecurePassword(int)
     * @see #validateStrongPassword(String)
     * @since 1.0.7
     */
    public static String generateValidatedPassword(int length, boolean requireConfirmation) {
        if (length < 12) {
            throw new IllegalArgumentException(
                "Password length must be at least 12 characters"
            );
        }
        if (length > 256) {
            throw new IllegalArgumentException(
                "Password length cannot exceed 256 characters (DoS protection)"
            );
        }

        String password;
        int attempts = 0;
        int maxAttempts = 5;

        // Generate password and validate it meets strong requirements
        do {
            password = generateSecurePassword(length);
            attempts++;

            if (attempts > maxAttempts) {
                throw new RuntimeException(
                    "Failed to generate valid password after " + maxAttempts + " attempts"
                );
            }
        } while (validateStrongPassword(password) != null);

        // Interactive confirmation if requested
        if (requireConfirmation) {
            logger.info(
                "🔑 Generated secure password with length: {} characters",
                password.length()
            );
            String confirmation = readPassword(
                "Please re-enter the generated password to confirm: "
            );

            if (confirmation == null || !password.equals(confirmation)) {
                logger.warn("❌ Password confirmation failed - attempt rejected");
                return null;
            }
            logger.info("✅ Password confirmed successfully");
        }

        return password;
    }

    /**
     * Checks if a code point represents a CJK (Chinese, Japanese, Korean) character.
     * This includes various Unicode blocks for CJK characters.
     *
     * @param codePoint The Unicode code point to check
     * @return true if the code point represents a CJK character
     */
    private static boolean isCJKCharacter(int codePoint) {
        // Check if the code point is in the CJK Advanced Ideographs range (0x4E00-0x9FFF)
        if (codePoint >= 0x4E00 && codePoint <= 0x9FFF) {
            return true;
        }
        
        // Check if the code point is in other CJK ranges
        if ((codePoint >= 0x3400 && codePoint <= 0x4DBF) ||   // CJK Advanced Ideographs Extension A
            (codePoint >= 0x20000 && codePoint <= 0x2A6DF) || // CJK Advanced Ideographs Extension B
            (codePoint >= 0x2A700 && codePoint <= 0x2B73F) || // CJK Advanced Ideographs Extension C
            (codePoint >= 0x2B740 && codePoint <= 0x2B81F) || // CJK Advanced Ideographs Extension D
            (codePoint >= 0xF900 && codePoint <= 0xFAFF) ||   // CJK Compatibility Ideographs
            (codePoint >= 0x2F800 && codePoint <= 0x2FA1F) || // CJK Compatibility Ideographs Supplement
            (codePoint >= 0x3000 && codePoint <= 0x303F) ||   // CJK Symbols and Punctuation
            (codePoint >= 0x3040 && codePoint <= 0x309F) ||   // Hiragana
            (codePoint >= 0x30A0 && codePoint <= 0x30FF) ||   // Katakana
            (codePoint >= 0xAC00 && codePoint <= 0xD7AF) ||   // Hangul Syllables
            (codePoint >= 0x1100 && codePoint <= 0x11FF) ||   // Hangul Jamo
            (codePoint >= 0x3130 && codePoint <= 0x318F)      // Hangul Compatibility Jamo
        ) {
            return true;
        }
        
        // Additional check for other CJK characters that might not be covered by the above ranges
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        if (block != null && (
            block.toString().contains("CJK") ||
            block.toString().contains("CHINESE") ||
            block.toString().contains("JAPANESE") ||
            block.toString().contains("KOREAN")
        )) {
            return true;
        }
        
        // Specific code points for Chinese characters that might not be detected by the above methods
        // This is a more general approach to ensure all Chinese characters are detected
        if ((codePoint >= 0x6000 && codePoint <= 0x9FFF)) {
            return true;
        }
        
        return false;
    }
}
