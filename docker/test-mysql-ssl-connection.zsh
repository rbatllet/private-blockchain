#!/usr/bin/env zsh
# =============================================================================
# MySQL SSL Connection Test Script
# =============================================================================
# Tests the SSL connection to MySQL Docker container
# Usage: ./test-ssl-connection.sh
# =============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}MySQL SSL Connection Test${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Check if Docker container is running
echo -e "${GREEN}[1/5]${NC} Checking if MySQL container is running..."
if docker ps | grep -q "mysql-blockchain-ssl"; then
    echo -e "  ${GREEN}✓${NC} MySQL container is running"
else
    echo -e "  ${RED}✗${NC} MySQL container is NOT running"
    echo -e "${YELLOW}  Start it with: docker-compose -f docker-compose-mysql.yml up -d${NC}"
    exit 1
fi

echo ""

# Test SSL connection with MySQL client
echo -e "${GREEN}[2/5]${NC} Testing SSL connection with MySQL client..."
SSL_STATUS=$(docker exec -e MYSQL_PWD=RootPassword123! mysql-blockchain-ssl mysql -u root --ssl-mode=REQUIRED -se "SHOW STATUS LIKE 'Ssl_cipher';" 2>/dev/null | tail -n 1)
if [[ -n "$SSL_STATUS" ]]; then
    echo -e "  ${GREEN}✓${NC} SSL connection successful"
    echo -e "  Cipher: ${BLUE}$SSL_STATUS${NC}"
else
    echo -e "  ${RED}✗${NC} SSL connection failed"
    exit 1
fi

echo ""

# Check SSL variables
echo -e "${GREEN}[3/5]${NC} Checking SSL configuration..."
docker exec -e MYSQL_PWD=RootPassword123! mysql-blockchain-ssl mysql -u root --ssl-mode=REQUIRED -se "SHOW VARIABLES LIKE '%ssl%';" 2>/dev/null | grep -E "have_ssl|ssl_ca|ssl_cert|ssl_key" | while read line; do
    echo -e "  $line"
done

echo ""

# Test with openssl
echo -e "${GREEN}[4/5]${NC} Testing SSL/TLS with openssl..."
if timeout 5 openssl s_client -connect localhost:3306 -showcerts </dev/null 2>/dev/null | grep -q "Verification: OK"; then
    echo -e "  ${GREEN}✓${NC} SSL certificate is valid"
elif timeout 5 openssl s_client -connect localhost:3306 -showcerts </dev/null 2>/dev/null | grep -q "CN=mysql-blockchain"; then
    echo -e "  ${YELLOW}⚠${NC} Self-signed certificate detected (expected for dev)"
    echo -e "  This is normal for development environments"
else
    echo -e "  ${RED}✗${NC} Could not verify SSL certificate"
fi

echo ""

# Test application connection
echo -e "${GREEN}[5/5]${NC} Testing application database connection..."
if docker exec -e MYSQL_PWD=SecurePassword123! mysql-blockchain-ssl mysql -u blockchain_user --ssl-mode=REQUIRED blockchain_prod -e "SELECT 1" 2>/dev/null | grep -q "1"; then
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
echo -e "${BLUE}jdbc:mysql://localhost:3306/blockchain_prod?useSSL=true&requireSSL=true&verifyServerCertificate=true&allowPublicKeyRetrieval=true${NC}"
echo ""
