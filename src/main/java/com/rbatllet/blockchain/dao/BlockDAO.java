package com.rbatllet.blockchain.dao;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.util.List;

public class BlockDAO {
    
    /**
     * Save a new block to the database
     */
    public void saveBlock(Block block) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(block);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving block", e);
        }
    }
    
    /**
     * Get a block by its number
     */
    public Block getBlockByNumber(int blockNumber) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Block> query = session.createQuery(
                "FROM Block WHERE blockNumber = :blockNumber", Block.class);
            query.setParameter("blockNumber", blockNumber);
            return query.uniqueResult();
        }
    }
    
    /**
     * Get the last block in the chain
     */
    public Block getLastBlock() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Block> query = session.createQuery(
                "FROM Block ORDER BY blockNumber DESC", Block.class);
            query.setMaxResults(1);
            List<Block> blocks = query.list();
            return blocks.isEmpty() ? null : blocks.get(0);
        }
    }
    
    /**
     * Get all blocks in the chain ordered by number
     */
    public List<Block> getAllBlocks() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Block> query = session.createQuery(
                "FROM Block ORDER BY blockNumber ASC", Block.class);
            return query.list();
        }
    }
    
    /**
     * Get blocks within a time range
     */
    public List<Block> getBlocksByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Block> query = session.createQuery(
                "FROM Block WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY blockNumber ASC", 
                Block.class);
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
            return query.list();
        }
    }
    
    /**
     * Get the total number of blocks
     */
    public long getBlockCount() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM Block", Long.class);
            return query.uniqueResult();
        }
    }
    
    /**
     * Check if a block with a specific hash exists
     */
    public boolean existsBlockWithHash(String hash) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM Block WHERE hash = :hash", Long.class);
            query.setParameter("hash", hash);
            return query.uniqueResult() > 0;
        }
    }
    
    /**
     * Delete a block by its number
     */
    public boolean deleteBlockByNumber(int blockNumber) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            Query<?> query = session.createQuery("DELETE FROM Block WHERE blockNumber = :blockNumber");
            query.setParameter("blockNumber", blockNumber);
            int deletedCount = query.executeUpdate();
            
            transaction.commit();
            return deletedCount > 0;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting block with number " + blockNumber, e);
        }
    }
    
    /**
     * Delete blocks with block numbers greater than the specified number
     */
    public int deleteBlocksAfter(int blockNumber) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            Query<?> query = session.createQuery("DELETE FROM Block WHERE blockNumber > :blockNumber");
            query.setParameter("blockNumber", blockNumber);
            int deletedCount = query.executeUpdate();
            
            transaction.commit();
            return deletedCount;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting blocks after " + blockNumber, e);
        }
    }
    
    /**
     * Delete all blocks (for import functionality)
     */
    public int deleteAllBlocks() {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            Query<?> query = session.createQuery("DELETE FROM Block");
            int deletedCount = query.executeUpdate();
            
            transaction.commit();
            return deletedCount;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting all blocks", e);
        }
    }
    
    /**
     * Search blocks by content (case-insensitive)
     */
    public List<Block> searchBlocksByContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Block> query = session.createQuery(
                "FROM Block WHERE LOWER(data) LIKE :content ORDER BY blockNumber ASC", Block.class);
            query.setParameter("content", "%" + content.toLowerCase() + "%");
            return query.list();
        }
    }
    
    /**
     * Get block by hash
     */
    public Block getBlockByHash(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            return null;
        }
        
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Block> query = session.createQuery(
                "FROM Block WHERE hash = :hash", Block.class);
            query.setParameter("hash", hash);
            return query.uniqueResult();
        }
    }
}
