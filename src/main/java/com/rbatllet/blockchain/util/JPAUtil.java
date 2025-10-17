package com.rbatllet.blockchain.util;

import com.rbatllet.blockchain.config.DatabaseConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe JPA Utility class with support for global transactions and database-agnostic configuration
 * Supports dynamic database selection via DatabaseConfig
 */
public class JPAUtil {

    private static final Logger logger = LoggerFactory.getLogger(JPAUtil.class);

    private static EntityManagerFactory entityManagerFactory;
    private static DatabaseConfig currentConfig;
    private static final ReentrantLock initLock = new ReentrantLock();
    
    // Thread-local storage for EntityManager to ensure each thread has its own
    private static final ThreadLocal<EntityManager> threadLocalEntityManager = new ThreadLocal<>();
    private static final ThreadLocal<EntityTransaction> threadLocalTransaction = new ThreadLocal<>();
    
    static {
        try {
            // Create the EntityManagerFactory with default SQLite configuration
            initializeDefault();

            // Add shutdown hook to clean up ThreadLocal variables
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                cleanupThreadLocals();
                shutdown();
            }));
        } catch (Throwable ex) {
            logger.error("Initial EntityManagerFactory creation failed", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Initialize with default configuration
     * Checks environment variables first with robust validation, falls back to H2 persistent storage if not properly configured
     */
    public static void initializeDefault() {
        if (isEnvironmentConfigurationComplete()) {
            logger.info("Environment variables detected for database configuration");
            initialize(DatabaseConfig.createProductionConfigFromEnv());
        } else {
            logger.debug("No complete environment configuration found, using default H2 persistent storage");
            initialize(DatabaseConfig.createH2Config());
        }
    }

    /**
     * Robust validation of environment variables for database configuration
     * Ensures all required variables are present and valid based on DB_TYPE
     *
     * @return true if environment configuration is complete and valid
     */
    private static boolean isEnvironmentConfigurationComplete() {
        String dbType = System.getenv("DB_TYPE");

        // If DB_TYPE is not set or is explicitly "sqlite", no additional validation needed
        if (dbType == null || dbType.trim().isEmpty() || dbType.equalsIgnoreCase("sqlite")) {
            return false;
        }

        // For PostgreSQL and MySQL, validate all required connection parameters
        if (dbType.equalsIgnoreCase("postgresql") || dbType.equalsIgnoreCase("mysql")) {
            String host = System.getenv("DB_HOST");
            String dbName = System.getenv("DB_NAME");
            String user = System.getenv("DB_USER");
            String password = System.getenv("DB_PASSWORD");

            // All fields must be present and non-empty (password can be empty but must be set)
            if (host == null || host.trim().isEmpty()) {
                logger.warn("DB_TYPE={} but DB_HOST is not set or empty", dbType);
                return false;
            }

            if (dbName == null || dbName.trim().isEmpty()) {
                logger.warn("DB_TYPE={} but DB_NAME is not set or empty", dbType);
                return false;
            }

            if (user == null || user.trim().isEmpty()) {
                logger.warn("DB_TYPE={} but DB_USER is not set or empty", dbType);
                return false;
            }

            if (password == null) {
                logger.warn("DB_TYPE={} but DB_PASSWORD is not set (empty password is allowed but variable must exist)", dbType);
                return false;
            }

            // Optional: Validate DB_PORT if provided
            String port = System.getenv("DB_PORT");
            if (port != null && !port.trim().isEmpty()) {
                try {
                    int portNum = Integer.parseInt(port.trim());
                    if (portNum < 1 || portNum > 65535) {
                        logger.warn("DB_PORT={} is not a valid port number (1-65535)", port);
                        return false;
                    }
                } catch (NumberFormatException e) {
                    logger.warn("DB_PORT={} is not a valid integer", port);
                    return false;
                }
            }

            logger.info("Complete environment configuration found for {}", dbType);
            return true;
        }

        // Unknown DB_TYPE
        logger.warn("Unknown DB_TYPE={}, falling back to SQLite", dbType);
        return false;
    }

    /**
     * Initialize with specific database configuration
     * This allows dynamic database selection at runtime
     *
     * @param config Database configuration
     */
    public static void initialize(DatabaseConfig config) {
        initLock.lock();
        try {
            // Validate configuration
            config.validate();

            // Close existing factory if open
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                logger.info("Closing existing EntityManagerFactory before switching database");
                entityManagerFactory.close();
            }

            // Store current configuration
            currentConfig = config;

            // Get persistence unit name from config
            String persistenceUnitName = config.getPersistenceUnitName();

            // Build properties to override from config
            Map<String, Object> properties = new HashMap<>();

            // Always override connection settings from config
            properties.put("jakarta.persistence.jdbc.url", config.getDatabaseUrl());

            if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                properties.put("jakarta.persistence.jdbc.user", config.getUsername());
            }

            if (config.getPassword() != null) {
                properties.put("jakarta.persistence.jdbc.password", config.getPassword());
            }

            // Override pool settings
            properties.put("hibernate.hikari.minimumIdle", String.valueOf(config.getPoolMinSize()));
            properties.put("hibernate.hikari.maximumPoolSize", String.valueOf(config.getPoolMaxSize()));
            properties.put("hibernate.hikari.connectionTimeout", String.valueOf(config.getConnectionTimeout()));
            properties.put("hibernate.hikari.idleTimeout", String.valueOf(config.getIdleTimeout()));
            properties.put("hibernate.hikari.maxLifetime", String.valueOf(config.getMaxLifetime()));

            if (config.getPoolName() != null && !config.getPoolName().isEmpty()) {
                properties.put("hibernate.hikari.poolName", config.getPoolName());
            }

            // Override schema management
            properties.put("hibernate.hbm2ddl.auto", config.getHbm2ddlAuto());

            // Override logging settings
            properties.put("hibernate.show_sql", String.valueOf(config.isShowSql()));
            properties.put("hibernate.format_sql", String.valueOf(config.isFormatSql()));
            properties.put("hibernate.highlight_sql", String.valueOf(config.isHighlightSql()));

            // Override statistics
            properties.put("hibernate.generate_statistics", String.valueOf(config.isEnableStatistics()));

            logger.info("Initializing EntityManagerFactory with persistence unit: {}", persistenceUnitName);
            logger.debug("Database configuration:\n{}", config.getSummary());

            // Create EntityManagerFactory with overridden properties
            entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName, properties);

            logger.info("✅ EntityManagerFactory initialized successfully for {}", config.getDatabaseType());

        } catch (Exception e) {
            logger.error("❌ Failed to initialize EntityManagerFactory: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed", e);
        } finally {
            initLock.unlock();
        }
    }

    /**
     * Get current database configuration
     * @return Current DatabaseConfig or null if not initialized with config
     */
    public static DatabaseConfig getCurrentConfig() {
        return currentConfig;
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
                logger.debug("🔄 Beginning transaction");
                beginTransaction();
            }

            T result = callback.execute(getEntityManager());

            if (shouldManageTransaction) {
                logger.debug("✅ Committing transaction");
                commitTransaction();
                logger.debug("✅ Transaction committed successfully");
            }

            return result;
        } catch (Exception e) {
            if (shouldManageTransaction) {
                logger.warn("❌ Rolling back transaction due to exception: {}", e.getMessage());
                rollbackTransaction();
                logger.debug("❌ Transaction rolled back");
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
    /**
     * Clean up ThreadLocal variables to prevent memory leaks
     * This method should be called when threads are done with database operations
     */
    public static void cleanupThreadLocals() {
        try {
            EntityManager em = threadLocalEntityManager.get();
            if (em != null && em.isOpen()) {
                try {
                    EntityTransaction tx = threadLocalTransaction.get();
                    if (tx != null && tx.isActive()) {
                        tx.rollback(); // Rollback any active transaction
                    }
                } catch (Exception e) {
                    logger.debug("Exception during transaction cleanup", e);
                }
                try {
                    em.close();
                } catch (Exception e) {
                    logger.debug("Exception during EntityManager cleanup", e);
                }
            }
        } catch (Exception e) {
            logger.debug("Exception during ThreadLocal cleanup", e);
        } finally {
            // Always remove ThreadLocal variables to prevent memory leaks
            threadLocalEntityManager.remove();
            threadLocalTransaction.remove();
        }
    }
    
    /**
     * Force cleanup of all thread-local variables (for testing)
     */
    public static void forceCleanupAllThreadLocals() {
        cleanupThreadLocals();
        logger.info("🧹 Forced cleanup of all ThreadLocal variables completed");
    }

    /**
     * Closes all active database connections from the connection pool.
     *
     * <p>This method is specifically designed for operations like SQLite VACUUM that require
     * exclusive database access. It closes all EntityManagers and forces the underlying
     * connection pool (HikariCP) to close all its connections.
     *
     * <p><strong>⚠️ Warning:</strong> This is a destructive operation that will close
     * ALL database connections. Use only when necessary (e.g., before VACUUM operations).
     *
     * <p><strong>Implementation:</strong>
     * <ol>
     *   <li>Closes all thread-local EntityManagers</li>
     *   <li>Forces cleanup of ThreadLocal variables</li>
     *   <li>Shuts down and reinitializes the EntityManagerFactory (which closes the pool)</li>
     * </ol>
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * // Before SQLite VACUUM operation
     * JPAUtil.closeAllConnections();
     *
     * // Now VACUUM can acquire exclusive lock
     * EntityManager em = JPAUtil.getEntityManager();
     * em.createNativeQuery("VACUUM").executeUpdate();
     * em.close();
     * }</pre>
     *
     * @since 1.0.5
     * @see #closeEntityManager()
     * @see #forceCleanupAllThreadLocals()
     */
    public static void closeAllConnections() {
        initLock.lock();
        try {
            logger.info("🔌 Closing all database connections...");

            // Step 1: Close current thread's EntityManager
            closeEntityManager();

            // Step 2: Force cleanup of all ThreadLocal variables
            forceCleanupAllThreadLocals();

            // Step 3: Close and reinitialize EntityManagerFactory
            // This forces HikariCP to close all pooled connections
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                DatabaseConfig savedConfig = currentConfig;

                // Close the factory (closes the connection pool)
                entityManagerFactory.close();
                logger.info("  └─ EntityManagerFactory closed");

                // Reinitialize with same configuration
                if (savedConfig != null) {
                    initialize(savedConfig);
                    logger.info("  └─ EntityManagerFactory reinitialized");
                } else {
                    // Fallback to default if no config was set
                    logger.warn("  └─ No configuration found, using default");
                    initializeDefault();
                }
            }

            logger.info("✅ All database connections closed and pool reinitialized");

        } finally {
            initLock.unlock();
        }
    }

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