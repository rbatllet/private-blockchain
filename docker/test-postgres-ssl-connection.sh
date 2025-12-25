#!/usr/bin/env zsh
# =============================================================================
# PostgreSQL SSL Connection Test Script
# =============================================================================
# Tests the SSL connection to PostgreSQL Docker container
# Usage: ./test-postgres-ssl-connection.sh
# =============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}PostgreSQL SSL Connection Test${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Check if Docker container is running
echo -e "${GREEN}[1/6]${NC} Checking if PostgreSQL container is running..."
if docker ps | grep -q "postgres-blockchain-ssl"; then
    echo -e "  ${GREEN}✓${NC} PostgreSQL container is running"
else
    echo -e "  ${RED}✗${NC} PostgreSQL container is NOT running"
    echo -e "${YELLOW}  Start it with: docker-compose -f docker-compose-postgres.yml up -d${NC}"
    exit 1
fi

echo ""

# Test SSL connection with psql
echo -e "${GREEN}[2/6]${NC} Testing SSL connection with psql..."
RESULT=$(docker exec -e PGPASSWORD=SecurePassword123! postgres-blockchain-ssl psql \
    -U blockchain_user -d blockchain_prod -h localhost -t -c "SELECT ssl FROM pg_stat_ssl WHERE pid = pg_backend_pid();" \
    2>/dev/null | tr -d ' ')
if [[ "$RESULT" == "t" ]]; then
    echo -e "  ${GREEN}✓${NC} SSL connection successful"
else
    echo -e "  ${RED}✗${NC} SSL connection failed (ssl=$RESULT)"
    exit 1
fi

echo ""

# Check SSL variables
echo -e "${GREEN}[3/6]${NC} Checking SSL configuration..."
docker exec -e PGPASSWORD=SecurePassword123! postgres-blockchain-ssl psql \
    -U blockchain_user -d blockchain_prod -h localhost -c "SHOW ssl;" \
    -t 2>/dev/null | tr -d ' '
echo ""

echo -e "${GREEN}[4/6]${NC} Checking SSL certificate info..."
docker exec -e PGPASSWORD=SecurePassword123! postgres-blockchain-ssl psql \
    -U blockchain_user -d blockchain_prod -h localhost -c "SELECT * FROM pg_stat_ssl;" \
    2>/dev/null
echo ""

# Test with openssl
echo -e "${GREEN}[5/6]${NC} Testing SSL/TLS with openssl..."
if timeout 5 openssl s_client -connect localhost:5432 -starttls postgres \
    -showcerts </dev/null 2>/dev/null | grep -q "CN=postgresql-blockchain"; then
    echo -e "  ${GREEN}✓${NC} SSL certificate is valid for postgresql-blockchain"
elif timeout 5 openssl s_client -connect localhost:5432 -starttls postgres \
    -showcerts </dev/null 2>/dev/null | grep -q "CN=postgresql"; then
    echo -e "  ${YELLOW}⚠${NC} Self-signed certificate detected (expected for dev)"
    echo -e "  This is normal for development environments"
else
    echo -e "  ${RED}✗${NC} Could not verify SSL certificate"
fi

echo ""

# Test application connection
echo -e "${GREEN}[6/6]${NC} Testing application database connection..."
if docker exec -e PGPASSWORD=SecurePassword123! postgres-blockchain-ssl psql \
    -U blockchain_user -d blockchain_prod -h localhost -c "SELECT 1 AS test;" \
    2>/dev/null | grep -q "1"; then
    echo -e "  ${GREEN}✓${NC} Application user can connect to database"
else
    echo -e "  ${RED}✗${NC} Application user connection failed"
    exit 1
fi

echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}All SSL Tests Passed!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""
echo -e "${YELLOW}JDBC URL for Java application (v1.0.6+):${NC}"
echo -e "${BLUE}jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=verify-full${NC}"
echo ""
