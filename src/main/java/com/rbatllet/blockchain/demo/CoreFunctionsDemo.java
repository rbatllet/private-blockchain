package com.rbatllet.blockchain.demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;

import java.util.List;

public class CoreFunctionsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== DEMONSTRATING ALL CORE FUNCTIONS ===\n");
        
        try {
            // ===============================
            // 1. CORE FUNCTION: Initialize Blockchain (Genesis Block)
            // ===============================
            System.out.println("1. DEMONSTRATING: Initialize Blockchain + Genesis Block");
            Blockchain blockchain = new Blockchain();
            
            // Verify genesis block exists
            long initialBlocks = blockchain.getBlockCount();
            System.out.println("   ✓ Genesis block created");
            System.out.println("   ✓ Initial block count: " + initialBlocks);
            assert initialBlocks == 1 : "Genesis block should exist";
            
            Block genesisBlock = blockchain.getBlock(0L);
            System.out.println("   ✓ Genesis block hash: " + genesisBlock.getHash().substring(0, 16) + "...");
            System.out.println("   ✓ Genesis block data: " + genesisBlock.getData());
            System.out.println("   SUCCESS: Blockchain initialized\n");
            
            // ===============================
            // 2. CORE FUNCTION: Add Authorized Keys
            // ===============================
            System.out.println("2. DEMONSTRATING: Add Authorized Keys");
            
            // Generate key pairs for test users
            KeyPair alice = CryptoUtil.generateKeyPair();
            KeyPair bob = CryptoUtil.generateKeyPair();
            KeyPair charlie = CryptoUtil.generateKeyPair();
            
            String alicePublicKey = CryptoUtil.publicKeyToString(alice.getPublic());
            String bobPublicKey = CryptoUtil.publicKeyToString(bob.getPublic());
            String charliePublicKey = CryptoUtil.publicKeyToString(charlie.getPublic());
            
            // Test adding authorized keys
            boolean alice_added = blockchain.addAuthorizedKey(alicePublicKey, "Alice");
            boolean bob_added = blockchain.addAuthorizedKey(bobPublicKey, "Bob");
            
            System.out.println("   ✓ Alice's key added: " + alice_added);
            System.out.println("   ✓ Bob's key added: " + bob_added);
            
            // Verify keys were added
            List<AuthorizedKey> keys = blockchain.getAuthorizedKeys();
            System.out.println("   ✓ Number of authorized keys: " + keys.size());
            assert keys.size() >= 2 : "Should have at least 2 authorized keys";
            System.out.println("   SUCCESS: Authorized keys added\n");
            
            // ===============================
            // 3. CORE FUNCTION: Add Blocks
            // ===============================
            System.out.println("3. DEMONSTRATING: Add Blocks");
            
            // Add blocks signed by different users
            boolean block1 = blockchain.addBlock("Alice's transaction data", alice.getPrivate(), alice.getPublic());
            boolean block2 = blockchain.addBlock("Bob's transaction data", bob.getPrivate(), bob.getPublic());
            
            System.out.println("   ✓ Alice's block added: " + block1);
            System.out.println("   ✓ Bob's block added: " + block2);
            
            // Verify blocks were added
            long blockCount = blockchain.getBlockCount();
            System.out.println("   ✓ Total blocks: " + blockCount);
            assert blockCount == 3 : "Should have 3 blocks (genesis + 2 new)";
            System.out.println("   SUCCESS: Blocks added\n");
            
            // ===============================
            // 4. CORE FUNCTION: Validate Chain
            // ===============================
            System.out.println("4. DEMONSTRATING: Validate Chain");
            
            boolean isValid = blockchain.validateChain();
            System.out.println("   ✓ Chain validation: " + isValid);
            assert isValid : "Chain should be valid";
            System.out.println("   SUCCESS: Chain validated\n");
            
            // ===============================
            // 5. CORE FUNCTION: Get Block by Number
            // ===============================
            System.out.println("5. DEMONSTRATING: Get Block by Number");
            
            Block block = blockchain.getBlock(1L);
            System.out.println("   ✓ Block #1 hash: " + block.getHash().substring(0, 16) + "...");
            System.out.println("   ✓ Block #1 data: " + block.getData());
            System.out.println("   ✓ Block #1 signer: " + block.getSignerPublicKey().substring(0, 16) + "...");
            System.out.println("   SUCCESS: Block retrieved\n");
            
            // ===============================
            // 6. CORE FUNCTION: Get All Blocks
            // ===============================
            System.out.println("6. DEMONSTRATING: Get All Blocks");
            
            List<Block> allBlocks = blockchain.getAllBlocks();
            System.out.println("   ✓ Retrieved " + allBlocks.size() + " blocks");
            
            for (Block b : allBlocks) {
                System.out.println("     Block #" + b.getBlockNumber() + ": " + b.getData());
            }
            System.out.println("   SUCCESS: All blocks retrieved\n");
            
            // ===============================
            // 7. CORE FUNCTION: Search Blocks by Content
            // ===============================
            System.out.println("7. DEMONSTRATING: Search Blocks by Content");
            
            List<Block> aliceBlocks = blockchain.searchBlocksByContent("Alice");
            System.out.println("   ✓ Found " + aliceBlocks.size() + " blocks containing 'Alice'");
            
            List<Block> bobBlocks = blockchain.searchBlocksByContent("Bob");
            System.out.println("   ✓ Found " + bobBlocks.size() + " blocks containing 'Bob'");
            System.out.println("   SUCCESS: Block search completed\n");
            
            // ===============================
            // 8. CORE FUNCTION: Unauthorized Key Test
            // ===============================
            System.out.println("8. DEMONSTRATING: Unauthorized Key Rejection");
            
            // Try to add block with unauthorized key
            boolean charlieBlock = blockchain.addBlock("Charlie's transaction data", 
                                                     charlie.getPrivate(), CryptoUtil.stringToPublicKey(charliePublicKey));
            
            System.out.println("   ✓ Charlie's block (unauthorized) added: " + charlieBlock);
            assert !charlieBlock : "Unauthorized block should be rejected";
            System.out.println("   SUCCESS: Unauthorized key rejected\n");
            
            // ===============================
            // FINAL VERIFICATION
            // ===============================
            System.out.println("FINAL VERIFICATION:");
            System.out.println("   ✓ Total blocks: " + blockchain.getBlockCount());
            System.out.println("   ✓ Authorized keys: " + blockchain.getAuthorizedKeys().size());
            System.out.println("   ✓ Chain is valid: " + blockchain.validateChain());
            
            System.out.println("\n=== ALL CORE FUNCTIONS DEMONSTRATED SUCCESSFULLY ===");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
