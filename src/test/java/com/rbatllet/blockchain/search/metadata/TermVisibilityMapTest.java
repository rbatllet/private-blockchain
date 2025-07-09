package com.rbatllet.blockchain.search.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.Arrays;

/**
 * Unit tests for TermVisibilityMap class
 * Tests granular term visibility control functionality
 */
public class TermVisibilityMapTest {
    
    private TermVisibilityMap visibilityMap;
    
    @BeforeEach
    void setUp() {
        visibilityMap = new TermVisibilityMap();
    }
    
    @Test
    void testDefaultPublicVisibility() {
        // Default should be PUBLIC
        assertEquals(TermVisibilityMap.VisibilityLevel.PUBLIC, visibilityMap.getDefaultLevel());
        
        // Unknown terms should default to PUBLIC
        assertTrue(visibilityMap.isPublic("unknown_term"));
        assertFalse(visibilityMap.isPrivate("unknown_term"));
    }
    
    @Test
    void testDefaultPrivateVisibility() {
        TermVisibilityMap privateDefault = new TermVisibilityMap(TermVisibilityMap.VisibilityLevel.PRIVATE);
        
        assertEquals(TermVisibilityMap.VisibilityLevel.PRIVATE, privateDefault.getDefaultLevel());
        assertTrue(privateDefault.isPrivate("unknown_term"));
        assertFalse(privateDefault.isPublic("unknown_term"));
    }
    
    @Test
    void testSetPublicTerms() {
        visibilityMap.setPublic("medical", "patient", "diagnosis");
        
        assertTrue(visibilityMap.isPublic("medical"));
        assertTrue(visibilityMap.isPublic("patient"));
        assertTrue(visibilityMap.isPublic("diagnosis"));
        
        // Case insensitive
        assertTrue(visibilityMap.isPublic("MEDICAL"));
        assertTrue(visibilityMap.isPublic("Patient"));
    }
    
    @Test
    void testSetPrivateTerms() {
        visibilityMap.setPrivate("john", "smith", "cancer");
        
        assertTrue(visibilityMap.isPrivate("john"));
        assertTrue(visibilityMap.isPrivate("smith"));
        assertTrue(visibilityMap.isPrivate("cancer"));
        
        // Case insensitive
        assertTrue(visibilityMap.isPrivate("JOHN"));
        assertTrue(visibilityMap.isPrivate("Smith"));
    }
    
    @Test
    void testMethodChaining() {
        TermVisibilityMap result = visibilityMap
            .setPublic("medical", "patient")
            .setPrivate("john", "smith")
            .setTerm("diagnosis", TermVisibilityMap.VisibilityLevel.PUBLIC);
        
        assertSame(visibilityMap, result);
        assertTrue(visibilityMap.isPublic("medical"));
        assertTrue(visibilityMap.isPrivate("john"));
        assertTrue(visibilityMap.isPublic("diagnosis"));
    }
    
    @Test
    void testGetPublicTerms() {
        visibilityMap.setPublic("medical", "patient");
        visibilityMap.setPrivate("john", "smith");
        
        Set<String> allTerms = Set.of("medical", "patient", "john", "smith", "unknown");
        Set<String> publicTerms = visibilityMap.getPublicTerms(allTerms);
        
        assertEquals(3, publicTerms.size()); // medical, patient, unknown (default)
        assertTrue(publicTerms.contains("medical"));
        assertTrue(publicTerms.contains("patient"));
        assertTrue(publicTerms.contains("unknown"));
        assertFalse(publicTerms.contains("john"));
        assertFalse(publicTerms.contains("smith"));
    }
    
    @Test
    void testGetPrivateTerms() {
        visibilityMap.setPublic("medical", "patient");
        visibilityMap.setPrivate("john", "smith");
        
        Set<String> allTerms = Set.of("medical", "patient", "john", "smith", "unknown");
        Set<String> privateTerms = visibilityMap.getPrivateTerms(allTerms);
        
        assertEquals(2, privateTerms.size()); // john, smith
        assertTrue(privateTerms.contains("john"));
        assertTrue(privateTerms.contains("smith"));
        assertFalse(privateTerms.contains("medical"));
        assertFalse(privateTerms.contains("patient"));
        assertFalse(privateTerms.contains("unknown"));
    }
    
    @Test
    void testOverrideTermVisibility() {
        visibilityMap.setPublic("medical");
        assertTrue(visibilityMap.isPublic("medical"));
        
        // Override to private
        visibilityMap.setPrivate("medical");
        assertTrue(visibilityMap.isPrivate("medical"));
        assertFalse(visibilityMap.isPublic("medical"));
    }
    
