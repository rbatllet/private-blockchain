package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central memory management service to prevent memory leaks in long-running operations
 * This service provides automated cleanup, monitoring, and memory management utilities
 */
public class MemoryManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryManagementService.class);
    
    private static volatile ScheduledExecutorService cleanupScheduler;
    private static final AtomicBoolean isStarted = new AtomicBoolean(false);
    private static final AtomicLong lastCleanupTime = new AtomicLong(System.currentTimeMillis());
    private static final AtomicLong lastForceGCTime = new AtomicLong(System.currentTimeMillis());
    
    // Configuration
    private static final long CLEANUP_INTERVAL_MINUTES = 60; // 1 hour
    private static final long FORCE_GC_INTERVAL_MINUTES = 120; // 2 hours
    private static final long MEMORY_THRESHOLD_PERCENTAGE = 80; // 80% memory usage
    
    /**
     * Start the memory management service
     */
    public static synchronized void start() {
        if (isStarted.compareAndSet(false, true)) {
            logger.info("🚀 Starting Memory Management Service");
            
            // Create new scheduler if needed
            if (cleanupScheduler == null || cleanupScheduler.isShutdown()) {
                cleanupScheduler = Executors.newScheduledThreadPool(1);
            }
            
            // Schedule regular cleanup
            cleanupScheduler.scheduleAtFixedRate(
                MemoryManagementService::performAutomaticCleanup,
                CLEANUP_INTERVAL_MINUTES,
                CLEANUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            );
            
            // Schedule memory monitoring
            cleanupScheduler.scheduleAtFixedRate(
                MemoryManagementService::monitorMemoryUsage,
                30, // Start after 30 seconds
                30, // Every 30 seconds
                TimeUnit.SECONDS
            );
            
            // Schedule periodic forced garbage collection
            cleanupScheduler.scheduleAtFixedRate(
                MemoryManagementService::performPeriodicGC,
                FORCE_GC_INTERVAL_MINUTES,
                FORCE_GC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            );
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(MemoryManagementService::shutdown));
            
            logger.info("✅ Memory Management Service started successfully");
        }
    }
    
    /**
     * Stop the memory management service
     */
    public static synchronized void shutdown() {
        if (isStarted.compareAndSet(true, false)) {
            logger.info("⏹️ Shutting down Memory Management Service");
            
            // Perform final cleanup
            performFinalCleanup();
            
            // Shutdown scheduler
            if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
                cleanupScheduler.shutdown();
                try {
                    if (!cleanupScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                        cleanupScheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    cleanupScheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            logger.info("✅ Memory Management Service shutdown completed");
        }
    }
    
    /**
     * Perform automatic cleanup of all managed resources
     */
    private static void performAutomaticCleanup() {
        try {
            logger.debug("🧹 Starting automatic memory cleanup");
            
            long startTime = System.currentTimeMillis();
            
            // Cleanup CryptoUtil key store
            CryptoUtil.cleanupExpiredKeys();
            
            // Cleanup JPAUtil ThreadLocal variables
            JPAUtil.cleanupThreadLocals();
            
            // Force garbage collection if memory usage is high
            if (isMemoryUsageHigh()) {
                logger.info("⚠️ High memory usage detected, forcing garbage collection");
                System.gc();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            lastCleanupTime.set(System.currentTimeMillis());
            
            logger.debug("✅ Automatic memory cleanup completed in {}ms", duration);
            
        } catch (Exception e) {
            logger.error("❌ Error during automatic memory cleanup", e);
        }
    }
    
    /**
     * Monitor memory usage and log warnings
     */
    private static void monitorMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double usagePercentage = (double) usedMemory / maxMemory * 100;
            
            if (usagePercentage > MEMORY_THRESHOLD_PERCENTAGE) {
                logger.warn("⚠️ High memory usage detected: {:.1f}% ({} MB / {} MB)", 
                           usagePercentage, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
                
                // Log key store statistics
                var keyStoreStats = CryptoUtil.getKeyStoreStats();
                logger.info("📊 Key store stats: {}", keyStoreStats);
            } else {
                logger.debug("📊 Memory usage: {:.1f}% ({} MB / {} MB)", 
                           usagePercentage, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error during memory monitoring", e);
        }
    }
    
    /**
     * Perform periodic forced garbage collection
     */
    private static void performPeriodicGC() {
        try {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastGC = currentTime - lastForceGCTime.get();
            
            // Only perform GC if enough time has passed and memory usage is above threshold
            if (timeSinceLastGC >= FORCE_GC_INTERVAL_MINUTES * 60 * 1000 && isMemoryUsageHigh()) {
                logger.info("🗑️ Performing periodic garbage collection due to high memory usage");
                
                long beforeGC = getUsedMemory();
                System.gc();
                long afterGC = getUsedMemory();
                
                long freedMemory = beforeGC - afterGC;
                lastForceGCTime.set(currentTime);
                
                logger.info("✅ Periodic GC completed - freed {} MB", freedMemory / 1024 / 1024);
            }
        } catch (Exception e) {
            logger.error("❌ Error during periodic garbage collection", e);
        }
    }
    
    /**
     * Get current used memory in bytes
     */
    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Check if memory usage is above the threshold
     */
    private static boolean isMemoryUsageHigh() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = getUsedMemory();
        
        double usagePercentage = (double) usedMemory / maxMemory * 100;
        return usagePercentage > MEMORY_THRESHOLD_PERCENTAGE;
    }
    
    /**
     * Force immediate cleanup of all managed resources
     */
    public static void forceCleanup() {
        logger.info("🧹 Forcing immediate memory cleanup");
        
        long startTime = System.currentTimeMillis();
        
        // Force cleanup of all components
        CryptoUtil.forceCleanupKeyStore();
        JPAUtil.forceCleanupAllThreadLocals();
        
        // Force garbage collection
        System.gc();
        
        long duration = System.currentTimeMillis() - startTime;
        lastCleanupTime.set(System.currentTimeMillis());
        
        logger.info("✅ Force cleanup completed in {}ms", duration);
    }
    
    /**
     * Perform final cleanup during shutdown
     */
    private static void performFinalCleanup() {
        logger.info("🧹 Performing final memory cleanup");
        
        try {
            // Final cleanup of all resources
            CryptoUtil.forceCleanupKeyStore();
            JPAUtil.forceCleanupAllThreadLocals();
            
            // Final garbage collection
            System.gc();
            
            logger.info("✅ Final memory cleanup completed");
        } catch (Exception e) {
            logger.error("❌ Error during final cleanup", e);
        }
    }
    
    /**
     * Get memory management statistics
     */
    public static MemoryStats getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return new MemoryStats(
            usedMemory,
            totalMemory,
            maxMemory,
            freeMemory,
            (double) usedMemory / maxMemory * 100,
            lastCleanupTime.get(),
            lastForceGCTime.get(),
            isStarted.get()
        );
    }
    
    /**
     * Memory statistics data class
     */
    public static class MemoryStats {
        private final long usedMemory;
        private final long totalMemory;
        private final long maxMemory;
        private final long freeMemory;
        private final double usagePercentage;
        private final long lastCleanupTime;
        private final long lastForceGCTime;
        private final boolean serviceRunning;
        
        public MemoryStats(long usedMemory, long totalMemory, long maxMemory, long freeMemory,
                          double usagePercentage, long lastCleanupTime, long lastForceGCTime, boolean serviceRunning) {
            this.usedMemory = usedMemory;
            this.totalMemory = totalMemory;
            this.maxMemory = maxMemory;
            this.freeMemory = freeMemory;
            this.usagePercentage = usagePercentage;
            this.lastCleanupTime = lastCleanupTime;
            this.lastForceGCTime = lastForceGCTime;
            this.serviceRunning = serviceRunning;
        }
        
        // Getters
        public long getUsedMemory() { return usedMemory; }
        public long getTotalMemory() { return totalMemory; }
        public long getMaxMemory() { return maxMemory; }
        public long getFreeMemory() { return freeMemory; }
        public double getUsagePercentage() { return usagePercentage; }
        public long getLastCleanupTime() { return lastCleanupTime; }
        public long getLastForceGCTime() { return lastForceGCTime; }
        public boolean isServiceRunning() { return serviceRunning; }
        
        @Override
        public String toString() {
            return String.format("MemoryStats{used=%dMB, total=%dMB, max=%dMB, usage=%.1f%%, serviceRunning=%s}",
                               usedMemory / 1024 / 1024, totalMemory / 1024 / 1024, 
                               maxMemory / 1024 / 1024, usagePercentage, serviceRunning);
        }
    }
}