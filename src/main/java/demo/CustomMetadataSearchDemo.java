package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CUSTOM METADATA SEARCH DEMONSTRATION
 *
 * Showcases the powerful custom metadata search capabilities that allow
 * querying blockchain blocks by their structured JSON metadata fields.
 *
 * Features demonstrated:
 * - Substring search in custom metadata (case-insensitive)
 * - Exact key-value pair matching
 * - Complex multi-criteria queries with AND logic
 * - Real-world use cases (medical, legal, e-commerce)
 */
public class CustomMetadataSearchDemo {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            System.out.println("🔍 CUSTOM METADATA SEARCH DEMO");
            System.out.println("================================");

            // Setup
            Blockchain blockchain = new Blockchain();

            // Load genesis admin keys
            KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
            api.setDefaultCredentials("GENESIS_ADMIN", genesisKeys);

            // Create demo user (authorized by genesis admin)
            KeyPair keyPair = api.createUser("demo_user");
            api.setDefaultCredentials("demo_user", keyPair);

            // Demo scenarios
            demonstrateBasicSubstringSearch(api, blockchain, keyPair);
            demonstrateKeyValueSearch(api, blockchain, keyPair);
            demonstrateMultipleCriteriaSearch(api, blockchain, keyPair);
            demonstrateMedicalDashboard(api, blockchain, keyPair);
            demonstrateLegalDocumentManagement(api, blockchain, keyPair);
            demonstrateECommerceTracking(api, blockchain, keyPair);

            System.out.println("\n✅ Custom Metadata Search Demo completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Force exit to stop background threads
        System.exit(0);
    }

    private static void demonstrateBasicSubstringSearch(
            UserFriendlyEncryptionAPI api,
            Blockchain blockchain,
            KeyPair keyPair) throws Exception {

        System.out.println("\n📋 DEMO 1: BASIC SUBSTRING SEARCH");
        System.out.println("==================================");
        System.out.println("ℹ️  Search across all metadata fields using substring matching (case-insensitive)");

        // Store blocks with different metadata
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("department", "cardiology");
        metadata1.put("priority", "high");
        metadata1.put("status", "urgent");

        Block block1 = blockchain.addBlockAndReturn("Medical data 1", keyPair.getPrivate(), keyPair.getPublic());
        block1.setCustomMetadata(jsonMapper.writeValueAsString(metadata1));
        blockchain.updateBlock(block1);
        System.out.println("✅ Stored cardiology block #" + block1.getBlockNumber());

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("department", "neurology");
        metadata2.put("priority", "low");
        metadata2.put("status", "routine");

        Block block2 = blockchain.addBlockAndReturn("Medical data 2", keyPair.getPrivate(), keyPair.getPublic());
        block2.setCustomMetadata(jsonMapper.writeValueAsString(metadata2));
        blockchain.updateBlock(block2);
        System.out.println("✅ Stored neurology block #" + block2.getBlockNumber());

        // Search by substring
        System.out.println("\n🔍 Searching for 'cardiology'...");
        List<Block> cardiologyResults = api.searchByCustomMetadata("cardiology");
        System.out.println("   📊 Found " + cardiologyResults.size() + " block(s) with 'cardiology'");

        System.out.println("\n🔍 Searching for 'URGENT' (case-insensitive)...");
        List<Block> urgentResults = api.searchByCustomMetadata("URGENT");
        System.out.println("   📊 Found " + urgentResults.size() + " block(s) with 'urgent'");

        System.out.println("\n🔍 Searching for 'priority' (appears in both blocks)...");
        List<Block> priorityResults = api.searchByCustomMetadata("priority");
        System.out.println("   📊 Found " + priorityResults.size() + " block(s) with 'priority'");

        System.out.println("\n💡 Key benefit: Quick text-based search across all metadata fields");
    }