    @Test
    void testNullAndEmptyTermHandling() {
        // Should handle null and empty terms gracefully
        visibilityMap.setPublic((String[]) null);
        visibilityMap.setPublic("", "  ", null);
        visibilityMap.setPrivate((String[]) null);
        visibilityMap.setPrivate("", "  ", null);
        
        assertEquals(0, visibilityMap.size());
        assertTrue(visibilityMap.isEmpty());
    }
    
    @Test
    void testExplicitlyConfiguredTerms() {
        visibilityMap.setPublic("medical", "patient");
        visibilityMap.setPrivate("john", "smith");
        
        Set<String> configured = visibilityMap.getExplicitlyConfiguredTerms();
        assertEquals(4, configured.size());
        assertTrue(configured.contains("medical"));
        assertTrue(configured.contains("patient"));
        assertTrue(configured.contains("john"));
        assertTrue(configured.contains("smith"));
    }
    
    @Test
    void testClearAndSize() {
        visibilityMap.setPublic("medical", "patient");
        visibilityMap.setPrivate("john", "smith");
        
        assertEquals(4, visibilityMap.size());
        assertFalse(visibilityMap.isEmpty());
        
        visibilityMap.clear();
        
        assertEquals(0, visibilityMap.size());
        assertTrue(visibilityMap.isEmpty());
    }
    
    @Test
    void testCopy() {
        visibilityMap.setPublic("medical", "patient");
        visibilityMap.setPrivate("john", "smith");
        visibilityMap.setDefaultLevel(TermVisibilityMap.VisibilityLevel.PRIVATE);
        
        TermVisibilityMap copy = visibilityMap.copy();
        
        // Should be separate instance
        assertNotSame(visibilityMap, copy);
        
        // Should have same configuration
        assertEquals(visibilityMap.getDefaultLevel(), copy.getDefaultLevel());
        assertEquals(visibilityMap.size(), copy.size());
        assertTrue(copy.isPublic("medical"));
        assertTrue(copy.isPrivate("john"));
        
        // Changes to copy shouldn't affect original
        copy.setPublic("new_term");
        assertFalse(visibilityMap.isPublic("new_term"));
    }
    
    @Test
    void testMedicalRecordScenario() {
        // Realistic medical record scenario
        visibilityMap
            .setPublic("medical", "patient", "diagnosis", "treatment", "therapy")
            .setPrivate("john", "smith", "cancer", "diabetes");
        
        Set<String> allTerms = Set.of("medical", "patient", "john", "smith", 
                                    "diagnosis", "cancer", "treatment", "diabetes");
        
        Set<String> publicTerms = visibilityMap.getPublicTerms(allTerms);
        Set<String> privateTerms = visibilityMap.getPrivateTerms(allTerms);
        
        // Public searchable: general medical terms
        assertEquals(4, publicTerms.size());
        assertTrue(publicTerms.containsAll(Arrays.asList("medical", "patient", "diagnosis", "treatment")));
        
        // Private searchable: personal identifiers and specific conditions
        assertEquals(4, privateTerms.size());
        assertTrue(privateTerms.containsAll(Arrays.asList("john", "smith", "cancer", "diabetes")));
    }
    
    @Test
    void testFinancialDataScenario() {
        // Realistic financial data scenario with default PRIVATE
        TermVisibilityMap financialMap = new TermVisibilityMap(TermVisibilityMap.VisibilityLevel.PRIVATE);
        financialMap.setPublic("swift", "transfer", "real", "estate", "transaction");
        
        Set<String> allTerms = Set.of("swift", "transfer", "50000", "123-456-789", 
                                    "john", "smith", "real", "estate", "tx789012");
        
        Set<String> publicTerms = financialMap.getPublicTerms(allTerms);
        Set<String> privateTerms = financialMap.getPrivateTerms(allTerms);
        
        // Public: transaction type information
        assertEquals(4, publicTerms.size());
        assertTrue(publicTerms.containsAll(Arrays.asList("swift", "transfer", "real", "estate")));
        
        // Private: sensitive financial details (default + explicit)
        assertEquals(5, privateTerms.size());
        assertTrue(privateTerms.containsAll(Arrays.asList("50000", "123-456-789", "john", "smith", "tx789012")));
    }
    
    @Test
    void testToString() {
        visibilityMap.setPublic("medical");
        visibilityMap.setPrivate("john");
        
        String result = visibilityMap.toString();
        assertTrue(result.contains("TermVisibilityMap"));
        assertTrue(result.contains("default=PUBLIC"));
        assertTrue(result.contains("explicit=2 terms"));
    }
    
    @Test
    void testToDetailedString() {
        visibilityMap.setPublic("medical");
        visibilityMap.setPrivate("john");
        
        String detailed = visibilityMap.toDetailedString();
        assertTrue(detailed.contains("Default Level: PUBLIC"));
        assertTrue(detailed.contains("medical -> PUBLIC"));
        assertTrue(detailed.contains("john -> PRIVATE"));
    }
}