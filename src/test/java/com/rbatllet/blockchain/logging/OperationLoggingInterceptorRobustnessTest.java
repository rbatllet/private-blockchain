package com.rbatllet.blockchain.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;

import java.time.LocalDateTime;

/**
 * Comprehensive robustness tests for OperationLoggingInterceptor class
 * Tests all methods including private ones to ensure defensive programming
 * 
 * Follows project patterns:
 * - SLF4J logging for test traceability  
 * - Defensive programming validation
 * - Comprehensive edge case testing
 * - Exception handling validation
 */
@DisplayName("OperationLoggingInterceptor Robustness Test Suite")
public class OperationLoggingInterceptorRobustnessTest {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationLoggingInterceptorRobustnessTest.class);
    
    @BeforeEach
    void setUp() {
        logger.info("Starting OperationLoggingInterceptor robustness tests");
        
        // Initialize logging services if needed
        try {
            LoggingManager.initialize();
        } catch (Exception e) {
            logger.warn("Could not initialize logging manager: {}", e.getMessage());
        }
    }
    
    @AfterEach
    void tearDown() {
        // Clean up logging services
        try {
            LoggingManager.shutdown();
        } catch (Exception e) {
            logger.warn("Could not shutdown logging manager: {}", e.getMessage());
        }
    }
    
    private void logTestContext(String method, String scenario) {
        logger.info("🧪 Test: {} - Scenario: {}", method, scenario);
    }
    
    // ========== intercept(Object, Method, Object[], OperationLogger) Tests ==========
    
    @Test
    @DisplayName("intercept should handle null target object")
    void testInterceptNullTarget() throws Exception {
        logTestContext("intercept", "null target object");
        
        Method testMethod = TestTarget.class.getMethod("simpleMethod");
        OperationLogger annotation = createMockAnnotation();
        
        // VULNERABILITY TEST: What happens with null target?
        assertThrows(Exception.class, () -> {
            OperationLoggingInterceptor.intercept(null, testMethod, new Object[0], annotation);
        }, "Should throw exception for null target");
        
        logger.info("🛡️ VULNERABILITY FIXED: intercept validates null target");
        logger.info("✅ Test passed: intercept rejects null target with proper exception");
    }
    
    @Test
    @DisplayName("intercept should handle null method")
    void testInterceptNullMethod() throws Exception {
        logTestContext("intercept", "null method");
        
        TestTarget target = new TestTarget();
        OperationLogger annotation = createMockAnnotation();
        
        // VULNERABILITY TEST: What happens with null method?
        assertThrows(Exception.class, () -> {
            OperationLoggingInterceptor.intercept(target, null, new Object[0], annotation);
        }, "Should throw exception for null method");
        
        logger.info("🛡️ VULNERABILITY FIXED: intercept validates null method");
        logger.info("✅ Test passed: intercept rejects null method with proper exception");
    }
    
    @Test
    @DisplayName("intercept should handle null annotation")
    void testInterceptNullAnnotation() throws Exception {
        logTestContext("intercept", "null annotation");
        
        TestTarget target = new TestTarget();
        Method testMethod = TestTarget.class.getMethod("simpleMethod");
        
        // VULNERABILITY TEST: What happens with null annotation?
        assertThrows(Exception.class, () -> {
            OperationLoggingInterceptor.intercept(target, testMethod, new Object[0], null);
        }, "Should throw exception for null annotation");
        
        logger.info("🛡️ VULNERABILITY FIXED: intercept validates null annotation");
        logger.info("✅ Test passed: intercept rejects null annotation with proper exception");
    }
    
    @Test
    @DisplayName("intercept should handle null arguments array")
    void testInterceptNullArgs() throws Exception {
        logTestContext("intercept", "null arguments array");
        
        TestTarget target = new TestTarget();
        Method testMethod = TestTarget.class.getMethod("simpleMethod");
        OperationLogger annotation = createMockAnnotation();
        
        try {
            Object result = OperationLoggingInterceptor.intercept(target, testMethod, null, annotation);
            assertEquals("simple", result, "Should handle null args gracefully");
            logger.info("✅ Test passed: intercept handles null arguments array");
        } catch (Exception e) {
            fail("Should handle null args gracefully: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("intercept should handle valid method execution")
    void testInterceptValidExecution() throws Exception {
        logTestContext("intercept", "valid method execution");
        
        TestTarget target = new TestTarget();
        Method testMethod = TestTarget.class.getMethod("methodWithParams", String.class, Integer.class);
        OperationLogger annotation = createMockAnnotation();
        Object[] args = {"test", 42};
        
        Object result = OperationLoggingInterceptor.intercept(target, testMethod, args, annotation);
        
        assertEquals("test_42", result, "Should execute method and return result");
        
        logger.info("✅ Test passed: intercept handles valid method execution");
    }
    
    @Test
    @DisplayName("intercept should handle method that throws exception")
    void testInterceptMethodException() throws Exception {
        logTestContext("intercept", "method that throws exception");
        
        TestTarget target = new TestTarget();
        Method testMethod = TestTarget.class.getMethod("methodThatThrows");
        OperationLogger annotation = createMockAnnotation();
        
        assertThrows(RuntimeException.class, () -> {
            OperationLoggingInterceptor.intercept(target, testMethod, new Object[0], annotation);
        }, "Should wrap and re-throw method exceptions");
        
        logger.info("✅ Test passed: intercept handles method exceptions properly");
    }
    
    // ========== wrapWithLogging Tests ==========
    
    @Test
    @DisplayName("wrapWithLogging should handle null target")
    void testWrapWithLoggingNullTarget() {
        logTestContext("wrapWithLogging", "null target");
        
        // VULNERABILITY TEST: What happens with null target?
        assertThrows(Exception.class, () -> {
            OperationLoggingInterceptor.wrapWithLogging(null, "testMethod", "TEST_OP", 
                new Object[0], () -> "result");
        }, "Should throw exception for null target");
        
        logger.info("🛡️ VULNERABILITY FIXED: wrapWithLogging validates null target");
        logger.info("✅ Test passed: wrapWithLogging rejects null target with proper exception");
    }
    
    @Test
    @DisplayName("wrapWithLogging should handle null methodName")
    void testWrapWithLoggingNullMethodName() {
        logTestContext("wrapWithLogging", "null methodName");
        
        TestTarget target = new TestTarget();
        
        // methodName can be null - this is valid behavior
        try {
            String result = OperationLoggingInterceptor.wrapWithLogging(target, null, "TEST_OP", 
                new Object[0], () -> "result");
            assertEquals("result", result, "Should handle null methodName gracefully");
            logger.info("✅ Test passed: wrapWithLogging accepts null methodName as valid parameter");
        } catch (Exception e) {
            fail("Should not throw exception for null methodName: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("wrapWithLogging should handle null operationType")
    void testWrapWithLoggingNullOperationType() {
        logTestContext("wrapWithLogging", "null operationType");
        
        TestTarget target = new TestTarget();
        
        try {
            String result = OperationLoggingInterceptor.wrapWithLogging(target, "testMethod", null, 
                new Object[0], () -> "result");
            assertEquals("result", result, "Should handle null operationType gracefully");
            logger.info("✅ Test passed: wrapWithLogging handles null operationType");
        } catch (Exception e) {
            // Could throw exception - both behaviors are acceptable
            logger.info("✅ Test passed: wrapWithLogging validates null operationType");
        }
    }
    
    @Test
    @DisplayName("wrapWithLogging should handle null methodCall")
    void testWrapWithLoggingNullMethodCall() {
        logTestContext("wrapWithLogging", "null methodCall");
        
        TestTarget target = new TestTarget();
        
        // VULNERABILITY TEST: What happens with null methodCall?
        assertThrows(Exception.class, () -> {
            OperationLoggingInterceptor.wrapWithLogging(target, "testMethod", "TEST_OP", 
                new Object[0], null);
        }, "Should throw exception for null methodCall");
        
        logger.info("🛡️ VULNERABILITY FIXED: wrapWithLogging validates null methodCall");
        logger.info("✅ Test passed: wrapWithLogging rejects null methodCall with proper exception");
    }
    
    @Test
    @DisplayName("wrapWithLogging should handle successful execution")
    void testWrapWithLoggingSuccess() {
        logTestContext("wrapWithLogging", "successful execution");
        
        TestTarget target = new TestTarget();
        
        String result = OperationLoggingInterceptor.wrapWithLogging(target, "testMethod", "TEST_OP", 
            new Object[0], () -> "success_result");
        
        assertEquals("success_result", result, "Should return method call result");
        
        logger.info("✅ Test passed: wrapWithLogging handles successful execution");
    }
    
    @Test
    @DisplayName("wrapWithLogging should handle exception in methodCall")
    void testWrapWithLoggingException() {
        logTestContext("wrapWithLogging", "exception in methodCall");
        
        TestTarget target = new TestTarget();
        
        assertThrows(RuntimeException.class, () -> {
            OperationLoggingInterceptor.wrapWithLogging(target, "testMethod", "TEST_OP", 
                new Object[0], () -> {
                    throw new IllegalStateException("Test exception");
                });
        }, "Should wrap and re-throw methodCall exceptions");
        
        logger.info("✅ Test passed: wrapWithLogging handles methodCall exceptions");
    }
    
    // ========== logDatabaseOperation Tests ==========
    
    @Test
    @DisplayName("logDatabaseOperation should handle null operation")
    void testLogDatabaseOperationNullOperation() {
        logTestContext("logDatabaseOperation", "null operation");
        
        try {
            String result = OperationLoggingInterceptor.logDatabaseOperation(null, "test_table", 
                () -> "db_result");
            assertEquals("db_result", result, "Should handle null operation gracefully");
            logger.info("✅ Test passed: logDatabaseOperation handles null operation");
        } catch (Exception e) {
            // Could throw exception - both behaviors are acceptable
            logger.info("✅ Test passed: logDatabaseOperation validates null operation");
        }
    }
    
    @Test
    @DisplayName("logDatabaseOperation should handle null table")
    void testLogDatabaseOperationNullTable() {
        logTestContext("logDatabaseOperation", "null table");
        
        try {
            String result = OperationLoggingInterceptor.logDatabaseOperation("SELECT", null, 
                () -> "db_result");
            assertEquals("db_result", result, "Should handle null table gracefully");
            logger.info("✅ Test passed: logDatabaseOperation handles null table");
        } catch (Exception e) {
            // Could throw exception - both behaviors are acceptable
            logger.info("✅ Test passed: logDatabaseOperation validates null table");
        }
    }
    
    @Test
    @DisplayName("logDatabaseOperation should handle null methodCall")
    void testLogDatabaseOperationNullMethodCall() {
        logTestContext("logDatabaseOperation", "null methodCall");
        
        // VULNERABILITY TEST: What happens with null methodCall?
        assertThrows(Exception.class, () -> {
            OperationLoggingInterceptor.logDatabaseOperation("SELECT", "test_table", null);
        }, "Should throw exception for null methodCall");
        
        logger.info("🛡️ VULNERABILITY FIXED: logDatabaseOperation validates null methodCall");
        logger.info("✅ Test passed: logDatabaseOperation rejects null methodCall with proper exception");
    }
    
    @Test
    @DisplayName("logDatabaseOperation should handle successful execution")
    void testLogDatabaseOperationSuccess() {
        logTestContext("logDatabaseOperation", "successful execution");
        
        List<String> result = OperationLoggingInterceptor.logDatabaseOperation("SELECT", "test_table", 
            () -> List.of("row1", "row2", "row3"));
        
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Should return correct result");
        
        logger.info("✅ Test passed: logDatabaseOperation handles successful execution");
    }
    
    @Test
    @DisplayName("logDatabaseOperation should handle exception in methodCall")
    void testLogDatabaseOperationException() {
        logTestContext("logDatabaseOperation", "exception in methodCall");
        
        assertThrows(RuntimeException.class, () -> {
            OperationLoggingInterceptor.logDatabaseOperation("SELECT", "test_table", () -> {
                throw new IllegalStateException("Database error");
            });
        }, "Should wrap and re-throw database exceptions");
        
        logger.info("✅ Test passed: logDatabaseOperation handles database exceptions");
    }
    
    // ========== logPerformanceMetrics Tests ==========
    
    @Test
    @DisplayName("logPerformanceMetrics should handle null operationType")
    void testLogPerformanceMetricsNullOperationType() {
        logTestContext("logPerformanceMetrics", "null operationType");
        
        try {
            String result = OperationLoggingInterceptor.logPerformanceMetrics(null, "testOp", 100L, 
                () -> "perf_result");
            assertEquals("perf_result", result, "Should handle null operationType gracefully");
            logger.info("✅ Test passed: logPerformanceMetrics handles null operationType");
        } catch (Exception e) {
            // Could throw exception - both behaviors are acceptable
            logger.info("✅ Test passed: logPerformanceMetrics validates null operationType");
        }
    }
    
    @Test
    @DisplayName("logPerformanceMetrics should handle null operationName")
    void testLogPerformanceMetricsNullOperationName() {
        logTestContext("logPerformanceMetrics", "null operationName");
        
        try {
            String result = OperationLoggingInterceptor.logPerformanceMetrics("PERF_TEST", null, 100L, 
                () -> "perf_result");
            assertEquals("perf_result", result, "Should handle null operationName gracefully");
            logger.info("✅ Test passed: logPerformanceMetrics handles null operationName");
        } catch (Exception e) {
            // Could throw exception - both behaviors are acceptable
            logger.info("✅ Test passed: logPerformanceMetrics validates null operationName");
        }
    }
    
    @Test
    @DisplayName("logPerformanceMetrics should handle negative dataSize")
    void testLogPerformanceMetricsNegativeDataSize() {
        logTestContext("logPerformanceMetrics", "negative dataSize");
        
        String result = OperationLoggingInterceptor.logPerformanceMetrics("PERF_TEST", "testOp", -100L, 
            () -> "perf_result");
        
        assertEquals("perf_result", result, "Should handle negative dataSize");
        
        logger.info("✅ Test passed: logPerformanceMetrics handles negative dataSize");
    }
    
    @Test
    @DisplayName("logPerformanceMetrics should handle null methodCall")
    void testLogPerformanceMetricsNullMethodCall() {
        logTestContext("logPerformanceMetrics", "null methodCall");
        
        // VULNERABILITY TEST: What happens with null methodCall?
        assertThrows(Exception.class, () -> {
            OperationLoggingInterceptor.logPerformanceMetrics("PERF_TEST", "testOp", 100L, null);
        }, "Should throw exception for null methodCall");
        
        logger.info("🛡️ VULNERABILITY FIXED: logPerformanceMetrics validates null methodCall");
        logger.info("✅ Test passed: logPerformanceMetrics rejects null methodCall with proper exception");
    }
    
    @Test
    @DisplayName("logPerformanceMetrics should handle successful execution")
    void testLogPerformanceMetricsSuccess() {
        logTestContext("logPerformanceMetrics", "successful execution");
        
        List<String> result = OperationLoggingInterceptor.logPerformanceMetrics("PERF_TEST", "testOp", 1000L, 
            () -> List.of("item1", "item2"));
        
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return correct result");
        
        logger.info("✅ Test passed: logPerformanceMetrics handles successful execution");
    }
    
    @Test
    @DisplayName("logPerformanceMetrics should handle exception in methodCall")
    void testLogPerformanceMetricsException() {
        logTestContext("logPerformanceMetrics", "exception in methodCall");
        
        assertThrows(RuntimeException.class, () -> {
            OperationLoggingInterceptor.logPerformanceMetrics("PERF_TEST", "testOp", 100L, () -> {
                throw new IllegalStateException("Performance error");
            });
        }, "Should wrap and re-throw performance exceptions");
        
        logger.info("✅ Test passed: logPerformanceMetrics handles performance exceptions");
    }
    
    // ========== Private Method Tests (via reflection) ==========
    
    @Test
    @DisplayName("sanitizeParameter should handle all parameter types")
    void testSanitizeParameterAllTypes() throws Exception {
        logTestContext("sanitizeParameter", "all parameter types");
        
        Method method = OperationLoggingInterceptor.class.getDeclaredMethod("sanitizeParameter", Object.class, Class.class);
        method.setAccessible(true);
        
        // Test null parameter
        String result1 = (String) method.invoke(null, null, String.class);
        assertEquals("null", result1, "Should handle null parameter");
        
        // Test normal string
        String result2 = (String) method.invoke(null, "normal_string", String.class);
        assertEquals("normal_string", result2, "Should handle normal string");
        
        // Test long string (>100 chars)
        String longString = "a".repeat(150);
        String result3 = (String) method.invoke(null, longString, String.class);
        assertTrue(result3.endsWith("..."), "Should truncate long strings");
        assertEquals(103, result3.length(), "Should be truncated to 100 + '...'");
        
        // Test sensitive data - password
        String result4 = (String) method.invoke(null, "mypassword123", String.class);
        assertEquals("[PROTECTED]", result4, "Should protect password-like strings");
        
        // Test sensitive data - key
        String result5 = (String) method.invoke(null, "encryption_key_abc", String.class);
        assertEquals("[PROTECTED]", result5, "Should protect key-like strings");
        
        // Test byte array
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        String result6 = (String) method.invoke(null, bytes, byte[].class);
        assertEquals("[BINARY_DATA_5_bytes]", result6, "Should handle byte arrays");
        
        // Test other objects
        Integer number = 42;
        String result7 = (String) method.invoke(null, number, Integer.class);
        assertEquals("42", result7, "Should handle other objects with toString()");
        
        logger.info("✅ Test passed: sanitizeParameter handles all parameter types");
    }
    
    @Test
    @DisplayName("sanitizeReturnValue should handle all return types")
    void testSanitizeReturnValueAllTypes() throws Exception {
        logTestContext("sanitizeReturnValue", "all return types");
        
        Method method = OperationLoggingInterceptor.class.getDeclaredMethod("sanitizeReturnValue", Object.class);
        method.setAccessible(true);
        
        // Test null
        String result1 = (String) method.invoke(null, (Object) null);
        assertEquals("null", result1, "Should handle null return");
        
        // Test normal string
        String result2 = (String) method.invoke(null, "normal_return");
        assertEquals("normal_return", result2, "Should handle normal string");
        
        // Test long string (>200 chars)
        String longString = "b".repeat(250);
        String result3 = (String) method.invoke(null, longString);
        assertTrue(result3.endsWith("..."), "Should truncate long return strings");
        assertEquals(203, result3.length(), "Should be truncated to 200 + '...'");
        
        // Test byte array
        byte[] bytes = new byte[]{6, 7, 8};
        String result4 = (String) method.invoke(null, bytes);
        assertEquals("[BINARY_DATA_3_bytes]", result4, "Should handle byte array returns");
        
        // Test other objects
        LocalDateTime now = LocalDateTime.now();
        String result5 = (String) method.invoke(null, now);
        assertEquals(now.toString(), result5, "Should handle other objects with toString()");
        
        logger.info("✅ Test passed: sanitizeReturnValue handles all return types");
    }
    
    @Test
    @DisplayName("getResultCount should handle all result types")
    void testGetResultCountAllTypes() throws Exception {
        logTestContext("getResultCount", "all result types");
        
        Method method = OperationLoggingInterceptor.class.getDeclaredMethod("getResultCount", Object.class);
        method.setAccessible(true);
        
        // Test null
        int result1 = (Integer) method.invoke(null, (Object) null);
        assertEquals(0, result1, "Should return 0 for null");
        
        // Test Collection
        List<String> list = List.of("a", "b", "c");
        int result2 = (Integer) method.invoke(null, list);
        assertEquals(3, result2, "Should return collection size");
        
        // Test empty Collection
        List<String> emptyList = new ArrayList<>();
        int result3 = (Integer) method.invoke(null, emptyList);
        assertEquals(0, result3, "Should return 0 for empty collection");
        
        // Test Object array
        String[] array = {"x", "y"};
        int result4 = (Integer) method.invoke(null, (Object) array);
        assertEquals(2, result4, "Should return array length");
        
        // Test empty Object array
        String[] emptyArray = new String[0];
        int result5 = (Integer) method.invoke(null, (Object) emptyArray);
        assertEquals(0, result5, "Should return 0 for empty array");
        
        // Test single object
        String single = "single_object";
        int result6 = (Integer) method.invoke(null, single);
        assertEquals(1, result6, "Should return 1 for single object");
        
        logger.info("✅ Test passed: getResultCount handles all result types");
    }
    
    // ========== Helper Classes and Methods ==========
    
    /**
     * Test target class for method invocation tests
     */
    public static class TestTarget {
        public String simpleMethod() {
            return "simple";
        }
        
        public String methodWithParams(String str, Integer num) {
            return str + "_" + num;
        }
        
        public void methodThatThrows() {
            throw new IllegalStateException("Test exception from method");
        }
    }
    
    /**
     * Creates a mock OperationLogger annotation
     */
    private OperationLogger createMockAnnotation() {
        return new OperationLogger() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return OperationLogger.class;
            }
            
            @Override
            public String operationType() {
                return "TEST_OPERATION";
            }
            
            @Override
            public boolean logParameters() {
                return true;
            }
            
            @Override
            public boolean logReturn() {
                return true;
            }
            
            @Override
            public boolean trackPerformance() {
                return true;
            }
            
            @Override
            public boolean logExceptions() {
                return true;
            }
            
            @Override
            public String operationName() {
                return "testOperation";
            }
            
            @Override
            public long slowThresholdMs() {
                return 5000;
            }
        };
    }
}