    private static void demonstrateKeyValueSearch(
            UserFriendlyEncryptionAPI api,
            Blockchain blockchain,
            KeyPair keyPair) throws Exception {

        System.out.println("\n📋 DEMO 2: EXACT KEY-VALUE PAIR SEARCH");
        System.out.println("=======================================");
        System.out.println("ℹ️  Search for blocks with specific field values (more precise)");

        // Store blocks with structured metadata
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("document_type", "contract");
        metadata1.put("status", "approved");
        metadata1.put("value", 50000);

        Block block1 = blockchain.addBlockAndReturn("Contract 1", keyPair.getPrivate(), keyPair.getPublic());
        block1.setCustomMetadata(jsonMapper.writeValueAsString(metadata1));
        blockchain.updateBlock(block1);
        System.out.println("✅ Stored approved contract #" + block1.getBlockNumber() + " ($50,000)");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("document_type", "contract");
        metadata2.put("status", "pending");
        metadata2.put("value", 75000);

        Block block2 = blockchain.addBlockAndReturn("Contract 2", keyPair.getPrivate(), keyPair.getPublic());
        block2.setCustomMetadata(jsonMapper.writeValueAsString(metadata2));
        blockchain.updateBlock(block2);
        System.out.println("✅ Stored pending contract #" + block2.getBlockNumber() + " ($75,000)");

        Map<String, Object> metadata3 = new HashMap<>();
        metadata3.put("document_type", "invoice");
        metadata3.put("status", "approved");
        metadata3.put("value", 25000);

        Block block3 = blockchain.addBlockAndReturn("Invoice 1", keyPair.getPrivate(), keyPair.getPublic());
        block3.setCustomMetadata(jsonMapper.writeValueAsString(metadata3));
        blockchain.updateBlock(block3);
        System.out.println("✅ Stored approved invoice #" + block3.getBlockNumber() + " ($25,000)");

        // Search by exact key-value pairs
        System.out.println("\n🔍 Searching for status='approved'...");
        List<Block> approvedDocs = api.searchByCustomMetadataKeyValue("status", "approved");
        System.out.println("   📊 Found " + approvedDocs.size() + " approved document(s)");

        System.out.println("\n🔍 Searching for document_type='contract'...");
        List<Block> contracts = api.searchByCustomMetadataKeyValue("document_type", "contract");
        System.out.println("   📊 Found " + contracts.size() + " contract(s)");

        System.out.println("\n🔍 Searching for value='50000' (numeric as string)...");
        List<Block> value50k = api.searchByCustomMetadataKeyValue("value", "50000");
        System.out.println("   📊 Found " + value50k.size() + " document(s) worth $50,000");

        System.out.println("\n💡 Key benefit: Precise filtering by specific fields with exact matching");
    }

    private static void demonstrateMultipleCriteriaSearch(
            UserFriendlyEncryptionAPI api,
            Blockchain blockchain,
            KeyPair keyPair) throws Exception {

        System.out.println("\n📋 DEMO 3: MULTIPLE CRITERIA SEARCH (AND LOGIC)");
        System.out.println("================================================");
        System.out.println("ℹ️  Search for blocks matching ALL specified criteria simultaneously");

        // Store blocks with rich metadata
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("department", "medical");
        metadata1.put("priority", "high");
        metadata1.put("status", "active");
        metadata1.put("physician", "Dr. Johnson");

        Block block1 = blockchain.addBlockAndReturn("Case 1", keyPair.getPrivate(), keyPair.getPublic());
        block1.setCustomMetadata(jsonMapper.writeValueAsString(metadata1));
        blockchain.updateBlock(block1);
        System.out.println("✅ Case 1: medical + high + active + Dr. Johnson");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("department", "medical");
        metadata2.put("priority", "high");
        metadata2.put("status", "pending");
        metadata2.put("physician", "Dr. Smith");

        Block block2 = blockchain.addBlockAndReturn("Case 2", keyPair.getPrivate(), keyPair.getPublic());
        block2.setCustomMetadata(jsonMapper.writeValueAsString(metadata2));
        blockchain.updateBlock(block2);
        System.out.println("✅ Case 2: medical + high + pending + Dr. Smith");

        Map<String, Object> metadata3 = new HashMap<>();
        metadata3.put("department", "medical");
        metadata3.put("priority", "low");
        metadata3.put("status", "active");
        metadata3.put("physician", "Dr. Lee");

        Block block3 = blockchain.addBlockAndReturn("Case 3", keyPair.getPrivate(), keyPair.getPublic());
        block3.setCustomMetadata(jsonMapper.writeValueAsString(metadata3));
        blockchain.updateBlock(block3);
        System.out.println("✅ Case 3: medical + low + active + Dr. Lee");

        // Query 1: Medical + High priority + Active
        System.out.println("\n🔍 Query 1: department=medical AND priority=high AND status=active");
        Map<String, String> criteria1 = new HashMap<>();
        criteria1.put("department", "medical");
        criteria1.put("priority", "high");
        criteria1.put("status", "active");

        List<Block> results1 = api.searchByCustomMetadataMultipleCriteria(criteria1);
        System.out.println("   📊 Found " + results1.size() + " case(s) (should be Case 1 only)");

        // Query 2: Medical + High priority (any status)
        System.out.println("\n🔍 Query 2: department=medical AND priority=high");
        Map<String, String> criteria2 = new HashMap<>();
        criteria2.put("department", "medical");
        criteria2.put("priority", "high");

        List<Block> results2 = api.searchByCustomMetadataMultipleCriteria(criteria2);
        System.out.println("   📊 Found " + results2.size() + " case(s) (should be Case 1 and Case 2)");

        // Query 3: Active cases (any priority)
        System.out.println("\n🔍 Query 3: department=medical AND status=active");
        Map<String, String> criteria3 = new HashMap<>();
        criteria3.put("department", "medical");
        criteria3.put("status", "active");

        List<Block> results3 = api.searchByCustomMetadataMultipleCriteria(criteria3);
        System.out.println("   📊 Found " + results3.size() + " case(s) (should be Case 1 and Case 3)");

        System.out.println("\n💡 Key benefit: Complex queries with multiple conditions (AND logic)");
    }

