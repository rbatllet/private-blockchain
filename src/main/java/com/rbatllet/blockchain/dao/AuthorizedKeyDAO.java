package com.rbatllet.blockchain.dao;

import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class AuthorizedKeyDAO {
    
    /**
     * Save a new authorized key
     */
    public void saveAuthorizedKey(AuthorizedKey authorizedKey) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(authorizedKey);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving authorized key", e);
        }
    }
    
    /**
     * Check if a public key is authorized and active
     */
    public boolean isKeyAuthorized(String publicKey) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM AuthorizedKey WHERE publicKey = :publicKey AND isActive = true", 
                Long.class);
            query.setParameter("publicKey", publicKey);
            return query.uniqueResult() > 0;
        }
    }
    
    /**
     * Get all active authorized keys
     */
    public List<AuthorizedKey> getActiveAuthorizedKeys() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<AuthorizedKey> query = session.createQuery(
                "FROM AuthorizedKey WHERE isActive = true ORDER BY createdAt ASC", 
                AuthorizedKey.class);
            return query.list();
        }
    }
    
    /**
     * Deactivate an authorized key
     */
    public void deactivateKey(String publicKey) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Query query = session.createQuery(
                "UPDATE AuthorizedKey SET isActive = false WHERE publicKey = :publicKey");
            query.setParameter("publicKey", publicKey);
            query.executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deactivating key", e);
        }
    }
    
    /**
     * Check if a public key was authorized at a specific time
     * This is used for validating historical blocks that may have been signed
     * by keys that have since been revoked
     */
    public boolean wasKeyAuthorizedAt(String publicKey, java.time.LocalDateTime timestamp) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM AuthorizedKey WHERE publicKey = :publicKey AND createdAt <= :timestamp", 
                Long.class);
            query.setParameter("publicKey", publicKey);
            query.setParameter("timestamp", timestamp);
            return query.uniqueResult() > 0;
        }
    }
    
    /**
     * Delete all authorized keys (for import functionality)
     */
    public int deleteAllAuthorizedKeys() {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            Query<?> query = session.createQuery("DELETE FROM AuthorizedKey");
            int deletedCount = query.executeUpdate();
            
            transaction.commit();
            return deletedCount;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting all authorized keys", e);
        }
    }
}
