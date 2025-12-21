package com.rbatllet.blockchain.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for handling custom metadata serialization and deserialization
 * Provides thread-safe JSON operations for block metadata
 */
public class CustomMetadataUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomMetadataUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Serialize metadata map to JSON string
     * 
     * @param metadata The metadata map to serialize
     * @return JSON string representation or null if empty
     * @throws RuntimeException if serialization fails
     */
    public static String serializeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        // Create defensive copy to avoid ConcurrentModificationException
        final Map<String, String> metadataCopy;
        try {
            metadataCopy = new LinkedHashMap<>(metadata);
        } catch (Exception e) {
            // Handle potential concurrent modification during copy
            logger.error("❌ Failed to create defensive copy of metadata", e);
            throw new RuntimeException("Failed to create defensive copy of metadata", e);
        }
        
        try {
            return objectMapper.writeValueAsString(metadataCopy);
        } catch (JacksonException e) {
            logger.error("❌ Failed to serialize custom metadata: {}", metadataCopy, e);
            throw new RuntimeException("Failed to serialize custom metadata", e);
        }
    }
    
    /**
     * Deserialize JSON string to metadata map
     * 
     * @param jsonString The JSON string to deserialize
     * @return Map containing metadata or empty map if null/empty
     * @throws RuntimeException if deserialization fails
     */
    public static Map<String, String> deserializeMetadata(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
            Map<String, String> result = objectMapper.readValue(jsonString, typeRef);
            // Return immutable map for thread safety
            return result != null ? Collections.unmodifiableMap(result) : Collections.emptyMap();
        } catch (JacksonException e) {
            logger.error("❌ Failed to deserialize custom metadata: {}", jsonString, e);
            throw new RuntimeException("Failed to deserialize custom metadata", e);
        }
    }
    
    /**
     * Validate metadata map for security and size constraints
     * 
     * @param metadata The metadata map to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateMetadata(Map<String, String> metadata) {
        if (metadata == null) {
            return;
        }
        
        // Size constraints
        final int MAX_METADATA_ENTRIES = 50;
        final int MAX_KEY_LENGTH = 100;
        final int MAX_VALUE_LENGTH = 1000;
        final int MAX_TOTAL_SIZE = 10000; // characters
        
        // Create defensive copy to avoid ConcurrentModificationException during validation
        final Map<String, String> metadataCopy;
        try {
            metadataCopy = new LinkedHashMap<>(metadata);
        } catch (Exception e) {
            // Handle potential concurrent modification during copy
            logger.error("❌ Failed to create defensive copy for validation", e);
            throw new RuntimeException("Failed to create defensive copy for validation", e);
        }
        
        if (metadataCopy.size() > MAX_METADATA_ENTRIES) {
            throw new IllegalArgumentException("Metadata cannot have more than " + MAX_METADATA_ENTRIES + " entries");
        }
        
        int totalSize = 0;
        for (Map.Entry<String, String> entry : metadataCopy.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Null checks
            if (key == null) {
                throw new IllegalArgumentException("Metadata key cannot be null");
            }
            if (value == null) {
                throw new IllegalArgumentException("Metadata value cannot be null for key: " + key);
            }
            
            // Length checks
            if (key.length() > MAX_KEY_LENGTH) {
                throw new IllegalArgumentException("Metadata key too long (max " + MAX_KEY_LENGTH + " chars): " + key);
            }
            if (value.length() > MAX_VALUE_LENGTH) {
                throw new IllegalArgumentException("Metadata value too long (max " + MAX_VALUE_LENGTH + " chars) for key: " + key);
            }
            
            // Security checks - prevent SQL injection and XSS
            if (key.contains("'") || key.contains("\"") || key.contains("<") || key.contains(">")) {
                throw new IllegalArgumentException("Metadata key contains invalid characters: " + key);
            }
            
            totalSize += key.length() + value.length();
        }
        
        if (totalSize > MAX_TOTAL_SIZE) {
            throw new IllegalArgumentException("Total metadata size too large (max " + MAX_TOTAL_SIZE + " chars): " + totalSize);
        }
    }
}