#!/usr/bin/env zsh

# Script to check which demo files need IndexingCoordinator.waitForCompletion()
# This ensures all demos wait for async/background indexing before performing searches
# Version: 1.0.0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_step() {
    echo -e "${BLUE}📝 $1${NC}"
}

print_step "🔍 Indexing Wait Check for Demo Files"
echo "========================================"

# Get the script directory and go to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

print_info "Working directory: $(pwd)"
echo ""

# Find all demo files
DEMO_FILES=(src/main/java/demo/*Demo.java)

# Counters
TOTAL=0
HAS_WAIT=0
NEEDS_WAIT=0
NO_SEARCH=0

# Arrays to store file categories
declare -a FILES_WITH_WAIT
declare -a FILES_NEED_WAIT
declare -a FILES_NO_SEARCH

print_step "Analyzing demo files..."
echo ""

for file in "${DEMO_FILES[@]}"; do
    if [[ ! -f "$file" ]]; then
        continue
    fi
    
    TOTAL=$((TOTAL + 1))
    filename=$(basename "$file")
    
    # Check if file has waitForCompletion()
    has_wait=$(grep -c "waitForCompletion()" "$file" 2>/dev/null)
    has_wait=${has_wait:-0}
    has_wait=$((has_wait))
    
    # Check if file has IndexingCoordinator import
    has_import=$(grep -c "import.*IndexingCoordinator" "$file" 2>/dev/null)
    has_import=${has_import:-0}
    has_import=$((has_import))
    
    # Check if file performs searches (various search methods)
    if grep -qE "(search|Search|find.*Identifier|retrieveSecret)" "$file" 2>/dev/null; then
        has_search=$(grep -E "(search|Search|find.*Identifier|retrieveSecret)" "$file" | grep -v "import" | grep -v "//" | wc -l | tr -d ' ')
    else
        has_search=0
    fi
    has_search=$((has_search))
    
    # Check if file creates blocks that need indexing
    if grep -qE "(addBlock|addEncryptedBlock|storeSecret|storeData|storeSearchable)" "$file" 2>/dev/null; then
        creates_blocks=$(grep -E "(addBlock|addEncryptedBlock|storeSecret|storeData|storeSearchable)" "$file" | grep -v "import" | grep -v "//" | wc -l | tr -d ' ')
    else
        creates_blocks=0
    fi
    creates_blocks=$((creates_blocks))
    
    if (( has_wait > 0 )); then
        HAS_WAIT=$((HAS_WAIT + 1))
        FILES_WITH_WAIT+=("$filename")
        print_success "$filename - Already has waitForCompletion()"
    elif (( has_search > 0 && creates_blocks > 0 )); then
        NEEDS_WAIT=$((NEEDS_WAIT + 1))
        FILES_NEED_WAIT+=("$filename")
        print_warning "$filename - NEEDS waitForCompletion() (creates blocks: $creates_blocks, searches: $has_search)"
    else
        NO_SEARCH=$((NO_SEARCH + 1))
        FILES_NO_SEARCH+=("$filename")
        print_info "$filename - No search after block creation (OK)"
    fi
done

echo ""
print_step "📊 Summary"
echo "========================================"
echo "Total demo files analyzed: $TOTAL"
echo "✅ Already has waitForCompletion(): $HAS_WAIT"
echo "⚠️  Needs waitForCompletion(): $NEEDS_WAIT"
echo "ℹ️  No search operations: $NO_SEARCH"
echo ""

if [[ $NEEDS_WAIT -gt 0 ]]; then
    print_warning "Files that need waitForCompletion():"
    for file in "${FILES_NEED_WAIT[@]}"; do
        echo "   - $file"
    done
    echo ""
    
    print_info "To add waitForCompletion(), ensure:"
    print_info "1. Import: import com.rbatllet.blockchain.indexing.IndexingCoordinator;"
    print_info "2. After creating blocks and BEFORE searching:"
    print_info "   System.out.println(\"⏳ Waiting for background indexing...\");"
    print_info "   IndexingCoordinator.getInstance().waitForCompletion();"
    print_info "   System.out.println(\"✅ Background indexing completed\");"
    echo ""
fi

if [[ $NEEDS_WAIT -eq 0 ]]; then
    print_success "🎉 All demos that perform searches have proper indexing waits!"
    exit 0
else
    print_warning "⚠️  Some demos need to be updated with waitForCompletion()"
    exit 1
fi
