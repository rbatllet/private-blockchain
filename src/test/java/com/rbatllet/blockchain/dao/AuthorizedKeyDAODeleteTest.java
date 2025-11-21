package com.rbatllet.blockchain.dao;

import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizedKeyDAODeleteTest {

    private AuthorizedKeyDAO keyDAO;
    private EntityManager entityManager;
    private EntityTransaction transaction;

    @BeforeEach
    void setUp() {
        keyDAO = new AuthorizedKeyDAO();
        entityManager = JPAUtil.getEntityManager();
        transaction = entityManager.getTransaction();
        transaction.begin();
    }

    @AfterEach
    void tearDown() {
        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
        }
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }

    @Test
    void testDeleteExistingKey() {
        // Create and save a test key
        String testPublicKey = "test-public-key-123";
        String testOwnerName = "test-owner";

        AuthorizedKey testKey = new AuthorizedKey(testPublicKey, testOwnerName, UserRole.USER, "TestCreator", java.time.LocalDateTime.now());
        keyDAO.saveAuthorizedKey(testKey);
        
        // Verify the key exists
        assertTrue(keyDAO.isKeyAuthorized(testPublicKey), "Key should exist before deletion");
        
        // Delete the key
        boolean deleted = keyDAO.deleteAuthorizedKey(testPublicKey);
        
        // Verify deletion
        assertTrue(deleted, "Delete operation should return true for existing key");
        assertFalse(keyDAO.isKeyAuthorized(testPublicKey), "Key should not exist after deletion");
    }

    @Test
    void testDeleteNonExistentKey() {
        // Try to delete a key that doesn't exist
        String nonExistentKey = "non-existent-key-456";
        
        // Verify the key doesn't exist
        assertFalse(keyDAO.isKeyAuthorized(nonExistentKey), "Key should not exist initially");
        
        // Try to delete the non-existent key
        boolean deleted = keyDAO.deleteAuthorizedKey(nonExistentKey);
        
        // Verify that delete returns false for non-existent key
        assertFalse(deleted, "Delete operation should return false for non-existent key");
    }

    @Test
    void testDeleteMultipleRecordsForSameKey() {
        // This test verifies that delete removes ALL records for a public key
        String testPublicKey = "multi-record-key-789";
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // First authorization
        AuthorizedKey firstKey = new AuthorizedKey(testPublicKey, "owner1", UserRole.USER, "TestCreator", now);
        keyDAO.saveAuthorizedKey(firstKey);
        
        // Verify first key is authorized
        assertTrue(keyDAO.isKeyAuthorized(testPublicKey), "First key should be authorized");

        // Create second authorization with same public key but different owner
        // This simulates re-authorization scenario
        AuthorizedKey secondKey = new AuthorizedKey(testPublicKey, "owner2", UserRole.USER, "TestCreator", now.plusMinutes(1));
        keyDAO.saveAuthorizedKey(secondKey);
        
        // Delete all records for this public key
        boolean deleted = keyDAO.deleteAuthorizedKey(testPublicKey);
        
        // Verify deletion worked
        assertTrue(deleted, "Delete operation should return true when records exist");
        assertFalse(keyDAO.isKeyAuthorized(testPublicKey), "Key should not be authorized after deletion");
    }
}
