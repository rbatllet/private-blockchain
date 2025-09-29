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

            // 2. Initialize advanced search system BEFORE storing data
            System.out.println("2️⃣ Initializing advanced search system...");

            // Generate realistic department-specific passwords (enterprise scenario)
            String medicalPassword = "Medical_Dept_2024!SecureKey_" + api.generateSecurePassword(12);
            String financialPassword = "Finance_Dept_2024!SecureKey_" + api.generateSecurePassword(12);
            String legalPassword = "Legal_Dept_2024!SecureKey_" + api.generateSecurePassword(12);
            String generalPassword = "IT_Dept_2024!SecureKey_" + api.generateSecurePassword(12);

            System.out.println("🔑 Generated department-specific passwords:");
            System.out.println("   🏥 Medical Department password created");
            System.out.println("   💰 Finance Department password created");
            System.out.println("   ⚖️ Legal Department password created");
            System.out.println("   🔐 IT Department password created");

            // EFFICIENT APPROACH: Initialize search ONCE with single password (like successful tests)
            System.out.println("ℹ️  For demo purposes, initializing with primary password");
            // Use medical password as primary for demonstration
            // NOTE: This will cause some 'Tag mismatch' errors when trying to decrypt
            // blocks from other departments - this is EXPECTED and shows security working
            blockchain.initializeAdvancedSearch(medicalPassword);
            System.out.println("✅ Search system initialized with medical department password!");
            System.out.println("ℹ️  Expected behavior: Some 'Tag mismatch' when accessing other departments' data");

            // 3. Store different types of sensitive data
            System.out.println("3️⃣ Storing sensitive data...");

            // Store patient data with user-defined search terms
            String medicalData =
                "Patient John Doe, DOB: 1985-03-15, Diagnosis: Hypertension, Treatment: Lisinopril 10mg daily, Blood pressure: 145/95";
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
            Block secretBlock = api.storeSecret(secretData, generalPassword);
            System.out.println(
                "🔐 Secret data stored: Block #" + secretBlock.getBlockNumber()
            );
            System.out.println();


            // 4. Demonstrate blockchain status
            System.out.println("4️⃣ Blockchain status...");
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

            // 5. Department-specific search operations (realistic enterprise scenario)
            System.out.println(
                "5️⃣ Department-specific privacy-preserving search..."
            );

            // Medical Department: Search with medical password
            var patientRecords = api.searchAndDecryptByTerms(
                new String[] { "patient" },
                medicalPassword,
                10
            );
            System.out.println(
                "🏥 Medical Dept found " + patientRecords.size() + " patient record(s)"
            );

            // Finance Department: Search with financial password
            var accountRecords = api.searchAndDecryptByTerms(
                new String[] { "account" },
                financialPassword,
                10
            );
            System.out.println(
                "💰 Finance Dept found " + accountRecords.size() + " account record(s)"
            );

            // Legal Department: Search with legal password
            var contractDocuments = api.searchAndDecryptByTerms(
                new String[] { "contract" },
                legalPassword,
                10
            );
            System.out.println(
                "⚖️ Legal Dept found " + contractDocuments.size() + " contract document(s)"
            );

            // Cross-department search: Finance data contains "2025"
            var blocks2025 = api.searchAndDecryptByTerms(
                new String[] { "2025" },
                financialPassword,
                10
            );
            System.out.println(
                "📅 Found " + blocks2025.size() + " block(s) from 2025 (Finance Dept)"
            );
            System.out.println();

            // 6. Department-specific data retrieval operations
            System.out.println("6️⃣ Department-specific data retrieval...");

            // Medical Department: Retrieve with medical password
            String retrievedMedical = api.retrieveSecret(
                medicalBlock.getId(),
                medicalPassword
            );
            System.out.println(
                "🏥 Medical record retrieved (Medical Dept): " +
                (retrievedMedical != null
                        ? retrievedMedical.substring(
                            0,
                            Math.min(50, retrievedMedical.length())
                        ) +
                        "..."
                        : "FAILED")
            );

            // Finance Department: Retrieve with financial password
            String retrievedFinancial = api.retrieveSecret(
                financialBlock.getId(),
                financialPassword
            );
            System.out.println(
                "💰 Financial record retrieved (Finance Dept): " +
                (retrievedFinancial != null
                        ? retrievedFinancial.substring(
                            0,
                            Math.min(50, retrievedFinancial.length())
                        ) +
                        "..."
                        : "FAILED")
            );

            // Security test: Try to access medical data with finance password (should fail)
            String crossDepartmentAccess = api.retrieveSecret(
                medicalBlock.getId(),
                financialPassword
            );
            System.out.println(
                "🔒 Cross-department security test (Finance trying Medical): " +
                (crossDepartmentAccess == null
                        ? "CORRECTLY BLOCKED ✅"
                        : "SECURITY BREACH ❌")
            );

            // Try with completely wrong password (should fail)
            String wrongPasswordResult = api.retrieveSecret(
                secretBlock.getId(),
                "wrongpassword"
            );
            System.out.println(
                "❌ Wrong password test: " +
                (wrongPasswordResult == null
                        ? "CORRECTLY FAILED ✅"
                        : "SECURITY ISSUE ❌")
            );
            System.out.println();

            // 7. Department-specific advanced search with decryption
            System.out.println("7️⃣ Department-specific advanced search with decryption...");

            // Medical Department: Search for medical-specific terms
            var decryptedMedical = api.searchAndDecryptByTerms(
                new String[] { "hypertension" },
                medicalPassword,
                10
            );
            System.out.println(
                "🔍 Medical Dept found " +
                decryptedMedical.size() +
                " medical record(s) containing 'Hypertension'"
            );

            // Finance Department: Search for financial-specific terms
            var decryptedFinancial = api.searchAndDecryptByTerms(
                new String[] { "transaction" },
                financialPassword,
                10
            );
            System.out.println(
                "🔍 Finance Dept found " +
                decryptedFinancial.size() +
                " financial record(s) containing 'Transaction'"
            );
            System.out.println();

            // 8. Advanced search demonstrations
            System.out.println("8️⃣ Advanced search (all blockchain data)...");

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

            // Advanced Search: Search everything (public data search)
            List<Block> publicSearch = api.searchEverything("announcement");
            System.out.println("🔍 Advanced search for 'announcement' (public data):");
            System.out.println("Found " + publicSearch.size() + " blocks");

            // Advanced Search: Search encrypted data with IT department password
            List<Block> passwordSearch = api.searchEverythingWithPassword(
                "API Key",
                generalPassword
            );
            System.out.println("🔍 Advanced search for 'API Key' with IT department password:");
            System.out.println(
                "Found " + passwordSearch.size() + " encrypted block(s) with full access"
            );
            System.out.println();

            // 9. Validation
            System.out.println("9️⃣ Blockchain validation...");
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

            // 🔟 Summary
            System.out.println("🔟 Demo summary...");
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
