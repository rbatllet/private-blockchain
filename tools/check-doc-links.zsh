#!/usr/bin/env zsh

# Script to check all internal markdown links in documentation
# Verifies that referenced files exist
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

echo "🔍 Checking documentation links..."
echo "📁 Project root: $BASE_DIR"
echo ""

BROKEN_LINKS=0
TOTAL_LINKS=0

# Find all markdown files and extract links
while IFS= read -r line; do
    file=$(echo "$line" | cut -d: -f1)
    link_match=$(echo "$line" | cut -d: -f2-)
    
    # Extract the .md filename from the link
    md_file=$(echo "$link_match" | grep -o '\[.*\](.*\.md)' | sed 's/.*(\(.*\.md\)).*/\1/' | head -1)
    
    if [[ -n "$md_file" ]]; then
        TOTAL_LINKS=$((TOTAL_LINKS + 1))
        
        # Get directory of source file
        file_dir=$(dirname "$file")
        
        # Resolve relative path
        if [[ "$md_file" == /* ]]; then
            # Absolute path
            target="$BASE_DIR$md_file"
        elif [[ "$md_file" == ../* ]]; then
            # Relative path with ..
            target="$file_dir/$md_file"
        else
            # Relative path in same or subdirectory
            target="$file_dir/$md_file"
        fi
        
        # Normalize path (resolve ..)
        target=$(cd "$(dirname "$target")" 2>/dev/null && echo "$(pwd)/$(basename "$target")")
        
        # Check if file exists
        if [[ ! -f "$target" ]]; then
            # Calculate relative path from BASE_DIR for cleaner output
            relative_target="${target#$BASE_DIR/}"
            
            echo "❌ BROKEN: $file"
            echo "   Link: $md_file"
            echo "   Expected: $relative_target"
            echo ""
            BROKEN_LINKS=$((BROKEN_LINKS + 1))
        fi
    fi
done < <(grep -r "\[.*\](.*\.md)" docs/ --include="*.md" 2>/dev/null | grep -v "http")

echo ""
echo "📊 Results:"
echo "   Total links checked: $TOTAL_LINKS"
echo "   Broken links: $BROKEN_LINKS"
echo ""

if [[ $BROKEN_LINKS -eq 0 ]]; then
    echo "✅ All documentation links are valid!"
    exit 0
else
    echo "❌ Found $BROKEN_LINKS broken link(s)"
    exit 1
fi
