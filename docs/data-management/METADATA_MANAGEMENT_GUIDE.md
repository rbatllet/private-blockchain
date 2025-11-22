# Metadata Management Guide

## Overview

The **Metadata Management** functionality in `UserFriendlyEncryptionAPI` provides a comprehensive solution for updating and managing block metadata without modifying encrypted content. This feature enables dynamic updates to searchable information, content categories, and custom metadata fields while maintaining data integrity and performance optimization.

## 📋 Table of Contents

- [Key Features](#key-features)
- [API Reference](#api-reference)
- [Usage Examples](#usage-examples)
- [Best Practices](#best-practices)
- [Performance Considerations](#performance-considerations)
- [Error Handling](#error-handling)
- [Integration with Search Framework](#integration-with-search-framework)

## 🚀 Key Features

### ✅ **Safe Metadata Updates**
- **Atomic Operations**: All metadata updates are transactional and safe
- **Validation**: Comprehensive input validation prevents invalid updates
- **Rollback Support**: Failed updates don't corrupt existing data

### ⚡ **Performance Optimization**
- **Automatic Cache Invalidation**: Clears search caches after updates
- **Index Synchronization**: Ensures search indexes stay updated
- **Efficient Database Operations**: Optimized SQL queries for metadata updates

### 🔍 **Search Integration**
- **Searchable Content Updates**: Modify searchable metadata for better discoverability
- **Category Management**: Update content categories dynamically
- **Keyword Enhancement**: Add or modify manual keywords for improved search

## 📖 API Reference

### `updateBlockMetadata(Block block)`

Updates the metadata of an existing block and invalidates relevant caches.

```java
public boolean updateBlockMetadata(Block block)
```

#### Parameters
- **`block`** (`Block`): The block with updated metadata
  - Must not be null
  - Must have a valid block number
  - Can contain updated: searchable content, categories, keywords, custom metadata

#### Returns
- **`boolean`**: `true` if update successful and caches invalidated, `false` otherwise

#### Exceptions
- **`IllegalArgumentException`**: If block is null or has invalid block number
- **`RuntimeException`**: If database update fails or cache invalidation fails

## 💡 Usage Examples

### Example 1: Basic Metadata Update

```java
// Initialize API
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);

// Find existing block
List<Block> blocks = api.getEncryptedBlocksOnly("patient-123");
if (!blocks.isEmpty()) {
    Block block = blocks.get(0);
    
    // Update metadata
    block.setContentCategory("medical-urgent");
    block.setManualKeywords("patient-123 emergency cardiology");
    block.setSearchableContent("Emergency cardiac consultation for patient 123");
    
    // Apply the update
    boolean success = api.updateBlockMetadata(block);
    if (success) {
        System.out.println("✅ Block metadata updated successfully");
    } else {
        System.out.println("❌ Failed to update block metadata");
    }
}
```

### Example 2: Custom Metadata with JSON

```java
// Create custom metadata
Map<String, Object> customData = new HashMap<>();
customData.put("priority", "high");
customData.put("department", "cardiology");
customData.put("lastUpdated", LocalDateTime.now().toString());
customData.put("reviewStatus", "pending");

// Find and update block
List<Block> blocks = api.getEncryptedBlocksOnly("case-456");
if (!blocks.isEmpty()) {
    Block block = blocks.get(0);
    
    // Serialize and set custom metadata
    try {
        String serializedMetadata = CustomMetadataUtil.serializeMetadata(customData);
        block.setCustomMetadata(serializedMetadata);
        
        // Apply update
        boolean success = api.updateBlockMetadata(block);
        if (success) {
            System.out.println("✅ Custom metadata updated successfully");
        }
    } catch (Exception e) {
        System.err.println("❌ Failed to serialize metadata: " + e.getMessage());
    }
}
```

### Example 3: Bulk Metadata Updates

```java
// Update multiple blocks with improved categories
List<Block> medicalBlocks = api.getEncryptedBlocksOnly("medical");

for (Block block : medicalBlocks) {
    // Enhance categorization
    String currentCategory = block.getContentCategory();
    if ("medical".equals(currentCategory)) {
        // Upgrade to more specific category
        block.setContentCategory("medical-consultation");
        
        // Add enhanced keywords
        String existingKeywords = block.getManualKeywords() != null ? 
            block.getManualKeywords() : "";
        block.setManualKeywords(existingKeywords + " consultation healthcare");
        
        // Update the block
        boolean success = api.updateBlockMetadata(block);
        if (success) {
            System.out.println("✅ Updated block #" + block.getBlockNumber());
        } else {
            System.err.println("❌ Failed to update block #" + block.getBlockNumber());
        }
    }
}

System.out.println("✅ Bulk metadata update completed");
```

### Example 4: Advanced Search Enhancement

```java
// Enhance blocks for better search capabilities
List<Block> blocks = api.getEncryptedBlocksOnly("patient");

for (Block block : blocks) {
    String searchableContent = block.getSearchableContent();
    if (searchableContent != null && searchableContent.contains("patient")) {
        
        // Extract patient ID patterns
        Pattern patientIdPattern = Pattern.compile("patient[\\s-]*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = patientIdPattern.matcher(searchableContent);
        
        if (matcher.find()) {
            String patientId = matcher.group(1);
            
            // Enhance keywords with structured patient ID
            String enhancedKeywords = "patient-" + patientId + " " + 
                                    (block.getManualKeywords() != null ? block.getManualKeywords() : "");
            block.setManualKeywords(enhancedKeywords.trim());
            
            // Update searchable content for better discoverability
            String enhancedContent = searchableContent + " [ID:" + patientId + "]";
            block.setSearchableContent(enhancedContent);
            
            // Apply update
            api.updateBlockMetadata(block);
            System.out.println("✅ Enhanced search for patient " + patientId);
        }
    }
}
```

## 🎯 Best Practices

### 1. **Input Validation**
```java
// Always validate before updating
if (block == null || block.getBlockNumber() == null) {
    throw new IllegalArgumentException("Invalid block for metadata update");
}

// Check if metadata actually changed to avoid unnecessary updates
if (hasMetadataChanged(block, originalBlock)) {
    api.updateBlockMetadata(block);
}
```

### 2. **Error Handling**
```java
try {
    boolean success = api.updateBlockMetadata(block);
    if (!success) {
        // Log failure and implement retry logic
        logger.warn("Metadata update failed for block #{}", block.getBlockNumber());
        // Implement retry strategy
    }
} catch (IllegalArgumentException e) {
    logger.error("Invalid block data: {}", e.getMessage());
} catch (RuntimeException e) {
    logger.error("System error during metadata update: {}", e.getMessage());
    // Implement fallback strategy
}
```

### 3. **Batch Operations**
```java
// Process in batches to avoid overwhelming the system
List<Block> blocksToUpdate = getBlocksNeedingUpdate();
int batchSize = 50;

for (int i = 0; i < blocksToUpdate.size(); i += batchSize) {
    List<Block> batch = blocksToUpdate.subList(i, 
        Math.min(i + batchSize, blocksToUpdate.size()));
    
    for (Block block : batch) {
        api.updateBlockMetadata(block);
    }
    
    // Small delay between batches
    Thread.sleep(100);
}
```

## ⚡ Performance Considerations

### **Cache Management**
- **Automatic Invalidation**: The API automatically invalidates search caches after each update
- **Batch Updates**: Consider batching multiple updates if updating many blocks
- **Cache Warm-up**: First search after updates might be slower due to cache rebuilding

### **Database Impact**
- **Transaction Overhead**: Each update is a separate database transaction
- **Index Updates**: Search indexes are updated automatically
- **Connection Pooling**: Uses efficient connection pooling for database operations

### **Memory Usage**
- **Cache Rebuilding**: Memory usage may spike during cache invalidation
- **Large Metadata**: Be mindful of very large custom metadata objects
- **Concurrent Updates**: Multiple simultaneous updates are thread-safe

## 🚨 Error Handling

### Common Error Scenarios

| Error | Cause | Solution |
|-------|-------|----------|
| `IllegalArgumentException` | Null block or invalid block number | Validate input before calling |
| `RuntimeException` | Database connection failure | Check database connectivity |
| `RuntimeException` | Cache invalidation failure | Check system resources |
| `false` return value | Block not found in database | Verify block exists |
| `false` return value | Insufficient permissions | Check user authorization |

### Error Recovery Strategies

```java
public boolean safeUpdateMetadata(Block block, int maxRetries) {
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            boolean success = api.updateBlockMetadata(block);
            if (success) {
                return true;
            }
            
            // Log retry attempt
            logger.warn("Metadata update attempt {} failed for block #{}", 
                       attempt, block.getBlockNumber());
            
            // Exponential backoff
            Thread.sleep(attempt * 1000);
            
        } catch (RuntimeException e) {
            logger.error("Error on attempt {} for block #{}: {}", 
                        attempt, block.getBlockNumber(), e.getMessage());
            
            if (attempt == maxRetries) {
                throw e; // Re-throw on final attempt
            }
        }
    }
    return false;
}
```

## 🔍 Integration with Search Framework

### **Automatic Index Updates**
The metadata updates automatically integrate with the Search Framework Engine:

```java
// After successful metadata update:
// 1. Block is updated in database ✅
// 2. Search caches are invalidated ✅  
// 3. Search indexes will be rebuilt on next search ✅
// 4. New metadata becomes immediately searchable ✅

// Example: Update makes block immediately discoverable
Block block = findBlockById(123);
block.setManualKeywords("urgent priority high");
api.updateBlockMetadata(block);

// Block is now discoverable via these new keywords
List<Block> urgentBlocks = api.getEncryptedBlocksOnly("urgent");
// Will include the updated block ✅
```

### **Search Performance Impact**
- **First Search After Update**: May be slower due to cache rebuilding
- **Subsequent Searches**: Full performance restored
- **Index Optimization**: Search indexes are optimized during rebuilding

## 🎯 Complete Practical Examples Collection

### **Example 5: Version Management System**
```java
/**
 * Document versioning with metadata tracking
 */
public void updateDocumentVersion(String documentId, String newVersion, String changeNote) {
    List<Block> docBlocks = api.getEncryptedBlocksOnly(documentId);
    
    if (!docBlocks.isEmpty()) {
        Block block = docBlocks.get(0);
        
        // Create version metadata
        Map<String, Object> versionData = new HashMap<>();
        versionData.put("version", newVersion);
        versionData.put("previousVersion", getCurrentVersion(block));
        versionData.put("changeNote", changeNote);
        versionData.put("updatedBy", getCurrentUser());
        versionData.put("updateTimestamp", LocalDateTime.now().toString());
        
        // Update searchable content with version info
        String newSearchableContent = block.getSearchableContent() + 
            " [v" + newVersion + "] " + changeNote;
        block.setSearchableContent(newSearchableContent);
        
        // Update keywords for version discovery
        String versionKeywords = "version-" + newVersion + " " + documentId + "-v" + newVersion;
        block.setManualKeywords(
            (block.getManualKeywords() != null ? block.getManualKeywords() + " " : "") + versionKeywords
        );
        
        // Set custom metadata with version tracking
        try {
            String serializedVersion = CustomMetadataUtil.serializeMetadata(versionData);
            block.setCustomMetadata(serializedVersion);
            
            boolean success = api.updateBlockMetadata(block);
            if (success) {
                System.out.println("✅ Document " + documentId + " updated to version " + newVersion);
            }
        } catch (Exception e) {
            System.err.println("❌ Version update failed: " + e.getMessage());
        }
    }
}

private String getCurrentVersion(Block block) {
    try {
        if (block.getCustomMetadata() != null) {
            Map<String, Object> metadata = CustomMetadataUtil.deserializeMetadata(block.getCustomMetadata());
            return (String) metadata.getOrDefault("version", "1.0");
        }
    } catch (Exception e) {
        // Fallback to default
    }
    return "1.0";
}
```

### **Example 6: Workflow Approval System**
```java
/**
 * Approval workflow with state tracking
 */
public class ApprovalWorkflow {
    
    public enum ApprovalStatus {
        DRAFT, PENDING_REVIEW, APPROVED, REJECTED, NEEDS_REVISION
    }
    
    public void updateApprovalStatus(String documentId, ApprovalStatus newStatus, 
                                   String reviewerName, String comments) {
        List<Block> blocks = api.getEncryptedBlocksOnly(documentId);
        
        if (!blocks.isEmpty()) {
            Block block = blocks.get(0);
            
            // Create approval metadata
            Map<String, Object> approvalData = new HashMap<>();
            approvalData.put("status", newStatus.toString());
            approvalData.put("reviewer", reviewerName);
            approvalData.put("reviewComments", comments);
            approvalData.put("reviewDate", LocalDateTime.now().toString());
            approvalData.put("previousStatus", getCurrentStatus(block));
            
            // Update content category based on status
            switch (newStatus) {
                case APPROVED -> block.setContentCategory("approved-content");
                case REJECTED -> block.setContentCategory("rejected-content");
                case PENDING_REVIEW -> block.setContentCategory("pending-review");
                case NEEDS_REVISION -> block.setContentCategory("needs-revision");
                default -> block.setContentCategory("draft-content");
            }
            
            // Update keywords for workflow discovery
            String workflowKeywords = "status-" + newStatus.toString().toLowerCase() + 
                                    " reviewer-" + reviewerName.replace(" ", "-");
            
            // Preserve existing keywords and add workflow keywords
            String existingKeywords = block.getManualKeywords() != null ? 
                block.getManualKeywords() : "";
            
            // Remove old status keywords and add new ones
            String cleanedKeywords = removeOldStatusKeywords(existingKeywords);
            block.setManualKeywords(cleanedKeywords + " " + workflowKeywords);
            
            // Update searchable content with status info
            String statusInfo = "[" + newStatus + "] Reviewed by: " + reviewerName;
            if (!comments.isEmpty()) {
                statusInfo += " - " + comments.substring(0, Math.min(comments.length(), 100));
            }
            
            String updatedSearchableContent = block.getSearchableContent() + " " + statusInfo;
            block.setSearchableContent(updatedSearchableContent);
            
            // Apply metadata
            try {
                String serializedApproval = CustomMetadataUtil.serializeMetadata(approvalData);
                block.setCustomMetadata(serializedApproval);
                
                boolean success = api.updateBlockMetadata(block);
                if (success) {
                    System.out.println("✅ " + documentId + " status updated to " + newStatus + 
                                     " by " + reviewerName);
                    
                    // Send notification based on status
                    sendNotification(documentId, newStatus, reviewerName);
                }
            } catch (Exception e) {
                System.err.println("❌ Approval update failed: " + e.getMessage());
            }
        }
    }
    
    private String removeOldStatusKeywords(String keywords) {
        return keywords.replaceAll("status-\\w+\\s*", "").trim();
    }
    
    private ApprovalStatus getCurrentStatus(Block block) {
        try {
            if (block.getCustomMetadata() != null) {
                Map<String, Object> metadata = CustomMetadataUtil.deserializeMetadata(block.getCustomMetadata());
                String status = (String) metadata.get("status");
                return status != null ? ApprovalStatus.valueOf(status) : ApprovalStatus.DRAFT;
            }
        } catch (Exception e) {
            // Fallback
        }
        return ApprovalStatus.DRAFT;
    }
    
    private void sendNotification(String documentId, ApprovalStatus status, String reviewer) {
        // Implementation for notifications
        System.out.println("📧 Notification: Document " + documentId + 
                          " is now " + status + " (reviewed by " + reviewer + ")");
    }
}
```

### **Example 7: Intelligent Content Indexing**
```java
/**
 * Automatic content analysis and keyword enhancement
 */
public class IntelligentIndexer {
    
    private final Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private final Pattern phonePattern = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
    private final Pattern datePattern = Pattern.compile("\\b\\d{4}[-/]\\d{2}[-/]\\d{2}\\b");
    private final Pattern codePattern = Pattern.compile("\\b[A-Z]{2,}[-_]?\\d{3,}\\b");
    
    public void enhanceContentIndexing(String searchTerm) {
        List<Block> blocks = api.getEncryptedBlocksOnly(searchTerm);
        
        for (Block block : blocks) {
            try {
                // Analyze searchable content
                String content = block.getSearchableContent();
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                
                Set<String> extractedKeywords = new HashSet<>();
                
                // Extract emails
                Matcher emailMatcher = emailPattern.matcher(content);
                while (emailMatcher.find()) {
                    String email = emailMatcher.group();
                    extractedKeywords.add("email-" + email.substring(0, email.indexOf("@")));
                    extractedKeywords.add("domain-" + email.substring(email.indexOf("@") + 1));
                }
                
                // Extract phone numbers
                Matcher phoneMatcher = phonePattern.matcher(content);
                while (phoneMatcher.find()) {
                    String phone = phoneMatcher.group().replaceAll("[^0-9]", "");
                    extractedKeywords.add("phone-" + phone.substring(0, 3)); // Area code
                }
                
                // Extract dates
                Matcher dateMatcher = datePattern.matcher(content);
                while (dateMatcher.find()) {
                    String date = dateMatcher.group();
                    String year = date.substring(0, 4);
                    extractedKeywords.add("year-" + year);
                    
                    if (date.length() >= 7) {
                        String month = date.substring(5, 7);
                        extractedKeywords.add("month-" + year + "-" + month);
                    }
                }
                
                // Extract codes and identifiers
                Matcher codeMatcher = codePattern.matcher(content);
                while (codeMatcher.find()) {
                    String code = codeMatcher.group();
                    extractedKeywords.add("code-" + code.toLowerCase());
                }
                
                // Extract important terms (capitalized words)
                String[] words = content.split("\\s+");
                for (String word : words) {
                    if (word.length() > 3 && Character.isUpperCase(word.charAt(0))) {
                        extractedKeywords.add("term-" + word.toLowerCase());
                    }
                }
                
                // Combine with existing keywords
                String existingKeywords = block.getManualKeywords() != null ? 
                    block.getManualKeywords() : "";
                
                String enhancedKeywords = existingKeywords;
                for (String keyword : extractedKeywords) {
                    if (!existingKeywords.contains(keyword)) {
                        enhancedKeywords += " " + keyword;
                    }
                }
                
                block.setManualKeywords(enhancedKeywords.trim());
                
                // Update content category based on analysis
                updateContentCategory(block, content, extractedKeywords);
                
                // Create analysis metadata
                Map<String, Object> analysisData = new HashMap<>();
                analysisData.put("analyzedDate", LocalDateTime.now().toString());
                analysisData.put("extractedKeywords", new ArrayList<>(extractedKeywords));
                analysisData.put("emailCount", countMatches(emailPattern, content));
                analysisData.put("phoneCount", countMatches(phonePattern, content));
                analysisData.put("dateCount", countMatches(datePattern, content));
                analysisData.put("codeCount", countMatches(codePattern, content));
                
                String serializedAnalysis = CustomMetadataUtil.serializeMetadata(analysisData);
                block.setCustomMetadata(serializedAnalysis);
                
                boolean success = api.updateBlockMetadata(block);
                if (success) {
                    System.out.println("✅ Enhanced indexing for block #" + block.getBlockNumber() + 
                                     " (+" + extractedKeywords.size() + " keywords)");
                }
                
            } catch (Exception e) {
                System.err.println("❌ Indexing failed for block #" + block.getBlockNumber() + 
                                 ": " + e.getMessage());
            }
        }
    }
    
    private void updateContentCategory(Block block, String content, Set<String> keywords) {
        String contentLower = content.toLowerCase();
        
        if (contentLower.contains("medical") || contentLower.contains("patient") || 
            contentLower.contains("diagnosis")) {
            block.setContentCategory("medical-content");
        } else if (contentLower.contains("financial") || contentLower.contains("invoice") || 
                  contentLower.contains("payment")) {
            block.setContentCategory("financial-content");
        } else if (contentLower.contains("legal") || contentLower.contains("contract") || 
                  contentLower.contains("agreement")) {
            block.setContentCategory("legal-content");
        } else if (keywords.stream().anyMatch(k -> k.startsWith("email-") || k.startsWith("phone-"))) {
            block.setContentCategory("contact-content");
        } else {
            block.setContentCategory("general-content");
        }
    }
    
    private long countMatches(Pattern pattern, String text) {
        return pattern.matcher(text).results().count();
    }
}
```

### **Example 8: External System Integration**
```java
/**
 * Synchronize blockchain metadata with external systems
 */
public class ExternalSystemIntegration {
    
    private final ExternalCRMService crmService;
    private final ExternalDocumentService docService;
    
    public ExternalSystemIntegration(ExternalCRMService crm, ExternalDocumentService doc) {
        this.crmService = crm;
        this.docService = doc;
    }
    
    /**
     * Sync customer information from CRM to blockchain metadata
     */
    public void syncCustomerData(String customerId) {
        try {
            // Get customer data from external CRM
            CustomerData customerData = crmService.getCustomer(customerId);
            
            // Find blockchain blocks related to this customer
            List<Block> customerBlocks = api.getEncryptedBlocksOnly("customer-" + customerId);
            
            for (Block block : customerBlocks) {
                // Create enriched metadata
                Map<String, Object> crmMetadata = new HashMap<>();
                crmMetadata.put("customerId", customerData.getId());
                crmMetadata.put("customerName", customerData.getName());
                crmMetadata.put("customerTier", customerData.getTier());
                crmMetadata.put("lastContactDate", customerData.getLastContact().toString());
                crmMetadata.put("accountManager", customerData.getAccountManager());
                crmMetadata.put("syncTimestamp", LocalDateTime.now().toString());
                
                // Update searchable content with CRM data
                String crmInfo = "Customer: " + customerData.getName() + 
                               " (Tier: " + customerData.getTier() + 
                               ", Manager: " + customerData.getAccountManager() + ")";
                
                String enhancedSearchable = block.getSearchableContent() + " " + crmInfo;
                block.setSearchableContent(enhancedSearchable);
                
                // Update keywords with CRM data
                String crmKeywords = "tier-" + customerData.getTier().toLowerCase() + 
                                   " manager-" + customerData.getAccountManager().replace(" ", "-") +
                                   " customer-" + customerData.getName().replace(" ", "-").toLowerCase();
                
                String existingKeywords = block.getManualKeywords() != null ? 
                    block.getManualKeywords() : "";
                block.setManualKeywords(existingKeywords + " " + crmKeywords);
                
                // Set category based on customer tier
                switch (customerData.getTier()) {
                    case PREMIUM -> block.setContentCategory("premium-customer");
                    case GOLD -> block.setContentCategory("gold-customer");
                    case STANDARD -> block.setContentCategory("standard-customer");
                    default -> block.setContentCategory("customer-content");
                }
                
                // Apply CRM metadata
                String serializedCRM = CustomMetadataUtil.serializeMetadata(crmMetadata);
                block.setCustomMetadata(serializedCRM);
                
                boolean success = api.updateBlockMetadata(block);
                if (success) {
                    System.out.println("✅ Synced CRM data for customer " + customerId + 
                                     " (block #" + block.getBlockNumber() + ")");
                }
            }
            
            // Update CRM with blockchain reference
            crmService.updateCustomerBlockchainRef(customerId, customerBlocks.size());
            
        } catch (Exception e) {
            System.err.println("❌ CRM sync failed for customer " + customerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Bidirectional sync with document management system
     */
    public void syncDocumentStatus(String documentId) {
        try {
            List<Block> docBlocks = api.getEncryptedBlocksOnly(documentId);
            
            if (!docBlocks.isEmpty()) {
                Block block = docBlocks.get(0);
                
                // Get current blockchain metadata
                Map<String, Object> currentMetadata = getCurrentMetadata(block);
                
                // Get external document status
                DocumentStatus externalStatus = docService.getDocumentStatus(documentId);
                
                // Create synchronized metadata
                Map<String, Object> syncMetadata = new HashMap<>(currentMetadata);
                syncMetadata.put("externalStatus", externalStatus.getStatus());
                syncMetadata.put("externalLastModified", externalStatus.getLastModified().toString());
                syncMetadata.put("externalOwner", externalStatus.getOwner());
                syncMetadata.put("externalTags", externalStatus.getTags());
                syncMetadata.put("lastSyncDate", LocalDateTime.now().toString());
                
                // Update searchable content with external data
                String externalInfo = "Status: " + externalStatus.getStatus() + 
                                    " Owner: " + externalStatus.getOwner() +
                                    " Tags: " + String.join(", ", externalStatus.getTags());
                
                String updatedSearchable = block.getSearchableContent() + " [External: " + externalInfo + "]";
                block.setSearchableContent(updatedSearchable);
                
                // Update keywords with external tags
                String tagKeywords = externalStatus.getTags().stream()
                    .map(tag -> "tag-" + tag.toLowerCase())
                    .collect(Collectors.joining(" "));
                
                String ownerKeyword = "owner-" + externalStatus.getOwner().replace(" ", "-").toLowerCase();
                String statusKeyword = "external-status-" + externalStatus.getStatus().toLowerCase();
                
                String externalKeywords = tagKeywords + " " + ownerKeyword + " " + statusKeyword;
                String existingKeywords = block.getManualKeywords() != null ? 
                    block.getManualKeywords() : "";
                block.setManualKeywords(existingKeywords + " " + externalKeywords);
                
                // Apply synchronized metadata
                String serializedSync = CustomMetadataUtil.serializeMetadata(syncMetadata);
                block.setCustomMetadata(serializedSync);
                
                boolean success = api.updateBlockMetadata(block);
                if (success) {
                    System.out.println("✅ Synced document " + documentId + " with external system");
                    
                    // Update external system with blockchain timestamp
                    docService.updateBlockchainSync(documentId, LocalDateTime.now());
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Document sync failed for " + documentId + ": " + e.getMessage());
        }
    }
    
    private Map<String, Object> getCurrentMetadata(Block block) {
        try {
            if (block.getCustomMetadata() != null) {
                return CustomMetadataUtil.deserializeMetadata(block.getCustomMetadata());
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not parse existing metadata: " + e.getMessage());
        }
        return new HashMap<>();
    }
    
    // Mock external service interfaces for the example
    interface ExternalCRMService {
        CustomerData getCustomer(String customerId);
        void updateCustomerBlockchainRef(String customerId, int blockCount);
    }
    
    interface ExternalDocumentService {
        DocumentStatus getDocumentStatus(String documentId);
        void updateBlockchainSync(String documentId, LocalDateTime syncTime);
    }
    
    // Mock data classes
    static class CustomerData {
        private String id, name, tier, accountManager;
        private LocalDateTime lastContact;
        
        // Getters and constructors...
        public String getId() { return id; }
        public String getName() { return name; }
        public String getTier() { return tier; }
        public String getAccountManager() { return accountManager; }
        public LocalDateTime getLastContact() { return lastContact; }
    }
    
    static class DocumentStatus {
        private String status, owner;
        private LocalDateTime lastModified;
        private List<String> tags;
        
        // Getters...
        public String getStatus() { return status; }
        public String getOwner() { return owner; }
        public LocalDateTime getLastModified() { return lastModified; }
        public List<String> getTags() { return tags; }
    }
}
```

### **Example 9: Advanced Error Recovery & Batch Processing**
```java
/**
 * Robust batch processing with error recovery
 */
public class RobustMetadataProcessor {
    
    private final UserFriendlyEncryptionAPI api;
    private final ExecutorService executor;
    private final int maxRetries = 3;
    private final int batchSize = 10;
    
    public RobustMetadataProcessor(UserFriendlyEncryptionAPI api) {
        this.api = api;
        this.executor = Executors.newFixedThreadPool(4);
    }
    
    /**
     * Process metadata updates with comprehensive error handling
     */
    public CompletableFuture<BatchProcessingResult> processMetadataUpdates(
            List<MetadataUpdate> updates) {
        
        return CompletableFuture.supplyAsync(() -> {
            BatchProcessingResult result = new BatchProcessingResult();
            
            // Process in batches to avoid overwhelming the system
            List<List<MetadataUpdate>> batches = partition(updates, batchSize);
            
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<MetadataUpdate> batch = batches.get(batchIndex);
                
                System.out.println("🔄 Processing batch " + (batchIndex + 1) + "/" + batches.size() + 
                                 " (" + batch.size() + " updates)");
                
                for (MetadataUpdate update : batch) {
                    try {
                        boolean success = processUpdateWithRetry(update);
                        if (success) {
                            result.addSuccess(update);
                        } else {
                            result.addFailure(update, "Max retries exceeded");
                        }
                    } catch (Exception e) {
                        result.addFailure(update, e.getMessage());
                        System.err.println("❌ Critical error processing update for block " +
                                         update.getBlockNumber() + ": " + e.getMessage());
                    }
                }
                
                // Small delay between batches to prevent system overload
                if (batchIndex < batches.size() - 1) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            return result;
        }, executor);
    }
    
    private boolean processUpdateWithRetry(MetadataUpdate update) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Find the block to update
                Block block = api.getBlock(update.getBlockNumber());
                if (block == null) {
                    throw new IllegalArgumentException("Block not found: " + update.getBlockNumber());
                }

                // Apply the update
                applyUpdate(block, update);

                // Perform the metadata update
                boolean success = api.updateBlockMetadata(block);

                if (success) {
                    System.out.println("✅ Updated block " + update.getBlockNumber() +
                                     " (attempt " + attempt + ")");
                    return true;
                } else {
                    throw new RuntimeException("Metadata update returned false");
                }

            } catch (Exception e) {
                lastException = e;
                System.err.println("⚠️ Attempt " + attempt + " failed for block " +
                                 update.getBlockNumber() + ": " + e.getMessage());
                
                if (attempt < maxRetries) {
                    // Exponential backoff
                    try {
                        Thread.sleep(attempt * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        System.err.println("❌ All " + maxRetries + " attempts failed for block " + update.getBlockNumber());
        if (lastException != null) {
            System.err.println("Last error: " + lastException.getMessage());
        }

        return false;
    }
    
    private void applyUpdate(Block block, MetadataUpdate update) {
        // Apply content category update
        if (update.getNewCategory() != null) {
            block.setContentCategory(update.getNewCategory());
        }
        
        // Apply keywords update
        if (update.getNewKeywords() != null) {
            if (update.isAppendKeywords()) {
                String existing = block.getManualKeywords() != null ? block.getManualKeywords() : "";
                block.setManualKeywords(existing + " " + update.getNewKeywords());
            } else {
                block.setManualKeywords(update.getNewKeywords());
            }
        }
        
        // Apply searchable content update
        if (update.getNewSearchableContent() != null) {
            if (update.isAppendContent()) {
                String existing = block.getSearchableContent() != null ? block.getSearchableContent() : "";
                block.setSearchableContent(existing + " " + update.getNewSearchableContent());
            } else {
                block.setSearchableContent(update.getNewSearchableContent());
            }
        }
        
        // Apply custom metadata update
        if (update.getCustomMetadata() != null) {
            Map<String, Object> currentMetadata = getCurrentMetadata(block);
            currentMetadata.putAll(update.getCustomMetadata());
            
            try {
                String serialized = CustomMetadataUtil.serializeMetadata(currentMetadata);
                block.setCustomMetadata(serialized);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize metadata: " + e.getMessage(), e);
            }
        }
    }
    
    private Map<String, Object> getCurrentMetadata(Block block) {
        try {
            if (block.getCustomMetadata() != null) {
                return new HashMap<>(CustomMetadataUtil.deserializeMetadata(block.getCustomMetadata()));
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not parse existing metadata: " + e.getMessage());
        }
        return new HashMap<>();
    }
    
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Supporting classes
    public static class MetadataUpdate {
        private Long blockNumber;
        private String newCategory;
        private String newKeywords;
        private boolean appendKeywords = false;
        private String newSearchableContent;
        private boolean appendContent = false;
        private Map<String, Object> customMetadata;

        // Constructors and getters...
        public MetadataUpdate(Long blockNumber) {
            this.blockNumber = blockNumber;
        }

        public Long getBlockNumber() { return blockNumber; }
        public String getNewCategory() { return newCategory; }
        public String getNewKeywords() { return newKeywords; }
        public boolean isAppendKeywords() { return appendKeywords; }
        public String getNewSearchableContent() { return newSearchableContent; }
        public boolean isAppendContent() { return appendContent; }
        public Map<String, Object> getCustomMetadata() { return customMetadata; }
        
        // Fluent setters...
        public MetadataUpdate withCategory(String category) {
            this.newCategory = category;
            return this;
        }
        
        public MetadataUpdate withKeywords(String keywords, boolean append) {
            this.newKeywords = keywords;
            this.appendKeywords = append;
            return this;
        }
        
        public MetadataUpdate withSearchableContent(String content, boolean append) {
            this.newSearchableContent = content;
            this.appendContent = append;
            return this;
        }
        
        public MetadataUpdate withCustomMetadata(Map<String, Object> metadata) {
            this.customMetadata = new HashMap<>(metadata);
            return this;
        }
    }
    
    public static class BatchProcessingResult {
        private final List<MetadataUpdate> successful = new ArrayList<>();
        private final Map<MetadataUpdate, String> failed = new HashMap<>();
        
        public void addSuccess(MetadataUpdate update) {
            successful.add(update);
        }
        
        public void addFailure(MetadataUpdate update, String reason) {
            failed.put(update, reason);
        }
        
        public int getSuccessCount() { return successful.size(); }
        public int getFailureCount() { return failed.size(); }
        public int getTotalCount() { return successful.size() + failed.size(); }
        
        public double getSuccessRate() {
            return getTotalCount() > 0 ? (double) getSuccessCount() / getTotalCount() : 0.0;
        }
        
        public void printSummary() {
            System.out.println("\n📊 Batch Processing Summary:");
            System.out.println("✅ Successful updates: " + getSuccessCount());
            System.out.println("❌ Failed updates: " + getFailureCount());
            System.out.println("📈 Success rate: " + String.format("%.1f%%", getSuccessRate() * 100));
            
            if (!failed.isEmpty()) {
                System.out.println("\n❌ Failed Updates:");
                failed.forEach((update, reason) -> {
                    System.out.println("  - Block " + update.getBlockNumber() + ": " + reason);
                });
            }
        }
    }
}
```

## 🔧 Troubleshooting

### **Common Issues**

1. **Metadata Not Searchable After Update**
   ```java
   // Solution: Force cache rebuild if needed
   api.rebuildEncryptedBlocksCache();
   ```

2. **Performance Degradation After Many Updates**
   ```java
   // Solution: Manual cache optimization
   blockchain.getSearchSpecialistAPI().optimizeIndexes();
   ```

3. **Memory Usage High After Updates**
   ```java
   // Solution: Garbage collection hint
   System.gc();
   ```

## 🎯 Use Cases

### **Content Management System**
- **Dynamic categorization** of documents
- **Keyword enhancement** for better search
- **Metadata enrichment** from external systems

### **Medical Records**
- **Patient information updates** without touching encrypted data
- **Priority classification** for urgent cases
- **Department routing** information

### **Document Management**
- **Version tracking** in custom metadata
- **Access control** information updates
- **Audit trail** enhancement

### **Compliance and Auditing**
- **Compliance status** updates
- **Review tracking** information
- **Regulatory metadata** management

---

## 🔍 Custom Metadata Search

The blockchain provides **three powerful methods** to search blocks by their custom metadata JSON fields. These methods enable flexible querying of structured metadata without decrypting block content.

### Search Methods Overview

| Method | Description | Use Case |
|--------|-------------|----------|
| `searchByCustomMetadata()` | Substring search (case-insensitive) | Quick text-based search across all metadata (max 10K results) |
| `searchByCustomMetadataKeyValue()` | Exact key-value pair matching | Precise filtering by specific fields (max 10K results) |
| `searchByCustomMetadataMultipleCriteria()` | Multiple criteria with AND logic | Complex multi-field queries (max 10K results) |

**Note**: All methods are memory-efficient and automatically limited to 10,000 results. For larger datasets, use the paginated versions in BlockRepository (`searchByCustomMetadataKeyValuePaginated()`, `searchByCustomMetadataMultipleCriteriaPaginated()`).

### Method 1: Substring Search

**Purpose**: Find blocks where custom metadata contains a specific substring (case-insensitive).

```java
public List<Block> searchByCustomMetadata(String searchTerm)
```

**Example: Search for all blocks related to "cardiology"**

```java
// Search across all custom metadata
List<Block> cardiologyBlocks = api.searchByCustomMetadata("cardiology");

System.out.println("Found " + cardiologyBlocks.size() + " cardiology-related blocks");
for (Block block : cardiologyBlocks) {
    System.out.println("Block #" + block.getBlockNumber() + ": " + block.getCustomMetadata());
}
```

**Example: Case-insensitive search**

```java
// All these searches return the same results
List<Block> results1 = api.searchByCustomMetadata("URGENT");
List<Block> results2 = api.searchByCustomMetadata("urgent");
List<Block> results3 = api.searchByCustomMetadata("Urgent");
```

**Features**:
- ✅ Case-insensitive
- ✅ Finds substring matches anywhere in JSON
- ✅ Fast for simple text searches
- ✅ Thread-safe with read locks
- ⚠️ Less precise than key-value search

---

### Method 2: Key-Value Pair Search

**Purpose**: Find blocks with an exact key-value pair in custom metadata JSON.

```java
public List<Block> searchByCustomMetadataKeyValue(String jsonKey, String jsonValue)
```

**Example: Find all high-priority medical records**

```java
// Find blocks where priority = "high"
List<Block> highPriority = api.searchByCustomMetadataKeyValue("priority", "high");

// Find blocks from specific department
List<Block> medicalDept = api.searchByCustomMetadataKeyValue("department", "medical");

// Find blocks with specific status
List<Block> approved = api.searchByCustomMetadataKeyValue("status", "approved");
```

**Example: Working with different data types**

```java
// Store metadata with mixed types
Map<String, Object> metadata = new HashMap<>();
metadata.put("priority_level", 5);           // Integer
metadata.put("cost", 1234.56);              // Double
metadata.put("is_urgent", true);            // Boolean
metadata.put("department", "finance");      // String

String jsonMetadata = objectMapper.writeValueAsString(metadata);
block.setCustomMetadata(jsonMetadata);
blockchain.updateBlock(block);

// Search using string representation of values
List<Block> level5 = api.searchByCustomMetadataKeyValue("priority_level", "5");
List<Block> cost = api.searchByCustomMetadataKeyValue("cost", "1234.56");
List<Block> urgent = api.searchByCustomMetadataKeyValue("is_urgent", "true");
```

**Features**:
- ✅ Exact matching on key-value pairs
- ✅ Parses JSON correctly
- ✅ Handles numeric, boolean, and string values
- ✅ Gracefully handles malformed JSON (skips invalid blocks)
- ✅ Case-sensitive for values
- ✅ **Memory-Efficient**: Automatically limited to 10,000 results
- ⚠️ Requires exact value match (no partial matches)
- 💡 **For large datasets**: Use `blockchain.searchByCustomMetadataKeyValuePaginated(key, value, offset, limit)` for custom pagination

---

### Method 3: Multiple Criteria Search (AND Logic)

**Purpose**: Find blocks matching ALL specified key-value pairs (AND logic).

```java
public List<Block> searchByCustomMetadataMultipleCriteria(Map<String, String> criteria)
```

**Example: Complex medical record query**

```java
// Find high-priority medical records that need review
Map<String, String> criteria = new HashMap<>();
criteria.put("department", "medical");
criteria.put("priority", "high");
criteria.put("status", "needs_review");

List<Block> matches = api.searchByCustomMetadataMultipleCriteria(criteria);  // Max 10K results

System.out.println("Found " + matches.size() + " high-priority medical records needing review");

// For large datasets with more than 10K results, use pagination:
// List<Block> batch = blockchain
//     .searchByCustomMetadataMultipleCriteriaPaginated(criteria, offset, limit);
```

**Example: Financial compliance query**

```java
// Find approved contracts from Q1 2025
Map<String, String> financialCriteria = new HashMap<>();
financialCriteria.put("type", "contract");
financialCriteria.put("status", "approved");
financialCriteria.put("quarter", "Q1");
financialCriteria.put("year", "2025");

List<Block> contracts = api.searchByCustomMetadataMultipleCriteria(financialCriteria);
```

**Features**:
- ✅ AND logic (all criteria must match)
- ✅ Supports multiple fields simultaneously
- ✅ Thread-safe and efficient
- ✅ Validates all criteria before search
- ⚠️ All criteria must match (no OR logic)

---

## 📝 Practical Examples

### Example 1: Medical Department Dashboard

```java
// Initialize API
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
ObjectMapper mapper = new ObjectMapper();

// Store patient records with rich metadata
Map<String, Object> patientMetadata = new HashMap<>();
patientMetadata.put("patient_id", "P12345");
patientMetadata.put("department", "cardiology");
patientMetadata.put("priority", "high");
patientMetadata.put("diagnosis", "hypertension");
patientMetadata.put("physician", "Dr. Johnson");
patientMetadata.put("admission_date", "2025-01-15");
patientMetadata.put("status", "active");

Block patientBlock = blockchain.addBlockAndReturn(
    "Encrypted patient data...",
    keyPair.getPrivate(),
    keyPair.getPublic()
);
patientBlock.setCustomMetadata(mapper.writeValueAsString(patientMetadata));
blockchain.updateBlock(patientBlock);

// Dashboard Query 1: All active cardiology cases
Map<String, String> activeCases = new HashMap<>();
activeCases.put("department", "cardiology");
activeCases.put("status", "active");
List<Block> activeCardiology = api.searchByCustomMetadataMultipleCriteria(activeCases);
System.out.println("Active cardiology cases: " + activeCardiology.size());

// Dashboard Query 2: All high-priority cases
List<Block> highPriority = api.searchByCustomMetadataKeyValue("priority", "high");
System.out.println("High priority cases: " + highPriority.size());

// Dashboard Query 3: Cases for specific physician
List<Block> drJohnsonCases = api.searchByCustomMetadataKeyValue("physician", "Dr. Johnson");
System.out.println("Dr. Johnson's cases: " + drJohnsonCases.size());
```

### Example 2: Legal Document Management

```java
// Store legal contract with detailed metadata
Map<String, Object> contractMetadata = new HashMap<>();
contractMetadata.put("document_type", "contract");
contractMetadata.put("contract_id", "CTR-2025-001");
contractMetadata.put("party_a", "Acme Corp");
contractMetadata.put("party_b", "XYZ Industries");
contractMetadata.put("value", 250000.00);
contractMetadata.put("status", "executed");
contractMetadata.put("execution_date", "2025-01-20");
contractMetadata.put("expiry_date", "2026-01-20");
contractMetadata.put("category", "vendor_agreement");

Block contractBlock = blockchain.addBlockAndReturn(
    "Encrypted contract content...",
    keyPair.getPrivate(),
    keyPair.getPublic()
);
contractBlock.setCustomMetadata(mapper.writeValueAsString(contractMetadata));
blockchain.updateBlock(contractBlock);

// Query 1: Find all executed vendor agreements
Map<String, String> executedVendorContracts = new HashMap<>();
executedVendorContracts.put("document_type", "contract");
executedVendorContracts.put("status", "executed");
executedVendorContracts.put("category", "vendor_agreement");

List<Block> results = api.searchByCustomMetadataMultipleCriteria(executedVendorContracts);

// Query 2: Find contracts with specific party
List<Block> acmeContracts = api.searchByCustomMetadata("Acme Corp");

// Query 3: Find all contracts expiring in 2026
List<Block> expiring2026 = api.searchByCustomMetadata("2026");
```

### Example 3: E-commerce Order Tracking

```java
// Store order with comprehensive metadata
Map<String, Object> orderMetadata = new HashMap<>();
orderMetadata.put("order_id", "ORD-2025-5678");
orderMetadata.put("customer_id", "CUST-001");
orderMetadata.put("order_date", "2025-01-25");
orderMetadata.put("status", "shipped");
orderMetadata.put("total_amount", 1599.99);
orderMetadata.put("payment_method", "credit_card");
orderMetadata.put("shipping_country", "Spain");
orderMetadata.put("priority_shipping", true);

Block orderBlock = blockchain.addBlockAndReturn(
    "Encrypted order details...",
    keyPair.getPrivate(),
    keyPair.getPublic()
);
orderBlock.setCustomMetadata(mapper.writeValueAsString(orderMetadata));
blockchain.updateBlock(orderBlock);

// Query 1: All shipped orders
List<Block> shippedOrders = api.searchByCustomMetadataKeyValue("status", "shipped");

// Query 2: Priority shipments to Spain
Map<String, String> prioritySpain = new HashMap<>();
prioritySpain.put("priority_shipping", "true");
prioritySpain.put("shipping_country", "Spain");
List<Block> priorityOrders = api.searchByCustomMetadataMultipleCriteria(prioritySpain);

// Query 3: Credit card payments
List<Block> creditCardOrders = api.searchByCustomMetadataKeyValue("payment_method", "credit_card");
```

---

## ⚠️ Important Considerations

### Input Validation

All search methods perform rigorous validation:

```java
// ❌ These will throw IllegalArgumentException
api.searchByCustomMetadata(null);           // Null search term
api.searchByCustomMetadata("");             // Empty search term
api.searchByCustomMetadata("   ");          // Whitespace only

api.searchByCustomMetadataKeyValue(null, "value");  // Null key
api.searchByCustomMetadataKeyValue("key", null);    // Null value
api.searchByCustomMetadataKeyValue("", "value");    // Empty key

api.searchByCustomMetadataMultipleCriteria(null);           // Null criteria
api.searchByCustomMetadataMultipleCriteria(new HashMap<>());  // Empty criteria
```

### Performance Tips

1. **Use specific key-value search** when possible (more efficient than substring)
2. **Limit criteria** in multiple criteria searches (fewer fields = faster)
3. **Index frequently searched fields** in your metadata design
4. **Avoid very large metadata** (keep < 10KB per block for best performance)

### Thread Safety

All search methods are **fully thread-safe**:

```java
// Safe to call from multiple threads simultaneously
ExecutorService executor = Executors.newFixedThreadPool(50);

for (int i = 0; i < 100; i++) {
    executor.submit(() -> {
        List<Block> results = api.searchByCustomMetadata("urgent");
        // Process results...
    });
}
```

### Error Handling

```java
try {
    List<Block> results = api.searchByCustomMetadata("medical");

    if (results.isEmpty()) {
        System.out.println("No matching blocks found");
    } else {
        System.out.println("Found " + results.size() + " matching blocks");
    }
} catch (IllegalArgumentException e) {
    System.err.println("Invalid search parameters: " + e.getMessage());
} catch (Exception e) {
    System.err.println("Search failed: " + e.getMessage());
    // Returns empty list on database errors (graceful degradation)
}
```

---

## 📚 Related Documentation

- [Search Framework Guide](../search/SEARCH_FRAMEWORK_GUIDE.md)
- [Encryption API Guide](../reference/API_GUIDE.md)
- [Performance Optimization](../reports/PERFORMANCE_OPTIMIZATION_PLAN.md)
- [Custom Metadata Utilities](../reference/UTILITY_CLASSES_GUIDE.md)

---

**Version**: 1.1  
**Last Updated**: September 12, 2025  
**Status**: Production Ready ✅