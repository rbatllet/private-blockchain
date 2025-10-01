package demo;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.config.JPAConfigurationStorage;
import com.rbatllet.blockchain.util.JPAUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstration of database-agnostic JPA configuration storage
 * Shows how JPAConfigurationStorage works with any database (SQLite, PostgreSQL, MySQL, H2)
 */
public class JPAConfigurationStorageDemo {

    public static void main(String[] args) {
        System.out.println("📊 JPA Configuration Storage Demo - Database Agnostic");
        System.out.println("=".repeat(70));
        System.out.println();

        try {
            // Demo 1: SQLite configuration storage
            demonstrateSQLiteStorage();

            System.out.println();

            // Demo 2: H2 configuration storage (same code, different database!)
            demonstrateH2Storage();

            System.out.println();

            // Demo 3: Configuration operations
            demonstrateConfigurationOperations();

            System.out.println();
            System.out.println("✅ Demo completed successfully!");
            System.out.println("\n💡 Note: Same code works with PostgreSQL, MySQL, H2, and SQLite!");

        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            JPAUtil.closeEntityManager();
        }
    }

    /**
     * Demo 1: Configuration storage with SQLite
     */
    private static void demonstrateSQLiteStorage() {
        System.out.println("📋 Demo 1: Configuration Storage with SQLite");
        System.out.println("-".repeat(70));

        // Initialize with SQLite (default)
        DatabaseConfig sqliteConfig = DatabaseConfig.createSQLiteConfig();
        JPAUtil.initialize(sqliteConfig);

        JPAConfigurationStorage storage = new JPAConfigurationStorage();

        System.out.println("Storage type: " + storage.getStorageType());
        System.out.println("Storage location: " + storage.getStorageLocation());
        System.out.println("Health check: " + (storage.isHealthy() ? "✅ Healthy" : "❌ Unhealthy"));

        // Save configuration
        Map<String, String> config = new HashMap<>();
        config.put("app.name", "PrivateBlockchain");
        config.put("app.version", "1.0.0");
        config.put("app.environment", "development");

        boolean saved = storage.saveConfiguration("application", config);
        System.out.println("\n📝 Configuration saved: " + (saved ? "✅ Success" : "❌ Failed"));

        // Load configuration
        Map<String, String> loaded = storage.loadConfiguration("application");
        System.out.println("📥 Configuration loaded: " + loaded.size() + " entries");
        loaded.forEach((key, value) -> System.out.println("   " + key + " = " + value));
    }

    /**
     * Demo 2: Configuration storage with H2 (same code, different database!)
     */
    private static void demonstrateH2Storage() {
        System.out.println("📋 Demo 2: Configuration Storage with H2 (Same Code!)");
        System.out.println("-".repeat(70));

        // Switch to H2
        DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
        JPAUtil.initialize(h2Config);

        JPAConfigurationStorage storage = new JPAConfigurationStorage();

        System.out.println("Storage type: " + storage.getStorageType());
        System.out.println("Storage location: " + storage.getStorageLocation());
        System.out.println("Health check: " + (storage.isHealthy() ? "✅ Healthy" : "❌ Unhealthy"));

        // Same configuration code as before
        Map<String, String> config = new HashMap<>();
        config.put("app.name", "PrivateBlockchain");
        config.put("app.version", "1.0.0");
        config.put("app.environment", "testing");

        boolean saved = storage.saveConfiguration("application", config);
        System.out.println("\n📝 Configuration saved: " + (saved ? "✅ Success" : "❌ Failed"));

        // Load configuration
        Map<String, String> loaded = storage.loadConfiguration("application");
        System.out.println("📥 Configuration loaded: " + loaded.size() + " entries");
        loaded.forEach((key, value) -> System.out.println("   " + key + " = " + value));

        System.out.println("\n✨ Notice: Exact same code, different database backend!");
    }

    /**
     * Demo 3: Advanced configuration operations
     */
    private static void demonstrateConfigurationOperations() {
        System.out.println("📋 Demo 3: Advanced Configuration Operations");
        System.out.println("-".repeat(70));

        JPAConfigurationStorage storage = new JPAConfigurationStorage();

        // Set individual value
        System.out.println("\n1️⃣  Setting individual values:");
        storage.setConfigurationValue("database", "host", "localhost");
        storage.setConfigurationValue("database", "port", "5432");
        storage.setConfigurationValue("database", "name", "blockchain_prod");

        // Get individual value
        String host = storage.getConfigurationValue("database", "host");
        System.out.println("   database.host = " + host);

        // Check if configuration exists
        System.out.println("\n2️⃣  Configuration existence check:");
        System.out.println("   'database' exists: " + storage.configurationExists("database"));
        System.out.println("   'nonexistent' exists: " + storage.configurationExists("nonexistent"));

        // Load all values
        System.out.println("\n3️⃣  Loading all database configuration:");
        Map<String, String> dbConfig = storage.loadConfiguration("database");
        dbConfig.forEach((key, value) -> System.out.println("   " + key + " = " + value));

        // Delete individual value
        System.out.println("\n4️⃣  Deleting configuration value:");
        boolean deleted = storage.deleteConfigurationValue("database", "port");
        System.out.println("   Deleted 'port': " + (deleted ? "✅ Success" : "❌ Failed"));

        // Show updated configuration
        Map<String, String> updated = storage.loadConfiguration("database");
        System.out.println("   Updated configuration (" + updated.size() + " entries):");
        updated.forEach((key, value) -> System.out.println("   " + key + " = " + value));

        // Audit log
        System.out.println("\n5️⃣  Audit log (last 10 changes):");
        String auditLog = storage.getAuditLog("database", 10);
        System.out.println(auditLog);

        // Reset configuration
        System.out.println("6️⃣  Resetting configuration:");
        boolean reset = storage.resetConfiguration("database");
        System.out.println("   Reset 'database': " + (reset ? "✅ Success" : "❌ Failed"));
        System.out.println("   Configuration exists after reset: " + storage.configurationExists("database"));
    }
}
