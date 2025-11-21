package com.rbatllet.blockchain.dao;

import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;

/**
 * DAO for AuthorizedKey operations
 * Thread-safety provided by GLOBAL_BLOCKCHAIN_LOCK in Blockchain.java
 *
 * <p>AuthorizedKeyDAO does NOT have its own locking to avoid deadlock issues with nested
 * lock acquisition. All operations are protected by GLOBAL_BLOCKCHAIN_LOCK (StampedLock)
 * in Blockchain.java. AuthorizedKeyDAO is only called from within Blockchain's lock scope,
 * never directly by external code.</p>
 *
 * @version 1.0.5
 */
public class AuthorizedKeyDAO {
    
    /**
     * Save a new authorized key
     * Uses global transaction if available, otherwise creates its own
     * FIXED: Better handling of external transactions
     */
    public void saveAuthorizedKey(AuthorizedKey authorizedKey) {
        if (authorizedKey == null) {
            throw new IllegalArgumentException("AuthorizedKey cannot be null");
        }

        EntityManager em = JPAUtil.getEntityManager();

        // Check if there's already an active transaction (from test or other source)
        boolean externalTransaction = em.getTransaction().isActive();

        if (externalTransaction) {
            // Use existing external transaction
            em.persist(authorizedKey);
        } else if (JPAUtil.hasActiveTransaction()) {
            // Use existing global transaction
            em.persist(authorizedKey);
        } else {
            // Create own transaction
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
                if (!externalTransaction && !JPAUtil.hasActiveTransaction()) {
                    em.close();
                }
            }
        }
    }
    
    /**
     * Check if a public key is authorized and active
     * FIXED: Uses simple logic but consistent temporal validation
     * FIXED: Only checks currently active authorizations
     */
    public boolean isKeyAuthorized(String publicKey) {
        if (publicKey == null || publicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be null or empty");
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<AuthorizedKey> query = em.createQuery(
                "SELECT ak FROM AuthorizedKey ak WHERE ak.publicKey = :publicKey AND ak.isActive = true ORDER BY ak.createdAt DESC",
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
            if (!JPAUtil.hasActiveTransaction()) {
                em.close();
            }
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
            if (!JPAUtil.hasActiveTransaction()) {
                em.close();
            }
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
            if (!JPAUtil.hasActiveTransaction()) {
                em.close();
            }
        }
    }
    
    /**
     * Revoke an authorized key with proper temporal tracking
     * FIXED: Now only revokes the most recent active authorization
     * FIXED: Better handling of external transactions
     */
    public void revokeAuthorizedKey(String publicKey) {
        if (publicKey == null || publicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be null or empty");
        }

        EntityManager em = JPAUtil.getEntityManager();
        boolean externalTransaction = em.getTransaction().isActive();

        if (externalTransaction || JPAUtil.hasActiveTransaction()) {
            // Use existing transaction
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
        } else {
            // Create own transaction
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
                throw new RuntimeException("Error revoking key", e);
            } finally {
                em.close();
            }
        }
    }
    
    /**
     * Delete a specific authorized key by public key
     * This will delete ALL records for this public key (active and revoked)
     */
    public boolean deleteAuthorizedKey(String publicKey) {
        if (publicKey == null || publicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be null or empty");
        }

        if (JPAUtil.hasActiveTransaction()) {
            EntityManager em = JPAUtil.getEntityManager();
            int deletedCount = em.createQuery("DELETE FROM AuthorizedKey ak WHERE ak.publicKey = :publicKey")
                    .setParameter("publicKey", publicKey)
                    .executeUpdate();
            return deletedCount > 0;
        } else {
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
    }

    /**
     * Delete all authorized keys (for import functionality)
     */
    public int deleteAllAuthorizedKeys() {
        if (JPAUtil.hasActiveTransaction()) {
            EntityManager em = JPAUtil.getEntityManager();
            return em.createQuery("DELETE FROM AuthorizedKey ak").executeUpdate();
        } else {
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
    
    /**
     * Find an authorized key by owner name
     * Returns the most recent active authorization for the given owner
     */
    public AuthorizedKey getAuthorizedKeyByOwner(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be null or empty");
        }

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
            if (!JPAUtil.hasActiveTransaction()) {
                em.close();
            }
        }
    }

    /**
     * Get an authorized key by its public key (RBAC v1.0.6+).
     * Returns the most recent authorization record for the given public key.
     *
     * @param publicKey The public key to search for
     * @return The AuthorizedKey entity, or null if not found
     * @since 1.0.6
     */
    public AuthorizedKey getAuthorizedKeyByPublicKey(String publicKey) {
        if (publicKey == null || publicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be null or empty");
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<AuthorizedKey> query = em.createQuery(
                "SELECT ak FROM AuthorizedKey ak WHERE ak.publicKey = :publicKey ORDER BY ak.createdAt DESC",
                AuthorizedKey.class);
            query.setParameter("publicKey", publicKey);
            query.setMaxResults(1);

            List<AuthorizedKey> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } finally {
            if (!JPAUtil.hasActiveTransaction()) {
                em.close();
            }
        }
    }

    /**
     * Check if a public key was authorized at a specific time
     * This is used for validating historical blocks that may have been signed
     * by keys that have since been revoked
     * FIXED: Now finds the authorization that was valid at the specific timestamp
     */
    public boolean wasKeyAuthorizedAt(String publicKey, java.time.LocalDateTime timestamp) {
        if (publicKey == null || publicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be null or empty");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }

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
            if (!JPAUtil.hasActiveTransaction()) {
                em.close();
            }
        }
    }
    
    /**
     * Clean up test data - Delete all authorized keys and reset state
     * Thread-safe method for test isolation
     * WARNING: This method is intended for testing purposes only
     */
    public void cleanupTestData() {
        if (JPAUtil.hasActiveTransaction()) {
            // Use existing global transaction
            EntityManager em = JPAUtil.getEntityManager();
            // Delete all authorized keys
            em.createQuery("DELETE FROM AuthorizedKey").executeUpdate();
            // Clear Hibernate session cache to avoid entity conflicts
            em.flush();
            em.clear();
        } else {
            // Create own transaction for cleanup
            JPAUtil.executeInTransaction(em -> {
                // Delete all authorized keys
                em.createQuery("DELETE FROM AuthorizedKey").executeUpdate();
                // Clear Hibernate session cache to avoid entity conflicts
                em.flush();
                em.clear();
                return null;
            });
        }
    }

    /**
     * Get total count of authorized keys in database.
     *
     * <p>This method is used for genesis admin bootstrap detection.
     * When count is 0, the blockchain will create the first authorized user (genesis admin).</p>
     *
     * @return Total number of authorized keys (active and revoked)
     * @since 1.0.6
     */
    public long getAuthorizedKeyCount() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(ak) FROM AuthorizedKey ak", Long.class
            );
            return query.getSingleResult();
        } finally {
            if (!JPAUtil.hasActiveTransaction()) {
                em.close();
            }
        }
    }

    /**
     * Count active SUPER_ADMIN users.
     *
     * <p>This method is used to enforce the security protection that prevents
     * revoking the last active SUPER_ADMIN (system lockout protection).</p>
     *
     * <p><strong>Use case:</strong> Before revoking a SUPER_ADMIN, check if there
     * are at least 2 active SUPER_ADMINs. If only 1 exists, revocation must be blocked.</p>
     *
     * @return Number of active SUPER_ADMIN users
     * @since 1.0.6
     */
    public long countActiveSuperAdmins() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(ak) FROM AuthorizedKey ak WHERE ak.role = :role AND ak.isActive = true",
                Long.class
            );
            query.setParameter("role", com.rbatllet.blockchain.security.UserRole.SUPER_ADMIN);
            return query.getSingleResult();
        } finally {
            if (!JPAUtil.hasActiveTransaction()) {
                em.close();
            }
        }
    }
}