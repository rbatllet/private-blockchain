#!/usr/bin/env zsh
# =============================================================================
# MySQL SSL Docker - Quick Start Script
# Private Blockchain Development Environment
# =============================================================================
# Usage: ./start-mysql.sh [--with-phpmyadmin] [--skip-certs]
# =============================================================================

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Script directory
SCRIPT_DIR="${0:A:h}"
cd "$SCRIPT_DIR"

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}MySQL SSL Docker - Quick Start${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Parse arguments
SKIP_CERTS=false
WITH_PHPMYADMIN=false

for arg in "$@"; do
    case $arg in
        --skip-certs)
            SKIP_CERTS=true
            ;;
        --with-phpmyadmin)
            WITH_PHPMYADMIN=true
            ;;
        *)
            echo -e "${RED}Unknown option: $arg${NC}"
            echo "Usage: $0 [--skip-certs] [--with-phpmyadmin]"
            exit 1
            ;;
    esac
done

# Step 1: Generate SSL certificates
if [ "$SKIP_CERTS" = false ]; then
    echo -e "${GREEN}[1/3]${NC} Generating SSL certificates..."
    if [ -f "mysql/certs/ca.pem" ]; then
        echo -e "${YELLOW}  ⚠ Certificates already exist. Skipping generation.${NC}"
        echo -e "${YELLOW}  To regenerate: rm -rf mysql/certs && ./start-mysql.sh${NC}"
    else
        cd mysql
        ./generate-certs.sh
        cd ..
    fi
else
    echo -e "${YELLOW}[1/3]${NC} ${YELLOW}⚠ Skipping certificate generation${NC}"
fi

echo ""

# Step 2: Create .env file if it doesn't exist
echo -e "${GREEN}[2/3]${NC} Checking environment configuration..."
if [ ! -f ".env" ]; then
    echo -e "  Creating .env from .env.example..."
    cp .env.example .env
    echo -e "${YELLOW}  ⚠ .env file created. Review passwords before starting!${NC}"
else
    echo -e "  ${GREEN}✓${NC} .env file exists"
fi

echo ""

# Step 3: Start Docker containers
echo -e "${GREEN}[3/3]${NC} Starting Docker containers..."
if [ "$WITH_PHPMYADMIN" = true ]; then
    echo -e "  Starting MySQL with phpMyAdmin..."
    docker-compose -f docker-compose-mysql.yml --profile tools up -d
    echo ""
    echo -e "${GREEN}✓ Services started:${NC}"
    echo -e "  • MySQL: ${BLUE}http://localhost:3306${NC}"
    echo -e "  • phpMyAdmin: ${BLUE}http://localhost:8080${NC}"
else
    echo -e "  Starting MySQL..."
    docker-compose -f docker-compose-mysql.yml up -d
    echo ""
    echo -e "${GREEN}✓ MySQL started: ${BLUE}jdbc:mysql://localhost:3306/blockchain_prod${NC}"
fi

echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo -e "  1. Wait for MySQL to be ready (~10 seconds)"
echo -e "  2. Verify SSL: ${YELLOW}docker exec -it mysql-blockchain-ssl mysql -u root -pRootPassword123! -e 'SHOW VARIABLES LIKE \"%ssl%\"'${NC}"
echo -e "  3. Run SSL test: ${YELLOW}./test-mysql-ssl-connection.sh${NC}"
echo -e "  4. Run tests: ${YELLOW}cd .. && mvn test${NC}"
echo ""
echo -e "${YELLOW}Useful commands:${NC}"
echo -e "  • View logs: ${YELLOW}docker-compose -f docker-compose-mysql.yml logs -f mysql${NC}"
echo -e "  • Stop: ${YELLOW}docker-compose -f docker-compose-mysql.yml down${NC}"
echo -e "  • MySQL shell: ${YELLOW}docker exec -it mysql-blockchain-ssl mysql -u blockchain_user -pSecurePassword123!${NC}"
echo ""
