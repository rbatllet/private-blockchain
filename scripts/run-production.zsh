#!/usr/bin/env zsh

# 🚀 Run blockchain in PRODUCTION mode  
# Optimized logging for maximum performance

echo "🏭 Starting Private Blockchain in PRODUCTION mode..."
echo "⚡ Logging: Optimized for performance (WARN+ only)"

# Change to project root directory
cd "$(dirname "$0")/.."

# Run with production profile (uses log4j2-production.xml automatically)
mvn clean compile -Pproduction
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pproduction

echo "✅ Production session completed"