    private static void demonstrateMedicalDashboard(
            UserFriendlyEncryptionAPI api,
            Blockchain blockchain,
            KeyPair keyPair) throws Exception {

        System.out.println("\n📋 DEMO 4: MEDICAL DASHBOARD (REAL-WORLD SCENARIO)");
        System.out.println("===================================================");
        System.out.println("ℹ️  Simulate a hospital dashboard with patient records");

        // Store realistic patient records
        Map<String, Object> patient1 = new HashMap<>();
        patient1.put("patient_id", "P12345");
        patient1.put("department", "cardiology");
        patient1.put("priority", "urgent");
        patient1.put("diagnosis", "hypertension");
        patient1.put("physician", "Dr. Martinez");
        patient1.put("admission_date", "2025-01-25");
        patient1.put("status", "active");

        Block block1 = blockchain.addBlockAndReturn("Patient record 1", keyPair.getPrivate(), keyPair.getPublic());
        block1.setCustomMetadata(jsonMapper.writeValueAsString(patient1));
        blockchain.updateBlock(block1);
        System.out.println("✅ Patient P12345: Cardiology - Urgent - Hypertension");

        Map<String, Object> patient2 = new HashMap<>();
        patient2.put("patient_id", "P67890");
        patient2.put("department", "cardiology");
        patient2.put("priority", "high");
        patient2.put("diagnosis", "arrhythmia");
        patient2.put("physician", "Dr. Chen");
        patient2.put("admission_date", "2025-01-26");
        patient2.put("status", "active");

        Block block2 = blockchain.addBlockAndReturn("Patient record 2", keyPair.getPrivate(), keyPair.getPublic());
        block2.setCustomMetadata(jsonMapper.writeValueAsString(patient2));
        blockchain.updateBlock(block2);
        System.out.println("✅ Patient P67890: Cardiology - High - Arrhythmia");

        Map<String, Object> patient3 = new HashMap<>();
        patient3.put("patient_id", "P11111");
        patient3.put("department", "neurology");
        patient3.put("priority", "routine");
        patient3.put("diagnosis", "migraine");
        patient3.put("physician", "Dr. Lee");
        patient3.put("admission_date", "2025-01-20");
        patient3.put("status", "completed");

        Block block3 = blockchain.addBlockAndReturn("Patient record 3", keyPair.getPrivate(), keyPair.getPublic());
        block3.setCustomMetadata(jsonMapper.writeValueAsString(patient3));
        blockchain.updateBlock(block3);
        System.out.println("✅ Patient P11111: Neurology - Routine - Migraine (completed)");

        // Dashboard queries
        System.out.println("\n📊 DASHBOARD QUERIES:");

        System.out.println("\n🔍 Query 1: All active cardiology cases");
        Map<String, String> activeCardio = new HashMap<>();
        activeCardio.put("department", "cardiology");
        activeCardio.put("status", "active");
        List<Block> activeCardioResults = api.searchByCustomMetadataMultipleCriteria(activeCardio);
        System.out.println("   📈 Result: " + activeCardioResults.size() + " active cardiology case(s)");

        System.out.println("\n🔍 Query 2: All urgent cases");
        List<Block> urgentCases = api.searchByCustomMetadataKeyValue("priority", "urgent");
        System.out.println("   📈 Result: " + urgentCases.size() + " urgent case(s)");

        System.out.println("\n🔍 Query 3: Cases for Dr. Martinez");
        List<Block> drMartinezCases = api.searchByCustomMetadataKeyValue("physician", "Dr. Martinez");
        System.out.println("   📈 Result: " + drMartinezCases.size() + " case(s) for Dr. Martinez");

        System.out.println("\n🔍 Query 4: Completed cases");
        List<Block> completedCases = api.searchByCustomMetadataKeyValue("status", "completed");
        System.out.println("   📈 Result: " + completedCases.size() + " completed case(s)");

        System.out.println("\n💡 Key benefit: Real-time dashboard queries without decrypting patient data");
    }

