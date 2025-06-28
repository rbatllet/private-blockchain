package com.rbatllet.blockchain.dao;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.BlockSequence;
import com.rbatllet.blockchain.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe DAO for Block operations
 * FIXED: Added thread-safety and support for global transactions
 */
public class BlockDAO {
    
    // Read-Write lock for thread safety on read operations
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Global lock for atomic block number generation - prevents race conditions
    private static final Object GLOBAL_BLOCK_NUMBER_LOCK = new Object();
    
    /**
     * Save a new block to the database
     * Uses global transaction if available, otherwise creates its own
     */
    public void saveBlock(Block block) {
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
                System.out.println("🔍 [" + threadName + "] Starting getNextBlockNumberAtomic() - shouldManageTransaction: " + shouldManageTransaction);
                
                if (shouldManageTransaction) {
                    transaction = em.getTransaction();
                    transaction.begin();
                    System.out.println("🔍 [" + threadName + "] Started new transaction");
                }
                
                // Use BlockSequence with pessimistic write lock for true atomicity
                System.out.println("🔍 [" + threadName + "] Acquiring PESSIMISTIC_WRITE lock on BlockSequence");
                BlockSequence sequence = em.find(BlockSequence.class, "block_number", LockModeType.PESSIMISTIC_WRITE);
                
                if (sequence == null) {
                    System.out.println("🔍 [" + threadName + "] BlockSequence not found, initializing...");
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
                    
                    System.out.println("🔍 [" + threadName + "] Creating new sequence with nextValue: " + nextValue);
                    sequence = new BlockSequence("block_number", nextValue);
                    em.persist(sequence);
                    em.flush(); // Force immediate persistence
                    
                    if (shouldManageTransaction && transaction != null) {
                        transaction.commit();
                        System.out.println("🔍 [" + threadName + "] Committed transaction for new sequence");
                    }
                    
                    long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                    System.out.println("✅ [" + threadName + "] Generated block number: " + nextValue + " (took " + elapsed + "ms)");
                    return nextValue;
                } else {
                    // FIXED: Correct atomic increment logic
                    Long blockNumberToUse = sequence.getNextValue(); // This is the number we'll use for the current block
                    System.out.println("🔍 [" + threadName + "] Found existing sequence, using block number: " + blockNumberToUse);
                    
                    // Increment the sequence for the next block
                    sequence.setNextValue(blockNumberToUse + 1);
                    em.merge(sequence);
                    em.flush(); // Force immediate persistence
                    
                    System.out.println("🔍 [" + threadName + "] Updated sequence to nextValue: " + (blockNumberToUse + 1));
                    
                    if (shouldManageTransaction && transaction != null) {
                        transaction.commit();
                        System.out.println("🔍 [" + threadName + "] Committed transaction for existing sequence");
                    }
                    
                    long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                    System.out.println("✅ [" + threadName + "] Generated block number: " + blockNumberToUse + " (took " + elapsed + "ms)");
                    return blockNumberToUse;
                }
                
            } catch (Exception e) {
                System.err.println("❌ [" + threadName + "] ERROR in getNextBlockNumberAtomic: " + e.getMessage());
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
     * Get the total number of blocks
     */
    public long getBlockCount() {
        lock.readLock().lock();
        try {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                TypedQuery<Long> query = em.createQuery("SELECT COUNT(b) FROM Block b", Long.class);
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
            return new java.util.ArrayList<>();
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
}
