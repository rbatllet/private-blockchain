package com.rbatllet.blockchain.demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;

public class SimpleDemo {
    
    public static void main(String[] args) {
        System.out.println("=== SIMPLE BLOCKCHAIN DEMO ===\n");
        
        try {
            // 1. Create blockchain instance
            Blockchain blockchain = new Blockchain();
            System.out.println("Blockchain created");
            
            // 2. Generate key pair
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            
            // 3. Add authorized key
            blockchain.addAuthorizedKey(publicKeyString, "TestUser");
            System.out.println("Key authorized");
            
            // 4. Add one block
            boolean added = blockchain.addBlock("Test data", keyPair.getPrivate(), keyPair.getPublic());
            System.out.println("Block added: " + added);
            
            // 5. Get all blocks and print details
            System.out.println("\n=== BLOCKS ===");
            for (Block block : blockchain.getAllBlocks()) {
                System.out.println("Block #" + block.getBlockNumber() + ": " + block.getData());
                System.out.println("  Hash: " + block.getHash());
                System.out.println("  Previous: " + block.getPreviousHash());
                System.out.println("  Signer: " + (block.getSignerPublicKey() != null ? 
                    (block.getSignerPublicKey().length() > 20 ? 
                        block.getSignerPublicKey().substring(0, 20) + "..." : 
                        block.getSignerPublicKey()) : "null"));
            }
            
            // 6. Validate chain
            boolean isValid = blockchain.validateChain();
            System.out.println("\nChain is valid: " + isValid);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
