# Common Functions Library

This directory contains shared ZSH functions used across all demo and test scripts.

## Usage

To use these functions in your script:

```zsh
#!/usr/bin/env zsh

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Now you can use any of the functions
cleanup_database
check_java
compile_project
```

## Available Functions

### Core Functions

#### `cleanup_database()`
Cleans up all database and temporary files created by the blockchain:
- Removes all `.db`, `.sqlite`, `.sqlite3` files
- Removes blockchain WAL and SHM files
- Removes off-chain data directories
- Removes export test JSON files
- Removes key files (`.key`, `.pem`)
- Removes compiled class files
- Removes log files

#### `check_java()`
Checks if Java is installed and meets minimum version requirements (Java 11+).
Returns 0 on success, 1 on failure.

#### `check_maven()`
Checks if Maven is installed and available in PATH.
Returns 0 on success, 1 on failure.

#### `compile_project()`
Compiles the project using Maven.
Returns 0 on success, 1 on failure.

### Output Functions

#### `print_separator()`
Prints a horizontal separator line for better visual organization.

#### `print_section(title)`
Prints a section header with the given title.

#### `handle_error(error_msg)`
Prints an error message and exits with status 1.

### Log Management

#### `cleanup_logs()`
Removes all log files from the logs directory.

#### `ensure_logs_dir()`
Creates the logs directory if it doesn't exist.

## Environment Variables

Scripts can use these environment variables to control behavior:

- `KEEP_TEST_FILES`: Set to "true" to preserve test files after execution
- `KEEP_LOGS`: Set to "true" to preserve log files after execution

## Example Script

```zsh
#!/usr/bin/env zsh

# Set script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Load common functions
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Check prerequisites
if ! check_java; then
    exit 1
fi

if ! check_maven; then
    exit 1
fi

# Clean and compile
cleanup_database
if ! compile_project; then
    handle_error "Compilation failed"
fi

print_separator

# Your demo code here
echo "Running demo..."

print_separator

# Cleanup (optional)
if [[ "${KEEP_TEST_FILES:-false}" != "true" ]]; then
    cleanup_database
fi
```

## Adding New Functions

When adding new functions to `common_functions.zsh`:

1. Add the function with clear documentation
2. Use consistent error handling
3. Return appropriate exit codes (0 for success, non-zero for failure)
4. Update this README with the function documentation

## Notes

- All functions use ZSH-specific features like `setopt NULL_GLOB`
- Functions are automatically available when sourcing (no export needed)
- The library handles file globs safely to avoid "no matches found" errors