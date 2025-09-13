package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import java.security.KeyPair;
import java.util.List;

/**
 * Demo showcasing the user-friendly encryption API
 * Shows how easy it is to work with encrypted blockchain data
 */
public class UserFriendlyEncryptionDemo {

    public static void main(String[] args) {
        System.out.println("=== 🔐 USER-FRIENDLY ENCRYPTION API DEMO ===\n");

        try {
            // 1. Setup - Create blockchain and user-friendly API
            System.out.println(
                "1️⃣ Setting up blockchain and user-friendly API..."
            );
            Blockchain blockchain = new Blockchain();
            UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(
                blockchain
            );

            // Create a user with generated keys
            KeyPair userKeys = api.createUser("Dr. Alice");
            api.setDefaultCredentials("Dr. Alice", userKeys);
            System.out.println(
                "✅ User 'Dr. Alice' created with generated keys"
            );
            System.out.println("✅ API ready for use\n");

            // 2. Store different types of sensitive data
            System.out.println("2️⃣ Storing sensitive data...");

            // Store patient data with user-defined search terms
            String medicalData =
                "Patient John Doe, DOB: 1985-03-15, Diagnosis: Hypertension, Treatment: Lisinopril 10mg daily, Blood pressure: 145/95";
            String medicalPassword = api.generateSecurePassword(16);
            String[] medicalTerms = {
                "patient",
                "hypertension",
                "lisinopril",
                "PATIENT_001",
            };
            Block medicalBlock = api.storeSearchableData(
                medicalData,
                medicalPassword,
                medicalTerms
            );
            System.out.println(
                "🏥 Patient record stored: Block #" +
                medicalBlock.getBlockNumber()
            );

            // Store account data with user-defined search terms
            String financialData =
                "Account: CHK_123456789, Balance: $45,230.50, Transaction: Mortgage payment $2,100.00, Date: 2025-01-15";
            String financialPassword = api.generateSecurePassword(16);
            String[] financialTerms = {
                "account",
                "balance",
                "transaction",
                "ACC_123456789",
            };
            Block financialBlock = api.storeSearchableData(
                financialData,
                financialPassword,
                financialTerms
            );
            System.out.println(
                "💰 Account record stored: Block #" +
                financialBlock.getBlockNumber()
            );

            // Store contract data with user-defined search terms
            String legalData =
                "Contract Agreement between ABC Corp and XYZ Ltd. Terms: 3-year licensing deal, Annual fee: $50,000, Jurisdiction: New York";
            String legalPassword = api.generateSecurePassword(16);
            String[] legalTerms = {
                "contract",
                "agreement",
                "licensing",
                "CASE_2025_001",
            };
            Block legalBlock = api.storeSearchableData(
                legalData,
                legalPassword,
                legalTerms
            );
            System.out.println(
                "⚖️ Contract document stored: Block #" +
                legalBlock.getBlockNumber()
            );

            // Store a generic secret
            String secretData =
                "API Key: sk_live_123abc456def789ghi, Database URL: postgresql://user:pass@db.example.com:5432/prod";
            String secretPassword = api.generateSecurePassword(16);
            Block secretBlock = api.storeSecret(secretData, secretPassword);
            System.out.println(
                "🔐 Secret data stored: Block #" + secretBlock.getBlockNumber()
            );
            System.out.println();

            // Initialize advanced search for indexed data (required for search functionality)
            System.out.println("🔄 Initializing advanced search system...");
            // Get the blockchain instance and initialize search with all stored passwords
            blockchain.initializeAdvancedSearch(medicalPassword);
            blockchain.initializeAdvancedSearch(financialPassword);
            blockchain.initializeAdvancedSearch(legalPassword);
            blockchain.initializeAdvancedSearch(secretPassword);
            System.out.println(
                "✅ Advanced search system initialized and ready!"
            );
            System.out.println();

            // 3. Demonstrate blockchain status
            System.out.println("3️⃣ Blockchain status...");
            System.out.println("📊 Blockchain summary:");
            System.out.println(api.getBlockchainSummary());
            System.out.println(
                "🔒 Has encrypted data: " + api.hasEncryptedData()
            );
            System.out.println(
                "🔐 Encrypted blocks: " + api.getEncryptedBlockCount()
            );
            System.out.println(
                "📖 Unencrypted blocks: " + api.getUnencryptedBlockCount()
            );
            System.out.println();

            // 4. Search operations (metadata only - privacy preserved)
            System.out.println(
                "4️⃣ Privacy-preserving search (metadata only)..."
            );

            // Use searchAndDecryptByTerms for proper search functionality
            var patientRecords = api.searchAndDecryptByTerms(
                new String[] { "patient" },
                medicalPassword,
                10
            );
            System.out.println(
                "🏥 Found " + patientRecords.size() + " patient record(s)"
            );

            var accountRecords = api.searchAndDecryptByTerms(
                new String[] { "account" },
                financialPassword,
                10
            );
            System.out.println(
                "💰 Found " + accountRecords.size() + " account record(s)"
            );

            var contractDocuments = api.searchAndDecryptByTerms(
                new String[] { "contract" },
                legalPassword,
                10
            );
            System.out.println(
                "⚖️ Found " + contractDocuments.size() + " contract document(s)"
            );

            // Search by year (metadata search) - this searches in content, not metadata
            var blocks2025 = api.searchAndDecryptByTerms(
                new String[] { "2025" },
                financialPassword,
                10
            );
            System.out.println(
                "📅 Found " + blocks2025.size() + " block(s) from 2025"
            );
            System.out.println();

            // 5. Retrieval operations
            System.out.println("5️⃣ Retrieving encrypted data...");

            // Retrieve medical record
            String retrievedMedical = api.retrieveSecret(
                medicalBlock.getId(),
                medicalPassword
            );
            System.out.println(
                "🏥 Medical record retrieved: " +
                (retrievedMedical != null
                        ? retrievedMedical.substring(
                            0,
                            Math.min(50, retrievedMedical.length())
                        ) +
                        "..."
                        : "FAILED")
            );

            // Retrieve financial record
            String retrievedFinancial = api.retrieveSecret(
                financialBlock.getId(),
                financialPassword
            );
            System.out.println(
                "💰 Financial record retrieved: " +
                (retrievedFinancial != null
                        ? retrievedFinancial.substring(
                            0,
                            Math.min(50, retrievedFinancial.length())
                        ) +
                        "..."
                        : "FAILED")
            );

            // Try with wrong password (should fail - stack trace expected)
            String wrongPasswordResult = api.retrieveSecret(
                secretBlock.getId(),
                "wrongpassword"
            );
            System.out.println(
                "❌ Wrong password test: " +
                (wrongPasswordResult == null
                        ? "CORRECTLY FAILED"
                        : "SECURITY ISSUE")
            );
            System.out.println();

            // 6. Search with decryption
            System.out.println("6️⃣ Advanced search with decryption...");

            // Search for medical data with decryption using keyword search
            var decryptedMedical = api.searchAndDecryptByTerms(
                new String[] { "hypertension" },
                medicalPassword,
                10
            );
            System.out.println(
                "🔍 Found " +
                decryptedMedical.size() +
                " medical record(s) containing 'Hypertension'"
            );

            // Search for financial data with decryption using keyword search
            var decryptedFinancial = api.searchAndDecryptByTerms(
                new String[] { "transaction" },
                financialPassword,
                10
            );
            System.out.println(
                "🔍 Found " +
                decryptedFinancial.size() +
                " financial record(s) containing 'Mortgage'"
            );
            System.out.println();

            // 7. Advanced search demonstrations
            System.out.println("7️⃣ Advanced search (all blockchain data)...");

            // Add some public data for comparison
            blockchain.addBlock(
                "Public announcement: New blockchain features available",
                userKeys.getPrivate(),
                userKeys.getPublic()
            );
            blockchain.addBlock(
                "System maintenance completed successfully",
                userKeys.getPrivate(),
                userKeys.getPublic()
            );

            // Advanced Search: Search everything
            List<Block> publicSearch = api.searchEverything("blockchain");
            System.out.println("🔍 Advanced search for 'blockchain':");
            System.out.println("Found " + publicSearch.size() + " blocks");

            // Advanced Search: Search with password
            List<Block> passwordSearch = api.searchEverythingWithPassword(
                "API",
                secretPassword
            );
            System.out.println("🔍 Advanced search for 'API' with password:");
            System.out.println(
                "Found " + passwordSearch.size() + " blocks with full access"
            );
            System.out.println();

            // 8. Validation
            System.out.println("8️⃣ Blockchain validation...");
            boolean isValid = api.validateEncryptedBlocks();
            System.out.println(
                "✅ Blockchain validation: " + (isValid ? "PASSED" : "FAILED")
            );

            if (isValid) {
                System.out.println(
                    "🔐 All encrypted blocks are secure and intact"
                );
            }
            System.out.println();

            // 9. Summary
            System.out.println("9️⃣ Demo summary...");
            System.out.println("✅ Successfully demonstrated:");
            System.out.println(
                "   • Easy storage of different types of encrypted data"
            );
            System.out.println(
                "   • Automatic user management and key generation"
            );
            System.out.println("   • Privacy-preserving metadata search");
            System.out.println(
                "   • Secure retrieval with password protection"
            );
            System.out.println(
                "   • Advanced search with decryption capabilities"
            );
            System.out.println(
                "   • Advanced search across encrypted and public data"
            );
            System.out.println(
                "   • Blockchain validation and integrity checking"
            );
            System.out.println();
            System.out.println(
                "🎉 User-Friendly Encryption API Demo completed successfully!"
            );
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
