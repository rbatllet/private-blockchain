#!/usr/bin/env zsh
# =============================================================================
# MySQL SSL Certificates Generator for Private Blockchain
# =============================================================================
# Generates self-signed SSL certificates for MySQL 8.0
# RSA 3072-bit (NIST-compliant), valid for 2 years (730 days)
# Usage: ./generate-certs.sh
# =============================================================================

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
CERTS_DIR="${0:A:h}/certs"
CERT_VALID_DAYS=730  # 2 years (NIST-compliant, industry best practice)
RSA_KEY_SIZE=3072    # NIST-compliant for post-2030 security
COUNTRY="ES"
STATE="Barcelona"
LOCALITY="Barcelona"
ORGANIZATION="PrivateBlockchain"
COMMON_NAME="mysql-blockchain"

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}MySQL SSL Certificates Generator${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Create certs directory if it doesn't exist
if [[ ! -d "$CERTS_DIR" ]]; then
    echo -e "${YELLOW}Creating certificates directory: $CERTS_DIR${NC}"
    mkdir -p "$CERTS_DIR"
fi

# Change to certs directory
cd "$CERTS_DIR"

echo -e "${GREEN}[1/5]${NC} Generating CA (Certificate Authority) with RSA $RSA_KEY_SIZE-bit..."
openssl genrsa $RSA_KEY_SIZE > ca-key.pem 2>/dev/null
openssl req -new -x509 -nodes -days $CERT_VALID_DAYS \
    -key ca-key.pem -out ca.pem \
    -subj "/C=$COUNTRY/ST=$STATE/L=$LOCALITY/O=$ORGANIZATION/CN=$COMMON_NAME-CA" \
    -addext "subjectAltName=DNS:$COMMON_NAME-CA" \
    2>/dev/null
echo -e "   ${GREEN}✓${NC} CA certificate generated: ca.pem (RSA $RSA_KEY_SIZE-bit, NIST-compliant)"

echo -e "${GREEN}[2/5]${NC} Generating server private key (RSA $RSA_KEY_SIZE-bit)..."
openssl req -newkey rsa:$RSA_KEY_SIZE -days $CERT_VALID_DAYS \
    -nodes -keyout server-key.pem -out server-req.pem \
    -subj "/C=$COUNTRY/ST=$STATE/L=$LOCALITY/O=$ORGANIZATION/CN=$COMMON_NAME" \
    -addext "subjectAltName=DNS:localhost,DNS:mysql-blockchain,DNS:mysql-blockchain-ssl,IP:127.0.0.1" \
    2>/dev/null
echo -e "   ${GREEN}✓${NC} Server key generated: server-key.pem (RSA $RSA_KEY_SIZE-bit)"

echo -e "${GREEN}[3/5]${NC} Signing server certificate with CA..."
openssl x509 -req -in server-req.pem -days $CERT_VALID_DAYS \
    -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out server-cert.pem \
    -copy_extensions copy \
    2>/dev/null
echo -e "   ${GREEN}✓${NC} Server certificate signed: server-cert.pem (with SAN extension)"

echo -e "${GREEN}[4/5]${NC} Generating client private key (RSA $RSA_KEY_SIZE-bit)..."
openssl req -newkey rsa:$RSA_KEY_SIZE -days $CERT_VALID_DAYS \
    -nodes -keyout client-key.pem -out client-req.pem \
    -subj "/C=$COUNTRY/ST=$STATE/L=$LOCALITY/O=$ORGANIZATION/CN=$COMMON_NAME-Client" \
    -addext "subjectAltName=DNS:$COMMON_NAME-Client" \
    2>/dev/null
echo -e "   ${GREEN}✓${NC} Client key generated: client-key.pem (RSA $RSA_KEY_SIZE-bit)"

echo -e "${GREEN}[5/5]${NC} Signing client certificate with CA..."
openssl x509 -req -in client-req.pem -days $CERT_VALID_DAYS \
    -CA ca.pem -CAkey ca-key.pem -set_serial 02 -out client-cert.pem \
    -copy_extensions copy \
    2>/dev/null
echo -e "   ${GREEN}✓${NC} Client certificate signed: client-cert.pem (with SAN extension)"

# Clean up temporary files
rm -f server-req.pem client-req.pem

# Set proper permissions
chmod 644 *.pem
chmod 600 *-key.pem

echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}SSL Certificates Generated Successfully!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""
echo -e "Generated files in ${YELLOW}$CERTS_DIR${NC}:"
echo -e "  ${GREEN}ca.pem${NC}              - CA Certificate (trust this in Java)"
echo -e "  ${GREEN}ca-key.pem${NC}          - CA Private Key"
echo -e "  ${GREEN}server-cert.pem${NC}     - Server Certificate (used by MySQL)"
echo -e "  ${GREEN}server-key.pem${NC}      - Server Private Key (used by MySQL)"
echo -e "  ${GREEN}client-cert.pem${NC}     - Client Certificate (optional, for client auth)"
echo -e "  ${GREEN}client-key.pem${NC}      - Client Private Key (optional)"
echo ""
echo -e "${YELLOW}Important:${NC}"
echo -e "  1. Certificates are valid for $CERT_VALID_DAYS days"
echo -e "  2. These are self-signed certificates (for development/testing)"
echo -e "  3. For production, use certificates from a trusted CA"
echo -e "  4. Keep the private keys (*-key.pem) secure!"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "  1. Run: ${YELLOW}cd docker && docker-compose up -d${NC}"
echo -e "  2. Verify SSL: ${YELLOW}docker exec -it mysql-blockchain mysql -u root -p${NC}"
echo -e "  3. In MySQL: ${YELLOW}SHOW VARIABLES LIKE '%ssl%';${NC}"
echo ""
