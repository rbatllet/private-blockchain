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
     * FIXED: Uses simple logic but consistent temporal validation
     */
    public boolean isKeyAuthorized(String publicKey) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<AuthorizedKey> query = session.createQuery(
                "FROM AuthorizedKey WHERE publicKey = :publicKey ORDER BY createdAt DESC", 
                AuthorizedKey.class);
            query.setParameter("publicKey", publicKey);
            query.setMaxResults(1);
            
            AuthorizedKey key = query.uniqueResult();
            if (key == null) {
                return false;
            }
            
            // Use temporal validation for current time
            return key.wasActiveAt(java.time.LocalDateTime.now());
        }
    }
    
    /**
     * Get all currently active authorized keys (only the most recent for each public key)
     * FIXED: Now returns only one record per public key
     */
    public List<AuthorizedKey> getActiveAuthorizedKeys() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Get the most recent authorization for each public key that is currently active
            Query<AuthorizedKey> query = session.createQuery(
                "SELECT ak FROM AuthorizedKey ak WHERE ak.createdAt = " +
                "(SELECT MAX(ak2.createdAt) FROM AuthorizedKey ak2 WHERE ak2.publicKey = ak.publicKey) " +
                "AND ak.isActive = true ORDER BY ak.createdAt ASC", 
                AuthorizedKey.class);
            return query.list();
        }
    }
    
    /**
     * Get ALL authorized keys (including revoked ones) for export functionality
     * This is needed to properly validate historical blocks during import
     */
    public List<AuthorizedKey> getAllAuthorizedKeys() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<AuthorizedKey> query = session.createQuery(
                "FROM AuthorizedKey ORDER BY createdAt ASC", 
                AuthorizedKey.class);
            return query.list();
        }
    }
    
    /**
     * Revoke an authorized key with proper temporal tracking
     * FIXED: Now only revokes the most recent active authorization
     * RENAMED: Changed from deactivateKey to revokeAuthorizedKey for consistency
     */
    public void revokeAuthorizedKey(String publicKey) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            // Find the most recent active authorization for this key
            Query<AuthorizedKey> selectQuery = session.createQuery(
                "FROM AuthorizedKey WHERE publicKey = :publicKey AND isActive = true ORDER BY createdAt DESC", 
                AuthorizedKey.class);
            selectQuery.setParameter("publicKey", publicKey);
            selectQuery.setMaxResults(1);
            
            AuthorizedKey keyToRevoke = selectQuery.uniqueResult();
            if (keyToRevoke != null) {
                keyToRevoke.setActive(false);
                keyToRevoke.setRevokedAt(java.time.LocalDateTime.now());
                session.update(keyToRevoke);
            }
            
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
     * FIXED: Now finds the authorization that was valid at the specific timestamp
     */
    public boolean wasKeyAuthorizedAt(String publicKey, java.time.LocalDateTime timestamp) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Get all authorizations for this key, ordered by creation time
            Query<AuthorizedKey> query = session.createQuery(
                "FROM AuthorizedKey WHERE publicKey = :publicKey ORDER BY createdAt ASC", 
                AuthorizedKey.class);
            query.setParameter("publicKey", publicKey);
            
            List<AuthorizedKey> keys = query.list();
            if (keys.isEmpty()) {
                return false;
            }
            
            // Find the authorization that was valid at the given timestamp
            AuthorizedKey validKey = null;
            for (AuthorizedKey key : keys) {
                if (key.getCreatedAt() != null && !timestamp.isBefore(key.getCreatedAt())) {
                    validKey = key; // This key could be valid
                } else {
                    break; // Keys are ordered by creation, so we can stop here
                }
            }
            
            if (validKey == null) {
                return false;
            }
            
            // Use the temporal validation method on the correct key
            return validKey.wasActiveAt(timestamp);
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
