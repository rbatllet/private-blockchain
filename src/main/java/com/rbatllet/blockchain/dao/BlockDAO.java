package com.rbatllet.blockchain.dao;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

public class BlockDAO {
    
    /**
     * Save a new block to the database
     */
    public void saveBlock(Block block) {
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
            em.close();
        }
    }
    
    /**
     * Get a block by its number
     */
    public Block getBlockByNumber(int blockNumber) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Block> query = em.createQuery(
                "SELECT b FROM Block b WHERE b.blockNumber = :blockNumber", Block.class);
            query.setParameter("blockNumber", blockNumber);
            
            List<Block> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }
    
    /**
     * Get the last block in the chain
     */
    public Block getLastBlock() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Block> query = em.createQuery(
                "SELECT b FROM Block b ORDER BY b.blockNumber DESC", Block.class);
            query.setMaxResults(1);
            
            List<Block> blocks = query.getResultList();
            return blocks.isEmpty() ? null : blocks.get(0);
        } finally {
            em.close();
        }
    }
    
    /**
     * Get all blocks in the chain ordered by number
     */
    public List<Block> getAllBlocks() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Block> query = em.createQuery(
                "SELECT b FROM Block b ORDER BY b.blockNumber ASC", Block.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    /**
     * Get blocks within a time range
     */
    public List<Block> getBlocksByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Block> query = em.createQuery(
                "SELECT b FROM Block b WHERE b.timestamp BETWEEN :startTime AND :endTime ORDER BY b.blockNumber ASC", 
                Block.class);
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    /**
     * Get the total number of blocks
     */
    public long getBlockCount() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery("SELECT COUNT(b) FROM Block b", Long.class);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }
    
    /**
     * Check if a block with a specific hash exists
     */
    public boolean existsBlockWithHash(String hash) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(b) FROM Block b WHERE b.hash = :hash", Long.class);
            query.setParameter("hash", hash);
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }
    
    /**
     * Delete a block by its number
     */
    public boolean deleteBlockByNumber(int blockNumber) {
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
    
    /**
     * Delete blocks with block numbers greater than the specified number
     */
    public int deleteBlocksAfter(int blockNumber) {
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
    
    /**
     * Delete all blocks (for import functionality)
     */
    public int deleteAllBlocks() {
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
    
    /**
     * Search blocks by content (case-insensitive)
     */
    public List<Block> searchBlocksByContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Block> query = em.createQuery(
                "SELECT b FROM Block b WHERE LOWER(b.data) LIKE :content ORDER BY b.blockNumber ASC", 
                Block.class);
            query.setParameter("content", "%" + content.toLowerCase() + "%");
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    /**
     * Get block by hash
     */
    public Block getBlockByHash(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            return null;
        }
        
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Block> query = em.createQuery(
                "SELECT b FROM Block b WHERE b.hash = :hash", Block.class);
            query.setParameter("hash", hash);
            
            List<Block> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }
}
