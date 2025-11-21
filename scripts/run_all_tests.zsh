#!/usr/bin/env zsh

# Comprehensive test runner for ALL Blockchain tests
# Executes all 46 test classes in logical categories (ML-DSA-87 post-quantum)
# Version: 2.0.1 - COMPLETE TEST COVERAGE (Post-Quantum Migration)

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

print_header "COMPREHENSIVE BLOCKCHAIN TEST RUNNER"
print_info "This script runs ALL 46 available test classes in logical categories"
print_info "Post-Quantum Migration: ML-DSA-87 (NIST FIPS 204)"
print_info "Project directory: $(pwd)"

# Check if we're in the correct directory
check_project_directory

# Check prerequisites
if ! check_java || ! check_maven; then
    exit 1
fi

# Clean database at start to prevent corruption
cleanup_database

# Compile project and tests
if ! compile_project_with_tests; then
    exit 1
fi

# Test results tracking
TOTAL_SUITES=0
PASSED_SUITES=0
FAILED_SUITES=0

# Function to run test suite
run_test_suite() {
    local suite_name="$1"
    local test_pattern="$2"
    local description="$3"
    
    print_step "Running $description..."
    print_info "Test pattern: $test_pattern"
    
    mvn test -Dtest="$test_pattern" -q
    local result=$?
    
    ((TOTAL_SUITES++))
    
    if [ $result -eq 0 ]; then
        print_success "✅ $suite_name: PASSED"
        ((PASSED_SUITES++))
    else
        print_error "❌ $suite_name: FAILED"
        ((FAILED_SUITES++))
    fi
    
    cleanup_database # Clean between test suites
    return $result
}

print_separator

# 1. CORE BLOCKCHAIN TESTS
print_header "1. CORE BLOCKCHAIN TESTS"
run_test_suite "Core Basic" "BlockchainTest" "Basic blockchain functionality"
run_test_suite "Core Advanced" "BlockchainAdditionalAdvancedFunctionsTest" "Advanced blockchain functions"
run_test_suite "Core Encryption" "BlockchainEncryptionTest" "Blockchain encryption features"
run_test_suite "Core Key Authorization" "BlockchainKeyAuthorizationTest" "Key authorization system"
run_test_suite "Core Validation" "DataConsistencyValidationTest" "Data consistency validation"
run_test_suite "Core Temporal" "SimpleTemporalValidationTest" "Temporal validation"
run_test_suite "Core Environment" "TestEnvironmentValidator" "Test environment validation"

print_separator

# 2. SECURITY & CRYPTOGRAPHY TESTS (ML-DSA-87 Post-Quantum)
print_header "2. SECURITY & CRYPTOGRAPHY TESTS"
run_test_suite "Crypto Utils" "CryptoUtilTest" "Cryptographic utility functions (ML-DSA-87)"
run_test_suite "Password Utils" "PasswordUtilTest" "Password utility functions"
run_test_suite "Key Storage" "SecureKeyStorageTest,SecureKeyStorageAdvancedTest" "Secure key storage (AES-256-GCM)"
run_test_suite "Key File Loader" "KeyFileLoaderTest" "Key file loading system"
run_test_suite "Encryption Config" "EncryptionConfigTest" "Encryption configuration"

print_separator

# 3. SEARCH & METADATA TESTS
print_header "3. SEARCH & METADATA TESTS"
run_test_suite "Search Basic" "SearchFrameworkBasicTest" "Basic search functionality"
run_test_suite "Search Exhaustive" "SearchFrameworkExhaustiveTest" "Exhaustive search testing"
run_test_suite "Search Debug" "DebugEncryptionFormatTest" "Search debug utilities"
run_test_suite "Metadata Compression" "MetadataCompressionTest" "Metadata compression"
run_test_suite "Term Visibility" "TermVisibilityMapTest" "Term visibility mapping"

print_separator

# 4. ENCRYPTION & DATA PROTECTION TESTS
print_header "4. ENCRYPTION & DATA PROTECTION TESTS"
run_test_suite "Block Encryption" "BlockDataEncryptionServiceTest" "Block data encryption"
run_test_suite "Secure Block Encryption" "SecureBlockEncryptionServiceTest" "Secure block encryption"
run_test_suite "Hybrid Encryption" "HybridEncryptionTest" "Hybrid encryption system"
run_test_suite "User Friendly API" "UserFriendlyEncryptionAPITest" "User-friendly encryption API"
run_test_suite "Block Validation" "EncryptedBlockValidationTest" "Encrypted block validation"

print_separator

