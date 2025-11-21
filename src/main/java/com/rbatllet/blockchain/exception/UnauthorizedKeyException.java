package com.rbatllet.blockchain.exception;

import java.time.LocalDateTime;

/**
 * Exception thrown when an unauthorized key attempts a protected blockchain operation.
 *
 * <p>This exception indicates critical authorization failures where a public key
 * that is not authorized attempts to:</p>
 * <ul>
 *   <li>Add a new block to the blockchain</li>
 *   <li>Sign blockchain data</li>
 *   <li>Perform operations requiring authorization</li>
 * </ul>
 *
 * <p><strong>Security Impact:</strong> This is a critical security violation.
 * All blockchain operations must be performed with pre-authorized keys only.
 * Applications should log these attempts and consider them as potential
 * security attacks.</p>
 *
 * <p><strong>RBAC v1.0.6+:</strong> All keys must be explicitly authorized via
 * {@code addAuthorizedKey()} before they can perform any blockchain operations.</p>
 *
 * @since 1.0.6+
 */
public class UnauthorizedKeyException extends RuntimeException {

    private final String publicKey;
    private final String operation;
    private final LocalDateTime attemptedAt;

    /**
     * Constructs a new UnauthorizedKeyException with the specified detail message.
     *
     * @param message the detail message
     */
    public UnauthorizedKeyException(String message) {
        super(message);
        this.publicKey = null;
        this.operation = null;
        this.attemptedAt = LocalDateTime.now();
    }

    /**
     * Constructs a new UnauthorizedKeyException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public UnauthorizedKeyException(String message, Throwable cause) {
        super(message, cause);
        this.publicKey = null;
        this.operation = null;
        this.attemptedAt = LocalDateTime.now();
    }

    /**
     * Constructs a new UnauthorizedKeyException with detailed authorization context.
     *
     * @param message the detail message
     * @param publicKey the unauthorized public key that attempted the operation
     * @param operation the operation that was attempted (e.g., "ADD_BLOCK", "SIGN_DATA")
     */
    public UnauthorizedKeyException(String message, String publicKey, String operation) {
        super(String.format("Unauthorized key attempted %s: %s (key: %s...)",
            operation,
            message,
            publicKey != null && publicKey.length() > 50 ? publicKey.substring(0, 50) : publicKey));
        this.publicKey = publicKey;
        this.operation = operation;
        this.attemptedAt = LocalDateTime.now();
    }

    /**
     * Constructs a new UnauthorizedKeyException with detailed authorization context and timestamp.
     *
     * @param message the detail message
     * @param publicKey the unauthorized public key
     * @param operation the operation that was attempted
     * @param attemptedAt the timestamp when the unauthorized attempt was made
     */
    public UnauthorizedKeyException(String message, String publicKey, String operation, LocalDateTime attemptedAt) {
        super(String.format("Unauthorized key attempted %s at %s: %s (key: %s...)",
            operation,
            attemptedAt,
            message,
            publicKey != null && publicKey.length() > 50 ? publicKey.substring(0, 50) : publicKey));
        this.publicKey = publicKey;
        this.operation = operation;
        this.attemptedAt = attemptedAt;
    }

    /**
     * Gets the unauthorized public key that attempted the operation.
     *
     * @return the public key, or null if not specified
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Gets the operation that was attempted.
     *
     * @return the operation (e.g., "ADD_BLOCK", "SIGN_DATA"), or null if not specified
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Gets the timestamp when the unauthorized attempt was made.
     *
     * @return the timestamp of the attempt
     */
    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }
}
