package com.rbatllet.blockchain.dao;

import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class AuthorizedKeyDAO {
    
    /**
     * Save a new authorized key
     */
    public void saveAuthorizedKey(AuthorizedKey authorizedKey) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction transaction = null;
        
        try {
            transaction = em.getTransaction();
            transaction.begin();
            
            em.persist(authorizedKey);
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving authorized key", e);
        } finally {
            em.close();
        }
    }
    
    /**
     * Check if a public key is authorized and active
     * FIXED: Uses simple logic but consistent temporal validation
     */
    public boolean isKeyAuthorized(String publicKey) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<AuthorizedKey> query = em.createQuery(
                "SELECT ak FROM AuthorizedKey ak WHERE ak.publicKey = :publicKey ORDER BY ak.createdAt DESC", 
                AuthorizedKey.class);
            query.setParameter("publicKey", publicKey);
            query.setMaxResults(1);
            
            List<AuthorizedKey> results = query.getResultList();
            if (results.isEmpty()) {
                return false;
            }
            
            AuthorizedKey key = results.get(0);
            
            // Use temporal validation for current time
            return key.wasActiveAt(java.time.LocalDateTime.now());
        } finally {
            em.close();
        }
    }
    
    /**
     * Get all currently active authorized keys (only the most recent for each public key)
     * FIXED: Now returns only one record per public key
     */
    public List<AuthorizedKey> getActiveAuthorizedKeys() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Get the most recent authorization for each public key that is currently active
            TypedQuery<AuthorizedKey> query = em.createQuery(
                "SELECT ak FROM AuthorizedKey ak WHERE ak.createdAt = " +
                "(SELECT MAX(ak2.createdAt) FROM AuthorizedKey ak2 WHERE ak2.publicKey = ak.publicKey) " +
                "AND ak.isActive = true ORDER BY ak.createdAt ASC", 
                AuthorizedKey.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    /**
     * Get ALL authorized keys (including revoked ones) for export functionality
     * This is needed to properly validate historical blocks during import
     */
    public List<AuthorizedKey> getAllAuthorizedKeys() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<AuthorizedKey> query = em.createQuery(
                "SELECT ak FROM AuthorizedKey ak ORDER BY ak.createdAt ASC", 
                AuthorizedKey.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    /**
     * Revoke an authorized key with proper temporal tracking
     * FIXED: Now only revokes the most recent active authorization
     * RENAMED: Changed from deactivateKey to revokeAuthorizedKey for consistency
     */
    public void revokeAuthorizedKey(String publicKey) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction transaction = null;
        
        try {
            transaction = em.getTransaction();
            transaction.begin();
            
            // Find the most recent active authorization for this key
            TypedQuery<AuthorizedKey> selectQuery = em.createQuery(
                "SELECT ak FROM AuthorizedKey ak WHERE ak.publicKey = :publicKey AND ak.isActive = true ORDER BY ak.createdAt DESC", 
                AuthorizedKey.class);
            selectQuery.setParameter("publicKey", publicKey);
            selectQuery.setMaxResults(1);
            
            List<AuthorizedKey> results = selectQuery.getResultList();
            if (!results.isEmpty()) {
                AuthorizedKey keyToRevoke = results.get(0);
                keyToRevoke.setActive(false);
                keyToRevoke.setRevokedAt(java.time.LocalDateTime.now());
                em.merge(keyToRevoke);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deactivating key", e);
        } finally {
            em.close();
        }
    }
    
    /**
     * Check if a public key was authorized at a specific time
     * This is used for validating historical blocks that may have been signed
     * by keys that have since been revoked
     * FIXED: Now finds the authorization that was valid at the specific timestamp
     */
    public boolean wasKeyAuthorizedAt(String publicKey, java.time.LocalDateTime timestamp) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Get all authorizations for this key, ordered by creation time
            TypedQuery<AuthorizedKey> query = em.createQuery(
                "SELECT ak FROM AuthorizedKey ak WHERE ak.publicKey = :publicKey ORDER BY ak.createdAt ASC", 
                AuthorizedKey.class);
            query.setParameter("publicKey", publicKey);
            
            List<AuthorizedKey> keys = query.getResultList();
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
        } finally {
            em.close();
        }
    }
    
    /**
     * Find an authorized key by owner name
     * Returns the most recent active authorization for the given owner
     */
    public AuthorizedKey getAuthorizedKeyByOwner(String ownerName) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<AuthorizedKey> query = em.createQuery(
                "SELECT ak FROM AuthorizedKey ak WHERE ak.ownerName = :ownerName AND ak.isActive = true ORDER BY ak.createdAt DESC", 
                AuthorizedKey.class);
            query.setParameter("ownerName", ownerName);
            query.setMaxResults(1);
            
            List<AuthorizedKey> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }

    /**
     * Delete a specific authorized key by public key
     * This will delete ALL records for this public key (active and revoked)
     */
    public boolean deleteAuthorizedKey(String publicKey) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction transaction = null;
        
        try {
            transaction = em.getTransaction();
            transaction.begin();
            
            int deletedCount = em.createQuery("DELETE FROM AuthorizedKey ak WHERE ak.publicKey = :publicKey")
                    .setParameter("publicKey", publicKey)
                    .executeUpdate();
            
            transaction.commit();
            return deletedCount > 0;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting authorized key", e);
        } finally {
            em.close();
        }
    }

    /**
     * Delete all authorized keys (for import functionality)
     */
    public int deleteAllAuthorizedKeys() {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction transaction = null;
        
        try {
            transaction = em.getTransaction();
            transaction.begin();
            
            int deletedCount = em.createQuery("DELETE FROM AuthorizedKey ak").executeUpdate();
            
            transaction.commit();
            return deletedCount;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting all authorized keys", e);
        } finally {
            em.close();
        }
    }
}
