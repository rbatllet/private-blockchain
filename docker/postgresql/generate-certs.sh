#!/usr/bin/env zsh
# =============================================================================
# PostgreSQL SSL Certificate Generator
# =============================================================================
# Generates self-signed SSL certificates for PostgreSQL 18
# RSA 3072-bit (NIST-compliant), valid for 2 years (730 days)
# =============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${(%):-%x}")" && pwd)"
CERTS_DIR="${SCRIPT_DIR}/certs"

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}PostgreSQL SSL Certificate Generator${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Create certs directory if it doesn't exist
mkdir -p "${CERTS_DIR}"
cd "${CERTS_DIR}"

# Check if certificates already exist
if [[ -f "ca.pem" && -f "server-cert.pem" && -f "server-key.pem" ]]; then
    echo -e "${YELLOW}⚠ SSL certificates already exist!${NC}"
    echo -e "${YELLOW}  Backup existing certificates before regenerating.${NC}"
    echo ""
    read -p "Do you want to regenerate certificates? (yes/no): " confirm
    if [[ "$confirm" != "yes" && "$confirm" != "y" ]]; then
        echo -e "${GREEN}✓ Certificate generation cancelled.${NC}"
        exit 0
    fi
    echo ""
fi

echo -e "${GREEN}[1/4]${NC} Generating CA private key (RSA 3072-bit for post-2030 security)..."
openssl genrsa -out ca-key.pem 3072 2>/dev/null
echo -e "  ${GREEN}✓${NC} CA private key generated (3072-bit, NIST-compliant for long-term use)"

echo ""
echo -e "${GREEN}[2/4]${NC} Generating CA certificate..."
openssl req -new -x509 -nodes -days 730 -key ca-key.pem -out ca.pem \
    -subj "/C=ES/ST=Barcelona/L=Barcelona/O=PrivateBlockchain/OU=PostgreSQL-CA/CN=postgresql-blockchain-CA" \
    -addext "subjectAltName=DNS:postgresql-blockchain-CA" 2>/dev/null
echo -e "  ${GREEN}✓${NC} CA certificate generated (valid for 2 years, with SAN extension)"

echo ""
echo -e "${GREEN}[3/4]${NC} Generating server private key (RSA 3072-bit)..."
openssl genrsa -out server-key.pem 3072 2>/dev/null
chmod 600 server-key.pem
echo -e "  ${GREEN}✓${NC} Server private key generated (3072-bit, NIST-compliant)"

echo ""
echo -e "${GREEN}[4/4]${NC} Generating server certificate signing request..."
openssl req -new -key server-key.pem -out server-req.pem \
    -subj "/C=ES/ST=Barcelona/L=Barcelona/O=PrivateBlockchain/OU=PostgreSQL-Server/CN=postgresql-blockchain" \
    -addext "subjectAltName=DNS:localhost,DNS:postgres-blockchain,DNS:postgres-blockchain-ssl,IP:127.0.0.1" 2>/dev/null

echo -e "  Signing server certificate with CA..."
openssl x509 -req -in server-req.pem -days 730 -CA ca.pem -CAkey ca-key.pem \
    -CAcreateserial -out server-cert.pem -copy_extensions copy 2>/dev/null
echo -e "  ${GREEN}✓${NC} Server certificate generated (valid for 2 years, with SAN extension)"

# Clean up temporary files
rm -f server-req.pem

echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}Certificate Generation Complete!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""
echo -e "${GREEN}Generated files:${NC}"
echo -e "  ${BLUE}ca.pem${NC}              - CA Certificate"
echo -e "  ${BLUE}ca-key.pem${NC}          - CA Private Key"
echo -e "  ${BLUE}server-cert.pem${NC}     - Server Certificate"
echo -e "  ${BLUE}server-key.pem${NC}      - Server Private Key"
echo ""
echo -e "${YELLOW}Permissions:${NC}"
chmod 600 ca-key.pem server-key.pem
chmod 644 ca.pem server-cert.pem
ls -la
echo ""
echo -e "${GREEN}✓ Ready to start PostgreSQL with SSL!${NC}"
echo -e "${YELLOW}  Run: docker-compose up -d${NC}"
echo ""
