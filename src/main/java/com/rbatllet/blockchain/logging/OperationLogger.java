package com.rbatllet.blockchain.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically log method operations with detailed tracking
 * Can be applied to methods that need comprehensive logging
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogger {
    
    /**
     * Operation type for categorization
     */
    String operationType() default "GENERAL";
    
    /**
     * Whether to log method parameters
     */
    boolean logParameters() default false;
    
    /**
     * Whether to log return values
     */
    boolean logReturn() default false;
    
    /**
     * Whether to track performance metrics
     */
    boolean trackPerformance() default true;
    
    /**
     * Whether to log exceptions
     */
    boolean logExceptions() default true;
    
    /**
     * Custom operation name (default uses method name)
     */
    String operationName() default "";
    
    /**
     * Expected operation duration threshold in milliseconds
     * Operations exceeding this will be logged as slow
     */
    long slowThresholdMs() default 5000;
}