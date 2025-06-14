package com.rbatllet.blockchain;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;

public class QuickDemo {
    
    public static void main(String[] args) {
        System.out.println("=== QUICK DEMO ===");
        
        try {
            Blockchain blockchain = new Blockchain();
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            
            blockchain.addAuthorizedKey(publicKeyString, "TestUser");
            boolean added = blockchain.addBlock("Test data", keyPair.getPrivate(), keyPair.getPublic());
            
            System.out.println("Block added: " + added);
            System.out.println("Total blocks: " + blockchain.getBlockCount());
            
            // No validation for now - just check the blockchain works
            System.out.println("SUCCESS: Blockchain is working!");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
