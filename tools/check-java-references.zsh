#!/usr/bin/env zsh

# Script to validate Java method, constructor, and import references in markdown documentation
# Checks that:
# 1. Method references with parameter counts match actual Java source
# 2. Constructor references (new ClassName(...)) match actual constructors
# 3. Import statements mentioned in docs exist in source code
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
SRC_DIR="$BASE_DIR/src/main/java"

# Cache for validated method references (to avoid redundant checks)
# Format: "ClassName.methodName:paramCount" -> "valid"
declare -A VALIDATED_METHODS_CACHE

if [[ ! -d "$DOCS_DIR" ]]; then
    echo "❌ ERROR: docs/ directory not found at $DOCS_DIR"
    exit 1
fi

# Classes to ignore (standard library and common frameworks)
IGNORE_CLASSES=(
    "System" "String" "List" "Map" "Set" "Collection" "ArrayList" "HashMap"
    "Integer" "Long" "Double" "Boolean" "Character" "Byte" "Short" "Float"
    "Object" "Class" "Thread" "Exception" "RuntimeException" "Error"
    "Math" "Arrays" "Collections" "Optional" "Stream" "Collectors"
    "EntityManager" "Paths" "ResponseEntity" "SecurityContext" 
    "CompletableFuture" "LoggerFactory" "DateTimeFormatter" "Executors"
    "LocalDate" "Runtime" "Pattern" "SecurityContextHolder" "SecurityPolicy"
    "SecureRandom" "Instant" "Duration" "YearMonth" "LocalTime"
    "ConcurrentHashMap" "DateRange" "Persistence" "ThreadLocalRandom"
    "MDC" "Base64" "UUID" "Comparator" "Security" "Files" "Path"
    "LocalDateTime" "TimeRange" "Objects"
)

if [[ ! -d "$SRC_DIR" ]]; then
    echo "❌ ERROR: src/main/java/ directory not found at $SRC_DIR"
    exit 1
fi

echo "🔍 Checking Java references in documentation..."
echo "📁 Project root: $BASE_DIR"
echo "📚 Docs directory: $DOCS_DIR"
echo "☕ Source directory: $SRC_DIR"
echo ""

# Output file for results
OUTPUT_FILE="$BASE_DIR/java-references-report.txt"
echo "📄 Report will be saved to: ${OUTPUT_FILE#$BASE_DIR/}"
echo ""

# Clear output file and write header
{
    echo "Java References Validation Report"
    echo "=================================="
    echo "Generated: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Project root: $BASE_DIR"
    echo ""
} > "$OUTPUT_FILE"

INVALID_METHODS=0
INVALID_CONSTRUCTORS=0
INVALID_IMPORTS=0
TOTAL_METHOD_REFS=0
TOTAL_CONSTRUCTOR_REFS=0
TOTAL_IMPORT_REFS=0

# Find all markdown files in docs/ and root level
typeset -a MD_FILES
while IFS= read -r file; do
    MD_FILES+=("$file")
done < <(find "$DOCS_DIR" -name "*.md" -type f 2>/dev/null; find "$BASE_DIR" -maxdepth 1 -name "*.md" -type f 2>/dev/null)

