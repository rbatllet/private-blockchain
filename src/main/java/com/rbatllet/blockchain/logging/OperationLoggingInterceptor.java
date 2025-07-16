package com.rbatllet.blockchain.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for automatic operation logging based on @OperationLogger annotation
 * Provides comprehensive logging for annotated methods
 */
public class OperationLoggingInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationLoggingInterceptor.class);
    
    /**
     * Intercept method execution and provide comprehensive logging
     * @param target Target object
     * @param method Method being called
     * @param args Method arguments
     * @param annotation OperationLogger annotation
     * @return Method result
     */
    public static Object intercept(Object target, Method method, Object[] args, OperationLogger annotation) {
        String operationType = annotation.operationType();
        String operationName = annotation.operationName().isEmpty() ? 
                              method.getName() : annotation.operationName();
        
        // Prepare context
        Map<String, String> context = new HashMap<>();
        context.put("class", target.getClass().getSimpleName());
        context.put("method", method.getName());
        
        if (annotation.logParameters() && args != null && args.length > 0) {
            context.put("paramCount", String.valueOf(args.length));
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    // Don't log sensitive data like passwords
                    String paramValue = sanitizeParameter(args[i], method.getParameterTypes()[i]);
                    context.put("param" + i, paramValue);
                }
            }
        }
        
        // Start operation tracking
        String operationId = AdvancedLoggingService.startOperation(operationType, operationName, context);
        
        Object result = null;
        boolean success = false;
        String errorMessage = null;
        
        try {
            // Execute the method
            result = method.invoke(target, args);
            success = true;
            
            return result;
            
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            
            if (annotation.logExceptions()) {
                logger.error("❌ Exception in {} [{}]: {}", operationType, operationId, e.getMessage(), e);
            }
            
            throw new RuntimeException("Operation failed: " + operationName, e);
            
        } finally {
            // End operation tracking
            int resultCount = getResultCount(result);
            String additionalInfo = null;
            
            if (annotation.logReturn() && result != null) {
                additionalInfo = "Result: " + sanitizeReturnValue(result);
            }
            
            if (errorMessage != null) {
                additionalInfo = (additionalInfo != null ? additionalInfo + ", " : "") + "Error: " + errorMessage;
            }
            
            AdvancedLoggingService.endOperation(operationId, success, resultCount, additionalInfo);
        }
    }
    
    /**
     * Wrap method calls with logging for classes that implement logging manually
     * @param target Target object
     * @param methodName Method name
     * @param operationType Operation type
     * @param args Method arguments
     * @param methodCall Functional interface for method execution
     * @return Method result
     */
    public static <T> T wrapWithLogging(Object target, String methodName, String operationType, 
                                       Object[] args, MethodCall<T> methodCall) {
        Map<String, String> context = new HashMap<>();
        context.put("class", target.getClass().getSimpleName());
        context.put("method", methodName);
        
        String operationId = AdvancedLoggingService.startOperation(operationType, methodName, context);
        
        try {
            T result = methodCall.call();
            
            int resultCount = getResultCount(result);
            AdvancedLoggingService.endOperation(operationId, true, resultCount, null);
            
            return result;
            
        } catch (Exception e) {
            AdvancedLoggingService.endOperation(operationId, false, 0, "Error: " + e.getMessage());
            throw new RuntimeException("Operation failed", e);
        }
    }
    
    /**
     * Log database operations with standard format
     * @param operation Database operation type
     * @param table Table name
     * @param methodCall Method to execute
     * @return Method result
     */
    public static <T> T logDatabaseOperation(String operation, String table, DatabaseCall<T> methodCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            T result = methodCall.call();
            
            long duration = System.currentTimeMillis() - startTime;
            int rowsAffected = getResultCount(result);
            
            AdvancedLoggingService.logDatabaseOperation(operation, table, duration, rowsAffected);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            AdvancedLoggingService.logDatabaseOperation(operation, table, duration, 0);
            throw new RuntimeException("Database operation failed", e);
        }
    }
    
    /**
     * Log performance metrics for operations
     * @param operationType Operation type
     * @param operationName Operation name
     * @param dataSize Size of data processed
     * @param methodCall Method to execute
     * @return Method result
     */
    public static <T> T logPerformanceMetrics(String operationType, String operationName, 
                                             long dataSize, MethodCall<T> methodCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            T result = methodCall.call();
            
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> details = new HashMap<>();
            details.put("resultCount", getResultCount(result));
            details.put("success", true);
            
            AdvancedLoggingService.logPerformanceMetrics(operationType, operationName, duration, dataSize, details);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> details = new HashMap<>();
            details.put("success", false);
            details.put("error", e.getMessage());
            
            AdvancedLoggingService.logPerformanceMetrics(operationType, operationName, duration, dataSize, details);
            throw new RuntimeException("Performance logging failed", e);
        }
    }
    
    // Helper methods
    
    private static String sanitizeParameter(Object param, Class<?> paramType) {
        if (param == null) return "null";
        
        // Don't log sensitive data
        if (paramType == String.class) {
            String str = (String) param;
            if (str.length() > 100) {
                return str.substring(0, 100) + "...";
            }
            // Check for potential passwords or keys
            if (str.toLowerCase().contains("password") || 
                str.toLowerCase().contains("key") ||
                str.toLowerCase().contains("secret")) {
                return "[PROTECTED]";
            }
        }
        
        if (param instanceof byte[]) {
            return "[BINARY_DATA_" + ((byte[]) param).length + "_bytes]";
        }
        
        return param.toString();
    }
    
    private static String sanitizeReturnValue(Object result) {
        if (result == null) return "null";
        
        if (result instanceof String) {
            String str = (String) result;
            if (str.length() > 200) {
                return str.substring(0, 200) + "...";
            }
        }
        
        if (result instanceof byte[]) {
            return "[BINARY_DATA_" + ((byte[]) result).length + "_bytes]";
        }
        
        return result.toString();
    }
    
    private static int getResultCount(Object result) {
        if (result == null) return 0;
        
        if (result instanceof java.util.Collection) {
            return ((java.util.Collection<?>) result).size();
        }
        
        if (result instanceof Object[]) {
            return ((Object[]) result).length;
        }
        
        return 1;
    }
    
    /**
     * Functional interface for method calls
     */
    @FunctionalInterface
    public interface MethodCall<T> {
        T call() throws Exception;
    }
    
    /**
     * Functional interface for database calls
     */
    @FunctionalInterface
    public interface DatabaseCall<T> {
        T call() throws Exception;
    }
}