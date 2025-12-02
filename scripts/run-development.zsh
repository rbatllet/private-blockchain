#!/usr/bin/env zsh

# 🚀 Run blockchain in DEVELOPMENT mode
# Detailed logging with colors and debug information

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

echo "🔧 Starting Private Blockchain in DEVELOPMENT mode..."
echo "📊 Logging: Detailed with colors and debug info"

# Change to project root directory
cd "$SCRIPT_DIR/.."

# Clean database before running
cleanup_database

# Run with development profile (uses log4j2.xml by default)
mvn clean compile -Pdevelopment
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pdevelopment

echo "✅ Development session completed"