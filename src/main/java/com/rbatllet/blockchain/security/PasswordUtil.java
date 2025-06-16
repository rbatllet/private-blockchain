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
                Scanner scanner = new Scanner(inputStream);
                String line = scanner.nextLine();
                return line;
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
     * Validate password strength (basic validation)
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            System.err.println("❌ Password must be at least 8 characters long");
            return false;
        }
        
        if (password.length() > 128) {
            System.err.println("❌ Password is too long (max 128 characters)");
            return false;
        }
        
        // Check for at least one letter and one number
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        
        if (!hasLetter || !hasDigit) {
            System.err.println("❌ Password must contain at least one letter and one number");
            return false;
        }
        
        return true;
    }
}
