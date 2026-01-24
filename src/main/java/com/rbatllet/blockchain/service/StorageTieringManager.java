package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Smart Storage Tiering Manager for blockchain data optimization
 * Automatically moves data between hot (on-chain) and cold (off-chain) storage
 * based on access patterns and configurable policies
 */
public class StorageTieringManager {
    
    private static final Logger logger = LoggerFactory.getLogger(StorageTieringManager.class);
    
    // Storage tiers
    public enum StorageTier {
        HOT("🔥 Hot - On-chain", 0),
        WARM("🌡️ Warm - Fast off-chain", 1),
        COLD("❄️ Cold - Compressed off-chain", 2),
        ARCHIVE("🗄️ Archive - Deep storage", 3);
        
        private final String displayName;
        private final int level;
        
        StorageTier(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }
        
        public String getDisplayName() { return displayName; }
        public int getLevel() { return level; }
    }
    
    // Tiering policies
    public static class TieringPolicy {
        private final Duration hotThreshold;      // Time before moving to warm
        private final Duration warmThreshold;     // Time before moving to cold
        private final Duration coldThreshold;     // Time before moving to archive
        private final long sizeThresholdBytes;    // Size threshold for immediate off-chain
        private final int accessCountThreshold;   // Min accesses to keep hot
        private final boolean compressOnCold;     // Compress when moving to cold
        private final boolean enableAutoTiering;  // Enable automatic tiering
        
        public TieringPolicy(Duration hotThreshold, Duration warmThreshold, 
                           Duration coldThreshold, long sizeThresholdBytes,
                           int accessCountThreshold, boolean compressOnCold,
                           boolean enableAutoTiering) {
            this.hotThreshold = hotThreshold;
            this.warmThreshold = warmThreshold;
            this.coldThreshold = coldThreshold;
            this.sizeThresholdBytes = sizeThresholdBytes;
            this.accessCountThreshold = accessCountThreshold;
            this.compressOnCold = compressOnCold;
            this.enableAutoTiering = enableAutoTiering;
        }
        
        // Default policy
        public static TieringPolicy getDefaultPolicy() {
            return new TieringPolicy(
                Duration.ofDays(7),      // Hot for 7 days
                Duration.ofDays(30),     // Warm for 30 days
                Duration.ofDays(90),     // Cold for 90 days
                1024 * 1024,             // 1MB size threshold
                5,                       // Min 5 accesses to stay hot
                true,                    // Compress cold data
                true                     // Auto-tiering enabled
            );
        }
        
        // Getters
        public Duration getHotThreshold() { return hotThreshold; }
        public Duration getWarmThreshold() { return warmThreshold; }
        public Duration getColdThreshold() { return coldThreshold; }
        public long getSizeThresholdBytes() { return sizeThresholdBytes; }
        public int getAccessCountThreshold() { return accessCountThreshold; }
        public boolean isCompressOnCold() { return compressOnCold; }
        public boolean isEnableAutoTiering() { return enableAutoTiering; }
    }
    
    // Storage statistics
    public static class StorageStatistics {
        private final Map<StorageTier, Integer> tierCounts;
        private final Map<StorageTier, Long> tierSizes;
        private final long totalDataSize;
        private final long totalCompressedSize;
        private final double compressionRatio;
        private final int totalMigrations;
        
        public StorageStatistics() {
            this.tierCounts = new EnumMap<>(StorageTier.class);
            this.tierSizes = new EnumMap<>(StorageTier.class);
            this.totalDataSize = 0;
            this.totalCompressedSize = 0;
            this.compressionRatio = 0.0;
            this.totalMigrations = 0;
        }
        
        public StorageStatistics(Map<StorageTier, Integer> tierCounts,
                               Map<StorageTier, Long> tierSizes,
                               long totalDataSize, long totalCompressedSize,
                               int totalMigrations) {
            this.tierCounts = new EnumMap<>(tierCounts);
            this.tierSizes = new EnumMap<>(tierSizes);
            this.totalDataSize = totalDataSize;
            this.totalCompressedSize = totalCompressedSize;
            this.compressionRatio = totalDataSize > 0 ? 
                (1.0 - (double)totalCompressedSize / totalDataSize) * 100 : 0.0;
            this.totalMigrations = totalMigrations;
        }
        
