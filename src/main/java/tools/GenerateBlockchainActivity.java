package tools;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Generates blockchain activity to test the dashboard
 * Creates blocks, searches, and validations to populate logs
 *
 * Uses batch write API (Phase 5.2) for improved throughput
 */
public class GenerateBlockchainActivity {

    private static final Logger logger = LogManager.getLogger(GenerateBlockchainActivity.class);

    public static void main(String[] args) {
        logger.info("🚀 Starting blockchain activity generator...");

        try {
            Blockchain blockchain = new Blockchain();
            KeyPair keyPair = CryptoUtil.generateKeyPair();

            // Create genesis admin (simplified method - v1.0.6+)
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(keyPair.getPublic()),
                "ActivityGenerator"
            );

            logger.info("📊 Generating blockchain activity for dashboard testing...");

            // Use batch write for initial regular blocks (Phase 5.2)
            logger.info("🔄 Creating 20 regular blocks using batch API...");
            List<Blockchain.BlockWriteRequest> regularBlocks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                regularBlocks.add(new Blockchain.BlockWriteRequest(
                    "Test block " + i + " - Regular data",
                    keyPair.getPrivate(),
                    keyPair.getPublic()
                ));
            }
            blockchain.addBlocksBatch(regularBlocks);
            logger.info("✅ Batch created 20 regular blocks");

            // Add some encrypted blocks individually (encryption requires different handling)
            for (int i = 0; i < 7; i++) {
                blockchain.addEncryptedBlock(
                    "Encrypted data " + i,
                    "password123",
                    keyPair.getPrivate(),
                    keyPair.getPublic()
                );
                Thread.sleep(100);
            }
            logger.info("✅ Created 7 encrypted blocks");

            // Perform search operations
            for (int i = 0; i < 5; i++) {
                blockchain.searchBlocks("test");
                Thread.sleep(100);
            }
            logger.info("✅ Performed 5 search operations");

            // Perform validations
            for (int i = 0; i < 3; i++) {
                blockchain.validateChainDetailed();
                Thread.sleep(100);
            }
            logger.info("✅ Performed 3 validation operations");
            
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