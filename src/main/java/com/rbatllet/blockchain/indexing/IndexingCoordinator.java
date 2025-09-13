package com.rbatllet.blockchain.indexing;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🎯 IndexingCoordinator - Elegant solution to prevent infinite indexing loops
 *
 * This coordinator manages all indexing operations to prevent race conditions
 * and infinite loops between different indexing systems (metadata, search framework, etc.)
 *
 * Key features:
 * - Single point of control for all indexing
 * - Prevents duplicate indexing operations
 * - Graceful cancellation and shutdown
 * - Test-friendly with controllable indexing
 */
public class IndexingCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(
        IndexingCoordinator.class
    );

    // Singleton pattern for global coordination
    private static volatile IndexingCoordinator instance;
    private static final Object instanceLock = new Object();

    // Coordination locks
    private final ReentrantReadWriteLock masterLock =
        new ReentrantReadWriteLock(true);
    private final Semaphore indexingSemaphore = new Semaphore(1, true); // Only one major indexing at a time
    public final ConcurrentHashMap<String, AtomicLong> indexingProgress =
        new ConcurrentHashMap<>();
    private final AtomicBoolean testMode = new AtomicBoolean(false);
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    // Indexing strategies
    private final ConcurrentHashMap<
        String,
        Consumer<IndexingRequest>
    > indexers = new ConcurrentHashMap<>();

    // Shutdown coordination improvements
    private final AtomicBoolean gracefulShutdownInProgress = new AtomicBoolean(false);
    private final Object shutdownLock = new Object();

    private IndexingCoordinator() {
        logger.info("🎯 IndexingCoordinator initialized");
    }

    public static IndexingCoordinator getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new IndexingCoordinator();
                }
            }
        }
        return instance;
    }

    /**
     * Enable test mode - disables automatic indexing, allows manual control
     */
    public void enableTestMode() {
        testMode.set(true);
        logger.info(
            "🧪 Test mode enabled - indexing operations will be controlled manually"
        );
    }

    /**
     * Disable test mode - re-enables automatic indexing
     */
    public void disableTestMode() {
        testMode.set(false);
        logger.info("🎯 Test mode disabled - automatic indexing re-enabled");
    }

    /**
     * Register an indexing strategy
     */
    public void registerIndexer(
        String name,
        Consumer<IndexingRequest> indexer
    ) {
        indexers.put(name, indexer);
        indexingProgress.put(name, new AtomicLong(0));
        logger.debug("📝 Registered indexer: {}", name);
    }

    /**
     * IMPROVED: Main indexing coordination method with enhanced shutdown safety
     */
    public CompletableFuture<IndexingResult> coordinateIndexing(
        IndexingRequest request
    ) {
        // IMPROVED: Check both shutdown flags for better coordination
        if (!isSafeToExecute()) {
            String reason = shutdownRequested.get() ? "Shutdown requested" : "Shutdown in progress";
            logger.debug("Rejecting indexing request '{}': {}", request.getOperation(), reason);
            return CompletableFuture.completedFuture(
                IndexingResult.cancelled(reason)
            );
        }

        // In test mode, only execute if explicitly requested
        if (testMode.get() && !request.isForceExecution()) {
            logger.debug(
                "🧪 Skipping indexing in test mode: {}",
                request.getOperation()
            );
            return CompletableFuture.completedFuture(
                IndexingResult.skipped("Test mode")
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Acquire indexing semaphore to prevent concurrent major operations
                if (!indexingSemaphore.tryAcquire()) {
                    if (request.isCanWait()) {
                        indexingSemaphore.acquire();
                    } else {
                        return IndexingResult.failed(
                            "Another indexing operation in progress"
                        );
                    }
                }

                try {
                    masterLock.writeLock().lock();
                    return executeIndexing(request);
                } finally {
                    masterLock.writeLock().unlock();
                    indexingSemaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return IndexingResult.cancelled("Interrupted");
            } catch (Exception e) {
                logger.error(
                    "❌ Error during coordinated indexing: {}",
                    e.getMessage(),
                    e
                );
                return IndexingResult.failed(e.getMessage());
            }
        });
    }

    private IndexingResult executeIndexing(IndexingRequest request) {
        String operation = request.getOperation();
        logger.info("🚀 Starting coordinated indexing: {}", operation);

        long startTime = System.currentTimeMillis();

        try {
            // IMPROVED: Double-check shutdown state before proceeding
            if (!isSafeToExecute()) {
                return IndexingResult.cancelled("Shutdown requested during indexing setup");
            }
            // Check if this operation is already completed recently
            AtomicLong lastExecution = indexingProgress.get(operation);
            if (lastExecution != null && !request.isForceRebuild()) {
                long timeSinceLastExecution =
                    System.currentTimeMillis() - lastExecution.get();
                if (timeSinceLastExecution < request.getMinIntervalMs()) {
                    logger.debug(
                        "⏭️ Skipping {} - executed {}ms ago",
                        operation,
                        timeSinceLastExecution
                    );
                    return IndexingResult.skipped("Recently executed");
                }
            }

            // Execute the indexing operation
            Consumer<IndexingRequest> indexer = indexers.get(operation);
            if (indexer != null) {
                // IMPROVED: Check shutdown state before executing indexer
                if (!isSafeToExecute()) {
                    return IndexingResult.cancelled("Shutdown requested before indexer execution");
                }
                
                indexer.accept(request);

                // IMPROVED: Check shutdown state after indexer execution
                if (!isSafeToExecute()) {
                    logger.warn("Shutdown occurred during indexing operation: {}", operation);
                    return IndexingResult.cancelled("Shutdown occurred during execution");
                }

                // Update progress tracking
                if (lastExecution != null) {
                    lastExecution.set(System.currentTimeMillis());
                }

                long duration = System.currentTimeMillis() - startTime;
                logger.info(
                    "✅ Completed coordinated indexing: {} ({}ms)",
                    operation,
                    duration
                );

                return IndexingResult.success(duration);
            } else {
                return IndexingResult.failed(
                    "Unknown indexing operation: " + operation
                );
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error(
                "❌ Failed coordinated indexing: {} ({}ms) - {}",
                operation,
                duration,
                e.getMessage()
            );
            return IndexingResult.failed(e.getMessage());
        }
    }

    /**
     * IMPROVED: Graceful shutdown with proper coordination
     * Fixes the "Shutdown requested" errors found in test logs
     */
    public void shutdown() {
        synchronized (shutdownLock) {
            if (!shutdownRequested.compareAndSet(false, true)) {
                logger.debug("Shutdown already requested");
                return;
            }
            
            if (!gracefulShutdownInProgress.compareAndSet(false, true)) {
                logger.debug("Graceful shutdown already in progress");
                return;
            }
            
            logger.info("🛑 IndexingCoordinator graceful shutdown initiated");
            
            try {
                // Phase 1: Stop accepting new operations (done by setting shutdownRequested)
                
                // Phase 2: Wait for current indexing operations to complete
                long shutdownStart = System.currentTimeMillis();
                long timeoutMs = 5000; // 5 second timeout
                
                while (indexingSemaphore.availablePermits() == 0) {
                    long elapsed = System.currentTimeMillis() - shutdownStart;
                    if (elapsed > timeoutMs) {
                        logger.warn("Graceful shutdown timeout reached while waiting for indexing operations");
                        break;
                    }
                    
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                // Phase 3: Acquire master lock to ensure clean shutdown
                try {
                    masterLock.writeLock().lock();
                    
                    // Clear any remaining indexers
                    int indexerCount = indexers.size();
                    indexers.clear();
                    indexingProgress.clear();
                    
                    long shutdownDuration = System.currentTimeMillis() - shutdownStart;
                    logger.info("🛑 IndexingCoordinator shutdown completed in {}ms (cleared {} indexers)", 
                               shutdownDuration, indexerCount);
                               
                } finally {
                    masterLock.writeLock().unlock();
                }
                
            } finally {
                gracefulShutdownInProgress.set(false);
            }
        }
    }

    /**
     * Force shutdown - immediately cancels all operations
     */
    public void forceShutdown() {
        shutdownRequested.set(true);
        logger.info("🛑 IndexingCoordinator force shutdown requested");
    }

    /**
     * Reset the coordinator state - for testing purposes
     */
    public void reset() {
        shutdownRequested.set(false);
        testMode.set(true);  // Enable test mode to skip interval checks
        indexers.clear();
        indexingProgress.clear();
        logger.info("🔄 IndexingCoordinator reset completed in test mode");
    }

    /**
     * Check if currently indexing
     */
    public boolean isIndexing() {
        return indexingSemaphore.availablePermits() == 0;
    }

    /**
     * Get current indexing progress for operation
     */
    public long getLastExecutionTime(String operation) {
        AtomicLong lastExecution = indexingProgress.get(operation);
        return lastExecution != null ? lastExecution.get() : 0L;
    }
    
    /**
     * Check if in test mode
     */
    public boolean isTestMode() {
        return testMode.get();
    }
    
    /**
     * IMPROVED: Check if shutdown was requested
     * Useful for external components to coordinate their own shutdown
     */
    public boolean isShutdownRequested() {
        return shutdownRequested.get();
    }
    
    /**
     * IMPROVED: Check if graceful shutdown is in progress
     * Prevents race conditions during shutdown
     */
    public boolean isShutdownInProgress() {
        return gracefulShutdownInProgress.get();
    }
    
    /**
     * IMPROVED: Safe execution check for operations
     * Returns true if it's safe to start new operations
     */
    public boolean isSafeToExecute() {
        return !shutdownRequested.get() && !gracefulShutdownInProgress.get();
    }

    // Inner classes for request/response

    public static class IndexingRequest {

        private final String operation;
        private final List<Block> blocks;
        private final Blockchain blockchain;
        private final boolean forceRebuild;
        private final boolean forceExecution;
        private final boolean canWait;
        private final long minIntervalMs;

        public IndexingRequest(String operation) {
            this(operation, null, null, false, false, true, 1000L);
        }

        public IndexingRequest(
            String operation,
            List<Block> blocks,
            Blockchain blockchain,
            boolean forceRebuild,
            boolean forceExecution,
            boolean canWait,
            long minIntervalMs
        ) {
            this.operation = operation;
            this.blocks = blocks;
            this.blockchain = blockchain;
            this.forceRebuild = forceRebuild;
            this.forceExecution = forceExecution;
            this.canWait = canWait;
            this.minIntervalMs = minIntervalMs;
        }

        // Getters
        public String getOperation() {
            return operation;
        }

        public List<Block> getBlocks() {
            return blocks;
        }

        public Blockchain getBlockchain() {
            return blockchain;
        }

        public boolean isForceRebuild() {
            return forceRebuild;
        }

        public boolean isForceExecution() {
            return forceExecution;
        }

        public boolean isCanWait() {
            return canWait;
        }

        public long getMinIntervalMs() {
            return minIntervalMs;
        }

        // Builder pattern for easy construction
        public static class Builder {

            private String operation;
            private List<Block> blocks;
            private Blockchain blockchain;
            private boolean forceRebuild = false;
            private boolean forceExecution = false;
            private boolean canWait = true;
            private long minIntervalMs = 1000L;

            public Builder operation(String operation) {
                this.operation = operation;
                return this;
            }

            public Builder blocks(List<Block> blocks) {
                this.blocks = blocks;
                return this;
            }

            public Builder blockchain(Blockchain blockchain) {
                this.blockchain = blockchain;
                return this;
            }

            public Builder forceRebuild() {
                this.forceRebuild = true;
                return this;
            }

            public Builder forceExecution() {
                this.forceExecution = true;
                return this;
            }

            public Builder cannotWait() {
                this.canWait = false;
                return this;
            }

            public Builder minInterval(long ms) {
                this.minIntervalMs = ms;
                return this;
            }

            public IndexingRequest build() {
                if (operation == null) {
                    throw new IllegalArgumentException("Operation is required");
                }
                return new IndexingRequest(
                    operation,
                    blocks,
                    blockchain,
                    forceRebuild,
                    forceExecution,
                    canWait,
                    minIntervalMs
                );
            }
        }
    }

    public static class IndexingResult {

        private final boolean success;
        private final String message;
        private final long durationMs;
        private final String status;

        private IndexingResult(
            boolean success,
            String message,
            long durationMs,
            String status
        ) {
            this.success = success;
            this.message = message;
            this.durationMs = durationMs;
            this.status = status;
        }

        public static IndexingResult success(long durationMs) {
            return new IndexingResult(true, "Success", durationMs, "COMPLETED");
        }

        public static IndexingResult failed(String message) {
            return new IndexingResult(false, message, 0L, "FAILED");
        }

        public static IndexingResult cancelled(String reason) {
            return new IndexingResult(false, reason, 0L, "CANCELLED");
        }

        public static IndexingResult skipped(String reason) {
            return new IndexingResult(true, reason, 0L, "SKIPPED");
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public String getStatus() {
            return status;
        }
    }
}
