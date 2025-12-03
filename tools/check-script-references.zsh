#!/usr/bin/env zsh

# ============================================================================
# check-script-references.zsh
# ============================================================================
# Verifies that all .zsh script references in the project actually exist
# Filters false positives intelligently (wildcards, examples, placeholders)
# ============================================================================

setopt extended_glob

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'  # No Color

# Get script directory and find project root
SCRIPT_DIR="${0:A:h}"

# Find project root by looking for pom.xml
BASE_DIR="$SCRIPT_DIR"
while [[ ! -f "$BASE_DIR/pom.xml" && "$BASE_DIR" != "/" ]]; do
    BASE_DIR="${BASE_DIR:h}"
done

if [[ ! -f "$BASE_DIR/pom.xml" ]]; then
    print -P "${RED}❌ Cannot find project root (pom.xml not found)${NC}"
    exit 1
fi

SCRIPTS_DIR="$BASE_DIR/scripts"
TOOLS_DIR="$BASE_DIR/tools"
DOCS_DIR="$BASE_DIR/docs"

print -P "${BLUE}🔍 Checking .zsh Script References${NC}"
print "Base directory: $BASE_DIR"
print "Scripts directory: $SCRIPTS_DIR"
print "Tools directory: $TOOLS_DIR"
print ""

# Index existing scripts
print -P "${CYAN}📦 Indexing existing .zsh scripts...${NC}"

# Use associative array for O(1) lookups
typeset -A existing_scripts

# Index all .zsh files from scripts/ (including subdirectories)
for script_path in $SCRIPTS_DIR/**/*.zsh(N); do
    # Store relative path from scripts/ dir
    rel_path="${script_path#$SCRIPTS_DIR/}"
    script_name="${script_path:t}"
    
    # Store both full relative path and just filename
    existing_scripts[$script_name]="$rel_path"
    existing_scripts[$rel_path]="$rel_path"
    # Also store with scripts/ prefix for references like "scripts/run_demo.zsh"
    existing_scripts["scripts/$rel_path"]="scripts/$rel_path"
done

# Index all .zsh files from tools/ directory
for script_path in $TOOLS_DIR/**/*.zsh(N); do
    # Store relative path from tools/ dir
    rel_path="${script_path#$TOOLS_DIR/}"
    script_name="${script_path:t}"
    
    # Store both full relative path and just filename
    existing_scripts[$script_name]="tools/$rel_path"
    existing_scripts["tools/$rel_path"]="tools/$rel_path"
done

