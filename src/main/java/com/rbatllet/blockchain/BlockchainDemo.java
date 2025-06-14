package com.rbatllet.blockchain;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;

/**
 * Demonstration of basic blockchain functionality
 * Shows how to create a blockchain, add authorized users, and process transactions
 */
public class BlockchainDemo {
    
    public static void main(String[] args) {
        System.out.println("=== PRIVATE BLOCKCHAIN DEMO ===\n");
        
        try {
            // 1. Create blockchain instance
            Blockchain blockchain = new Blockchain();
            
            // 2. Generate key pairs for two users
            System.out.println("Generating key pairs...");
            KeyPair userAlice = CryptoUtil.generateKeyPair();
            KeyPair userBob = CryptoUtil.generateKeyPair();
            
            // 3. Authorize the keys
            System.out.println("\nAuthorizing keys...");
            String alicePublicKey = CryptoUtil.publicKeyToString(userAlice.getPublic());
            String bobPublicKey = CryptoUtil.publicKeyToString(userBob.getPublic());
            
            blockchain.addAuthorizedKey(alicePublicKey, "Alice");
            blockchain.addAuthorizedKey(bobPublicKey, "Bob");
            
            // 4. Add some blocks to the chain
            System.out.println("\nAdding blocks to blockchain...");
            
            blockchain.addBlock("First transaction: Alice sends data", 
                              userAlice.getPrivate(), userAlice.getPublic());
            
            blockchain.addBlock("Second transaction: Bob receives data", 
                              userBob.getPrivate(), userBob.getPublic());
            
            blockchain.addBlock("Third transaction: Alice updates record", 
                              userAlice.getPrivate(), userAlice.getPublic());
            
            // 5. Validate the entire chain
            System.out.println("\nValidating blockchain...");
            boolean isValid = blockchain.validateChain();
            System.out.println("Blockchain is valid: " + isValid);
            
            // 6. Display chain information
            System.out.println("\n=== BLOCKCHAIN STATUS ===");
            System.out.println("Total blocks: " + blockchain.getBlockCount());
            System.out.println("Authorized keys: " + blockchain.getAuthorizedKeys().size());
            
        } catch (Exception e) {
            System.err.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== DEMO COMPLETED ===");
    }
}
