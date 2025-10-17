package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;

import java.security.KeyPair;

/**
 * Demonstration of database-agnostic configuration
 *
 * Shows how to switch between different databases (SQLite, H2, PostgreSQL, MySQL)
 * using DatabaseConfig and JPAUtil
 */
public class DatabaseConfigDemo {

    public static void main(String[] args) {
        System.out.println("📊 Database Configuration Demo - Multi-Database Support");
        System.out.println("=".repeat(70));
        System.out.println();

        try {
            // Initialize with SQLite for this demo (instead of default H2)
            System.out.println("ℹ️  Initializing with SQLite for demonstration purposes...");
            // Close any existing EntityManagerFactory first
            JPAUtil.closeEntityManager();
            // Now initialize with SQLite
            JPAUtil.initialize(DatabaseConfig.createSQLiteConfig());
            System.out.println();

            // Demo 1: SQLite configuration (now active)
            demonstrateSQLiteConfig();

            System.out.println();

            // Demo 2: H2 in-memory for testing
            demonstrateH2Config();

            System.out.println();

            // Demo 3: Configuration summaries
            demonstrateConfigurationSummaries();

            System.out.println();

            // Demo 4: Production configuration from environment
            demonstrateProductionConfig();

            System.out.println();
            System.out.println("✅ Demo completed successfully!");
            System.out.println("\n💡 Note: PostgreSQL and MySQL demos require actual database servers");
            System.out.println("   Set environment variables DB_TYPE, DB_HOST, DB_USER, DB_PASSWORD to test");

        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup
            JPAUtil.closeEntityManager();
        }
    }

    /**
     * Demo 1: SQLite configuration (explicitly initialized)
     */
    private static void demonstrateSQLiteConfig() {
        System.out.println("📋 Demo 1: SQLite Configuration");
        System.out.println("-".repeat(70));

        // SQLite is now initialized explicitly (H2 is the default since v1.0.5)
        DatabaseConfig currentConfig = JPAUtil.getCurrentConfig();

        System.out.println("Current database: " + currentConfig.getDatabaseType());
        System.out.println("Persistence unit: " + currentConfig.getPersistenceUnitName());
        System.out.println("Pool size: " + currentConfig.getPoolMinSize() +
                          " - " + currentConfig.getPoolMaxSize());
        System.out.println("✅ SQLite is ready (single writer, ideal for development)");

        // Test basic blockchain operation
        System.out.println("\n🔗 Testing blockchain operation with SQLite...");
        testBlockchainOperation();
    }

    /**
     * Demo 2: H2 in-memory configuration
     */
    private static void demonstrateH2Config() {
        System.out.println("📋 Demo 2: H2 In-Memory Configuration (Testing)");
        System.out.println("-".repeat(70));

        // Switch to H2 for testing
        DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
        System.out.println("Switching to H2 in-memory database...");
        JPAUtil.initialize(h2Config);

        System.out.println("✅ H2 database initialized");
        System.out.println("Database type: " + h2Config.getDatabaseType());
        System.out.println("Persistence unit: " + h2Config.getPersistenceUnitName());
        System.out.println("Schema management: " + h2Config.getHbm2ddlAuto() +
                          " (tables created automatically)");
        System.out.println("Pool size: " + h2Config.getPoolMinSize() +
                          " - " + h2Config.getPoolMaxSize());

        // Test blockchain operation with H2
        System.out.println("\n🔗 Testing blockchain operation with H2...");
        testBlockchainOperation();

        // Switch back to SQLite
        System.out.println("\n🔄 Switching back to SQLite...");
        JPAUtil.initialize(DatabaseConfig.createSQLiteConfig());
        System.out.println("✅ Back to SQLite");
    }

