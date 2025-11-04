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

# Create arrays to store markdown files
typeset -a MD_FILES

# Find all markdown files in docs directory AND root level markdown files
while IFS= read -r file; do
    MD_FILES+=("$file")
done < <(find "$BASE_DIR/docs" -name "*.md" -type f 2>/dev/null; find "$BASE_DIR" -maxdepth 1 -name "*.md" -type f 2>/dev/null)

# Process each markdown file
for source_file in "${MD_FILES[@]}"; do
    source_dir="$(dirname "$source_file")"
    
    # Extract all markdown links from the file
    while IFS= read -r link_full; do
        [[ -z "$link_full" ]] && continue
        
        # Extract just the link part (without [text])
        link=$(echo "$link_full" | sed 's/.*(\([^)]*\)).*/\1/')

        # Skip external links (http/https)
        [[ "$link" =~ ^https?:// ]] && continue

        # Skip anchors-only links (#section)
        [[ "$link" =~ ^# ]] && continue

        # Remove anchor from link if present (file.md#section -> file.md)
        link_file="${link%%#*}"
        
        # Skip empty links
        [[ -z "$link_file" ]] && continue

        ((TOTAL_LINKS++))

        # Resolve the target path
        if [[ "$link_file" == /* ]]; then
            # Absolute path from project root
            target="$BASE_DIR$link_file"
        else
            # Relative path - resolve from source file's directory
            target="$source_dir/$link_file"
        fi

        # Normalize the path
        if command -v realpath >/dev/null 2>&1; then
            # Use realpath if available (GNU coreutils)
            target=$(realpath -m "$target" 2>/dev/null || echo "$target")
        else
            # Fallback: manual normalization for macOS
            target=$(cd "$(dirname "$target")" 2>/dev/null && echo "$(pwd)/$(basename "$target")" || echo "$target")
        fi

        # Check if target file exists
        if [[ ! -f "$target" ]]; then
            # Calculate relative paths for cleaner output
            rel_source="${source_file#$BASE_DIR/}"
            rel_target="${target#$BASE_DIR/}"

            echo "❌ BROKEN LINK:"
            echo "   Source: $rel_source"
            echo "   Link: $link_file"
            echo "   Expected at: $rel_target"
            echo "   (file does not exist)"
            echo ""
            ((BROKEN_LINKS++))
        fi
    done < <(grep -o '\[[^]]*\]([^)]*\.md[^)]*)' "$source_file" 2>/dev/null)
done

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
