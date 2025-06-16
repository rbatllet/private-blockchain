package com.rbatllet.blockchain.util.validation;

/**
 * Class to represent the result of a block validation
 * Migrated from CLI project to core for better reusability
 */
public class BlockValidationResult {
    private boolean previousHashValid;
    private boolean blockNumberValid;
    private boolean hashIntegrityValid;
    private boolean signatureValid;
    private boolean authorizedKeyValid;
    private String errorMessage;
    
    /**
     * Default constructor
     */
    public BlockValidationResult() {
        this.previousHashValid = true;
        this.blockNumberValid = true;
        this.hashIntegrityValid = true;
        this.signatureValid = true;
        this.authorizedKeyValid = true;
        this.errorMessage = null;
    }
    
    /**
     * Check if the block is valid overall
     * @return true if all validations pass
     */
    public boolean isValid() {
        return previousHashValid && blockNumberValid && hashIntegrityValid && 
               signatureValid && authorizedKeyValid;
    }
    
    /**
     * Set the validation result for previous hash
     * @param valid true if valid
     * @return this object for chaining
     */
    public BlockValidationResult setPreviousHashValid(boolean valid) {
        this.previousHashValid = valid;
        return this;
    }
    
    /**
     * Set the validation result for block number
     * @param valid true if valid
     * @return this object for chaining
     */
    public BlockValidationResult setBlockNumberValid(boolean valid) {
        this.blockNumberValid = valid;
        return this;
    }
    
    /**
     * Set the validation result for hash integrity
     * @param valid true if valid
     * @return this object for chaining
     */
    public BlockValidationResult setHashIntegrityValid(boolean valid) {
        this.hashIntegrityValid = valid;
        return this;
    }
    
    /**
     * Set the validation result for signature
     * @param valid true if valid
     * @return this object for chaining
     */
    public BlockValidationResult setSignatureValid(boolean valid) {
        this.signatureValid = valid;
        return this;
    }
    
    /**
     * Set the validation result for authorized key
     * @param valid true if valid
     * @return this object for chaining
     */
    public BlockValidationResult setAuthorizedKeyValid(boolean valid) {
        this.authorizedKeyValid = valid;
        return this;
    }
    
    /**
     * Set an error message
     * @param message the error message
     * @return this object for chaining
     */
    public BlockValidationResult setErrorMessage(String message) {
        this.errorMessage = message;
        return this;
    }
    
    /**
     * Check if previous hash is valid
     * @return true if valid
     */
    public boolean isPreviousHashValid() {
        return previousHashValid;
    }
    
    /**
     * Check if block number is valid
     * @return true if valid
     */
    public boolean isBlockNumberValid() {
        return blockNumberValid;
    }
    
    /**
     * Check if hash integrity is valid
     * @return true if valid
     */
    public boolean isHashIntegrityValid() {
        return hashIntegrityValid;
    }
    
    /**
     * Check if signature is valid
     * @return true if valid
     */
    public boolean isSignatureValid() {
        return signatureValid;
    }
    
    /**
     * Check if authorized key is valid
     * @return true if valid
     */
    public boolean isAuthorizedKeyValid() {
        return authorizedKeyValid;
    }
    
    /**
     * Get the error message if any
     * @return the error message or null if none
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
