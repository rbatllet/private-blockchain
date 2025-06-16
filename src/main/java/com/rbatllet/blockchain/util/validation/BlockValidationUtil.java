package com.rbatllet.blockchain.util.validation;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.core.Blockchain;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for blockchain validation operations
 * Migrated from CLI project to core for better reusability
 */
public class BlockValidationUtil {
    
    /**
     * Validate genesis block specifically
     * @param genesisBlock The genesis block to validate
     * @return true if the genesis block is valid
     */
    public static boolean validateGenesisBlock(Block genesisBlock) {
        boolean isValid = true;
        final String GENESIS_PREVIOUS_HASH = "0";
        
        if (genesisBlock.getBlockNumber() == null || !genesisBlock.getBlockNumber().equals(0L)) {
            isValid = false;
        }
        
        if (!GENESIS_PREVIOUS_HASH.equals(genesisBlock.getPreviousHash())) {
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Helper method to check if a key was authorized at a specific timestamp
     * @param blockchain The blockchain instance
     * @param publicKeyString The public key string to check
     * @param timestamp The timestamp to check authorization at
     * @return true if the key was authorized at the given timestamp
     */
    public static boolean wasKeyAuthorizedAt(Blockchain blockchain, String publicKeyString, LocalDateTime timestamp) {
        try {
            return blockchain.wasKeyAuthorizedAt(publicKeyString, timestamp);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Truncate a hash string for display purposes
     * @param hash The hash string to truncate
     * @return The truncated hash string
     */
    public static String truncateHash(String hash) {
        if (hash == null) return "null";
        return hash.length() > 32 ? 
            hash.substring(0, 16) + "..." + hash.substring(hash.length() - 20, hash.length()) :
            hash;
    }
}
