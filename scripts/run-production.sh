#!/usr/bin/env zsh

# 🚀 Run blockchain in PRODUCTION mode  
# Optimized logging for maximum performance

echo "🏭 Starting Private Blockchain in PRODUCTION mode..."
echo "⚡ Logging: Optimized for performance (WARN+ only)"

# Set production logging configuration
export LOGBACK_CONFIG_FILE=logback-production.xml

# Run with production profile
mvn clean compile -Pproduction
mvn exec:java -Pproduction -Dexec.mainClass="demo.BlockchainDemo"

echo "✅ Production session completed"