        // Getters
        public Map<StorageTier, Integer> getTierCounts() { return Collections.unmodifiableMap(tierCounts); }
        public Map<StorageTier, Long> getTierSizes() { return Collections.unmodifiableMap(tierSizes); }
        public long getTotalDataSize() { return totalDataSize; }
        public long getTotalCompressedSize() { return totalCompressedSize; }
        public double getCompressionRatio() { return compressionRatio; }
        public int getTotalMigrations() { return totalMigrations; }
        
        public String getFormattedSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("📊 Storage Tiering Statistics\n");
            sb.append("================================\n\n");
            
            sb.append("📈 Tier Distribution:\n");
            for (StorageTier tier : StorageTier.values()) {
                int count = tierCounts.getOrDefault(tier, 0);
                long size = tierSizes.getOrDefault(tier, 0L);
                if (count > 0) {
                    sb.append(String.format("  %s: %d blocks (%.2f MB)\n", 
                        tier.getDisplayName(), count, size / (1024.0 * 1024.0)));
                }
            }
            
            sb.append("\n💾 Storage Efficiency:\n");
            sb.append(String.format("  - Total Data Size: %.2f MB\n", 
                totalDataSize / (1024.0 * 1024.0)));
            sb.append(String.format("  - Compressed Size: %.2f MB\n", 
                totalCompressedSize / (1024.0 * 1024.0)));
            sb.append(String.format("  - Compression Ratio: %.2f%%\n", compressionRatio));
            sb.append(String.format("  - Total Migrations: %d\n", totalMigrations));
            