    /**
     * Demo 3: Show different configuration summaries
     */
    private static void demonstrateConfigurationSummaries() {
        System.out.println("📋 Demo 3: Configuration Summaries");
        System.out.println("-".repeat(70));

        // SQLite Configuration
        System.out.println("\n1️⃣  SQLite Configuration:");
        System.out.println("-".repeat(50));
        DatabaseConfig sqliteConfig = DatabaseConfig.createSQLiteConfig();
        System.out.println(sqliteConfig.getSummary());

        // PostgreSQL Configuration
        System.out.println("\n2️⃣  PostgreSQL Configuration:");
        System.out.println("-".repeat(50));
        DatabaseConfig pgConfig = DatabaseConfig.createPostgreSQLConfig(
            "localhost", "blockchain_prod", "blockchain_user", "secure_password"
        );
        System.out.println(pgConfig.getSummary());

        // MySQL Configuration
        System.out.println("\n3️⃣  MySQL Configuration:");
        System.out.println("-".repeat(50));
        DatabaseConfig mysqlConfig = DatabaseConfig.createMySQLConfig(
            "localhost", "blockchain_prod", "blockchain_user", "secure_password"
        );
        System.out.println(mysqlConfig.getSummary());

        // H2 Test Configuration
        System.out.println("\n4️⃣  H2 Test Configuration:");
        System.out.println("-".repeat(50));
        DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
        System.out.println(h2Config.getSummary());

        // Development Configuration
        System.out.println("\n5️⃣  Development Configuration (with SQL logging):");
        System.out.println("-".repeat(50));
        DatabaseConfig devConfig = DatabaseConfig.createDevelopmentConfig();
        System.out.println(devConfig.getSummary());
    }

    /**
     * Demo 4: Production configuration from environment variables
     */
    private static void demonstrateProductionConfig() {
        System.out.println("📋 Demo 4: Production Configuration from Environment");
        System.out.println("-".repeat(70));

        String dbType = System.getenv("DB_TYPE");
        if (dbType != null && !dbType.isEmpty()) {
            System.out.println("Environment variable DB_TYPE detected: " + dbType);
            System.out.println("Creating production configuration...");

            DatabaseConfig prodConfig = DatabaseConfig.createProductionConfigFromEnv();
            System.out.println("\n" + prodConfig.getSummary());

            System.out.println("\n✅ Production configuration loaded from environment");
        } else {
            System.out.println("ℹ️  No DB_TYPE environment variable set");
            System.out.println("   To test production config, set these environment variables:");
            System.out.println("   - DB_TYPE=postgresql (or mysql, sqlite)");
            System.out.println("   - DB_HOST=localhost");
            System.out.println("   - DB_PORT=5432 (or 3306 for MySQL)");
            System.out.println("   - DB_NAME=blockchain_prod");
            System.out.println("   - DB_USER=blockchain_user");
            System.out.println("   - DB_PASSWORD=your_password");

            System.out.println("\n📌 Example:");
            System.out.println("   export DB_TYPE=postgresql");
            System.out.println("   export DB_HOST=prod-db.example.com");
            System.out.println("   export DB_NAME=blockchain_prod");
            System.out.println("   export DB_USER=blockchain_user");
            System.out.println("   export DB_PASSWORD=secure_password");
        }
    }

    /**
     * Test basic blockchain operation with current database
     */
    private static void testBlockchainOperation() {
        try {
            // Create blockchain instance
            Blockchain blockchain = new Blockchain();

            // Generate test keys
            KeyPair keys = CryptoUtil.generateKeyPair();
            String publicKeyStr = CryptoUtil.publicKeyToString(keys.getPublic());

            // Add authorized key
            blockchain.addAuthorizedKey(publicKeyStr, "TestUser");

            // Add a test block
            String testData = "Test data - " + System.currentTimeMillis();
            boolean added = blockchain.addBlock(testData, keys.getPrivate(), keys.getPublic());

            if (added) {
                System.out.println("   ✅ Block added successfully");
                System.out.println("   📦 Database connection working correctly");
            } else {
                System.out.println("   ❌ Failed to add block");
            }

        } catch (Exception e) {
            System.out.println("   ⚠️  Blockchain operation: " + e.getMessage());
        }
    }
}
