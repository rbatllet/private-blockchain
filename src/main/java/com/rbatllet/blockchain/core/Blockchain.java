package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.dao.AuthorizedKeyDAO;
import com.rbatllet.blockchain.dao.BlockDAO;
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
     * CORE FUNCTION: Add a new block to the chain
     */
    public boolean addBlock(String data, PrivateKey signerPrivateKey, PublicKey signerPublicKey) {
        try {
            // 1. Verify that the key is authorized
            String publicKeyString = CryptoUtil.publicKeyToString(signerPublicKey);
            if (!authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                System.err.println("Unauthorized key attempting to add block");
                return false;
            }
            
            // 2. Get the last block
            Block lastBlock = blockDAO.getLastBlock();
            if (lastBlock == null) {
                System.err.println("No genesis block found");
                return false;
            }
            
            // 3. Create the new block
            Block newBlock = new Block();
            newBlock.setBlockNumber(lastBlock.getBlockNumber() + 1);
            newBlock.setPreviousHash(lastBlock.getHash());
            newBlock.setData(data);
            newBlock.setTimestamp(LocalDateTime.now());
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
            
            // 5. Verify that the key is authorized
            if (!authorizedKeyDAO.isKeyAuthorized(block.getSignerPublicKey())) {
                System.err.println("Block signed by unauthorized key for block #" + block.getBlockNumber());
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error validating block: " + e.getMessage());
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
     */
    public boolean addAuthorizedKey(String publicKeyString, String ownerName) {
        try {
            // Verify that the key doesn't already exist
            if (authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                System.err.println("Key already authorized");
                return false;
            }
            
            AuthorizedKey authorizedKey = new AuthorizedKey(publicKeyString, ownerName);
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
     */
    public boolean revokeAuthorizedKey(String publicKeyString) {
        try {
            if (!authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                System.err.println("Key not found or already inactive");
                return false;
            }
            
            authorizedKeyDAO.deactivateKey(publicKeyString);
            System.out.println("Key revoked successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error revoking key: " + e.getMessage());
            return false;
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
}