            return sb.toString();
        }
    }
    
    // Access tracking
    private static class AccessInfo {
        LocalDateTime lastAccessed;
        int accessCount;
        long dataSize;
        StorageTier currentTier;
        OffChainData offChainReference; // Store reference for retrieval
        
        AccessInfo(long dataSize) {
            this.lastAccessed = LocalDateTime.now();
            this.accessCount = 1;
            this.dataSize = dataSize;
            this.currentTier = StorageTier.HOT;
            this.offChainReference = null;
        }
        
        void recordAccess() {
            this.lastAccessed = LocalDateTime.now();
            this.accessCount++;
        }
    }
    
    private final Map<Long, AccessInfo> accessTracking;
    private final TieringPolicy policy;
    private final AtomicLong totalMigrations;
    private final OffChainStorageService offChainStorage;
    
    public StorageTieringManager(TieringPolicy policy, OffChainStorageService offChainStorage) {
        this.policy = policy;
        this.offChainStorage = offChainStorage;
        this.accessTracking = new ConcurrentHashMap<>();
        this.totalMigrations = new AtomicLong(0);
        
        logger.info("🚀 Storage tiering manager initialized with policy: auto-tiering={}", 
                   policy.isEnableAutoTiering());
    }
    
    /**
     * Analyze block and determine optimal storage tier
     */
    public StorageTier analyzeStorageTier(Block block) {
        Long blockNumber = block.getBlockNumber();
        AccessInfo info = accessTracking.get(blockNumber);
        
        if (info == null) {
            // New block - determine initial tier based on size
            long dataSize = block.getData() != null ? block.getData().length() : 0;
            info = new AccessInfo(dataSize);
            
            if (dataSize > policy.getSizeThresholdBytes()) {
                info.currentTier = StorageTier.WARM;
                logger.debug("📦 Block #{} exceeds size threshold, starting in WARM tier", blockNumber);
            }
            
            accessTracking.put(blockNumber, info);
        }
        
        // Calculate age and time since last access
        Duration age = Duration.between(block.getTimestamp(), LocalDateTime.now());
        Duration timeSinceLastAccess = Duration.between(info.lastAccessed, LocalDateTime.now());
        
        // Determine recommended tier based on age and access patterns
        StorageTier recommendedTier;
        if (age.compareTo(policy.getHotThreshold()) < 0 && 
            info.accessCount >= policy.getAccessCountThreshold() &&
            timeSinceLastAccess.compareTo(Duration.ofDays(1)) < 0) { // Recently accessed
            recommendedTier = StorageTier.HOT;
        } else if (age.compareTo(policy.getWarmThreshold()) < 0) {
            recommendedTier = StorageTier.WARM;
        } else if (age.compareTo(policy.getColdThreshold()) < 0) {
            recommendedTier = StorageTier.COLD;
        } else {
            recommendedTier = StorageTier.ARCHIVE;
        }
        
        return recommendedTier;
    }
    
    /**
     * Record data access for tiering decisions
     */
    public void recordAccess(Long blockNumber) {
        AccessInfo info = accessTracking.get(blockNumber);
        if (info != null) {
            info.recordAccess();
            logger.debug("📊 Access recorded for block #{} (total: {})", 
                        blockNumber, info.accessCount);
        }
    }
    
    /**
     * Migrate block to recommended tier
     */
    public TieringResult migrateToTier(Block block, StorageTier targetTier) {
        Long blockNumber = block.getBlockNumber();
        AccessInfo info = accessTracking.get(blockNumber);
        
        if (info == null) {
            info = new AccessInfo(block.getData() != null ? block.getData().length() : 0);
            accessTracking.put(blockNumber, info);
        }
        
        StorageTier currentTier = info.currentTier;
        
        if (currentTier == targetTier) {
            return new TieringResult(false, currentTier, targetTier, 
                "Block already in target tier");
        }
        
        try {
            boolean success = false;
            String message = "";
            
            // Perform migration based on tiers
            if (targetTier.getLevel() > currentTier.getLevel()) {
                // Moving to colder storage
                success = migrateToColder(block, currentTier, targetTier);
                message = "Migrated to colder storage";
            } else {
                // Moving to hotter storage
                success = migrateToHotter(block, currentTier, targetTier);
                message = "Migrated to hotter storage";
            }
            
            if (success) {
                info.currentTier = targetTier;
                totalMigrations.incrementAndGet();
                logger.info("✅ Block #{} migrated from {} to {}", 
                           blockNumber, currentTier.getDisplayName(), 
                           targetTier.getDisplayName());
            }
            
            return new TieringResult(success, currentTier, targetTier, message);
            
        } catch (Exception e) {
            logger.error("❌ Failed to migrate block #{} to {}", blockNumber, targetTier, e);
            return new TieringResult(false, currentTier, targetTier, 
                "Migration failed: " + e.getMessage());
        }
    }
    
    /**
     * Perform automatic tiering for all blocks
     */
    public TieringReport performAutoTiering(List<Block> blocks) {
        if (!policy.isEnableAutoTiering()) {
            return new TieringReport(0, 0, "Auto-tiering is disabled");
        }
        
        logger.info("🔄 Starting automatic tiering analysis for {} blocks", blocks.size());
        
        int analyzed = 0;
        int migrated = 0;
        List<TieringResult> results = new ArrayList<>();
        
        for (Block block : blocks) {
            analyzed++;
            
            StorageTier currentTier = getCurrentTier(block.getBlockNumber());
            StorageTier recommendedTier = analyzeStorageTier(block);
            
            if (currentTier != recommendedTier) {
                TieringResult result = migrateToTier(block, recommendedTier);
                if (result.isSuccess()) {
                    migrated++;
                }
                results.add(result);
            }
        }
        
        logger.info("✅ Auto-tiering completed: {}/{} blocks migrated", migrated, analyzed);
        
        return new TieringReport(analyzed, migrated, results);
    }
    
    /**
     * Get current storage statistics
     */
    public StorageStatistics getStatistics() {
        Map<StorageTier, Integer> tierCounts = new EnumMap<>(StorageTier.class);
        Map<StorageTier, Long> tierSizes = new EnumMap<>(StorageTier.class);
        long totalSize = 0;
        long compressedSize = 0;
        
        for (AccessInfo info : accessTracking.values()) {
            StorageTier tier = info.currentTier;
            tierCounts.merge(tier, 1, Integer::sum);
            tierSizes.merge(tier, info.dataSize, Long::sum);
            totalSize += info.dataSize;
            
            // Estimate compressed size for cold/archive tiers
            if (tier == StorageTier.COLD || tier == StorageTier.ARCHIVE) {
                // Validate before multiplication to prevent overflow
                if (info.dataSize > Long.MAX_VALUE / 2) {
                    throw new IllegalStateException(
                        "Data size too large for compression calculation: " + info.dataSize + " bytes. " +
                        "Maximum supported size for compression statistics: " + (Long.MAX_VALUE / 2) + " bytes " +
                        "(approximately 4.6 exabytes). Consider splitting the data or reviewing storage strategy."
                    );
                }
                compressedSize += (long)(info.dataSize * 0.4); // Assume 60% compression
            } else {
                compressedSize += info.dataSize;
            }
        }
        
        return new StorageStatistics(tierCounts, tierSizes, totalSize, 
                                   compressedSize, totalMigrations.intValue());
    }
    
    /**
     * Get storage recommendations for optimization
     */
    public List<String> getOptimizationRecommendations() {
        List<String> recommendations = new ArrayList<>();
        StorageStatistics stats = getStatistics();
        
        // Check hot tier size
        long hotSize = stats.getTierSizes().getOrDefault(StorageTier.HOT, 0L);
        if (hotSize > 100 * 1024 * 1024) { // > 100MB
            recommendations.add("⚠️ Hot tier is large (>100MB). Consider more aggressive tiering.");
        }
        
        // Check compression ratio
        if (stats.getCompressionRatio() < 40) {
            recommendations.add("💡 Low compression ratio. Consider different compression algorithms.");
        }
        
        // Check access patterns
        long infrequentlyAccessed = accessTracking.values().stream()
            .filter(info -> info.accessCount < 3 && info.currentTier == StorageTier.HOT)
            .count();
        
        if (infrequentlyAccessed > 10) {
            recommendations.add("🔄 " + infrequentlyAccessed + 
                " blocks in hot tier with low access. Run auto-tiering.");
        }
        
        return recommendations;
    }
    
    // Helper methods
    
    private StorageTier getCurrentTier(Long blockNumber) {
        AccessInfo info = accessTracking.get(blockNumber);
        return info != null ? info.currentTier : StorageTier.HOT;
    }
    
    private boolean migrateToColder(Block block, StorageTier from, StorageTier to) {
        // Implementation would move data to off-chain storage
        logger.debug("🔄 Migrating block #{} from {} to {}", 
                    block.getBlockNumber(), from, to);
        
        if (to == StorageTier.COLD || to == StorageTier.ARCHIVE) {
            // Use off-chain storage service for cold data
            String dataKey = "block_" + block.getBlockNumber();
            logger.debug("💾 Storing data in off-chain storage with key: {}", dataKey);
            
            // Check if off-chain storage is available
            if (offChainStorage != null) {
                String dataToStore = block.getData();
                
                // Compress data if policy requires it
                if (policy.isCompressOnCold()) {
                    logger.debug("🗜️ Compressing data for cold storage");
                    // Basic compression implementation for tiering
                    dataToStore = compressData(dataToStore);
                }
                
                // Store in off-chain storage
                try {
                    // Use the off-chain storage service to store the data
                    OffChainData offChainData = storeInOffChain(dataKey, dataToStore);
                    if (offChainData != null) {
                        // Update access info with off-chain reference
                        AccessInfo info = accessTracking.get(block.getBlockNumber());
                        if (info != null) {
                            info.offChainReference = offChainData;
                        }
                        logger.debug("📤 Data stored in off-chain service for block #{}", block.getBlockNumber());
                    } else {
                        logger.warn("⚠️ Off-chain storage returned null for block #{}", block.getBlockNumber());
                        return false;
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ Failed to store data in off-chain storage: {}", e.getMessage());
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private boolean migrateToHotter(Block block, StorageTier from, StorageTier to) {
        // Implementation would move data back to on-chain or faster storage
        logger.debug("🔄 Migrating block #{} from {} to {}", 
                    block.getBlockNumber(), from, to);
        
        if (from == StorageTier.COLD || from == StorageTier.ARCHIVE) {
            // Retrieve data from off-chain storage
            String dataKey = "block_" + block.getBlockNumber();
            logger.debug("📦 Retrieving data from off-chain storage with key: {}", dataKey);
            
            // Check if off-chain storage is available
            if (offChainStorage != null) {
                try {
                    // Get the off-chain reference from access tracking
                    AccessInfo info = accessTracking.get(block.getBlockNumber());
                    if (info != null && info.offChainReference != null) {
                        // Retrieve data from off-chain storage service
                        String retrievedData = retrieveFromOffChain(info.offChainReference);
                        if (retrievedData != null) {
                            logger.debug("📥 Data retrieved from off-chain service for block #{}", block.getBlockNumber());
                            
                            // Decompress data if it was compressed
                            String finalData;
                            try {
                                finalData = decompressData(retrievedData);
                                logger.debug("📂 Data decompressed from cold storage ({} -> {} chars)", 
                                           retrievedData.length(), finalData.length());
                            } catch (Exception e) {
                                logger.debug("📂 Using data as-is (not compressed or decompression failed)");
                                finalData = retrievedData;
                            }
                            
                            // Restore block with decompressed data
                            logger.debug("✅ Ready to restore block #{} with {} chars of data", 
                                       block.getBlockNumber(), finalData.length());
                        } else {
                            logger.warn("⚠️ No data found in off-chain storage for block #{}", block.getBlockNumber());
                            return false;
                        }
                    } else {
                        logger.warn("⚠️ No off-chain reference found for block #{}", block.getBlockNumber());
                        return false;
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ Failed to retrieve data from off-chain storage: {}", e.getMessage());
                    return false;
                }
            }
        }
        
        return true;
    }
    
    // Helper methods for off-chain storage operations
    
    private OffChainData storeInOffChain(String key, String data) {
        try {
            // Use the real off-chain storage service
            byte[] dataBytes = data.getBytes();
            String contentType = "application/blockchain-tier";
            
            // Store using off-chain service (no encryption for tiering data)
            OffChainData offChainData = offChainStorage.storeData(dataBytes, null, null, null, contentType);
            
            logger.debug("🗄️ Stored {} bytes for key: {}", data.length(), key);
            return offChainData;
        } catch (Exception e) {
            logger.error("❌ Failed to store data in off-chain storage", e);
            return null;
        }
    }
    
    private String retrieveFromOffChain(OffChainData offChainData) {
        try {
            // Use the real off-chain storage service to retrieve data
            byte[] retrievedData = offChainStorage.retrieveData(offChainData, null);
            
            if (retrievedData != null) {
                logger.debug("🔍 Retrieved {} bytes from off-chain storage", retrievedData.length);
                return new String(retrievedData);
            } else {
                logger.debug("🔍 No data found in off-chain storage");
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ Failed to retrieve data from off-chain storage", e);
            return null;
        }
    }
    
    private String compressData(String data) {
        try {
            // Use GZIP compression for data tiering
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
            gzipOut.write(data.getBytes(StandardCharsets.UTF_8));
            gzipOut.close();
            
            // Convert to Base64 for string storage
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.warn("⚠️ Compression failed, storing uncompressed: {}", e.getMessage());
            return data;
        }
    }
    
    private String decompressData(String compressedData) {
        try {
            // Decode from Base64 and decompress with GZIP
            byte[] compressedBytes = Base64.getDecoder().decode(compressedData);
            ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
            GZIPInputStream gzipIn = new GZIPInputStream(bais);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            gzipIn.close();
            
            return baos.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("⚠️ Decompression failed, returning as-is: {}", e.getMessage());
            return compressedData;
        }
    }
    
    // Result classes
    
    public static class TieringResult {
        private final boolean success;
        private final StorageTier fromTier;
        private final StorageTier toTier;
        private final String message;
        
        public TieringResult(boolean success, StorageTier fromTier, 
                           StorageTier toTier, String message) {
            this.success = success;
            this.fromTier = fromTier;
            this.toTier = toTier;
            this.message = message;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public StorageTier getFromTier() { return fromTier; }
        public StorageTier getToTier() { return toTier; }
        public String getMessage() { return message; }
    }
    
    public static class TieringReport {
        private final int blocksAnalyzed;
        private final int blocksMigrated;
        private final List<TieringResult> results;
        private final String summary;
        
        public TieringReport(int blocksAnalyzed, int blocksMigrated, String summary) {
            this(blocksAnalyzed, blocksMigrated, new ArrayList<>(), summary);
        }
        
        public TieringReport(int blocksAnalyzed, int blocksMigrated, 
                           List<TieringResult> results) {
            this(blocksAnalyzed, blocksMigrated, results, 
                 String.format("Analyzed %d blocks, migrated %d", 
                              blocksAnalyzed, blocksMigrated));
        }
        
        private TieringReport(int blocksAnalyzed, int blocksMigrated,
                            List<TieringResult> results, String summary) {
            this.blocksAnalyzed = blocksAnalyzed;
            this.blocksMigrated = blocksMigrated;
            this.results = results;
            this.summary = summary;
        }
        
        // Getters
        public int getBlocksAnalyzed() { return blocksAnalyzed; }
        public int getBlocksMigrated() { return blocksMigrated; }
        public List<TieringResult> getResults() { return Collections.unmodifiableList(results); }
        public String getSummary() { return summary; }
    }
}