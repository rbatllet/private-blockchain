#!/usr/bin/env zsh

# Script to check that each docs/ subdirectory has a README.md
# and that all markdown files in that directory are referenced in README.md
# Auto-detects project root directory

# Detect project root (looks for pom.xml or .git directory)
SCRIPT_DIR="${0:a:h}"  # Get absolute path of script directory
BASE_DIR="$(dirname "$SCRIPT_DIR")"  # Start from parent of tools/ directory

# Navigate up until we find project root markers
while [[ "$BASE_DIR" != "/" ]]; do
    if [[ -f "$BASE_DIR/pom.xml" ]] || [[ -d "$BASE_DIR/.git" ]]; then
        break
    fi
    BASE_DIR="$(dirname "$BASE_DIR")"
done

if [[ "$BASE_DIR" == "/" ]]; then
    echo "❌ ERROR: Could not find project root (no pom.xml or .git found)"
    exit 1
fi

DOCS_DIR="$BASE_DIR/docs"

if [[ ! -d "$DOCS_DIR" ]]; then
    echo "❌ ERROR: docs/ directory not found at $DOCS_DIR"
    exit 1
fi

echo "🔍 Checking README.md coverage in documentation..."
echo "📁 Project root: $BASE_DIR"
echo "📚 Docs directory: $DOCS_DIR"
echo ""

MISSING_README=0
MISSING_REFERENCES=0
TOTAL_DIRS=0
TOTAL_FILES=0

# Find all subdirectories in docs/ at first level only
typeset -a SUBDIRS
while IFS= read -r dir; do
    SUBDIRS+=("$dir")
done < <(find "$DOCS_DIR" -mindepth 1 -maxdepth 1 -type d | sort)

# Check each subdirectory
for dir in "${SUBDIRS[@]}"; do
    ((TOTAL_DIRS++))
    
    rel_dir="${dir#$BASE_DIR/}"
    readme_file="$dir/README.md"
    
    # Check if README.md exists
    if [[ ! -f "$readme_file" ]]; then
        echo "❌ MISSING README.md:"
        echo "   Directory: $rel_dir"
        echo "   Expected: ${rel_dir}/README.md"
        echo ""
        ((MISSING_README++))
        continue
    fi
    
    # Read README.md content
    readme_content=$(<"$readme_file")
    
    # Find all markdown files in this directory (excluding README.md and subdirectories)
    md_files=()  # Clear/initialize array for this directory
    while IFS= read -r file; do
        md_files+=("$file")
    done < <(find "$dir" -maxdepth 1 -name "*.md" -type f ! -name "README.md" | sort)
    
    # Check if each markdown file is referenced in README.md
    for md_file in "${md_files[@]}"; do
        ((TOTAL_FILES++))
        
        basename_file="$(basename "$md_file")"
        
        # Check if the file is referenced in README.md (by filename or relative path)
        # Look for references like: [text](filename.md) or [text](./filename.md)
        if ! echo "$readme_content" | grep -qE "\(\.?/?${basename_file}[#)]|\(\.?/?${basename_file}\)"; then
            rel_file="${md_file#$BASE_DIR/}"
            rel_readme="${readme_file#$BASE_DIR/}"
            
            echo "⚠️  UNREFERENCED FILE:"
            echo "   File: $rel_file"
            echo "   Not found in: $rel_readme"
            echo ""
            ((MISSING_REFERENCES++))
        fi
    done
    
    # Only show errors - removed success messages
done

echo ""
echo "📊 Results:"
echo "   Directories checked: $TOTAL_DIRS"
echo "   Markdown files checked: $TOTAL_FILES"
echo "   Missing README.md: $MISSING_README"
echo "   Unreferenced files: $MISSING_REFERENCES"
echo ""

if [[ $MISSING_README -eq 0 ]] && [[ $MISSING_REFERENCES -eq 0 ]]; then
    echo "✅ All directories have README.md and all files are properly referenced!"
    exit 0
else
    if [[ $MISSING_README -gt 0 ]]; then
        echo "❌ Found $MISSING_README director(ies) without README.md"
    fi
    if [[ $MISSING_REFERENCES -gt 0 ]]; then
        echo "❌ Found $MISSING_REFERENCES unreferenced file(s)"
    fi
    exit 1
fi
