package tools;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.KeyPair;
import java.util.concurrent.*;

/**
 * Generates blockchain activity to test the dashboard
 * Creates blocks, searches, and validations to populate logs
 */
public class GenerateBlockchainActivity {
    
    private static final Logger logger = LogManager.getLogger(GenerateBlockchainActivity.class);
    
    public static void main(String[] args) {
        logger.info("🚀 Starting blockchain activity generator...");
        
        try {
            Blockchain blockchain = new Blockchain();
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            
            // Add authorized key
            blockchain.addAuthorizedKey(
                CryptoUtil.publicKeyToString(keyPair.getPublic()),
                "ActivityGenerator"
            );
            
            logger.info("📊 Generating blockchain activity for dashboard testing...");
            
            // Generate various types of operations
            for (int i = 0; i < 20; i++) {
                // Regular block
                blockchain.addBlockAndReturn(
                    "Test block " + i + " - Regular data",
                    keyPair.getPrivate(),
                    keyPair.getPublic()
                );
                
                Thread.sleep(100);
                
                // Encrypted block
                if (i % 3 == 0) {
                    blockchain.addEncryptedBlock(
                        "Encrypted data " + i,
                        "password123",
                        keyPair.getPrivate(),
                        keyPair.getPublic()
                    );
                }
                
                // Search operation
                if (i % 2 == 0) {
                    blockchain.searchBlocks("test");
                }
                
                // Validation
                if (i % 5 == 0) {
                    blockchain.validateChainDetailed();
                }
            }
            
            // Generate some large data for off-chain storage
            StringBuilder largeData = new StringBuilder();
            for (int j = 0; j < 15000; j++) {
                largeData.append("Large-Data-Block-Content-");
            }
            blockchain.addBlockAndReturn(
                largeData.toString(),
                keyPair.getPrivate(),
                keyPair.getPublic()
            );
            
            logger.info("✅ Activity generation completed!");
            logger.info("📊 Total blocks created: " + blockchain.getBlockCount());
            
            // Keep generating activity for 30 seconds
            logger.info("🔄 Continuing with periodic activity for 30 seconds...");
            
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    blockchain.addBlockAndReturn(
                        "Periodic block at " + System.currentTimeMillis(),
                        keyPair.getPrivate(),
                        keyPair.getPublic()
                    );
                } catch (Exception e) {
                    logger.error("Error in periodic activity", e);
                }
            }, 0, 2, TimeUnit.SECONDS);
            
            Thread.sleep(30000);
            
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
            logger.info("🛑 Activity generator stopped.");
            
        } catch (Exception e) {
            logger.error("Error generating activity", e);
        }
    }
}