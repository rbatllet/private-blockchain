package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.dao.AuthorizedKeyDAO;
import com.rbatllet.blockchain.dao.BlockDAO;
import com.rbatllet.blockchain.dto.ChainExportData;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.List;

public class Blockchain {
    
    private final BlockDAO blockDAO;
    private final AuthorizedKeyDAO authorizedKeyDAO;
    private static final String GENESIS_PREVIOUS_HASH = "0";
    
    // CORE FUNCTION: Block size validation constants
    // FIXED: Added clear documentation about the relationship between limits
    private static final int MAX_BLOCK_SIZE_BYTES = 1024 * 1024; // 1MB max per block (for large binary data)
    private static final int MAX_BLOCK_DATA_LENGTH = 10000; // 10K characters max (for text content)
    // NOTE: Character limit is separate from byte limit to handle both text and binary data efficiently
    // 10K UTF-8 characters typically use ~10-40KB, well within the 1MB byte limit
    
    public Blockchain() {
        this.blockDAO = new BlockDAO();
        this.authorizedKeyDAO = new AuthorizedKeyDAO();
        initializeGenesisBlock();
    }
    
    /**
     * Creates the genesis block if it doesn't exist
     */
    private void initializeGenesisBlock() {
        if (blockDAO.getBlockCount() == 0) {
            Block genesisBlock = new Block();
            genesisBlock.setBlockNumber(0);
            genesisBlock.setPreviousHash(GENESIS_PREVIOUS_HASH);
            genesisBlock.setData("Genesis Block - Private Blockchain Initialized");
            genesisBlock.setTimestamp(LocalDateTime.now());
            
            String blockContent = buildBlockContent(genesisBlock);
            genesisBlock.setHash(CryptoUtil.calculateHash(blockContent));
            genesisBlock.setSignature("GENESIS");
            genesisBlock.setSignerPublicKey("GENESIS");
            
            blockDAO.saveBlock(genesisBlock);
            System.out.println("Genesis block created successfully!");
        }
    }
    
    /**
     * CORE FUNCTION: Add a new block to the chain (with size validation)
     * FIXED: Added synchronization to prevent concurrent block number conflicts
     */
    public synchronized boolean addBlock(String data, PrivateKey signerPrivateKey, PublicKey signerPublicKey) {
        try {
            // 0. CORE: Validate block size
            if (!validateBlockSize(data)) {
                System.err.println("Block data exceeds maximum allowed size");
                return false;
            }
            
            // 1. Verify that the key is authorized at the time of block creation
            String publicKeyString = CryptoUtil.publicKeyToString(signerPublicKey);
            LocalDateTime blockTimestamp = LocalDateTime.now();
            if (!authorizedKeyDAO.wasKeyAuthorizedAt(publicKeyString, blockTimestamp)) {
                System.err.println("Unauthorized key attempting to add block");
                return false;
            }
            
            // 2. Get the last block
            Block lastBlock = blockDAO.getLastBlock();
            if (lastBlock == null) {
                System.err.println("No genesis block found");
                return false;
            }
            
            // 3. Create the new block with the same timestamp used for validation
            Block newBlock = new Block();
            newBlock.setBlockNumber(lastBlock.getBlockNumber() + 1);
            newBlock.setPreviousHash(lastBlock.getHash());
            newBlock.setData(data);
            newBlock.setTimestamp(blockTimestamp); // Use the same timestamp
            newBlock.setSignerPublicKey(publicKeyString);
            
            // 4. Calculate block hash
            String blockContent = buildBlockContent(newBlock);
            newBlock.setHash(CryptoUtil.calculateHash(blockContent));
            
            // 5. Sign the block
            String signature = CryptoUtil.signData(blockContent, signerPrivateKey);
            newBlock.setSignature(signature);
            
            // 6. Validate the block before saving
            if (!validateBlock(newBlock, lastBlock)) {
                System.err.println("Block validation failed");
                return false;
            }
            
            // 7. Save the block
            blockDAO.saveBlock(newBlock);
            System.out.println("Block #" + newBlock.getBlockNumber() + " added successfully!");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error adding block: " + e.getMessage());
            return false;
        }
    }    