script_count=${#existing_scripts}/2
print "Found $script_count existing .zsh scripts (including scripts/ and tools/ subdirectories)"
print ""

# Arrays to hold results
typeset -a valid_refs
typeset -a example_refs
typeset -a false_positives
typeset -a broken_refs

print -P "${YELLOW}📝 Scanning for .zsh script references...${NC}"
print ""

total_refs=0
checked_files=0

# Function to check if script exists
check_script_exists() {
    local script_ref="$1"
    local source_file="$2"
    local line_num="$3"
    local line_content="$4"
    
    # Clean reference
    script_ref="${script_ref//\"/}"
    script_ref="${script_ref//\'/}"
    script_ref="${script_ref## }"
    script_ref="${script_ref%% }"
    script_ref="${script_ref#./}"
    
    # Skip if empty or not .zsh
    [[ -z "$script_ref" ]] && return
    [[ "$script_ref" != *.zsh ]] && return
    
    # FALSE POSITIVE: Absolute paths (production examples like /opt/blockchain/scripts/...)
    if [[ "$script_ref" == /* ]]; then
        ((total_refs++))
        false_positives+=("$source_file:$line_num -> $script_ref [absolute path example]")
        return
    fi
    
    # FALSE POSITIVE: Wildcard in script name itself (e.g., "*.zsh" or "script*.zsh")
    if [[ "$script_ref" == *\** ]]; then
        ((total_refs++))
        false_positives+=("$source_file:$line_num -> $script_ref [wildcard in name]")
        return
    fi
    
    # FALSE POSITIVE: Just ".zsh" extension
    if [[ "$script_ref" == ".zsh" ]]; then
        ((total_refs++))
        false_positives+=("$source_file:$line_num -> $script_ref [extension only]")
        return
    fi
    
    # FALSE POSITIVE: Contains regex operators
    if [[ "$script_ref" == *\.\** ]] || [[ "$script_ref" == *source\** ]]; then
        ((total_refs++))
        false_positives+=("$source_file:$line_num -> $script_ref [regex pattern]")
        return
    fi
    
    # Extract just the filename
    local script_name="${script_ref:t}"
    
    # Skip self-references (script referencing itself)
    if [[ "$source_file" == *.zsh ]]; then
        [[ "$script_name" == "${source_file:t}" ]] && return
    fi
    
    ((total_refs++))
    
    # PLACEHOLDER: Example/template names
    if [[ "$script_name" =~ (your|my|example|_test_name|new_test|_name).*\.zsh ]]; then
        example_refs+=("$source_file:$line_num -> $script_name [placeholder]")
        return
    fi
    
    # SUGGESTED: Scripts mentioned in docs as examples but not implemented
    if [[ "$script_name" =~ (validate-chain|validate-large-chain|quick-validate|health_check|start|stop|run_advanced_search_demo)\.zsh ]]; then
        example_refs+=("$source_file:$line_num -> $script_name [suggested/example]")
        return
    fi
    
    # Check if script exists
    local found=0
    
    # Try filename lookup
    if [[ -n "${existing_scripts[$script_name]}" ]]; then
        found=1
    fi
    
    # Try relative path lookup
    if [[ "$script_ref" == */* ]]; then
        local clean_path="${script_ref#scripts/}"
        
        if [[ -f "$SCRIPTS_DIR/$clean_path" ]]; then
            found=1
        elif [[ -n "${existing_scripts[$clean_path]}" ]]; then
            found=1
        fi
    fi
    
    # Store result
    if [[ $found -eq 1 ]]; then
        valid_refs+=("$source_file:$line_num -> $script_ref")
    else
        broken_refs+=("$source_file:$line_num -> $script_ref")
    fi
}

# Check documentation files
print -P "${BLUE}Checking documentation files (*.md)...${NC}"

for md_file in $DOCS_DIR/**/*.md(N); do
    ((checked_files++))
    
    local line_num=0
    while IFS= read -r line; do
        ((line_num++))
        
        # Skip if line doesn't contain .zsh
        [[ "$line" != *.zsh* ]] && continue
        
        # Skip comment lines in markdown
        [[ "$line" =~ ^[[:space:]]*# ]] && continue
        
        # Skip chmod commands with wildcards (these are shell commands, not script references)
        if [[ "$line" == *chmod*\*.zsh* ]]; then
            continue
        fi
        
        # Extract markdown links [text](path.zsh)
        for ref in ${(M)${(s:):)line}:#*\.zsh*}; do
            ref="${ref#*\(}"
            ref="${ref%\)*}"
            check_script_exists "$ref" "${md_file#$BASE_DIR/}" "$line_num" "$line"
        done
        
        # Extract direct mentions of .zsh files (path/to/script.zsh)
        for word in ${=line}; do
            if [[ "$word" == *.zsh* ]]; then
                # Clean word from surrounding punctuation
                local clean="${word}"
                clean="${clean#\`}"
                clean="${clean%\`}"
                clean="${clean%,}"
                clean="${clean%:}"
                clean="${clean%\)}"
                clean="${clean#\(}"
                
                [[ "$clean" == *.zsh ]] && check_script_exists "$clean" "${md_file#$BASE_DIR/}" "$line_num" "$line"
            fi
        done
    done < "$md_file"
done

# Check script files (looking for references to other scripts)
print -P "${BLUE}Checking .zsh script files...${NC}"

for script_file in $SCRIPTS_DIR/**/*.zsh(N); do
    # Skip checking this script itself
    [[ "${script_file:t}" == "check-script-references.zsh" ]] && continue
    
    ((checked_files++))
    
    local line_num=0
    while IFS= read -r line; do
        ((line_num++))
        
        # Skip if line doesn't contain .zsh
        [[ "$line" != *.zsh* ]] && continue
        
        # Skip full-line comments
        [[ "$line" =~ ^[[:space:]]*# ]] && continue
        
        # Extract from source/. commands
        if [[ "$line" =~ (source|\.)[[:space:]]+ ]]; then
            for word in ${=line}; do
                if [[ "$word" == *.zsh ]]; then
                    local clean="${word//\"/}"
                    clean="${clean//\'/}"
                    clean="${clean//\$\{*\}/}"  # Remove ${VARS}
                    
                    [[ "$clean" == *.zsh ]] && check_script_exists "$clean" "${script_file#$BASE_DIR/}" "$line_num" "$line"
                fi
            done
        fi
        
        # Extract from direct mentions
        for word in ${=line}; do
            if [[ "$word" == *.zsh* ]]; then
                local clean="${word}"
                clean="${clean#\`}"
                clean="${clean%\`}"
                clean="${clean%,}"
                clean="${clean%:}"
                clean="${clean%\)}"
                clean="${clean#\(}"
                clean="${clean//\"/}"
                clean="${clean//\'/}"
                
                [[ "$clean" == *.zsh ]] && check_script_exists "$clean" "${script_file#$BASE_DIR/}" "$line_num" "$line"
            fi
        done
    done < "$script_file"
done

print ""

# Print results
print -P "${BLUE}════════════════════════════════════════${NC}"
print -P "${BLUE}Summary${NC}"
print -P "${BLUE}════════════════════════════════════════${NC}"
print "Files checked: $checked_files"
print "Existing .zsh scripts: $script_count"
print "Total references found: $total_refs"
print -P "${GREEN}✓ Valid references: ${#valid_refs}${NC}"
print -P "${YELLOW}ℹ Example/suggested scripts: ${#example_refs}${NC}"
print -P "${CYAN}~ False positives filtered: ${#false_positives}${NC}"
print -P "${RED}✗ Broken references: ${#broken_refs}${NC}"
print ""

# Show valid references (first 20)
if [[ ${#valid_refs} -gt 0 ]]; then
    print -P "${GREEN}✓ Valid References:${NC}"
    local count=0
    for ref in "${valid_refs[@]}"; do
        print "  $ref"
        ((count++))
        [[ $count -ge 20 ]] && break
    done
    [[ ${#valid_refs} -gt 20 ]] && print "  ... and $((${#valid_refs} - 20)) more"
    print ""
fi

# Show examples/suggested
if [[ ${#example_refs} -gt 0 ]]; then
    print -P "${YELLOW}ℹ Example/Suggested Scripts (documented but not implemented):${NC}"
    for ref in "${example_refs[@]}"; do
        print "  $ref"
    done
    print ""
fi

# Show false positives (first 10)
if [[ ${#false_positives} -gt 0 ]]; then
    print -P "${CYAN}~ False Positives Filtered (wildcards, patterns, etc.):${NC}"
    local count=0
    for ref in "${false_positives[@]}"; do
        print "  $ref"
        ((count++))
        [[ $count -ge 10 ]] && break
    done
    [[ ${#false_positives} -gt 10 ]] && print "  ... and $((${#false_positives} - 10)) more"
    print ""
fi

# Show broken references
if [[ ${#broken_refs} -gt 0 ]]; then
    print -P "${RED}✗ Broken References (need fixing):${NC}"
    for ref in "${broken_refs[@]}"; do
        print "  $ref"
    done
    print ""
    print -P "${RED}❌ Found ${#broken_refs} broken script reference(s)${NC}"
    exit 1
else
    print -P "${GREEN}✅ All .zsh script references are valid!${NC}"
    exit 0
fi
