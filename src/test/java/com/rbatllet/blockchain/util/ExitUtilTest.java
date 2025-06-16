package com.rbatllet.blockchain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ExitUtil
 */
public class ExitUtilTest {
    
    @BeforeEach
    public void setUp() {
        // Ensure exit is enabled before each test
        ExitUtil.enableExit();
    }
    
    @AfterEach
    public void tearDown() {
        // Ensure exit is enabled after each test
        ExitUtil.enableExit();
    }
    
    @Test
    public void testDisableExit() {
        ExitUtil.disableExit();
        assertTrue(ExitUtil.isExitDisabled(), "Exit should be disabled after calling disableExit()");
        assertEquals(0, ExitUtil.getLastExitCode(), "Last exit code should be 0 initially");
    }
    
    @Test
    public void testEnableExit() {
        ExitUtil.disableExit();
        assertTrue(ExitUtil.isExitDisabled(), "Exit should be disabled after calling disableExit()");
        
        ExitUtil.enableExit();
        assertFalse(ExitUtil.isExitDisabled(), "Exit should be enabled after calling enableExit()");
    }
    
    @Test
    public void testExitWithDisabledExit() {
        ExitUtil.disableExit();
        
        // This would normally terminate the JVM, but with exit disabled it just records the code
        ExitUtil.exit(42);
        
        assertEquals(42, ExitUtil.getLastExitCode(), "Last exit code should be 42");
        assertTrue(ExitUtil.isExitDisabled(), "Exit should still be disabled");
    }
    
    @Test
    public void testResetExitCodeOnDisable() {
        ExitUtil.disableExit();
        ExitUtil.exit(42);
        assertEquals(42, ExitUtil.getLastExitCode(), "Last exit code should be 42");
        
        ExitUtil.disableExit(); // Should reset the exit code
        assertEquals(0, ExitUtil.getLastExitCode(), "Last exit code should be reset to 0");
    }
    
    @Test
    public void testResetExitCodeOnEnable() {
        ExitUtil.disableExit();
        ExitUtil.exit(42);
        assertEquals(42, ExitUtil.getLastExitCode(), "Last exit code should be 42");
        
        ExitUtil.enableExit(); // Should reset the exit code
        assertEquals(0, ExitUtil.getLastExitCode(), "Last exit code should be reset to 0");
    }
}
