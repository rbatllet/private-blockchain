#!/usr/bin/env zsh

# Script to run application with different logging profiles

echo "🚀 Private Blockchain - Selecciona perfil de logging"
echo "======================================================"
echo ""
echo "1) Development (logging detallat)"
echo "2) Production (logging mínim)"
echo ""
read -r "profile?Selecciona perfil [1-2]: "

# Change to project root directory
cd "$(dirname "$0")/.."

case $profile in
    1)
        echo "📊 Executant amb perfil DEVELOPMENT..."
        # Uses default configuration (log4j2.xml)
        mvn clean compile exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pdevelopment
        ;;
    2)
        echo "🔐 Executant amb perfil PRODUCTION..."
        # Uses production configuration automatically
        mvn clean compile exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pproduction
        ;;
    *)
        echo "❌ Opció invàlida"
        exit 1
        ;;
esac