    /**
     * CORE FUNCTION: Validate an individual block
     */
    private boolean validateBlock(Block block, Block previousBlock) {
        try {
            // Skip validation for genesis block
            if (block.getBlockNumber() == 0) {
                return true;
            }
            
            // 1. Verify that the previous block hash matches
            if (!block.getPreviousHash().equals(previousBlock.getHash())) {
                System.err.println("Previous hash mismatch");
                return false;
            }
            
            // 2. Verify that the block number is sequential
            if (block.getBlockNumber() != previousBlock.getBlockNumber() + 1) {
                System.err.println("Block number sequence error");
                return false;
            }
            
            // 3. Verify hash integrity
            String calculatedHash = CryptoUtil.calculateHash(buildBlockContent(block));
            if (!block.getHash().equals(calculatedHash)) {
                System.err.println("Block hash integrity check failed for block #" + block.getBlockNumber());
                return false;
            }
            
            // 4. Verify digital signature
            PublicKey signerPublicKey = CryptoUtil.stringToPublicKey(block.getSignerPublicKey());
            String blockContent = buildBlockContent(block);
            if (!CryptoUtil.verifySignature(blockContent, block.getSignature(), signerPublicKey)) {
                System.err.println("Block signature verification failed for block #" + block.getBlockNumber());
                return false;
            }
            
            // 5. Verify that the key was authorized at the time of block creation
            if (!authorizedKeyDAO.wasKeyAuthorizedAt(block.getSignerPublicKey(), block.getTimestamp())) {
                System.err.println("Block signed by key that was not authorized at time of creation for block #" + block.getBlockNumber());
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error validating block: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * PUBLIC: Validate a single block (used by recovery manager)
     * @param block The block to validate
     * @return true if block is valid, false otherwise
     */
    public boolean validateSingleBlock(Block block) {
        try {
            // Skip validation for genesis block
            if (block.getBlockNumber() == 0) {
                return true;
            }
            
            // Find the previous block for validation
            Block previousBlock = blockDAO.getBlockByNumber(block.getBlockNumber() - 1);
            if (previousBlock == null) {
                return false; // Previous block not found
            }
            
            return validateBlock(block, previousBlock);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * CORE FUNCTION: Validate the entire blockchain
     */
    public boolean validateChain() {
        try {
            List<Block> allBlocks = blockDAO.getAllBlocks();
            
            if (allBlocks.isEmpty()) {
                System.err.println("No blocks found in chain");
                return false;
            }
            
            // Verify genesis block
            Block genesisBlock = allBlocks.get(0);
            if (genesisBlock.getBlockNumber() != 0 || 
                !genesisBlock.getPreviousHash().equals(GENESIS_PREVIOUS_HASH)) {
                System.err.println("Invalid genesis block");
                return false;
            }
            
            // Verify all blocks sequentially
            for (int i = 1; i < allBlocks.size(); i++) {
                Block currentBlock = allBlocks.get(i);
                Block previousBlock = allBlocks.get(i - 1);
                
                if (!validateBlock(currentBlock, previousBlock)) {
                    System.err.println("Chain validation failed at block #" + currentBlock.getBlockNumber());
                    return false;
                }
            }
            
            System.out.println("Chain validation successful! Total blocks: " + allBlocks.size());
            return true;
            
        } catch (Exception e) {
            System.err.println("Error validating chain: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * CORE FUNCTION: Build block content for hashing and signing (without signature)
     */
    private String buildBlockContent(Block block) {
        // Use epoch seconds for timestamp to ensure consistency
        long timestampSeconds = block.getTimestamp() != null ? 
            block.getTimestamp().toEpochSecond(java.time.ZoneOffset.UTC) : 0;
            
        String content = block.getBlockNumber() + 
               (block.getPreviousHash() != null ? block.getPreviousHash() : "") + 
               (block.getData() != null ? block.getData() : "") + 
               timestampSeconds +
               (block.getSignerPublicKey() != null ? block.getSignerPublicKey() : "");
        return content;
    }    

    /**
     * CORE FUNCTION: Add an authorized key
     * IMPROVED: Now allows re-authorization of previously revoked keys
     * FIXED: Added synchronization for thread safety
     */
    public synchronized boolean addAuthorizedKey(String publicKeyString, String ownerName) {
        return addAuthorizedKey(publicKeyString, ownerName, null);
    }
    
    /**
     * CORE FUNCTION: Add an authorized key with specific timestamp (for CLI operations)
     * FIXED: Allows setting specific creation time to avoid race conditions
     */
    public synchronized boolean addAuthorizedKey(String publicKeyString, String ownerName, LocalDateTime creationTime) {
        try {
            // Check if key is currently authorized
            if (authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                System.err.println("Key already authorized");
                return false;
            }
            
            // Allow re-authorization: create new authorization record
            AuthorizedKey authorizedKey = new AuthorizedKey(publicKeyString, ownerName);
            
            // Set specific creation time if provided
            if (creationTime != null) {
                authorizedKey.setCreatedAt(creationTime);
            }
            
            authorizedKeyDAO.saveAuthorizedKey(authorizedKey);
            System.out.println("Authorized key added for: " + ownerName);
            return true;
            
        } catch (Exception e) {
            System.err.println("Error adding authorized key: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * CORE FUNCTION: Revoke an authorized key
     * FIXED: Added synchronization for thread safety
     */
    public synchronized boolean revokeAuthorizedKey(String publicKeyString) {
        try {
            if (!authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                System.err.println("Key not found or already inactive");
                return false;
            }
            
            authorizedKeyDAO.revokeAuthorizedKey(publicKeyString);
            System.out.println("Key revoked successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error revoking key: " + e.getMessage());
            return false;
        }
    }
    
    // ===============================
    // CORE FUNCTIONS: ADVANCED ADDITIONS
    // ===============================
    
    /**
     * CORE FUNCTION 1: Block Size Validation
     */
    private boolean validateBlockSize(String data) {
        if (data == null) {
            return true; // Allow null data
        }
        
        // Check character length
        if (data.length() > MAX_BLOCK_DATA_LENGTH) {
            System.err.println("Block data length (" + data.length() + 
                             ") exceeds maximum allowed (" + MAX_BLOCK_DATA_LENGTH + " characters)");
            return false;
        }
        
        // Check byte size (important for UTF-8 strings)
        byte[] dataBytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (dataBytes.length > MAX_BLOCK_SIZE_BYTES) {
            System.err.println("Block data size (" + dataBytes.length + 
                             " bytes) exceeds maximum allowed (" + MAX_BLOCK_SIZE_BYTES + " bytes)");
            return false;
        }
        
        return true;
    }
    
    /**
     * CORE FUNCTION 2: Chain Export - Backup blockchain to file
     */
    public boolean exportChain(String filePath) {
        try {
            List<Block> allBlocks = blockDAO.getAllBlocks();
            List<AuthorizedKey> allKeys = authorizedKeyDAO.getAllAuthorizedKeys(); // FIXED: Export ALL keys, not just active ones
            
            // Create export data structure
            ChainExportData exportData = new ChainExportData();
            exportData.setBlocks(allBlocks);
            exportData.setAuthorizedKeys(allKeys);
            exportData.setExportTimestamp(LocalDateTime.now());
            exportData.setVersion("1.0");
            exportData.setTotalBlocks(allBlocks.size());
            
            // Convert to JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            
            // Write to file
            java.io.File file = new java.io.File(filePath);
            mapper.writeValue(file, exportData);
            
            System.out.println("Chain exported successfully to: " + filePath);
            System.out.println("Exported " + allBlocks.size() + " blocks and " + allKeys.size() + " authorized keys");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error exporting chain: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * CORE FUNCTION 3: Chain Import - Restore blockchain from file
     */
    public boolean importChain(String filePath) {
        try {
            // Read and parse JSON file
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                System.err.println("Import file not found: " + filePath);
                return false;
            }
            
            ChainExportData importData = mapper.readValue(file, ChainExportData.class);
            
            // Validate import data
            if (importData.getBlocks() == null || importData.getBlocks().isEmpty()) {
                System.err.println("No blocks found in import file");
                return false;
            }
            
            // Clear existing data (WARNING: This will delete current blockchain!)
            System.out.println("WARNING: This will replace the current blockchain!");
            
            // Clear existing blocks and keys
            blockDAO.deleteAllBlocks();
            authorizedKeyDAO.deleteAllAuthorizedKeys();
            
            // Import authorized keys first with corrected timestamps
            if (importData.getAuthorizedKeys() != null) {
                // First, find the earliest and latest timestamp for each public key from the blocks
                java.util.Map<String, LocalDateTime> earliestBlockTimestamps = new java.util.HashMap<>();
                java.util.Map<String, LocalDateTime> latestBlockTimestamps = new java.util.HashMap<>();
                
                for (Block block : importData.getBlocks()) {
                    if (block.getSignerPublicKey() != null && !"GENESIS".equals(block.getSignerPublicKey())) {
                        String publicKey = block.getSignerPublicKey();
                        LocalDateTime blockTimestamp = block.getTimestamp();
                        
                        if (blockTimestamp != null) {
                            earliestBlockTimestamps.merge(publicKey, blockTimestamp, 
                                (existing, current) -> existing.isBefore(current) ? existing : current);
                            latestBlockTimestamps.merge(publicKey, blockTimestamp, 
                                (existing, current) -> existing.isAfter(current) ? existing : current);
                        }
                    }
                }
                
                // Import authorized keys with adjusted timestamps
                for (AuthorizedKey key : importData.getAuthorizedKeys()) {
                    // Reset ID for new insertion
                    key.setId(null);
                    
                    // FIXED: Maintain temporal consistency during import considering ALL events
                    String publicKey = key.getPublicKey();
                    
                    // Find the earliest event timestamp for this key (blocks or revocation)
                    LocalDateTime earliestEventTime = null;
                    
                    // Check earliest block signed by this key
                    if (earliestBlockTimestamps.containsKey(publicKey)) {
                        earliestEventTime = earliestBlockTimestamps.get(publicKey);
                    }
                    
                    // Check if revocation time is earlier than first block
                    if (key.getRevokedAt() != null) {
                        if (earliestEventTime == null || key.getRevokedAt().isBefore(earliestEventTime)) {
                            earliestEventTime = key.getRevokedAt();
                        }
                    }
                    
                    // Set key creation time to be before ALL events related to this key
                    if (earliestEventTime != null) {
                        key.setCreatedAt(earliestEventTime.minusMinutes(1));
                    }
                    
                    // Handle revoked keys without revocation timestamp
                    if (!key.isActive() && key.getRevokedAt() == null) {
                        if (latestBlockTimestamps.containsKey(publicKey)) {
                            // Set revocation time after the latest block
                            LocalDateTime latestBlockTime = latestBlockTimestamps.get(publicKey);
                            key.setRevokedAt(latestBlockTime.plusMinutes(1));
                        } else {
                            // For keys without blocks, set reasonable revocation time
                            key.setRevokedAt(key.getCreatedAt().plusMinutes(1));
                        }
                    }
                    
                    authorizedKeyDAO.saveAuthorizedKey(key);
                }
                System.out.println("Imported " + importData.getAuthorizedKeys().size() + " authorized keys with adjusted timestamps");
            }
            
            // Import blocks
            for (Block block : importData.getBlocks()) {
                // Reset ID for new insertion
                block.setId(null);
                blockDAO.saveBlock(block);
            }
            
            System.out.println("Chain imported successfully from: " + filePath);
            System.out.println("Imported " + importData.getBlocks().size() + " blocks");
            
            // Validate imported chain
            boolean isValid = validateChain();
            if (isValid) {
                System.out.println("Imported chain validation: SUCCESS");
            } else {
                System.err.println("Imported chain validation: FAILED");
            }
            
            return isValid;
            
        } catch (Exception e) {
            System.err.println("Error importing chain: " + e.getMessage());
            return false;
        }
    }

    /**
     * CORE FUNCTION 4: Block Rollback - Remove last N blocks
     */
    public boolean rollbackBlocks(int numberOfBlocks) {
        try {
            if (numberOfBlocks <= 0) {
                System.err.println("Number of blocks to rollback must be positive");
                return false;
            }
            
            long currentBlockCount = blockDAO.getBlockCount();
            if (numberOfBlocks >= currentBlockCount) {
                System.err.println("Cannot rollback " + numberOfBlocks + 
                                 " blocks. Only " + currentBlockCount + " blocks exist (including genesis)");
                return false;
            }
            
            // Get blocks to remove (excluding genesis block)
            List<Block> allBlocks = blockDAO.getAllBlocks();
            List<Block> blocksToRemove = new java.util.ArrayList<>();
            
            // Collect last N blocks (but not genesis)
            for (int i = allBlocks.size() - 1; i >= 1 && blocksToRemove.size() < numberOfBlocks; i--) {
                blocksToRemove.add(allBlocks.get(i));
            }
            
            if (blocksToRemove.size() != numberOfBlocks) {
                System.err.println("Could only find " + blocksToRemove.size() + 
                                 " blocks to rollback (genesis block cannot be removed)");
                return false;
            }
            
            // Actually remove blocks using DAO
            System.out.println("Rolling back " + numberOfBlocks + " blocks:");
            for (Block block : blocksToRemove) {
                String data = block.getData();
                String displayData = data != null ? data.substring(0, Math.min(50, data.length())) : "null";
                System.out.println("  - Removing Block #" + block.getBlockNumber() + ": " + displayData);
                blockDAO.deleteBlockByNumber(block.getBlockNumber());
            }
            
            System.out.println("Rollback completed successfully");
            System.out.println("Chain now has " + blockDAO.getBlockCount() + " blocks");
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error during rollback: " + e.getMessage());
            return false;
        }
    }

    /**
     * CORE FUNCTION 4b: Rollback to specific block number
     */
    public boolean rollbackToBlock(int targetBlockNumber) {
        try {
            if (targetBlockNumber < 0) {
                System.err.println("Target block number cannot be negative");
                return false;
            }
            
            long currentBlockCount = blockDAO.getBlockCount();
            if (targetBlockNumber >= currentBlockCount) {
                System.err.println("Target block " + targetBlockNumber + 
                                 " does not exist. Current max block: " + (currentBlockCount - 1));
                return false;
            }
            
            int blocksToRemove = (int)(currentBlockCount - targetBlockNumber - 1);
            if (blocksToRemove <= 0) {
                System.out.println("Chain is already at or before block " + targetBlockNumber);
                return true;
            }
            
            // Use the deleteBlocksAfter method for better performance
            int deletedCount = blockDAO.deleteBlocksAfter(targetBlockNumber);
            
            System.out.println("Rollback to block " + targetBlockNumber + " completed successfully");
            System.out.println("Removed " + deletedCount + " blocks");
            System.out.println("Chain now has " + blockDAO.getBlockCount() + " blocks");
            
            return deletedCount > 0 || blocksToRemove == 0;
            
        } catch (Exception e) {
            System.err.println("Error during rollback to block: " + e.getMessage());
            return false;
        }
    }

    /**
     * CORE FUNCTION 5: Advanced Search - Search blocks by content
     */
    public List<Block> searchBlocksByContent(String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            // Use the DAO method for better performance
            List<Block> matchingBlocks = blockDAO.searchBlocksByContent(searchTerm);
            
            System.out.println("Found " + matchingBlocks.size() + 
                             " blocks containing: '" + searchTerm + "'");
            return matchingBlocks;
            
        } catch (Exception e) {
            System.err.println("Error searching blocks: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * CORE FUNCTION 6: Advanced Search - Get block by hash
     */
    public Block getBlockByHash(String hash) {
        try {
            if (hash == null || hash.trim().isEmpty()) {
                return null;
            }
            
            // Use the DAO method for better performance
            Block block = blockDAO.getBlockByHash(hash);
            
            if (block == null) {
                System.out.println("No block found with hash: " + hash);
            }
            
            return block;
            
        } catch (Exception e) {
            System.err.println("Error searching block by hash: " + e.getMessage());
            return null;
        }
    }

    /**
     * CORE FUNCTION 7: Advanced Search - Get blocks by date range
     */
    public List<Block> getBlocksByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        try {
            if (startDate == null || endDate == null) {
                System.err.println("Start date and end date cannot be null");
                return new java.util.ArrayList<>();
            }
            
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            return blockDAO.getBlocksByTimeRange(startDateTime, endDateTime);
            
        } catch (Exception e) {
            System.err.println("Error searching blocks by date range: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
    
    // ===== QUERY METHODS =====
    
    /**
     * Get a block by number
     */
    public Block getBlock(int blockNumber) {
        return blockDAO.getBlockByNumber(blockNumber);
    }
    
    /**
     * Get all blocks
     */
    public List<Block> getAllBlocks() {
        return blockDAO.getAllBlocks();
    }
    
    /**
     * Get the last block
     */
    public Block getLastBlock() {
        return blockDAO.getLastBlock();
    }
    
    /**
     * Get the total number of blocks
     */
    public long getBlockCount() {
        return blockDAO.getBlockCount();
    }
    
    /**
     * Get blocks by time range
     */
    public List<Block> getBlocksByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return blockDAO.getBlocksByTimeRange(startTime, endTime);
    }
    
    /**
     * Get active authorized keys
     */
    public List<AuthorizedKey> getAuthorizedKeys() {
        return authorizedKeyDAO.getActiveAuthorizedKeys();
    }
    
    /**
     * Get all authorized keys (including revoked ones) for export functionality
     */
    public List<AuthorizedKey> getAllAuthorizedKeys() {
        return authorizedKeyDAO.getAllAuthorizedKeys();
    }
    
    /**
     * Get an authorized key by owner name
     */
    public com.rbatllet.blockchain.entity.AuthorizedKey getAuthorizedKeyByOwner(String ownerName) {
        return authorizedKeyDAO.getAuthorizedKeyByOwner(ownerName);
    }
    
    /**
     * Get blockchain size limits for validation
     */
    public int getMaxBlockSizeBytes() {
        return MAX_BLOCK_SIZE_BYTES;
    }
    /**
     * Test utility: Clear all data and reinitialize with genesis block
     * WARNING: This method is for testing purposes only
     */
    public void clearAndReinitialize() {
        try {
            // Clear all blocks and keys
            blockDAO.deleteAllBlocks();
            authorizedKeyDAO.deleteAllAuthorizedKeys();
            
            // Reinitialize genesis block
            initializeGenesisBlock();
            
            System.out.println("Database cleared and reinitialized for testing");
        } catch (Exception e) {
            System.err.println("Error clearing database: " + e.getMessage());
        }
    }

    public int getMaxBlockDataLength() {
        return MAX_BLOCK_DATA_LENGTH;
    }

    // ===============================
    // DANGEROUS KEY DELETION METHODS
    // ===============================

    /**
     * Check if an authorized key can be safely deleted
     * Returns information about the impact of deletion
     * 
     * @param publicKey The public key to analyze
     * @return KeyDeletionImpact object with safety information
     */
    public KeyDeletionImpact canDeleteAuthorizedKey(String publicKey) {
        try {
            // Check if key exists and is active
            boolean keyExists = authorizedKeyDAO.isKeyAuthorized(publicKey);
            if (!keyExists) {
                // Check if key existed but is revoked
                List<AuthorizedKey> allKeys = authorizedKeyDAO.getAllAuthorizedKeys();
                boolean keyExistedBefore = allKeys.stream()
                    .anyMatch(key -> key.getPublicKey().equals(publicKey));
                
                if (keyExistedBefore) {
                    long affectedBlocks = blockDAO.countBlocksBySignerPublicKey(publicKey);
                    return new KeyDeletionImpact(true, affectedBlocks, 
                        "Key is revoked but still has " + affectedBlocks + " historical blocks");
                } else {
                    return new KeyDeletionImpact(false, 0, "Key not found in database");
                }
            }
            
            // Count affected blocks for active key
            long affectedBlocks = blockDAO.countBlocksBySignerPublicKey(publicKey);
            
            String message = affectedBlocks == 0 ? 
                "Key can be safely deleted (no historical blocks)" : 
                "Key deletion will affect " + affectedBlocks + " historical blocks";
                
            return new KeyDeletionImpact(true, affectedBlocks, message);
            
        } catch (Exception e) {
            System.err.println("Error checking deletion impact: " + e.getMessage());
            return new KeyDeletionImpact(false, -1, "Error checking deletion impact: " + e.getMessage());
        }
    }

    /**
     * DANGEROUS: Permanently delete an authorized key from the database
     * ⚠️ WARNING: This operation is IRREVERSIBLE and may break blockchain validation
     * 
     * @param publicKey The public key to delete
     * @param force If true, deletes even if it affects historical blocks
     * @param reason Reason for deletion (for audit logging)
     * @return true if key was deleted, false otherwise
     */
    public synchronized boolean dangerouslyDeleteAuthorizedKey(String publicKey, boolean force, String reason) {
        try {
            System.out.println("🚨 CRITICAL OPERATION: Attempting to permanently delete authorized key");
            System.out.println("🔑 Key fingerprint: " + publicKey.substring(0, Math.min(32, publicKey.length())) + "...");
            System.out.println("📝 Reason: " + (reason != null ? reason : "No reason provided"));
            System.out.println("⚡ Force mode: " + force);
            System.out.println("⏰ Timestamp: " + java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 1. Check deletion impact
            KeyDeletionImpact impact = canDeleteAuthorizedKey(publicKey);
            if (!impact.keyExists()) {
                System.err.println("❌ " + impact.getMessage());
                return false;
            }
            
            // 2. Safety check for historical blocks
            if (impact.isSevereImpact() && !force) {
                System.err.println("❌ SAFETY BLOCK: Cannot delete key that signed " + 
                                 impact.getAffectedBlocks() + " historical blocks");
                System.err.println("💡 Use force=true to override this safety check");
                System.err.println("⚠️ WARNING: Forcing deletion will break blockchain validation for these blocks!");
                System.err.println("📊 Impact details: " + impact.getMessage());
                return false;
            }
            
            // 3. Show impact warning for forced deletions
            if (impact.isSevereImpact()) {
                System.out.println("⚠️ ⚠️ ⚠️ CRITICAL WARNING ⚠️ ⚠️ ⚠️");
                System.out.println("This deletion will affect " + impact.getAffectedBlocks() + " historical blocks:");
                
                // Show sample of affected blocks
                List<Block> affectedBlocks = blockDAO.getBlocksBySignerPublicKey(publicKey);
                int sampleSize = Math.min(5, affectedBlocks.size());
                for (int i = 0; i < sampleSize; i++) {
                    Block block = affectedBlocks.get(i);
                    String data = block.getData();
                    String preview = data != null ? data.substring(0, Math.min(50, data.length())) : "null";
                    System.out.println("  - Block #" + block.getBlockNumber() + 
                                     " (" + block.getTimestamp() + "): " + preview + "...");
                }
                if (affectedBlocks.size() > sampleSize) {
                    System.out.println("  ... and " + (affectedBlocks.size() - sampleSize) + " more blocks");
                }
                System.out.println("⚠️ These blocks will FAIL validation after key deletion!");
                System.out.println("🔥 Proceeding with IRREVERSIBLE deletion...");
            }
            
            // 4. Get key info for logging before deletion
            List<AuthorizedKey> keyRecords = authorizedKeyDAO.getAllAuthorizedKeys().stream()
                .filter(key -> key.getPublicKey().equals(publicKey))
                .toList();
            
            // 5. Perform deletion
            boolean deleted = authorizedKeyDAO.deleteAuthorizedKey(publicKey);
            
            // 6. Comprehensive audit logging
            if (deleted) {
                System.out.println("🗑️ ✅ Key permanently deleted from database");
                System.out.println("📊 Deletion summary:");
                System.out.println("   - Key records removed: " + keyRecords.size());
                System.out.println("   - Historical blocks affected: " + impact.getAffectedBlocks());
                System.out.println("   - Force mode used: " + force);
                System.out.println("   - Deletion reason: " + reason);
                
                for (AuthorizedKey keyRecord : keyRecords) {
                    System.out.println("   - Deleted record: " + keyRecord.getOwnerName() + 
                                     " (created: " + keyRecord.getCreatedAt() + 
                                     ", active: " + keyRecord.isActive() + ")");
                }
                
                System.out.println("📝 Audit log: Key deletion completed at " + 
                                 java.time.LocalDateTime.now().format(
                                     java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                System.out.println("⚠️ WARNING: This action was IRREVERSIBLE!");
                
                // Recommend validation check if blocks were affected
                if (impact.isSevereImpact()) {
                    System.out.println("💡 STRONGLY RECOMMENDED: Run validateChain() to verify blockchain integrity");
                    System.out.println("🔧 Consider running blockchain repair tools if validation fails");
                }
            } else {
                System.err.println("❌ Failed to delete key from database");
                System.err.println("💡 This might indicate the key was already deleted or a database error occurred");
            }
            
            return deleted;
            
        } catch (Exception e) {
            System.err.println("💥 Critical error during key deletion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Convenience method for dangerous key deletion with force=false
     * @param publicKey The public key to delete
     * @param reason Reason for deletion (for audit logging)
     * @return true if key was deleted, false otherwise
     */
    public boolean dangerouslyDeleteAuthorizedKey(String publicKey, String reason) {
        return dangerouslyDeleteAuthorizedKey(publicKey, false, reason);
    }

    /**
     * Expose the regular deleteAuthorizedKey method referenced in documentation
     * This is a safe wrapper that only deletes keys with no historical impact
     * @param publicKey The public key to delete
     * @return true if key was safely deleted, false otherwise
     */
    public boolean deleteAuthorizedKey(String publicKey) {
        KeyDeletionImpact impact = canDeleteAuthorizedKey(publicKey);
        
        if (!impact.keyExists()) {
            System.err.println("Cannot delete key: " + impact.getMessage());
            return false;
        }
        
        if (impact.isSevereImpact()) {
            System.err.println("Cannot delete key: " + impact.getMessage());
            System.err.println("Use dangerouslyDeleteAuthorizedKey() with force=true if deletion is absolutely necessary");
            return false;
        }
        
        // Safe to delete - no historical blocks affected
        System.out.println("Safely deleting authorized key (no historical blocks affected)");
        return authorizedKeyDAO.deleteAuthorizedKey(publicKey);
    }

    // ===============================
    // CHAIN RECOVERY METHODS
    // ===============================

    /**
     * Manual recovery method for corrupted chains
     * @param deletedPublicKey The key that was deleted and caused corruption
     * @param ownerName Original owner name for re-authorization
     * @return RecoveryResult with success status and details
     */
    public com.rbatllet.blockchain.recovery.ChainRecoveryManager.RecoveryResult recoverCorruptedChain(String deletedPublicKey, String ownerName) {
        com.rbatllet.blockchain.recovery.ChainRecoveryManager recovery = 
            new com.rbatllet.blockchain.recovery.ChainRecoveryManager(this);
        return recovery.recoverCorruptedChain(deletedPublicKey, ownerName);
    }

    /**
     * Diagnose chain corruption
     * @return ChainDiagnostic with detailed information about corruption
     */
    public com.rbatllet.blockchain.recovery.ChainRecoveryManager.ChainDiagnostic diagnoseCorruption() {
        com.rbatllet.blockchain.recovery.ChainRecoveryManager recovery = 
            new com.rbatllet.blockchain.recovery.ChainRecoveryManager(this);
        return recovery.diagnoseCorruption();
    }

    /**
     * Validate chain with automatic recovery attempt
     * @return true if chain is valid or was successfully recovered, false otherwise
     */
    public boolean validateChainWithRecovery() {
        if (validateChain()) {
            return true;
        }
        
        System.out.println("🔧 Chain validation failed, attempting diagnostic...");
        com.rbatllet.blockchain.recovery.ChainRecoveryManager.ChainDiagnostic diagnostic = diagnoseCorruption();
        System.out.println("📊 Diagnostic: " + diagnostic);
        
        if (!diagnostic.isHealthy()) {
            System.out.println("💡 Use recoverCorruptedChain() for manual recovery");
        }
        
        return false;
    }

    /**
     * Information about the impact of deleting an authorized key
     * Used to assess safety before performing dangerous key deletion operations
     */
    public static class KeyDeletionImpact {
        private final boolean keyExists;
        private final long affectedBlocks;
        private final String message;
        
        public KeyDeletionImpact(boolean keyExists, long affectedBlocks, String message) {
            this.keyExists = keyExists;
            this.affectedBlocks = affectedBlocks;
            this.message = message;
        }
        
        /**
         * @return true if the key exists in the database
         */
        public boolean keyExists() { 
            return keyExists; 
        }
        
        /**
         * @return true if the key can be deleted without affecting any historical blocks
         */
        public boolean canSafelyDelete() { 
            return keyExists && affectedBlocks == 0; 
        }
        
        /**
         * @return number of historical blocks that would be affected by deletion
         */
        public long getAffectedBlocks() { 
            return affectedBlocks; 
        }
        
        /**
         * @return human-readable message describing the impact
         */
        public String getMessage() { 
            return message; 
        }
        
        /**
         * @return true if deletion would have severe consequences (affects blocks)
         */
        public boolean isSevereImpact() {
            return affectedBlocks > 0;
        }
        
        @Override
        public String toString() {
            return String.format("KeyDeletionImpact{exists=%s, canSafelyDelete=%s, affectedBlocks=%d, message='%s'}", 
                               keyExists, canSafelyDelete(), affectedBlocks, message);
        }
    }
}