    private static void demonstrateLegalDocumentManagement(
            UserFriendlyEncryptionAPI api,
            Blockchain blockchain,
            KeyPair keyPair) throws Exception {

        System.out.println("\n📋 DEMO 5: LEGAL DOCUMENT MANAGEMENT");
        System.out.println("=====================================");
        System.out.println("ℹ️  Track contracts and legal documents with structured metadata");

        // Store contracts
        Map<String, Object> contract1 = new HashMap<>();
        contract1.put("document_type", "contract");
        contract1.put("contract_id", "CTR-2025-001");
        contract1.put("party_a", "Acme Corp");
        contract1.put("party_b", "XYZ Industries");
        contract1.put("value", 250000.00);
        contract1.put("status", "executed");
        contract1.put("category", "vendor_agreement");

        Block block1 = blockchain.addBlockAndReturn("Contract 1", keyPair.getPrivate(), keyPair.getPublic());
        block1.setCustomMetadata(jsonMapper.writeValueAsString(contract1));
        blockchain.updateBlock(block1);
        System.out.println("✅ Contract CTR-2025-001: $250k vendor agreement (executed)");

        // Legal queries
        System.out.println("\n🔍 Query 1: All executed contracts");
        List<Block> executedContracts = api.searchByCustomMetadataKeyValue("status", "executed");
        System.out.println("   📈 Result: " + executedContracts.size() + " executed contract(s)");

        System.out.println("\n🔍 Query 2: Contracts with Acme Corp");
        List<Block> acmeContracts = api.searchByCustomMetadata("Acme Corp");
        System.out.println("   📈 Result: " + acmeContracts.size() + " contract(s) with Acme Corp");

        System.out.println("\n💡 Key benefit: Efficient contract tracking and compliance reporting");
    }

    private static void demonstrateECommerceTracking(
            UserFriendlyEncryptionAPI api,
            Blockchain blockchain,
            KeyPair keyPair) throws Exception {

        System.out.println("\n📋 DEMO 6: E-COMMERCE ORDER TRACKING");
        System.out.println("=====================================");
        System.out.println("ℹ️  Track orders with detailed metadata for analytics");

        // Store orders
        Map<String, Object> order1 = new HashMap<>();
        order1.put("order_id", "ORD-2025-5678");
        order1.put("customer_id", "CUST-001");
        order1.put("status", "shipped");
        order1.put("total_amount", 1599.99);
        order1.put("payment_method", "credit_card");
        order1.put("shipping_country", "Spain");
        order1.put("priority_shipping", true);

        Block block1 = blockchain.addBlockAndReturn("Order 1", keyPair.getPrivate(), keyPair.getPublic());
        block1.setCustomMetadata(jsonMapper.writeValueAsString(order1));
        blockchain.updateBlock(block1);
        System.out.println("✅ Order ORD-2025-5678: $1,599.99 - Shipped to Spain (Priority)");

        Map<String, Object> order2 = new HashMap<>();
        order2.put("order_id", "ORD-2025-5679");
        order2.put("customer_id", "CUST-002");
        order2.put("status", "processing");
        order2.put("total_amount", 899.50);
        order2.put("payment_method", "paypal");
        order2.put("shipping_country", "France");
        order2.put("priority_shipping", false);

        Block block2 = blockchain.addBlockAndReturn("Order 2", keyPair.getPrivate(), keyPair.getPublic());
        block2.setCustomMetadata(jsonMapper.writeValueAsString(order2));
        blockchain.updateBlock(block2);
        System.out.println("✅ Order ORD-2025-5679: $899.50 - Processing to France (Standard)");

        // E-commerce queries
        System.out.println("\n🔍 Query 1: All shipped orders");
        List<Block> shippedOrders = api.searchByCustomMetadataKeyValue("status", "shipped");
        System.out.println("   📈 Result: " + shippedOrders.size() + " shipped order(s)");

        System.out.println("\n🔍 Query 2: Priority shipments to Spain");
        Map<String, String> prioritySpain = new HashMap<>();
        prioritySpain.put("priority_shipping", "true");
        prioritySpain.put("shipping_country", "Spain");
        List<Block> priorityOrders = api.searchByCustomMetadataMultipleCriteria(prioritySpain);
        System.out.println("   📈 Result: " + priorityOrders.size() + " priority order(s) to Spain");

        System.out.println("\n🔍 Query 3: Credit card payments");
        List<Block> creditCardOrders = api.searchByCustomMetadataKeyValue("payment_method", "credit_card");
        System.out.println("   📈 Result: " + creditCardOrders.size() + " credit card order(s)");

        System.out.println("\n💡 Key benefit: Real-time order analytics and business intelligence");
    }
}