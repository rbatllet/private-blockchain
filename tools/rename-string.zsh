#!/usr/bin/env zsh

# Generic script to rename any string across the entire codebase
# This includes Java source files, tests, documentation, and README files
# Usage: ./tools/rename-string.zsh <old_string> <new_string>

set -e

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo "${RED}❌ $1${NC}"
}

print_header() {
    echo ""
    echo "${BLUE}📊 $1${NC}"
    echo "${BLUE}$(printf '=%.0s' {1..50})${NC}"
}

# Check arguments
if [[ $# -ne 2 ]]; then
    print_error "Usage: $0 <old_string> <new_string>"
    echo ""
    echo "Example:"
    echo "  $0 searchSimple searchAll"
    echo "  $0 oldMethodName newMethodName"
    echo ""
    exit 1
fi

OLD_STRING="$1"
NEW_STRING="$2"

# Validate input
if [[ -z "$OLD_STRING" || -z "$NEW_STRING" ]]; then
    print_error "Both old_string and new_string must be non-empty"
    exit 1
fi

if [[ "$OLD_STRING" == "$NEW_STRING" ]]; then
    print_error "Old string and new string are identical!"
    exit 1
fi

# Get script directory and project root
SCRIPT_DIR="${0:A:h}"

# Find project root by looking for pom.xml
PROJECT_ROOT="$SCRIPT_DIR"
while [[ ! -f "$PROJECT_ROOT/pom.xml" && "$PROJECT_ROOT" != "/" ]]; do
    PROJECT_ROOT="${PROJECT_ROOT:h}"
done

if [[ ! -f "$PROJECT_ROOT/pom.xml" ]]; then
    print_error "Cannot find project root (pom.xml not found)"
    exit 1
fi

cd "$PROJECT_ROOT"

print_header "RENAME '$OLD_STRING' → '$NEW_STRING'"

# Discover files containing the old string
print_info "Searching for files containing '$OLD_STRING'..."
echo ""

# Search in Java files
JAVA_FILES=()
while IFS= read -r file; do
    file="${file#./}"
    JAVA_FILES+=("$file")
done < <(grep -rl "$OLD_STRING" --include="*.java" . 2>/dev/null)

# Search in Markdown files
MD_FILES=()
while IFS= read -r file; do
    file="${file#./}"
    MD_FILES+=("$file")
done < <(grep -rl "$OLD_STRING" --include="*.md" . 2>/dev/null)

# Combine all files
ALL_FILES=("${JAVA_FILES[@]}" "${MD_FILES[@]}")

if [[ ${#ALL_FILES[@]} -eq 0 ]]; then
    print_warning "No files found containing '$OLD_STRING'"
    exit 0
fi

print_info "Found ${#ALL_FILES[@]} files containing '$OLD_STRING'"
echo ""

# Count occurrences before replacement
print_info "Counting occurrences before replacement..."
TOTAL_BEFORE=0
for file in "${ALL_FILES[@]}"; do
    if [[ -f "$file" ]]; then
        COUNT=$(grep -o "$OLD_STRING" "$file" 2>/dev/null | wc -l | tr -d ' ')
        if [[ $COUNT -gt 0 ]]; then
            echo "  📝 $file: $COUNT occurrences"
            TOTAL_BEFORE=$((TOTAL_BEFORE + COUNT))
        fi
    fi
done

echo ""
print_info "Total occurrences found: $TOTAL_BEFORE"
echo ""

# Show preview of changes
print_info "Preview of some changes (first 5 lines):"
echo ""
COUNT_PREVIEW=0
for file in "${ALL_FILES[@]}"; do
    if [[ $COUNT_PREVIEW -ge 5 ]]; then
        break
    fi

    if [[ -f "$file" ]]; then
        # Show first line containing the old string
        PREVIEW_LINE=$(grep -n "$OLD_STRING" "$file" 2>/dev/null | head -1)
        if [[ -n "$PREVIEW_LINE" ]]; then
            LINE_NUM=$(echo "$PREVIEW_LINE" | cut -d: -f1)
            LINE_CONTENT=$(echo "$PREVIEW_LINE" | cut -d: -f2-)
            NEW_CONTENT=$(echo "$LINE_CONTENT" | sed "s/$OLD_STRING/$NEW_STRING/g")

            echo "  📄 $file:$LINE_NUM"
            echo "    ${RED}❌ $LINE_CONTENT${NC}"
            echo "    ${GREEN}✅ $NEW_CONTENT${NC}"
            echo ""
            COUNT_PREVIEW=$((COUNT_PREVIEW + 1))
        fi
    fi
done

# Ask for confirmation
print_warning "This will replace ALL $TOTAL_BEFORE occurrences of '$OLD_STRING' with '$NEW_STRING' in ${#ALL_FILES[@]} files"
echo -n "Do you want to continue? (y/N): "
read -r CONFIRMATION

if [[ "$CONFIRMATION" != "y" && "$CONFIRMATION" != "Y" ]]; then
    print_error "Operation cancelled by user"
    exit 1
fi

echo ""
print_header "PERFORMING REPLACEMENTS"

# Perform replacements
MODIFIED_COUNT=0
for file in "${ALL_FILES[@]}"; do
    if [[ -f "$file" ]]; then
        # Use sed for in-place replacement
        # Escape special characters for sed
        OLD_ESCAPED=$(printf '%s\n' "$OLD_STRING" | sed 's/[]\.*^$/[]/\\&/g')
        NEW_ESCAPED=$(printf '%s\n' "$NEW_STRING" | sed 's/[]\.*^$/[]/\\&/g')

        # macOS uses 'sed -i ""', Linux uses 'sed -i'
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            sed -i '' "s/$OLD_ESCAPED/$NEW_ESCAPED/g" "$file"
        else
            # Linux
            sed -i "s/$OLD_ESCAPED/$NEW_ESCAPED/g" "$file"
        fi

        print_success "Modified: $file"
        MODIFIED_COUNT=$((MODIFIED_COUNT + 1))
    fi
done

echo ""
print_header "VERIFICATION"

# Count occurrences after replacement
print_info "Verifying replacements..."
TOTAL_AFTER=0
TOTAL_NEW=0
for file in "${ALL_FILES[@]}"; do
    if [[ -f "$file" ]]; then
        COUNT_OLD=$(grep -o "$OLD_STRING" "$file" 2>/dev/null | wc -l | tr -d ' ')
        COUNT_NEW=$(grep -o "$NEW_STRING" "$file" 2>/dev/null | wc -l | tr -d ' ')

        if [[ $COUNT_OLD -gt 0 ]]; then
            print_warning "$file: Still has $COUNT_OLD occurrences of '$OLD_STRING'!"
            TOTAL_AFTER=$((TOTAL_AFTER + COUNT_OLD))
        fi

        if [[ $COUNT_NEW -gt 0 ]]; then
            TOTAL_NEW=$((TOTAL_NEW + COUNT_NEW))
        fi
    fi
done

echo ""
print_header "SUMMARY"
echo "  📝 Files modified: $MODIFIED_COUNT"
echo "  🔍 Occurrences before: $TOTAL_BEFORE"
echo "  ✅ New '$NEW_STRING': $TOTAL_NEW"
echo "  ⚠️  Remaining '$OLD_STRING': $TOTAL_AFTER"
echo ""

if [[ $TOTAL_AFTER -eq 0 ]]; then
    print_success "All replacements completed successfully!"
    echo ""
    print_info "Next steps:"
    echo "  1. Compile the project: mvn clean compile"
    echo "  2. Run tests: mvn test"
    echo "  3. Review changes: git diff"
    echo "  4. Commit changes: git add . && git commit -m 'refactor: rename $OLD_STRING to $NEW_STRING'"
else
    print_error "Some occurrences of '$OLD_STRING' remain!"
    print_warning "Please review manually and fix remaining occurrences"
fi

echo ""
