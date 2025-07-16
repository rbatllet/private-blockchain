package com.rbatllet.blockchain.dao;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.BlockSequence;
import com.rbatllet.blockchain.util.JPAUtil;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;
import com.rbatllet.blockchain.search.SearchLevel;
import com.rbatllet.blockchain.logging.LoggingManager;
import com.rbatllet.blockchain.logging.OperationLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe DAO for Block operations
 * FIXED: Added thread-safety and support for global transactions
 * ENHANCED: Added support for encrypted block data using AES-256-GCM
 */
public class BlockDAO {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockDAO.class);
    
    // Read-Write lock for thread safety on read operations
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Global lock for atomic block number generation - prevents race conditions
    private static final Object GLOBAL_BLOCK_NUMBER_LOCK = new Object();
    
    /**
     * Save a new block to the database
     * Uses global transaction if available, otherwise creates its own
     */
    public void saveBlock(Block block) {
        LoggingManager.logBlockchainOperation(
            "BLOCK_SAVE",
            "save_block",
            block.getBlockNumber(),
            block.getData() != null ? block.getData().length() : 0,
            () -> {
                lock.writeLock().lock();
                try {
                    if (JPAUtil.hasActiveTransaction()) {
                        // Use existing global transaction
                        EntityManager em = JPAUtil.getEntityManager();
                        em.persist(block);
                    } else {
                        // Create own transaction
                        EntityManager em = JPAUtil.getEntityManager();
                        EntityTransaction transaction = null;
                        
                        try {
                            transaction = em.getTransaction();
                            transaction.begin();
                            
                            em.persist(block);
                            
                            transaction.commit();
                        } catch (Exception e) {
                            if (transaction != null && transaction.isActive()) {
                                transaction.rollback();
                            }
                            throw new RuntimeException("Error saving block", e);
                        } finally {
                            if (!JPAUtil.hasActiveTransaction()) {
                                em.close();
                            }
                        }
                    }
                } finally {
                    lock.writeLock().unlock();
                }
                return "Block saved successfully";
            }
        );
    }
    
    /**
     * Get the next block number atomically using BlockSequence with pessimistic locking
     * FIXED: Corrected logic to prevent race conditions and ensure proper sequence
     */
    public Long getNextBlockNumberAtomic() {
        // Global synchronization ensures only one thread can get a number at a time
        synchronized (GLOBAL_BLOCK_NUMBER_LOCK) {
            long startTime = System.nanoTime();
            String threadName = Thread.currentThread().getName();
            
            EntityManager em = JPAUtil.getEntityManager();
            EntityTransaction transaction = null;
            boolean shouldManageTransaction = !JPAUtil.hasActiveTransaction();
            
            try {
                logger.debug("🔍 [{}] Starting getNextBlockNumberAtomic() - shouldManageTransaction: {}", threadName, shouldManageTransaction);
                
                if (shouldManageTransaction) {
                    transaction = em.getTransaction();
                    transaction.begin();
                    logger.debug("🔍 [{}] Started new transaction", threadName);
                }
                
                // Use BlockSequence with pessimistic write lock for true atomicity
                logger.debug("🔍 [{}] Acquiring PESSIMISTIC_WRITE lock on BlockSequence", threadName);
                BlockSequence sequence = em.find(BlockSequence.class, "block_number", LockModeType.PESSIMISTIC_WRITE);
                
                if (sequence == null) {
                    logger.debug("🔍 [{}] BlockSequence not found, initializing...", threadName);
                    // Initialize sequence correctly - should start at 1 for first non-genesis block
                    TypedQuery<Long> maxQuery = em.createQuery("SELECT MAX(b.blockNumber) FROM Block b", Long.class);
                    Long maxBlockNumber = maxQuery.getSingleResult();
                    
                    Long nextValue;
                    if (maxBlockNumber == null || maxBlockNumber.equals(0L)) {
                        // Only genesis block (0) exists or no blocks exist, next should be 1
                        nextValue = 1L;
                    } else {
                        // Regular case: next block number after the highest existing one
                        nextValue = maxBlockNumber + 1L;
                    }
                    
                    logger.debug("🔍 [{}] Creating new sequence with nextValue: {}", threadName, (nextValue + 1));
                    sequence = new BlockSequence("block_number", nextValue + 1); // Store next available number
                    em.persist(sequence);
                    em.flush(); // Force immediate persistence
                    
                    if (shouldManageTransaction && transaction != null) {
                        transaction.commit();
                        logger.debug("🔍 [{}] Committed transaction for new sequence", threadName);
                    }
                    
                    long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                    logger.info("✅ [{}] Generated block number: {} (took {}ms)", threadName, nextValue, elapsed);
                    return nextValue; // Return the number we're using for current block
                } else {
                    // FIXED: Correct atomic increment logic
                    Long blockNumberToUse = sequence.getNextValue(); // This is the number we'll use for the current block
                    logger.debug("🔍 [{}] Found existing sequence, using block number: {}", threadName, blockNumberToUse);
                    
                    // Increment the sequence for the next block
                    sequence.setNextValue(blockNumberToUse + 1);
                    em.merge(sequence);
                    em.flush(); // Force immediate persistence
                    
                    logger.debug("🔍 [{}] Updated sequence to nextValue: {}", threadName, (blockNumberToUse + 1));
                    
                    if (shouldManageTransaction && transaction != null) {
                        transaction.commit();
                        logger.debug("🔍 [{}] Committed transaction for existing sequence", threadName);
                    }
                    
                    long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                    logger.info("✅ [{}] Generated block number: {} (took {}ms)", threadName, blockNumberToUse, elapsed);
                    return blockNumberToUse;
                }
                
            } catch (Exception e) {
                logger.error("❌ [{}] ERROR in getNextBlockNumberAtomic: {}", threadName, e.getMessage(), e);
                if (shouldManageTransaction && transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw new RuntimeException("Error getting next block number atomically", e);
            } finally {
                if (shouldManageTransaction) {
                    em.close();
                }
            }
        }
    }

    /**
     * Get the last block with pessimistic locking to prevent race conditions
     * FIXED: Added locking to prevent concurrent access issues
     */
    public Block getLastBlockWithLock() {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b ORDER BY b.blockNumber DESC", Block.class);
                query.setMaxResults(1);
                query.setLockMode(LockModeType.PESSIMISTIC_READ);
                
                List<Block> blocks = query.getResultList();
                return blocks.isEmpty() ? null : blocks.get(0);
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get a block by its number
     */
    public Block getBlockByNumber(Long blockNumber) {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b WHERE b.blockNumber = :blockNumber", Block.class);
                query.setParameter("blockNumber", blockNumber);
                
                List<Block> results = query.getResultList();
                return results.isEmpty() ? null : results.get(0);
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get the last block in the chain (without locking for regular queries)
     */
    public Block getLastBlock() {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b ORDER BY b.blockNumber DESC", Block.class);
                query.setMaxResults(1);
                
                List<Block> blocks = query.getResultList();
                return blocks.isEmpty() ? null : blocks.get(0);
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the last block with forced refresh to see latest committed data
     * CRITICAL: This method ensures we see the most recent data even in high-concurrency scenarios
     * Used to prevent race conditions where reads happen before writes are fully committed
     */
    public Block getLastBlockWithRefresh() {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                // Clear the persistence context to force fresh read from database
                em.clear();
                
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b ORDER BY b.blockNumber DESC", Block.class);
                query.setMaxResults(1);
                
                List<Block> blocks = query.getResultList();
                return blocks.isEmpty() ? null : blocks.get(0);
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get all blocks in the chain ordered by number
     */
    public List<Block> getAllBlocks() {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b LEFT JOIN FETCH b.offChainData ORDER BY b.blockNumber ASC", Block.class);
                return query.getResultList();
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get blocks paginated for better performance
     * @param offset starting position
     * @param limit maximum number of blocks to return
     * @return list of blocks within the specified range
     */
    public List<Block> getBlocksPaginated(int offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        
        return OperationLoggingInterceptor.logDatabaseOperation("SELECT", "blocks_paginated", () -> {
            lock.readLock().lock();
            try {
                EntityManager em = JPAUtil.getEntityManager();
                try {
                    TypedQuery<Block> query = em.createQuery(
                        "SELECT b FROM Block b LEFT JOIN FETCH b.offChainData ORDER BY b.blockNumber ASC", Block.class);
                    query.setFirstResult(offset);
                    query.setMaxResults(limit);
                    return query.getResultList();
                } finally {
                    if (!JPAUtil.hasActiveTransaction()) {
                        em.close();
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
        });
    }
    
    /**
     * Get blocks without off-chain data for lightweight operations
     * @return list of blocks without eager loading off-chain data
     */
    public List<Block> getAllBlocksLightweight() {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b ORDER BY b.blockNumber ASC", Block.class);
                return query.getResultList();
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get total number of blocks in the blockchain
     * @return total block count
     */
    public long getBlockCount() {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(b) FROM Block b", Long.class);
                return query.getSingleResult();
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get blocks within a time range
     */
    public List<Block> getBlocksByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b WHERE b.timestamp BETWEEN :startTime AND :endTime ORDER BY b.blockNumber ASC", 
                    Block.class);
                query.setParameter("startTime", startTime);
                query.setParameter("endTime", endTime);
                return query.getResultList();
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Check if a block with specific number exists
     * FIXED: Added for race condition detection
     */
    public boolean existsBlockWithNumber(Long blockNumber) {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(b) FROM Block b WHERE b.blockNumber = :blockNumber", Long.class);
                query.setParameter("blockNumber", blockNumber);
                return query.getSingleResult() > 0;
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if a block with a specific hash exists
     */
    public boolean existsBlockWithHash(String hash) {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(b) FROM Block b WHERE b.hash = :hash", Long.class);
                query.setParameter("hash", hash);
                return query.getSingleResult() > 0;
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }    
    /**
     * Delete a block by its number
     */
    public boolean deleteBlockByNumber(Long blockNumber) {
        lock.writeLock().lock();
        try {
            if (JPAUtil.hasActiveTransaction()) {
                EntityManager em = JPAUtil.getEntityManager();
                int deletedCount = em.createQuery("DELETE FROM Block b WHERE b.blockNumber = :blockNumber")
                        .setParameter("blockNumber", blockNumber)
                        .executeUpdate();
                return deletedCount > 0;
            } else {
                EntityManager em = JPAUtil.getEntityManager();
                EntityTransaction transaction = null;
                
                try {
                    transaction = em.getTransaction();
                    transaction.begin();
                    
                    int deletedCount = em.createQuery("DELETE FROM Block b WHERE b.blockNumber = :blockNumber")
                            .setParameter("blockNumber", blockNumber)
                            .executeUpdate();
                    
                    transaction.commit();
                    return deletedCount > 0;
                } catch (Exception e) {
                    if (transaction != null && transaction.isActive()) {
                        transaction.rollback();
                    }
                    throw new RuntimeException("Error deleting block with number " + blockNumber, e);
                } finally {
                    em.close();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get all blocks signed by a specific public key
     * Used for impact assessment before key deletion
     */
    public List<Block> getBlocksBySignerPublicKey(String signerPublicKey) {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b WHERE b.signerPublicKey = :signerPublicKey ORDER BY b.blockNumber ASC", 
                    Block.class);
                query.setParameter("signerPublicKey", signerPublicKey);
                return query.getResultList();
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Count blocks signed by a specific public key
     * Optimized version for quick impact check
     */
    public long countBlocksBySignerPublicKey(String signerPublicKey) {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(b) FROM Block b WHERE b.signerPublicKey = :signerPublicKey", 
                    Long.class);
                query.setParameter("signerPublicKey", signerPublicKey);
                return query.getSingleResult();
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get all blocks after a specific block number (for off-chain cleanup before deletion)
     */
    public List<Block> getBlocksAfter(Long blockNumber) {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                List<Block> blocks = em.createQuery("SELECT b FROM Block b LEFT JOIN FETCH b.offChainData WHERE b.blockNumber > :blockNumber ORDER BY b.blockNumber ASC", Block.class)
                        .setParameter("blockNumber", blockNumber)
                        .getResultList();
                return blocks;
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public int deleteBlocksAfter(Long blockNumber) {
        lock.writeLock().lock();
        try {
            if (JPAUtil.hasActiveTransaction()) {
                EntityManager em = JPAUtil.getEntityManager();
                return em.createQuery("DELETE FROM Block b WHERE b.blockNumber > :blockNumber")
                        .setParameter("blockNumber", blockNumber)
                        .executeUpdate();
            } else {
                EntityManager em = JPAUtil.getEntityManager();
                EntityTransaction transaction = null;
                
                try {
                    transaction = em.getTransaction();
                    transaction.begin();
                    
                    int deletedCount = em.createQuery("DELETE FROM Block b WHERE b.blockNumber > :blockNumber")
                            .setParameter("blockNumber", blockNumber)
                            .executeUpdate();
                    
                    transaction.commit();
                    return deletedCount;
                } catch (Exception e) {
                    if (transaction != null && transaction.isActive()) {
                        transaction.rollback();
                    }
                    throw new RuntimeException("Error deleting blocks after " + blockNumber, e);
                } finally {
                    em.close();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Delete all blocks (for import functionality)
     */
    public int deleteAllBlocks() {
        lock.writeLock().lock();
        try {
            if (JPAUtil.hasActiveTransaction()) {
                EntityManager em = JPAUtil.getEntityManager();
                return em.createQuery("DELETE FROM Block b").executeUpdate();
            } else {
                EntityManager em = JPAUtil.getEntityManager();
                EntityTransaction transaction = null;
                
                try {
                    transaction = em.getTransaction();
                    transaction.begin();
                    
                    int deletedCount = em.createQuery("DELETE FROM Block b").executeUpdate();
                    
                    transaction.commit();
                    return deletedCount;
                } catch (Exception e) {
                    if (transaction != null && transaction.isActive()) {
                        transaction.rollback();
                    }
                    throw new RuntimeException("Error deleting all blocks", e);
                } finally {
                    em.close();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Search blocks by content (case-insensitive)
     */
    public List<Block> searchBlocksByContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b WHERE LOWER(b.data) LIKE :content ORDER BY b.blockNumber ASC", 
                    Block.class);
                query.setParameter("content", "%" + content.toLowerCase() + "%");
                return query.getResultList();
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get block by hash
     */
    public Block getBlockByHash(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b WHERE b.hash = :hash", Block.class);
                query.setParameter("hash", hash);
                
                List<Block> results = query.getResultList();
                return results.isEmpty() ? null : results.get(0);
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Synchronize the block sequence with the actual max block number
     * Useful for initialization or after manual database operations
     */
    public void synchronizeBlockSequence() {
        lock.writeLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            EntityTransaction transaction = null;
            boolean shouldManageTransaction = !JPAUtil.hasActiveTransaction();
            
            try {
                if (shouldManageTransaction) {
                    transaction = em.getTransaction();
                    transaction.begin();
                }
                
                // Get actual max block number
                TypedQuery<Long> maxQuery = em.createQuery("SELECT MAX(b.blockNumber) FROM Block b", Long.class);
                Long maxBlockNumber = maxQuery.getSingleResult();
                Long nextValue = (maxBlockNumber != null) ? maxBlockNumber + 1 : 0L;
                
                // Get or create sequence
                BlockSequence sequence = em.find(BlockSequence.class, "block_number", LockModeType.PESSIMISTIC_WRITE);
                if (sequence == null) {
                    sequence = new BlockSequence("block_number", nextValue);
                    em.persist(sequence);
                } else {
                    sequence.setNextValue(nextValue);
                    em.merge(sequence);
                }
                
                if (shouldManageTransaction && transaction != null) {
                    transaction.commit();
                }
                
            } catch (Exception e) {
                if (shouldManageTransaction && transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw new RuntimeException("Error synchronizing block sequence", e);
            } finally {
                if (shouldManageTransaction) {
                    em.close();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // =============== SEARCH FUNCTIONALITY ===============
    
    /**
     * Search blocks by content with different search levels
     */
    public List<Block> searchBlocksByContentWithLevel(String searchTerm, SearchLevel level) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        lock.readLock().lock();
        try {
            String term = "%" + searchTerm.toLowerCase() + "%";
            EntityManager em = JPAUtil.getEntityManager();
            
            try {
                String queryString = buildSearchQuery(level);
                TypedQuery<Block> query = em.createQuery(queryString, Block.class);
                
                // Only set the term parameter if the query actually uses it
                if (level != SearchLevel.INCLUDE_DATA) {
                    query.setParameter("term", term);
                }
                
                List<Block> results = query.getResultList();
                
                // Sort by priority (manual keywords first)
                return results.stream()
                    .sorted(this::compareSearchPriority)
                    .collect(java.util.stream.Collectors.toList());
                    
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Search blocks by category
     */
    public List<Block> searchByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b WHERE UPPER(b.contentCategory) = :category ORDER BY b.blockNumber ASC", 
                    Block.class);
                query.setParameter("category", category.toUpperCase());
                return query.getResultList();
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get all blocks that have off-chain data for exhaustive search
     */
    public List<Block> getAllBlocksWithOffChainData() {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b WHERE b.offChainData IS NOT NULL ORDER BY b.blockNumber ASC", 
                    Block.class);
                return query.getResultList();
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private String buildSearchQuery(SearchLevel level) {
        StringBuilder query = new StringBuilder("SELECT b FROM Block b WHERE ");
        
        switch (level) {
            case FAST_ONLY:
                // Keywords only
                query.append("(LOWER(b.manualKeywords) LIKE :term OR LOWER(b.autoKeywords) LIKE :term OR LOWER(b.searchableContent) LIKE :term)");
                break;
                
            case INCLUDE_DATA:
            case EXHAUSTIVE_OFFCHAIN:
                // Keywords + block data
                query.append("(LOWER(b.manualKeywords) LIKE :term OR LOWER(b.autoKeywords) LIKE :term OR LOWER(b.searchableContent) LIKE :term OR LOWER(b.data) LIKE :term)");
                break;
                
            // Switch handled by the three current search levels above
        }
        
        query.append(" ORDER BY b.blockNumber ASC");
        return query.toString();
    }
    
    private int compareSearchPriority(Block a, Block b) {
        // Priority: manual keywords > auto keywords > data > recency
        // Higher priority blocks appear first in search results
        
        // 1. Prioritize blocks with manual keywords
        boolean aHasManual = a.getManualKeywords() != null && !a.getManualKeywords().trim().isEmpty();
        boolean bHasManual = b.getManualKeywords() != null && !b.getManualKeywords().trim().isEmpty();
        
        if (aHasManual && !bHasManual) return -1;  // a comes first
        if (!aHasManual && bHasManual) return 1;   // b comes first
        
        // 2. If both have manual keywords or both don't, prioritize auto keywords
        boolean aHasAuto = a.getAutoKeywords() != null && !a.getAutoKeywords().trim().isEmpty();
        boolean bHasAuto = b.getAutoKeywords() != null && !b.getAutoKeywords().trim().isEmpty();
        
        if (aHasAuto && !bHasAuto) return -1;
        if (!aHasAuto && bHasAuto) return 1;
        
        // 3. Finally, sort by recency (higher block number = more recent)
        return Long.compare(b.getBlockNumber(), a.getBlockNumber()); // Descending order
    }
    
    /**
     * Save a block with encrypted data using password-based encryption
     * The data field will be encrypted and stored in encryptionMetadata
     * 
     * @param block The block to save
     * @param password The password for encryption
     */
    public void saveBlockWithEncryption(Block block, String password) {
        if (block == null) {
            throw new IllegalArgumentException("Block cannot be null");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        lock.writeLock().lock();
        try {
            // Encrypt the data if it's not already encrypted
            if (!block.isDataEncrypted() && block.getData() != null && !block.getData().trim().isEmpty()) {
                String originalData = block.getData();
                
                // Encrypt data using secure encryption service
                String encryptedData = SecureBlockEncryptionService.encryptToString(originalData, password);
                
                // Store encrypted data in metadata and clear original data
                block.setEncryptionMetadata(encryptedData);
                block.setData("[ENCRYPTED]"); // Placeholder to indicate encryption
                block.setIsEncrypted(true);
            }
            
            // Save the block normally
            saveBlock(block);
            
        } catch (Exception e) {
            throw new RuntimeException("Error saving block with encryption: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Retrieve and decrypt block data using password
     * 
     * @param blockId The block ID to retrieve
     * @param password The password for decryption
     * @return The block with decrypted data in the data field
     */
    public Block getBlockWithDecryption(Long blockId, String password) {
        if (blockId == null) {
            throw new IllegalArgumentException("Block ID cannot be null");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            Block block;
            try {
                block = em.find(Block.class, blockId);
                if (block == null) {
                    return null;
                }
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
            
            // Decrypt data if the block is encrypted
            if (block.isDataEncrypted() && block.getEncryptionMetadata() != null) {
                try {
                    String decryptedData = SecureBlockEncryptionService.decryptFromString(
                        block.getEncryptionMetadata(), password);
                    block.setData(decryptedData);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decrypt block data. Invalid password or corrupted data.", e);
                }
            }
            
            return block;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get all encrypted blocks (returns with encrypted data intact)
     * Use getBlockWithDecryption() to decrypt individual blocks
     */
    public List<Block> getAllEncryptedBlocks() {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Block> query = em.createQuery(
                    "SELECT b FROM Block b WHERE b.isEncrypted = true ORDER BY b.blockNumber ASC", 
                    Block.class);
                return query.getResultList();
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Check if block data can be decrypted with given password
     * 
     * @param blockId The block ID to check
     * @param password The password to test
     * @return true if password can decrypt the block, false otherwise
     */
    public boolean verifyBlockPassword(Long blockId, String password) {
        if (blockId == null || password == null || password.trim().isEmpty()) {
            return false;
        }
        
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            Block block;
            try {
                block = em.find(Block.class, blockId);
                if (block == null || !block.isDataEncrypted()) {
                    return false;
                }
            } finally {
                if (!JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
            
            try {
                SecureBlockEncryptionService.decryptFromString(block.getEncryptionMetadata(), password);
                return true;
            } catch (Exception e) {
                return false;
            }
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Convert an existing unencrypted block to encrypted
     * 
     * @param blockId The block ID to encrypt
     * @param password The password for encryption
     * @return true if encryption was successful, false if block was already encrypted or not found
     */
    public boolean encryptExistingBlock(Long blockId, String password) {
        if (blockId == null) {
            throw new IllegalArgumentException("Block ID cannot be null");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        lock.writeLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            EntityTransaction transaction = null;
            boolean shouldManageTransaction = !JPAUtil.hasActiveTransaction();
            
            try {
                if (shouldManageTransaction) {
                    transaction = em.getTransaction();
                    transaction.begin();
                }
                
                Block block = em.find(Block.class, blockId);
                if (block == null || block.isDataEncrypted()) {
                    return false; // Block not found or already encrypted
                }
                
                if (block.getData() == null || block.getData().trim().isEmpty()) {
                    return false; // No data to encrypt
                }
                
                // Encrypt the data
                String originalData = block.getData();
                String encryptedData = SecureBlockEncryptionService.encryptToString(originalData, password);
                
                // Update block
                block.setEncryptionMetadata(encryptedData);
                block.setData("[ENCRYPTED]");
                block.setIsEncrypted(true);
                
                em.merge(block);
                
                if (shouldManageTransaction) {
                    transaction.commit();
                }
                
                return true;
                
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw new RuntimeException("Error encrypting existing block: " + e.getMessage(), e);
            } finally {
                if (shouldManageTransaction && !JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Clean up test data - Delete all blocks except genesis block (block 0) and reset sequence
     * Thread-safe method for test isolation
     * WARNING: This method is intended for testing purposes only
     */
    public void cleanupTestData() {
        lock.writeLock().lock();
        try {
            if (JPAUtil.hasActiveTransaction()) {
                // Use existing global transaction
                EntityManager em = JPAUtil.getEntityManager();
                // Delete all blocks except genesis (block 0)
                em.createQuery("DELETE FROM Block b WHERE b.blockNumber > 0").executeUpdate();
                // Reset block sequence counter
                em.createQuery("DELETE FROM BlockSequence").executeUpdate();
                // Clear Hibernate session cache to avoid entity conflicts
                em.flush();
                em.clear();
            } else {
                // Create own transaction for cleanup
                JPAUtil.executeInTransaction(em -> {
                    // Delete all blocks except genesis (block 0)
                    em.createQuery("DELETE FROM Block b WHERE b.blockNumber > 0").executeUpdate();
                    // Reset block sequence counter
                    em.createQuery("DELETE FROM BlockSequence").executeUpdate();
                    // Clear Hibernate session cache to avoid entity conflicts
                    em.flush();
                    em.clear();
                    return null;
                });
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Complete database cleanup - Delete ALL blocks including genesis and reset everything
     * Thread-safe method for complete test isolation
     * WARNING: This method is intended for testing purposes only - removes ALL data
     */
    public void completeCleanupTestData() {
        lock.writeLock().lock();
        try {
            if (JPAUtil.hasActiveTransaction()) {
                // Use existing global transaction
                EntityManager em = JPAUtil.getEntityManager();
                // Delete ALL blocks (including genesis)
                em.createQuery("DELETE FROM Block").executeUpdate();
                // Reset block sequence counter
                em.createQuery("DELETE FROM BlockSequence").executeUpdate();
                // Clear Hibernate session cache to avoid entity conflicts
                em.flush();
                em.clear();
            } else {
                // Create own transaction for cleanup
                JPAUtil.executeInTransaction(em -> {
                    // Delete ALL blocks (including genesis)
                    em.createQuery("DELETE FROM Block").executeUpdate();
                    // Reset block sequence counter
                    em.createQuery("DELETE FROM BlockSequence").executeUpdate();
                    // Clear Hibernate session cache to avoid entity conflicts
                    em.flush();
                    em.clear();
                    return null;
                });
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