# 5. INTEGRATION TESTS
print_header "5. INTEGRATION TESTS"
run_test_suite "Block Encryption Integration" "BlockEncryptionIntegrationTest" "Block encryption integration"
run_test_suite "Granular Visibility Integration" "GranularTermVisibilityIntegrationTest" "Granular term visibility"
run_test_suite "Export/Import Encrypted" "EncryptedChainExportImportTest" "Encrypted chain export/import"
run_test_suite "Export/Import Edge Cases" "ExportImportEdgeCasesTest" "Export/import edge cases"

print_separator

# 6. THREAD SAFETY & CONCURRENCY TESTS
print_header "6. THREAD SAFETY & CONCURRENCY TESTS"
run_test_suite "Comprehensive Thread Safety" "ComprehensiveThreadSafetyTest" "Comprehensive thread safety"
run_test_suite "Advanced Thread Safety" "AdvancedThreadSafetyTest" "Advanced thread safety"
run_test_suite "Extreme Thread Safety" "ExtremeThreadSafetyTest" "Extreme thread safety"
run_test_suite "Edge Case Thread Safety" "EdgeCaseThreadSafetyTest" "Edge case thread safety"
run_test_suite "Block Sequence Thread Safety" "BlockSequenceThreadSafetyTest" "Block sequence thread safety"
run_test_suite "Encrypted Block Thread Safety" "EncryptedBlockThreadSafetyTest" "Encrypted block thread safety"
run_test_suite "Data Integrity Thread Safety" "DataIntegrityThreadSafetyTest" "Data integrity thread safety"
run_test_suite "Thread Safe Export/Import" "ThreadSafeExportImportTest" "Thread-safe export/import"
run_test_suite "Race Condition Fix" "RaceConditionFixTest" "Race condition fixes"
run_test_suite "Critical Consistency" "CriticalConsistencyTest" "Critical consistency"

print_separator

# 7. STORAGE & RECOVERY TESTS
print_header "7. STORAGE & RECOVERY TESTS"
run_test_suite "Off-Chain Storage" "OffChainStorageTest" "Off-chain storage system"
run_test_suite "Chain Recovery" "ChainRecoveryManagerTest" "Chain recovery management"
run_test_suite "Rollback Strategy" "ImprovedRollbackStrategyTest" "Improved rollback strategy"
run_test_suite "Recovery Config" "RecoveryConfigTest" "Recovery configuration"

print_separator

# 8. DAO & DATA ACCESS TESTS
print_header "8. DAO & DATA ACCESS TESTS"
run_test_suite "Authorized Key DAO" "AuthorizedKeyDAODeleteTest" "Authorized key DAO operations"
run_test_suite "Dangerous Delete" "DangerousDeleteAuthorizedKeyTest" "Dangerous delete operations"

print_separator

# 9. UTILITY & VALIDATION TESTS
print_header "9. UTILITY & VALIDATION TESTS"
run_test_suite "Block Validation Utils" "BlockValidationUtilTest" "Block validation utilities"
run_test_suite "Block Validation Result" "BlockValidationResultTest" "Block validation results"
run_test_suite "Format Utils" "FormatUtilTest" "Format utility functions"
run_test_suite "Compression Utils" "CompressionUtilTest" "Compression utilities"
run_test_suite "Exit Utils" "ExitUtilTest" "Exit utility functions"

print_separator

# 10. DEBUG & DEVELOPMENT TESTS
print_header "10. DEBUG & DEVELOPMENT TESTS"
run_test_suite "Debug Block Sequence" "DebugBlockSequenceTest" "Debug block sequence"

print_separator

# FINAL SUMMARY
print_header "COMPLETE TEST EXECUTION SUMMARY"

print_info "📊 Test Suite Statistics:"
print_info "  Total test suites: $TOTAL_SUITES"
print_info "  Passed suites: $PASSED_SUITES"
print_info "  Failed suites: $FAILED_SUITES"

if [ $FAILED_SUITES -eq 0 ]; then
    print_success "🎉 ALL $TOTAL_SUITES TEST SUITES PASSED!"
    print_success "Complete test coverage achieved - all 46 test classes executed (ML-DSA-87)"
else
    print_error "❌ $FAILED_SUITES out of $TOTAL_SUITES test suites failed"
    print_info "Check the output above for detailed failure information"
fi

print_separator

# Final cleanup
if [[ "${KEEP_TEST_FILES:-false}" != "true" ]]; then
    cleanup_database
fi

# Exit with appropriate code
if [ $FAILED_SUITES -eq 0 ]; then
    print_success "All tests completed successfully!"
    exit 0
else
    error_exit "Some tests failed. Check the output above."
fi
