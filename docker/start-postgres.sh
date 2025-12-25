#!/usr/bin/env zsh
# =============================================================================
# PostgreSQL Docker SSL Setup - Quick Start Script
# =============================================================================
# This script automates the setup of PostgreSQL with SSL/TLS
# Usage: ./start-postgres.sh
# =============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${(%):-%x}")" && pwd)"

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}PostgreSQL Docker SSL Setup${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Step 1: Generate certificates
echo -e "${GREEN}[1/3]${NC} Generating SSL certificates..."
cd "${SCRIPT_DIR}/postgresql"
if [[ ! -f "certs/ca.pem" ]]; then
    chmod +x generate-certs.sh
    ./generate-certs.sh
else
    echo -e "  ${YELLOW}⚠${NC} Certificates already exist, skipping generation"
fi
echo ""

# Step 2: Start Docker container
echo -e "${GREEN}[2/3]${NC} Starting PostgreSQL Docker container..."
cd "${SCRIPT_DIR}"
docker-compose -f docker-compose-postgres.yml up -d
echo ""

# Step 3: Wait for PostgreSQL to be ready and test SSL
echo -e "${GREEN}[3/3]${NC} Waiting for PostgreSQL to be ready..."
sleep 10

echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}PostgreSQL SSL Setup Complete!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""
echo -e "${YELLOW}Connection Information:${NC}"
echo -e "  ${BLUE}Host:${NC}     localhost"
echo -e "  ${BLUE}Port:${NC}     5432"
echo -e "  ${BLUE}Database:${NC} blockchain_prod"
echo -e "  ${BLUE}User:${NC}     blockchain_user"
echo -e "  ${BLUE}Password:${NC} SecurePassword123!"
echo ""
echo -e "${YELLOW}Test Connection:${NC}"
echo -e "  ${BLUE}./test-postgres-ssl-connection.sh${NC}"
echo ""
echo -e "${YELLOW}JDBC URL (v1.0.6+):${NC}"
echo -e "  ${BLUE}jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=verify-full${NC}"
echo ""
echo -e "${YELLOW}Stop containers:${NC}"
echo -e "  ${BLUE}docker-compose -f docker-compose-postgres.yml down${NC}"
echo ""
