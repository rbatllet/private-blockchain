package com.rbatllet.blockchain.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe JPA Utility class with support for global transactions
 * FIXED: Added thread-safety improvements and transaction management
 */
public class JPAUtil {
    
    private static EntityManagerFactory entityManagerFactory;
    private static final ReentrantLock initLock = new ReentrantLock();
    
    // Thread-local storage for EntityManager to ensure each thread has its own
    private static final ThreadLocal<EntityManager> threadLocalEntityManager = new ThreadLocal<>();
    private static final ThreadLocal<EntityTransaction> threadLocalTransaction = new ThreadLocal<>();
    
    static {
        try {
            // Create the EntityManagerFactory from persistence.xml
            entityManagerFactory = Persistence.createEntityManagerFactory("blockchainPU");
        } catch (Throwable ex) {
            System.err.println("Initial EntityManagerFactory creation failed: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
    
    public static EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }
    
    /**
     * Get EntityManager for current thread
     * Creates a new one if none exists for this thread
     */
    public static EntityManager getEntityManager() {
        EntityManager em = threadLocalEntityManager.get();
        if (em == null || !em.isOpen()) {
            em = entityManagerFactory.createEntityManager();
            threadLocalEntityManager.set(em);
        }
        return em;
    }
    
    /**
     * Begin a global transaction for the current thread
     * This allows multiple DAO operations to participate in the same transaction
     * FIXED: Handle case where transaction already exists
     */
    public static void beginTransaction() {
        EntityManager em = getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        if (!transaction.isActive()) {
            transaction.begin();
            threadLocalTransaction.set(transaction);
        } else {
            // Transaction already active, just store reference
            threadLocalTransaction.set(transaction);
        }
    }
    
    /**
     * Commit the global transaction for the current thread
     */
    public static void commitTransaction() {
        EntityTransaction transaction = threadLocalTransaction.get();
        if (transaction != null && transaction.isActive()) {
            transaction.commit();
            threadLocalTransaction.remove();
        }
    }
    
    /**
     * Rollback the global transaction for the current thread
     */
    public static void rollbackTransaction() {
        EntityTransaction transaction = threadLocalTransaction.get();
        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
            threadLocalTransaction.remove();
        }
    }
    
    /**
     * Check if there's an active global transaction
     */
    public static boolean hasActiveTransaction() {
        EntityManager em = threadLocalEntityManager.get();
        if (em != null && em.isOpen()) {
            EntityTransaction transaction = em.getTransaction();
            return transaction != null && transaction.isActive();
        }
        return false;
    }
    
    /**
     * Close the EntityManager for the current thread
     * This should be called at the end of request processing
     */
    public static void closeEntityManager() {
        EntityManager em = threadLocalEntityManager.get();
        if (em != null && em.isOpen()) {
            em.close();
            threadLocalEntityManager.remove();
        }
        threadLocalTransaction.remove();
    }
    
    /**
     * Execute a block of code within a transaction
     * Automatically handles commit/rollback
     */
    public static <T> T executeInTransaction(TransactionCallback<T> callback) {
        boolean shouldManageTransaction = !hasActiveTransaction();
        
        try {
            if (shouldManageTransaction) {
                beginTransaction();
            }
            
            T result = callback.execute(getEntityManager());
            
            if (shouldManageTransaction) {
                commitTransaction();
            }
            
            return result;
        } catch (Exception e) {
            if (shouldManageTransaction) {
                rollbackTransaction();
            }
            throw new RuntimeException("Transaction failed", e);
        } finally {
            if (shouldManageTransaction) {
                closeEntityManager();
            }
        }
    }
    
    /**
     * Thread-safe shutdown method
     */
    public static void shutdown() {
        initLock.lock();
        try {
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
            }
        } finally {
            initLock.unlock();
        }
    }
    
    /**
     * Functional interface for transaction callbacks
     */
    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute(EntityManager em) throws Exception;
    }
}