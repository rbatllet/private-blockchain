package com.rbatllet.blockchain;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.List;

public class CoreFunctionsTest {
    
    public static void main(String[] args) {
        System.out.println("=== TESTING ALL CORE FUNCTIONS ===\n");
        
        try {
            // ===============================
            // 1. CORE FUNCTION: Initialize Blockchain (Genesis Block)
            // ===============================
            System.out.println("1. TESTING: Initialize Blockchain + Genesis Block");
            Blockchain blockchain = new Blockchain();
            
            // Verify genesis block exists
            long initialBlocks = blockchain.getBlockCount();
            System.out.println("   ✓ Genesis block created");
            System.out.println("   ✓ Initial block count: " + initialBlocks);
            assert initialBlocks == 1 : "Genesis block should exist";
            
            Block genesisBlock = blockchain.getBlock(0);
            System.out.println("   ✓ Genesis block hash: " + genesisBlock.getHash().substring(0, 16) + "...");
            System.out.println("   ✓ Genesis block data: " + genesisBlock.getData());
            System.out.println("   SUCCESS: Blockchain initialized\n");
            
            // ===============================
            // 2. CORE FUNCTION: Add Authorized Keys
            // ===============================
            System.out.println("2. TESTING: Add Authorized Keys");
            
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
            boolean charlie_added = blockchain.addAuthorizedKey(charliePublicKey, "Charlie");
            
            System.out.println("   ✓ Alice added: " + alice_added);
            System.out.println("   ✓ Bob added: " + bob_added);
            System.out.println("   ✓ Charlie added: " + charlie_added);
            assert alice_added && bob_added && charlie_added : "All keys should be added successfully";
            
            // Test duplicate key (should fail)
            boolean duplicate = blockchain.addAuthorizedKey(alicePublicKey, "Alice Again");
            System.out.println("   ✓ Duplicate key rejected: " + !duplicate);
            assert !duplicate : "Duplicate key should be rejected";
            
            // Verify authorized keys count
            List<AuthorizedKey> authorizedKeys = blockchain.getAuthorizedKeys();
            System.out.println("   ✓ Total authorized keys: " + authorizedKeys.size());
            assert authorizedKeys.size() == 3 : "Should have 3 authorized keys";
            System.out.println("   SUCCESS: Authorized keys management working\n");
            
            // ===============================
            // 3. CORE FUNCTION: Add Blocks to Chain
            // ===============================
            System.out.println("3. TESTING: Add Blocks to Chain");
            
            // Test adding blocks with authorized keys
            boolean block1 = blockchain.addBlock("Alice's first transaction", 
                                                alice.getPrivate(), alice.getPublic());
            boolean block2 = blockchain.addBlock("Bob joins the network", 
                                                bob.getPrivate(), bob.getPublic());
            boolean block3 = blockchain.addBlock("Charlie sends data to Alice", 
                                                charlie.getPrivate(), charlie.getPublic());
            boolean block4 = blockchain.addBlock("Alice processes Charlie's data", 
                                                alice.getPrivate(), alice.getPublic());
            
            System.out.println("   ✓ Alice's block added: " + block1);
            System.out.println("   ✓ Bob's block added: " + block2);
            System.out.println("   ✓ Charlie's block added: " + block3);
            System.out.println("   ✓ Alice's second block added: " + block4);
            assert block1 && block2 && block3 && block4 : "All blocks should be added successfully";
            
            // Test unauthorized key (should fail)
            KeyPair unauthorized = CryptoUtil.generateKeyPair();
            boolean unauthorizedBlock = blockchain.addBlock("Unauthorized attempt", 
                                                          unauthorized.getPrivate(), unauthorized.getPublic());
            System.out.println("   ✓ Unauthorized block rejected: " + !unauthorizedBlock);
            assert !unauthorizedBlock : "Unauthorized block should be rejected";
            
            // Verify total blocks
            long totalBlocks = blockchain.getBlockCount();
            System.out.println("   ✓ Total blocks: " + totalBlocks);
            assert totalBlocks == 5 : "Should have 5 blocks (1 genesis + 4 data)";
            System.out.println("   SUCCESS: Block addition working correctly\n");
            
            // ===============================
            // 4. CORE FUNCTION: Validate Entire Chain
            // ===============================
            System.out.println("4. TESTING: Validate Entire Chain");
            
            boolean chainValid = blockchain.validateChain();
            System.out.println("   ✓ Chain validation result: " + chainValid);
            assert chainValid : "Chain should be valid";
            
            // Display all blocks
            System.out.println("   ✓ All blocks in chain:");
            List<Block> allBlocks = blockchain.getAllBlocks();
            for (Block block : allBlocks) {
                System.out.println("      Block #" + block.getBlockNumber() + ": " + 
                                 block.getData().substring(0, Math.min(30, block.getData().length())) + 
                                 (block.getData().length() > 30 ? "..." : ""));
            }
            System.out.println("   SUCCESS: Chain validation working\n");
            
            // ===============================
            // 5. CORE FUNCTION: Revoke Authorized Key
            // ===============================
            System.out.println("5. TESTING: Revoke Authorized Key");
            
            // Revoke Charlie's access
            boolean charlieRevoked = blockchain.revokeAuthorizedKey(charliePublicKey);
            System.out.println("   ✓ Charlie's key revoked: " + charlieRevoked);
            assert charlieRevoked : "Charlie's key should be revoked successfully";
            
            // Test that Charlie can no longer add blocks
            boolean charlieBlockAfterRevoke = blockchain.addBlock("Charlie tries after revoke", 
                                                                charlie.getPrivate(), charlie.getPublic());
            System.out.println("   ✓ Charlie's block after revoke rejected: " + !charlieBlockAfterRevoke);
            assert !charlieBlockAfterRevoke : "Charlie should not be able to add blocks after revoke";
            
            // Test revoking non-existent key
            boolean nonExistentRevoke = blockchain.revokeAuthorizedKey("non-existent-key");
            System.out.println("   ✓ Non-existent key revoke handled: " + !nonExistentRevoke);
            assert !nonExistentRevoke : "Non-existent key revoke should fail gracefully";
            
            // Verify active keys count
            List<AuthorizedKey> activeKeys = blockchain.getAuthorizedKeys();
            System.out.println("   ✓ Active authorized keys: " + activeKeys.size());
            assert activeKeys.size() == 2 : "Should have 2 active keys (Alice and Bob)";
            System.out.println("   SUCCESS: Key revocation working\n");
            
            // ===============================
            // 6. FINAL VALIDATION
            // ===============================
            System.out.println("6. FINAL VALIDATION");
            
            // Validate chain is still valid after all operations
            boolean finalValidation = blockchain.validateChain();
            System.out.println("   ✓ Final chain validation: " + finalValidation);
            assert finalValidation : "Chain should still be valid after all operations";
            
            // Display final statistics
            System.out.println("   ✓ Final Statistics:");
            System.out.println("      - Total blocks: " + blockchain.getBlockCount());
            System.out.println("      - Active authorized keys: " + blockchain.getAuthorizedKeys().size());
            System.out.println("      - Chain integrity: " + finalValidation);
            
            // Test query functions
            Block lastBlock = blockchain.getLastBlock();
            System.out.println("      - Last block data: " + lastBlock.getData());
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);
            List<Block> recentBlocks = blockchain.getBlocksByTimeRange(oneHourAgo, now);
            System.out.println("      - Recent blocks: " + recentBlocks.size());
            
            System.out.println("   SUCCESS: Final validation complete\n");
            
            // ===============================
            // SUMMARY
            // ===============================
            System.out.println("=== ALL CORE FUNCTIONS TEST RESULTS ===");
            System.out.println("✓ Genesis Block Creation: PASSED");
            System.out.println("✓ Add Authorized Keys: PASSED");
            System.out.println("✓ Add Blocks to Chain: PASSED");
            System.out.println("✓ Validate Entire Chain: PASSED");
            System.out.println("✓ Revoke Authorized Keys: PASSED");
            System.out.println("✓ Security Controls: PASSED");
            System.out.println("✓ Error Handling: PASSED");
            System.out.println();
            System.out.println("🎉 ALL CORE FUNCTIONS WORKING PERFECTLY! 🎉");
            
        } catch (Exception e) {
            System.err.println("❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
        } catch (AssertionError e) {
            System.err.println("❌ ASSERTION FAILED: " + e.getMessage());
        }
    }
}