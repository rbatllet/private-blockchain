#!/usr/bin/env zsh

# ============================================================================
# check-java-refs-in-scripts.zsh
# ============================================================================
# Validates Java class references in .zsh scripts
# Checks that classes used in mvn exec:java -Dexec.mainClass="..." exist
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
SRC_MAIN_DIR="$BASE_DIR/src/main/java"
SRC_TEST_DIR="$BASE_DIR/src/test/java"

print -P "${BLUE}☕ Checking Java Class References in .zsh Scripts${NC}"
print "Project root: $BASE_DIR"
print "Main source directory: $SRC_MAIN_DIR"
print "Test source directory: $SRC_TEST_DIR"
print ""

# Index existing Java classes
print -P "${CYAN}📦 Indexing Java classes...${NC}"

typeset -A java_classes

# Index all .java files from both main and test
for java_file in $SRC_MAIN_DIR/**/*.java(N) $SRC_TEST_DIR/**/*.java(N); do
    # Extract package and class name from file
    package=""
    class_name=""
    
    while IFS= read -r line; do
        # Extract package
        if [[ "$line" =~ '^package[[:space:]]+([a-zA-Z0-9_.]+);' ]]; then
            package="${match[1]}"
        fi
        
        # Extract class/interface/enum name - improved regex
        # Match: [public/private/protected] [class/interface/enum] ClassName
        if [[ "$line" =~ '(public|private|protected)[[:space:]]+(class|interface|enum)[[:space:]]+([A-Za-z0-9_]+)' ]] || \
           [[ "$line" =~ '^[[:space:]]*(class|interface|enum)[[:space:]]+([A-Za-z0-9_]+)' ]]; then
            if [[ -n "${match[3]}" ]]; then
                class_name="${match[3]}"
            else
                class_name="${match[2]}"
            fi
            break
        fi
    done < "$java_file"
    
    if [[ -n "$package" && -n "$class_name" ]]; then
        fqn="${package}.${class_name}"
        java_classes[$fqn]="$java_file"
    fi
done

class_count=${#java_classes}
print "Found $class_count Java classes"
print ""

# Arrays for results
typeset -a valid_refs
typeset -a broken_refs
typeset -a demo_refs

total_refs=0
valid_count=0
broken_count=0
demo_count=0

print -P "${YELLOW}📝 Scanning .zsh scripts for Java class references...${NC}"
print ""

# Function to check Java class reference
check_java_class() {
    local class_ref="$1"
    local script_file="$2"
    local line_num="$3"
    
    ((total_refs++))
    
    # Check if class exists
    if [[ -n "${java_classes[$class_ref]}" ]]; then
        ((valid_count++))
        valid_refs+=("$script_file:$line_num -> $class_ref ✓")
        return 0
    else
        # Check if it's a demo/test class (might be documented but not implemented)
        if [[ "$class_ref" =~ (Demo|Example|Test)$ ]]; then
            ((demo_count++))
            demo_refs+=("$script_file:$line_num -> $class_ref [demo/test class]")
            return 1
        else
            ((broken_count++))
            broken_refs+=("$script_file:$line_num -> $class_ref ✗")
            return 1
        fi
    fi
}

# Scan all .zsh scripts in scripts/ and tools/
for script_file in $SCRIPTS_DIR/**/*.zsh(N) $TOOLS_DIR/**/*.zsh(N); do
    rel_path="${script_file#$BASE_DIR/}"
    line_num=0
    
    # Skip this script itself (contains example patterns)
    if [[ "$script_file" == *"check-java-refs-in-scripts.zsh" ]]; then
        continue
    fi
    
    while IFS= read -r line; do
        ((line_num++))
        
        # Skip comment lines (avoid false positives from examples in comments)
        if [[ "$line" =~ '^[[:space:]]*#' ]]; then
            continue
        fi
        
        # Skip echo/print statements (suggestions, not actual executions)
        if [[ "$line" =~ '(echo|print)' ]]; then
            continue
        fi
        
        # Pattern 1: mvn exec:java -Dexec.mainClass="package.ClassName"
        if [[ "$line" =~ 'exec:java.*-Dexec\.mainClass="?([a-zA-Z0-9_.]+)"?' ]]; then
            class_ref="${match[1]}"
            check_java_class "$class_ref" "$rel_path" "$line_num"
        fi
        
        # Pattern 2: mvn test -Dtest=ClassName or -Dtest=package.ClassName
        if [[ "$line" =~ 'mvn test.*-Dtest=([A-Za-z0-9_.]+)' ]]; then
            class_ref="${match[1]}"
            
            # Skip wildcards (e.g., AdvancedSearch*)
            if [[ "$class_ref" =~ '\*' ]]; then
                continue
            fi
            
            # If it's already fully qualified (has dots), check directly
            if [[ "$class_ref" =~ '\.' ]]; then
                check_java_class "$class_ref" "$rel_path" "$line_num"
            else
                # Try to find full qualified name
                class_name="$class_ref"
                found=false
                for fqn in ${(k)java_classes}; do
                    if [[ "$fqn" =~ "\.${class_name}$" ]]; then
                        check_java_class "$fqn" "$rel_path" "$line_num"
                        found=true
                        break
                    fi
                done
                
                if [[ "$found" == "false" ]]; then
                    ((total_refs++))
                    ((broken_count++))
                    broken_refs+=("$rel_path:$line_num -> $class_name (test class not found) ✗")
                fi
            fi
        fi
        
        # Pattern 3: java -cp ... package.ClassName
        if [[ "$line" =~ 'java[[:space:]]+-cp.*[[:space:]]+([a-zA-Z0-9_.]+)' ]]; then
            class_ref="${match[1]}"
            # Only check if it looks like a fully qualified name (has at least one dot)
            if [[ "$class_ref" =~ '\.' ]]; then
                check_java_class "$class_ref" "$rel_path" "$line_num"
            fi
        fi
        
    done < "$script_file"
done

# Print results
print ""
print -P "${CYAN}════════════════════════════════════════${NC}"
print -P "${CYAN}Summary${NC}"
print -P "${CYAN}════════════════════════════════════════${NC}"
print "Total Java class references found: $total_refs"
print -P "${GREEN}✓ Valid references: $valid_count${NC}"
print -P "${YELLOW}ℹ Demo/Test classes: $demo_count${NC}"
print -P "${RED}✗ Broken references: $broken_count${NC}"
print ""

if [[ $valid_count -gt 0 ]]; then
    print -P "${GREEN}✓ Valid References:${NC}"
    for ref in "${valid_refs[@]}"; do
        print "  $ref"
    done | head -20
    if [[ ${#valid_refs} -gt 20 ]]; then
        print "  ... and $((${#valid_refs} - 20)) more"
    fi
    print ""
fi

if [[ $demo_count -gt 0 ]]; then
    print -P "${YELLOW}ℹ Demo/Test Class References:${NC}"
    for ref in "${demo_refs[@]}"; do
        print "  $ref"
    done
    print ""
fi

if [[ $broken_count -gt 0 ]]; then
    print -P "${RED}✗ Broken References (need fixing):${NC}"
    for ref in "${broken_refs[@]}"; do
        print "  $ref"
    done
    print ""
    print -P "${RED}❌ Found $broken_count broken Java class reference(s)${NC}"
    print ""
    exit 1
else
    print -P "${GREEN}✅ All Java class references are valid!${NC}"
    print ""
    exit 0
fi
