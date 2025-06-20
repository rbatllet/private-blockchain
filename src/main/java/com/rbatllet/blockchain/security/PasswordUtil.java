package com.rbatllet.blockchain.security;

import java.io.Console;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Utility class for secure password input
 */
public class PasswordUtil {
    
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
            java.util.Arrays.fill(passwordArray, ' ');
            return password;
        } else {
            // Fallback for environments without Console (IDEs, tests)
            System.out.print(prompt + " (WARNING: Password will be visible): ");
            
            // Simple implementation for testing with ByteArrayInputStream
            if (inputStream instanceof java.io.ByteArrayInputStream) {
                try {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                    return reader.readLine();
                } catch (java.io.IOException e) {
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
        if (inputStream instanceof java.io.ByteArrayInputStream) {
            try {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                
                System.out.print(prompt + " (WARNING: Password will be visible): ");
                String password = reader.readLine();
                if (password == null) {
                    return null;
                }
                
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
            } catch (java.io.IOException e) {
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
     * Checks if a code point represents a CJK (Chinese, Japanese, Korean) character.
     * This includes various Unicode blocks for CJK characters.
     *
     * @param codePoint The Unicode code point to check
     * @return true if the code point represents a CJK character
     */
    private static boolean isCJKCharacter(int codePoint) {
        // Check if the code point is in the CJK Unified Ideographs range (0x4E00-0x9FFF)
        if (codePoint >= 0x4E00 && codePoint <= 0x9FFF) {
            return true;
        }
        
        // Check if the code point is in other CJK ranges
        if ((codePoint >= 0x3400 && codePoint <= 0x4DBF) ||   // CJK Unified Ideographs Extension A
            (codePoint >= 0x20000 && codePoint <= 0x2A6DF) || // CJK Unified Ideographs Extension B
            (codePoint >= 0x2A700 && codePoint <= 0x2B73F) || // CJK Unified Ideographs Extension C
            (codePoint >= 0x2B740 && codePoint <= 0x2B81F) || // CJK Unified Ideographs Extension D
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
