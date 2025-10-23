# 📊 Logging System Guide

## Overview

The Private Blockchain project uses **SLF4J with Log4j2** for professional logging. The system provides automatic configuration based on Maven profiles with specialized appenders for different log types.

## 🎯 Key Features

- **Automatic profile-based configuration** via Maven
- **Specialized log files** for different event types
- **Thread-safe concurrent logging** with proper synchronization
- **Configurable log levels** per environment
- **Automatic file rotation** with compression
- **Zero manual configuration** required

## 📁 Configuration Files

### Active Configurations
- `log4j2-core.xml` - Development configuration (default)
- `log4j2-core-production.xml` - Production configuration
- `log4j2-test.xml` - Test configuration (automatic during tests)

### Log Files Generated
- `logs/blockchain.log` - Main application logs
- `logs/structured-alerts.log` - JSON-formatted alerts
- `logs/performance-metrics.log` - Performance tracking data
- `logs/security-events.log` - Security-related events
- `logs/test-app.log` - Test execution logs (tests only)

## ⚙️ Maven Profiles

### Development Profile (Default)
```bash
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pdevelopment
# OR
./scripts/run-development.zsh
```

**Configuration**: `log4j2-core.xml` (automatic)
- **Application logs**: DEBUG level
- **Framework logs**: INFO level
- **Console output**: Enabled with thread names
- **SQL logging**: Enabled for development
- **File rotation**: 10MB files, 10 files max

### Production Profile
```bash
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pproduction
# OR
./scripts/run-production.zsh
```

**Configuration**: `log4j2-core-production.xml` (automatic)
- **Application logs**: WARN level only
- **Framework logs**: ERROR level only
- **Console output**: Errors only to STDERR
- **SQL logging**: Disabled
- **File rotation**: 50-100MB files, 30-180 files retention

## 🚀 Usage Examples

### Running the Application
```bash
# Interactive profile selection
./scripts/run-with-profile.zsh

# Direct execution
./scripts/run-development.zsh
./scripts/run-production.zsh

# Manual Maven execution
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pdevelopment
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pproduction
```

### Logging in Code
```java
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

// Standard application logging
logger.info("Processing block {}", blockId);
logger.error("Failed to validate block", exception);

// Specialized loggers
Logger alertLogger = LoggerFactory.getLogger("alerts.structured");
alertLogger.error(structuredAlert);

Logger perfLogger = LoggerFactory.getLogger("performance.metrics");
perfLogger.info(performanceData);

Logger secLogger = LoggerFactory.getLogger("security.events");
secLogger.warn(securityEvent);
```

## 📈 Performance Impact

| Environment | Console Output | File Logging | Performance |
|-------------|---------------|--------------|-------------|
| **Development** | Full (with colors) | Verbose | Optimized for debugging |
| **Production** | Errors only | Minimal | Maximum performance |
| **Testing** | Application logs | Separate files | Fast execution |

## 🔧 Technical Implementation

### Maven Configuration
```xml
<profiles>
    <profile>
        <id>development</id>
        <properties>
            <log4j2.configurationFile>log4j2-core.xml</log4j2.configurationFile>
        </properties>
    </profile>
    <profile>
        <id>production</id>
        <properties>
            <log4j2.configurationFile>log4j2-core-production.xml</log4j2.configurationFile>
        </properties>
    </profile>
</profiles>
```

### Exec Plugin Configuration
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <configuration>
        <mainClass>demo.BlockchainDemo</mainClass>
        <systemProperties>
            <systemProperty>
                <key>log4j2.configurationFile</key>
                <value>file:src/main/resources/${log4j2.configurationFile}</value>
            </systemProperty>
        </systemProperties>
    </configuration>
</plugin>
```

## 🎨 Icon System

Following project guidelines, all log messages use consistent icons:

| Icon | Usage | Example |
|------|-------|---------|
| 🔍 | Debug/Validation | `logger.debug("🔍 Validating block {}", id)` |
| ⚠️ | Warnings | `logger.warn("⚠️ High memory usage: {}MB", usage)` |
| ❌ | Errors | `logger.error("❌ Block validation failed", ex)` |
| ✅ | Success | `logger.info("✅ Block processed successfully")` |
| 📊 | Statistics | `logger.info("📊 Processed {} blocks", count)` |
| 🔐 | Security | `logger.warn("🔐 Security event detected")` |
| 📝 | Data | `logger.info("📝 Data operation completed")` |

## 🔍 Troubleshooting

### Common Issues

**Logs not creating:**
1. Check `logs/` directory permissions
2. Verify Maven profile is active: `mvn help:active-profiles`
3. Ensure Log4j2 dependencies are present

**Wrong log levels:**
1. Verify correct profile: `mvn help:active-profiles`
2. Check configuration file being used
3. Override with system property: `-Dlog4j2.level=DEBUG`

**Performance issues:**
- Use production profile for performance-critical operations
- Check file rotation settings
- Verify async logging configuration

### Verification Commands
```bash
# Check active Maven profile
mvn help:active-profiles

# Verify log files are created
ls -la logs/

# Check log file sizes
wc -l logs/*.log

# Monitor logs in real-time
tail -f logs/blockchain.log
```

## 📚 Migration from Previous System

The system has been fully migrated from Logback to Log4j2:
- ✅ All Logback configurations removed
- ✅ Maven profiles configured for automatic selection
- ✅ Specialized appenders for different log types
- ✅ Performance optimized for production use
- ✅ Test configuration isolated from main application

## 🎉 Summary

The Private Blockchain logging system provides:
- **Zero-configuration** profile-based logging
- **Automatic environment detection** via Maven profiles
- **Specialized log files** for different event types
- **Production-optimized performance** with minimal overhead
- **Developer-friendly** detailed logging in development mode
- **Enterprise-ready** with proper file rotation and retention