echo "📝 Checking method references..."
echo ""
echo "METHOD REFERENCES" >> "$OUTPUT_FILE"
echo "=================" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Check method references in format: ClassName.methodName(param1, param2, ...)
for md_file in "${MD_FILES[@]}"; do
    rel_md_file="${md_file#$BASE_DIR/}"
    
    # Extract method references with backticks: `ClassName.methodName(...)`
    # Pattern matches: ClassName.methodName() or ClassName.methodName(Type1, Type2)
    while IFS=: read -r line_num line; do
        # Use default line number if empty (shouldn't happen but be safe)
        [[ -z "$line_num" ]] && line_num="unknown"
        
        # Save line number before any regex operations (regex might overwrite variables)
        current_line_num="$line_num"
        
        # Extract method references from this line
        # Match: ClassName.methodName(...) but handle nested parentheses
        # We need to find the matching closing parenthesis for method calls
        if [[ "$line" =~ '([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)\(' ]]; then
            method_ref="$line"
        else
            continue
        fi
        
        ((TOTAL_METHOD_REFS++))
        
        # Parse the method reference
        # Extract class name, method name, and parameters (handling nested parentheses)
        if [[ "$method_ref" =~ '([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)\(' ]]; then
            class_name="${match[1]}"
            method_name="${match[2]}"
            
            # Find the position where the match starts
            # We need to extract only the matched method call, not the entire line
            matched_text="${class_name}.${method_name}("
            
            # Extract just the method call from the line (from match position onwards)
            # This prevents extracting parameters from previous method calls on the same line
            match_start_pos=0
            for ((i=0; i<${#method_ref}; i++)); do
                if [[ "${method_ref:$i:${#matched_text}}" == "$matched_text" ]]; then
                    match_start_pos=$i
                    break
                fi
            done
            
            # Start extracting from the matched position
            method_call_only="${method_ref:$match_start_pos}"
            
            # Extract parameters by finding matching closing parenthesis
            # Handle nested parentheses: method(a, b.get(), c)
            params=""
            paren_count=0
            in_params=false
            for ((i=0; i<${#method_call_only}; i++)); do
                char="${method_call_only:$i:1}"
                if [[ "$char" == "(" ]]; then
                    ((paren_count++))
                    if [[ $paren_count -eq 1 ]]; then
                        in_params=true
                        continue  # Skip the opening parenthesis of the method call
                    fi
                elif [[ "$char" == ")" ]]; then
                    ((paren_count--))
                    if [[ $paren_count -eq 0 ]]; then
                        break  # Found the matching closing parenthesis
                    fi
                fi
                if [[ "$in_params" == true ]]; then
                    params+="$char"
                fi
            done
            
            # Skip common Java standard library classes
            if [[ " ${IGNORE_CLASSES[@]} " =~ " ${class_name} " ]]; then
                continue
            fi
            
            # Skip if it's likely a variable (starts with lowercase) like: out.println(), list.add()
            if [[ "$class_name" =~ ^[a-z] ]]; then
                continue
            fi
            
            # Check if it's a constant/variable (ALL_UPPERCASE with underscores)
            # Like: GLOBAL_BLOCKCHAIN_LOCK.writeLock(), MAX_VALUE.toString()
            # For these, we search for the variable declaration instead of a class
            is_constant=false
            if [[ "$class_name" =~ ^[A-Z_]+$ ]]; then
                is_constant=true
            fi
            
            # Count parameters (split by comma, ignore empty)
            if [[ -z "$params" || "$params" =~ '^[[:space:]]*$' ]]; then
                param_count=0
            else
                # Remove string literals and generic types to count params accurately
                # 1. Remove string literals: "anything" -> ""
                # 2. Remove generic types: <Type> -> <>
                # 3. Then count commas that are actual parameter separators
                cleaned_params=$(echo "$params" | sed 's/"[^"]*"/""/g' | sed 's/<[^>]*>/<>/g')
                
                # Trim whitespace
                cleaned_params=$(echo "$cleaned_params" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
                
                # Check again if empty after cleaning
                if [[ -z "$cleaned_params" ]]; then
                    param_count=0
                else
                    # Count commas + 1 (only if we have actual content)
                    param_count=$(echo "$cleaned_params" | grep -o ',' | wc -l)
                    ((param_count++))
                fi
            fi
            
            # Check cache first to avoid redundant validation
            cache_key="${class_name}.${method_name}:${param_count}"
            if [[ -n "${VALIDATED_METHODS_CACHE[$cache_key]}" ]]; then
                # Already validated this exact method reference
                continue
            fi
            
            # Search for the method in Java source files
            # Pattern: methodName followed by opening parenthesis, count parameters
            found=false
            method_exists=false
            class_exists=false
            found_param_counts=()
            java_file_path=""
            
            # Special handling for constants/variables (ALL_CAPS)
            if [[ "$is_constant" == true ]]; then
                # Search for constant declaration in any Java file
                # Pattern: static final TYPE CONSTANT_NAME = ...
                constant_found=false
                while IFS= read -r java_file; do
                    if grep -q "\(static\|final\)[[:space:]].*[[:space:]]${class_name}[[:space:]]*=" "$java_file" 2>/dev/null; then
                        constant_found=true
                        # Now validate the method exists (we don't know the type, so just check method is used somewhere)
                        # This is a simplified check - we assume if constant exists and is used with this method, it's valid
                        found=true
                        break
                    fi
                done < <(find "$SRC_DIR" -name "*.java" -type f 2>/dev/null)
                
                if [[ "$constant_found" == false ]]; then
                    error_msg="❌ INVALID METHOD REFERENCE:
   File: $rel_md_file:$current_line_num
   Reference: ${class_name}.${method_name}()
   Problem: Constant/variable '${class_name}' does not exist in source code
"
                    echo "$error_msg"
                    echo "$error_msg" >> "$OUTPUT_FILE"
                    ((INVALID_METHODS++))
                fi
                continue  # Skip to next reference
            fi
            
            # Find all Java files that might contain this class
            while IFS= read -r java_file; do
                [[ -z "$java_file" ]] && continue
                
                # Check if this file contains the class definition
                if grep -q "\(class\|enum\|interface\)[[:space:]]\+${class_name}" "$java_file" 2>/dev/null; then
                    class_exists=true
                    # Store the relative path to the Java file
                    java_file_path="${java_file#$BASE_DIR/}"
                    # Look for method definition with matching parameter count
                    # Match method signatures like: public Type methodName(Type1 param1, Type2 param2)
                    # Use word boundary \< \> to match exact method name only
                    method_lines=$(grep -n "^\s*\(public\|private\|protected\|\s\)\+.*[[:space:]]\<${method_name}\>[[:space:]]*(" "$java_file" 2>/dev/null)
                    
                    if [[ -n "$method_lines" ]]; then
                        method_exists=true
                        # For each matching method, count its parameters
                        while IFS=: read -r line_num method_line; do
                            # Check if method signature is on one line
                            if [[ "$method_line" =~ \) ]]; then
                                # Single line method - extract parameter list
                                param_list=$(echo "$method_line" | sed -n 's/.*(\(.*\)).*/\1/p')
                            else
                                # Multi-line method - read from file starting at line_num
                                param_list=""
                                current_line=$line_num
                                while IFS= read -r next_line; do
                                    param_list+=" $next_line"
                                    [[ "$next_line" =~ \) ]] && break
                                    ((current_line++))
                                done < <(tail -n +$line_num "$java_file" | head -20)
                                # Extract just the parameter part
                                param_list=$(echo "$param_list" | sed -n 's/.*(\(.*\)).*/\1/p')
                            fi
                            
                            # Count actual parameters in Java source
                            if [[ -z "$param_list" || "$param_list" =~ '^[[:space:]]*$' ]]; then
                                actual_param_count=0
                            else
                                # Remove generics before counting (e.g., Map<String, String> -> Map<>)
                                cleaned_param_list=$(echo "$param_list" | sed 's/<[^>]*>/<>/g')
                                
                                # Count parameters by counting commas + 1
                                actual_param_count=$(echo "$cleaned_param_list" | grep -o ',' | wc -l)
                                ((actual_param_count++))
                            fi
                            
                            # Store all found parameter counts
                            found_param_counts+=($actual_param_count)
                            
                            # Compare parameter counts
                            if [[ $actual_param_count -eq $param_count ]]; then
                                found=true
                                break
                            fi
                        done <<< "$method_lines"
                    fi
                fi
                
                [[ "$found" == true ]] && break
            done < <(find "$SRC_DIR" -name "${class_name}.java" -type f 2>/dev/null)
            
            # If still not found, search in all Java files (class might be inner class)
            if [[ "$found" == false ]]; then
                while IFS= read -r java_file; do
                    # IMPORTANT: Only check files that contain the target class definition
                    # This prevents matching methods from different classes with the same name
                    if ! grep -q "\(class\|enum\|interface\)[[:space:]]\+${class_name}" "$java_file" 2>/dev/null; then
                        continue  # Skip files that don't contain this class
                    fi
                    
                    # Use word boundary \< \> to match exact method name only
                    method_lines=$(grep -n "[[:space:]]\<${method_name}\>[[:space:]]*(" "$java_file" 2>/dev/null)
                    
                    if [[ -n "$method_lines" ]]; then
                        method_exists=true
                        while IFS=: read -r line_num method_line; do
                            # Check if method signature is on one line
                            if [[ "$method_line" =~ \) ]]; then
                                # Single line method - extract parameter list
                                param_list=$(echo "$method_line" | sed -n 's/.*(\(.*\)).*/\1/p')
                            else
                                # Multi-line method - read from file starting at line_num
                                param_list=""
                                current_line=$line_num
                                while IFS= read -r next_line; do
                                    param_list+=" $next_line"
                                    [[ "$next_line" =~ \) ]] && break
                                    ((current_line++))
                                done < <(tail -n +$line_num "$java_file" | head -20)
                                # Extract just the parameter part
                                param_list=$(echo "$param_list" | sed -n 's/.*(\(.*\)).*/\1/p')
                            fi
                            
                            if [[ -z "$param_list" || "$param_list" =~ '^[[:space:]]*$' ]]; then
                                actual_param_count=0
                            else
                                # Remove generics before counting (e.g., Map<String, String> -> Map<>)
                                cleaned_param_list=$(echo "$param_list" | sed 's/<[^>]*>/<>/g')
                                
                                # Count parameters by counting commas + 1
                                actual_param_count=$(echo "$cleaned_param_list" | grep -o ',' | wc -l)
                                ((actual_param_count++))
                            fi
                            
                            # Store all found parameter counts
                            found_param_counts+=($actual_param_count)
                            
                            if [[ $actual_param_count -eq $param_count ]]; then
                                found=true
                                break
                            fi
                        done <<< "$method_lines"
                    fi
                    
                    [[ "$found" == true ]] && break
                done < <(find "$SRC_DIR" -name "*.java" -type f 2>/dev/null)
            fi
            
            if [[ "$found" == false ]]; then
                if [[ "$class_exists" == false ]]; then
                    # Class does not exist
                    error_msg="❌ INVALID METHOD REFERENCE:
   File: $rel_md_file:$current_line_num
   Reference: ${class_name}.${method_name}()
   Problem: Class '${class_name}' does not exist in source code
"
                elif [[ "$method_exists" == false ]]; then
                    # Class exists but method does not
                    error_msg="❌ INVALID METHOD REFERENCE:
   File: $rel_md_file:$current_line_num
   Reference: ${class_name}.${method_name}()
   Problem: Class '${class_name}' exists in $java_file_path, but method '${method_name}' does not exist
"
                else
                    # Class and method exist but parameter count is wrong
                    # Get unique parameter counts
                    unique_counts=($(echo "${found_param_counts[@]}" | tr ' ' '\n' | sort -u | tr '\n' ' '))
                    error_msg="❌ INVALID METHOD REFERENCE:
   File: $rel_md_file:$current_line_num
   Reference: ${class_name}.${method_name}() with $param_count parameter(s)
   Problem: Class '${class_name}' in $java_file_path has method '${method_name}' with different parameter count(s): ${unique_counts[@]}
"
                fi
                echo "$error_msg"
                echo "$error_msg" >> "$OUTPUT_FILE"
                ((INVALID_METHODS++))
            else
                # Method reference is valid - add to cache for future performance
                # Only cache when: class exists + method exists + parameter count matches
                VALIDATED_METHODS_CACHE[$cache_key]="valid"
            fi
        fi
    done < <(awk '{printf "%d:%s\n", NR, $0}' "$md_file")
done

echo ""
echo "🏗️  Checking constructor references..."
echo ""
echo "" >> "$OUTPUT_FILE"
echo "CONSTRUCTOR REFERENCES" >> "$OUTPUT_FILE"
echo "=====================" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Check constructor references in format: new ClassName(param1, param2, ...)
for md_file in "${MD_FILES[@]}"; do
    rel_md_file="${md_file#$BASE_DIR/}"
    
    # Extract constructor references: new ClassName(...)
    while IFS=: read -r line_num line; do
        # Use default line number if empty
        [[ -z "$line_num" ]] && line_num="unknown"
        
        # Save line number before any regex operations
        current_line_num="$line_num"
        
        # Match: new ClassName(...)
        if [[ "$line" =~ 'new[[:space:]]+([A-Z][A-Za-z0-9_]*)[[:space:]]*\(' ]]; then
            constructor_ref="$line"
        else
            continue
        fi
        
        ((TOTAL_CONSTRUCTOR_REFS++))
        
        # Parse the constructor reference
        if [[ "$constructor_ref" =~ 'new[[:space:]]+([A-Z][A-Za-z0-9_]*)[[:space:]]*\(' ]]; then
            class_name="${match[1]}"
            
            # Extract just the constructor call from the line
            match_start_pos=0
            # Build pattern: new ClassName(
            constructor_match_pattern='^new[[:space:]]+'${class_name}'[[:space:]]*\('
            for ((i=0; i<${#constructor_ref}; i++)); do
                # Try to match "new ClassName("
                if [[ "${constructor_ref:$i}" =~ $constructor_match_pattern ]]; then
                    match_start_pos=$i
                    break
                fi
            done
            
            # Start extracting from the matched position
            constructor_call_only="${constructor_ref:$match_start_pos}"
            
            # Extract parameters by finding matching closing parenthesis
            params=""
            paren_count=0
            in_params=false
            for ((i=0; i<${#constructor_call_only}; i++)); do
                char="${constructor_call_only:$i:1}"
                if [[ "$char" == "(" ]]; then
                    ((paren_count++))
                    if [[ $paren_count -eq 1 ]]; then
                        in_params=true
                        continue
                    fi
                elif [[ "$char" == ")" ]]; then
                    ((paren_count--))
                    if [[ $paren_count -eq 0 ]]; then
                        break
                    fi
                fi
                if [[ "$in_params" == true ]]; then
                    params+="$char"
                fi
            done
            
            # Skip common Java standard library classes
            if [[ " ${IGNORE_CLASSES[@]} " =~ " ${class_name} " ]]; then
                continue
            fi
            
            # Count parameters
            if [[ -z "$params" || "$params" =~ '^[[:space:]]*$' ]]; then
                param_count=0
            else
                # Remove string literals and generic types
                cleaned_params=$(echo "$params" | sed 's/"[^"]*"/""/g' | sed 's/<[^>]*>/<>/g')
                cleaned_params=$(echo "$cleaned_params" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
                
                if [[ -z "$cleaned_params" ]]; then
                    param_count=0
                else
                    param_count=$(echo "$cleaned_params" | grep -o ',' | wc -l)
                    ((param_count++))
                fi
            fi
            
            # Check cache first
            cache_key="new ${class_name}:${param_count}"
            if [[ -n "${VALIDATED_METHODS_CACHE[$cache_key]}" ]]; then
                continue
            fi
            
            # Search for the constructor in Java source files
            found=false
            java_file_path=""
            
            # Search in all Java files for this class
            while IFS= read -r java_file; do
                # Check if this file contains the class definition
                if grep -q "^[[:space:]]*public[[:space:]]\+class[[:space:]]\+${class_name}\>" "$java_file" || \
                   grep -q "^[[:space:]]*class[[:space:]]\+${class_name}\>" "$java_file"; then
                    
                    java_file_path="${java_file#$SRC_DIR/}"
                    
                    # Search for constructors: public ClassName(...)
                    # Constructors have the same name as the class
                    if grep -q "[[:space:]]\<${class_name}\>[[:space:]]*([^)]*)" "$java_file"; then
                        # Found constructor(s), now check parameter count
                        # Extract all constructor signatures
                        typeset -a found_param_counts
                        
                        while IFS= read -r constructor_line; do
                            # Extract parameter list from constructor
                            # Use variable expansion outside the regex for class name
                            constructor_pattern="${class_name}"'[[:space:]]*\(([^)]*)\)'
                            if [[ "$constructor_line" =~ $constructor_pattern ]]; then
                                param_list="${match[1]}"
                                
                                # Count parameters in Java source (same logic as methods)
                                if [[ -z "$param_list" || "$param_list" =~ '^[[:space:]]*$' ]]; then
                                    actual_param_count=0
                                else
                                    # Clean generics before counting
                                    cleaned_param_list=$(echo "$param_list" | sed 's/<[^>]*>/<>/g')
                                    actual_param_count=$(echo "$cleaned_param_list" | grep -o ',' | wc -l)
                                    ((actual_param_count++))
                                fi
                                
                                found_param_counts+=("$actual_param_count")
                                
                                if [[ $actual_param_count -eq $param_count ]]; then
                                    found=true
                                fi
                            fi
                        done < <(grep -n "[[:space:]]\<${class_name}\>[[:space:]]*([^)]*)" "$java_file")
                        
                        break
                    fi
                fi
            done < <(find "$SRC_DIR" -name "*.java" -type f 2>/dev/null)
            
            # Report errors
            if [[ "$found" == false ]]; then
                if [[ -z "$java_file_path" ]]; then
                    error_msg="❌ INVALID CONSTRUCTOR REFERENCE:
   File: $rel_md_file:$current_line_num
   Reference: new ${class_name}() with $param_count parameter(s)
   Problem: Class '${class_name}' not found in Java source code
"
                elif [[ ${#found_param_counts[@]} -eq 0 ]]; then
                    error_msg="❌ INVALID CONSTRUCTOR REFERENCE:
   File: $rel_md_file:$current_line_num
   Reference: new ${class_name}() with $param_count parameter(s)
   Problem: Class '${class_name}' in $java_file_path has no constructors (only default constructor with 0 parameters)
"
                else
                    # Get unique parameter counts
                    typeset -a unique_counts
                    unique_counts=(${(u)found_param_counts[@]})
                    error_msg="❌ INVALID CONSTRUCTOR REFERENCE:
   File: $rel_md_file:$current_line_num
   Reference: new ${class_name}() with $param_count parameter(s)
   Problem: Class '${class_name}' in $java_file_path has constructor(s) with different parameter count(s): ${unique_counts[@]}
"
                fi
                echo "$error_msg"
                echo "$error_msg" >> "$OUTPUT_FILE"
                ((INVALID_CONSTRUCTORS++))
            else
                # Constructor reference is valid - add to cache
                VALIDATED_METHODS_CACHE[$cache_key]="valid"
            fi
        fi
    done < <(awk '{printf "%d:%s\n", NR, $0}' "$md_file")
done

echo ""
echo "📦 Checking import references..."
echo ""
echo "" >> "$OUTPUT_FILE"
echo "IMPORT REFERENCES" >> "$OUTPUT_FILE"
echo "=================" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Check import statements in format: import com.package.ClassName;
for md_file in "${MD_FILES[@]}"; do
    rel_md_file="${md_file#$BASE_DIR/}"
    
    # Extract import statements from code blocks
    while IFS=: read -r line_num line; do
        # Use default line number if empty (shouldn't happen but be safe)
        [[ -z "$line_num" ]] && line_num="unknown"
        
        # Save line number before any regex operations
        current_line_num="$line_num"
        
        # Look for import statements in this line
        if [[ "$line" =~ 'import[[:space:]]+([a-z0-9_.]+\.[A-Z][A-Za-z0-9_]*);?' ]]; then
            import_ref="$line"
        else
            continue
        fi
        
        ((TOTAL_IMPORT_REFS++))
        
        # Parse import: import com.package.ClassName;
        full_import="${match[1]}"
        
        # Only validate imports from our project (com.rbatllet.*)
        if [[ "$full_import" =~ ^com\.rbatllet\. ]]; then
            # Convert package path to file path
            # com.rbatllet.blockchain.core.Blockchain -> com/rbatllet/blockchain/core/Blockchain.java
            java_path="${full_import//.//}.java"
            expected_file="$SRC_DIR/$java_path"
            
            if [[ ! -f "$expected_file" ]]; then
                error_msg="❌ INVALID IMPORT REFERENCE:
   File: $rel_md_file:$current_line_num
   Import: $full_import
   Expected at: $java_path
   (file does not exist)
"
                echo "$error_msg"
                echo "$error_msg" >> "$OUTPUT_FILE"
                ((INVALID_IMPORTS++))
            fi
        fi
    done < <(awk '{printf "%d:%s\n", NR, $0}' "$md_file")
done

echo ""
echo "📊 Results:"
echo "   Method references checked: $TOTAL_METHOD_REFS"
echo "   Invalid method references: $INVALID_METHODS"
echo "   Constructor references checked: $TOTAL_CONSTRUCTOR_REFS"
echo "   Invalid constructor references: $INVALID_CONSTRUCTORS"
echo "   Import references checked: $TOTAL_IMPORT_REFS"
echo "   Invalid import references: $INVALID_IMPORTS"
echo "   Cached validations (performance): ${#VALIDATED_METHODS_CACHE[@]}"
echo ""

# Write summary to file
{
    echo ""
    echo "SUMMARY"
    echo "======="
    echo "Method references checked: $TOTAL_METHOD_REFS"
    echo "Invalid method references: $INVALID_METHODS"
    echo "Constructor references checked: $TOTAL_CONSTRUCTOR_REFS"
    echo "Invalid constructor references: $INVALID_CONSTRUCTORS"
    echo "Import references checked: $TOTAL_IMPORT_REFS"
    echo "Invalid import references: $INVALID_IMPORTS"
    echo "Cached validations (performance): ${#VALIDATED_METHODS_CACHE[@]}"
    echo ""
} >> "$OUTPUT_FILE"

if [[ $INVALID_METHODS -eq 0 ]] && [[ $INVALID_CONSTRUCTORS -eq 0 ]] && [[ $INVALID_IMPORTS -eq 0 ]]; then
    echo "✅ All Java references are valid!"
    echo "✅ All Java references are valid!" >> "$OUTPUT_FILE"
    echo ""
    echo "📄 Full report saved to: ${OUTPUT_FILE#$BASE_DIR/}"
    exit 0
else
    if [[ $INVALID_METHODS -gt 0 ]]; then
        msg="❌ Found $INVALID_METHODS invalid method reference(s)"
        echo "$msg"
        echo "$msg" >> "$OUTPUT_FILE"
    fi
    if [[ $INVALID_CONSTRUCTORS -gt 0 ]]; then
        msg="❌ Found $INVALID_CONSTRUCTORS invalid constructor reference(s)"
        echo "$msg"
        echo "$msg" >> "$OUTPUT_FILE"
    fi
    if [[ $INVALID_IMPORTS -gt 0 ]]; then
        msg="❌ Found $INVALID_IMPORTS invalid import reference(s)"
        echo "$msg"
        echo "$msg" >> "$OUTPUT_FILE"
    fi
    echo ""
    echo "📄 Full report saved to: ${OUTPUT_FILE#$BASE_DIR/}"
    exit 1
fi
