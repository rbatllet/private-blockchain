package com.rbatllet.blockchain.exception;

/**
 * Exception thrown when blockchain block validation fails.
 *
 * <p>This exception indicates critical validation failures that compromise
 * blockchain integrity, such as:</p>
 * <ul>
 *   <li>Invalid hash (tampering detected)</li>
 *   <li>Invalid signature (cryptographic verification failed)</li>
 *   <li>Invalid block number sequence</li>
 *   <li>Invalid previous hash reference</li>
 *   <li>Block signed by unauthorized key</li>
 * </ul>
 *
 * <p><strong>Security Impact:</strong> These failures indicate potential
 * security breaches or data corruption. Applications should treat these
 * exceptions as critical security events.</p>
 *
 * @since 1.0.6+
 */
public class BlockValidationException extends RuntimeException {

    private final Long blockNumber;
    private final String validationType;

    /**
     * Constructs a new BlockValidationException with the specified detail message.
     *
     * @param message the detail message
     */
    public BlockValidationException(String message) {
        super(message);
        this.blockNumber = null;
        this.validationType = null;
    }

    /**
     * Constructs a new BlockValidationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BlockValidationException(String message, Throwable cause) {
        super(message, cause);
        this.blockNumber = null;
        this.validationType = null;
    }

    /**
     * Constructs a new BlockValidationException with detailed validation context.
     *
     * @param message the detail message
     * @param blockNumber the block number that failed validation
     * @param validationType the type of validation that failed (e.g., "HASH", "SIGNATURE", "AUTHORIZATION")
     */
    public BlockValidationException(String message, Long blockNumber, String validationType) {
        super(String.format("Block #%d validation failed (%s): %s", blockNumber, validationType, message));
        this.blockNumber = blockNumber;
        this.validationType = validationType;
    }

    /**
     * Constructs a new BlockValidationException with detailed validation context and cause.
     *
     * @param message the detail message
     * @param blockNumber the block number that failed validation
     * @param validationType the type of validation that failed
     * @param cause the cause
     */
    public BlockValidationException(String message, Long blockNumber, String validationType, Throwable cause) {
        super(String.format("Block #%d validation failed (%s): %s", blockNumber, validationType, message), cause);
        this.blockNumber = blockNumber;
        this.validationType = validationType;
    }

    /**
     * Gets the block number that failed validation.
     *
     * @return the block number, or null if not specified
     */
    public Long getBlockNumber() {
        return blockNumber;
    }

    /**
     * Gets the type of validation that failed.
     *
     * @return the validation type (e.g., "HASH", "SIGNATURE", "AUTHORIZATION"), or null if not specified
     */
    public String getValidationType() {
        return validationType;
    }
}
